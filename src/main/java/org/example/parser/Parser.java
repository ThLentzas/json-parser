package org.example.parser;

import org.example.exception.*;
import org.example.tokenizer.Tokenizer;
import org.example.tokenizer.TokenizerToken;
import org.example.tokenizer.TokenizerTokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// toDo: explain why we can't have a char[] buffer on Parser. Shortly as tokenizer progress through its buffer it
//  will replace the content in case of an escape sequence and then the 2 buffers will not be the same anymore
public final class Parser {
    private List<ParserToken> tokens;
    private Tokenizer tokenizer;
    private int position;
    private Deque<Character> stack;

    public Parser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.tokens = new ArrayList<>();
        this.stack = new ArrayDeque<>();
    }

    public void parse() {
        TokenizerToken token = this.tokenizer.consume();
        if (token == null) {
            return;
        }
        switch (token.getType()) {
            case NUMBER -> parseNumber(token);
            case NULL -> parseNull(token);
            case BOOLEAN -> parseBoolean(token);
            case STRING -> parseString(token);
            case LEFT_CURLY_BRACKET -> parseObject(token);
            case LEFT_SQUARE_BRACKET -> parserArray(token);
            case RIGHT_CURLY_BRACKET, RIGHT_SQUARE_BRACKET, COLON, COMMA ->
                    throw new UnexpectedCharacterException("Unexpected character: " + token.getType() + " for root");
        }
    }

    /*
        Why do we need peek()?

        When we encounter '[', we begin parsing an array by calling parseArray(). The next step is to determine if the
        array is empty by consuming the following token:
            If the next token is null, it means we’ve reached the end of the input unexpectedly, resulting in an
            unterminated array error.
            If the next token is ']', it indicates an empty array.
            If the token is neither null nor ], we assume it should be the start of a value. However, parseValue() will
            call nextToken() internally to identify that value. Because we’ve already consumed one token to check for
            emptiness, parseValue() effectively “skips” the current token and looks ahead. If it then encounters a ','
            (potentially more values) or ']' instead of a valid value token ({, [, number, string, boolean, or null),
            it will throw an exception since those characters are not valid as standalone values in JSON.

        What does peek() do? We look ahead at the next token without actually advancing the tokenizer's reading position.
        By peeking, we gain information about what the next token will be if we were to consume it. This means that
        we look at the next token without advancing this.position, the index the tokenizer uses to keep track of the
        current token. This way we peek(), and then we check if we have null or ']', parseValue() gets called
        which will return the token we just peeked . After the value is parsed we expect a comma as long as no exception
        was previously throw. Notice that we call consume() and not parseValue() because comma is not a JSON value.
     */
    private void parserArray(TokenizerToken token) {
        addToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.ARRAY_START);
        this.stack.push('[');
        TokenizerToken nextToken = this.tokenizer.peek();
        if (nextToken == null) {
            throw new MalformedStructureException("Expected: ']' for JSON Array");
        }

        // Can't be null, because next token will ALWAYS have the same value that was assigned with peek() when consumed
        // is called but the IDE has no way of knowing that, so it complains. If next token was null the previous would
        // have handled it, at this point it can't be null
        // Empty array.
        if (nextToken.getType().equals(TokenizerTokenType.RIGHT_SQUARE_BRACKET)) {
            nextToken = this.tokenizer.consume();
            addToken(nextToken.getStartIndex(), nextToken.getEndIndex(), ParserTokenType.ARRAY_END);
            this.stack.pop();

            try {
                nextToken = this.tokenizer.peek();
                // No more nested objects/arrays and no more tones = valid empty array
                if (stack.isEmpty() && nextToken == null) {
                    return;
                }

                /*
                    We have more characters after an empty array and no nested objects
                    Let's consider this example: jsonText = "[]]";

                    First we call consume(), stack = {'['}, this.position = 1. Now in parseArray() we can have an early return
                    if the array is empty and valid. We peek() in the next token, why we do that is explained in parseArray(),
                    we encounter ']', we remove from the stack, we have a valid pair, and this.position is increased to 2. Control
                    returns to parseArray() which checks if the token was ']' and if so it consumes it.
                        We have 3 cases where we can have tokens after a closing square bracket.
                            1. Nested arrays [ [1, 2], 3, 4 ]
                            2. {
                                    "numbers": [1, 2],
                                    "status": "ok"
                               } Value within an object
                            3. Empty array []
                       We peek() in the next token, we encounter ']' and our stack is empty which means not a nested or a
                       value in an object. Tokenizer will return a non-null nextToken while our stack is empty, so we throw
                       a MalformedStructureException. This will be the case for any non-null value while our
                       stack is empty.
                 */
                if (this.stack.isEmpty()) {
                    throw new MalformedStructureException("Unexpected character after: ']'");
                }
            } catch (TokenizerException te) {
                throw new MalformedStructureException("Unexpected character after: ']'");
            }
        }

        parseValue();
        nextToken = this.tokenizer.consume();
        while (nextToken != null && nextToken.getType().equals(TokenizerTokenType.COMMA)) {
            addToken(nextToken.getStartIndex(), nextToken.getEndIndex(), ParserTokenType.VALUE_SEPARATOR);
            parseValue();
            nextToken = this.tokenizer.consume();
        }

        if (nextToken == null || !nextToken.getType().equals(TokenizerTokenType.RIGHT_SQUARE_BRACKET)) {
            throw new UnterminatedValueException("Unterminated value. Expected: ']' for JSON array");
        }
        addToken(nextToken.getStartIndex(), nextToken.getEndIndex(), ParserTokenType.ARRAY_END);
        this.stack.pop();
    }

    private void parseObject(TokenizerToken token) {
        addToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.OBJECT_START);
        this.stack.push('{');
        // check for empty object
        TokenizerToken nextToken = this.tokenizer.consume();
        if (nextToken == null) {
            throw new UnterminatedValueException("Unterminated value. Expected: " + ParserTokenType.OBJECT_END + " for JSON Object");
        }

        Set<String> names = new HashSet<>();
        while (true) {
            if (nextToken == null || !nextToken.getType().equals(TokenizerTokenType.STRING)) {
                throw new UnexpectedCharacterException("Unexpected character. Expected: double-quoted value for object name");
            }
            // Consider removing quotation marks, - 2
            String name = new String(this.tokenizer.getBuffer(), nextToken.getStartIndex(), nextToken.getEndIndex() - nextToken.getStartIndex() + 1);
            if (names.contains(name)) {
                throw new DuplicateObjectNameException("Duplicate object name: " + name);
            }
            names.add(name);
            parseString(nextToken);

           /*
                Why do we need to call peek()?

                Let's consider this case:  {"key" % "value"}
                The next token we expect is ':' to separate the key we just parsed from the value, but what happens if we
                get an invalid token when we call consume()? In the above case, when the tokenizer tries to tokenize
                '%' it will throw an UnrecognizedCharacterException because none of the valid JSON values starts with %.
                This exception though does not reflect the current context. It should be something like 'expected ':'
                to separate name-value'. This is what we handle by catching the tokenizer exception in this context.
                Another example is {"key" 0001 "value"}. If we follow the logic we just described we would have an error
                like "Leading zeros are not allowed" which also does not reflect the current context.
                If the token is valid we consume it, and we check if is the expected one. We have a similar case bellow.
             */
            try {
                this.tokenizer.peek();
            } catch (TokenizerException te) {
                // We use this.tokenizer.getCurrentElement() because the token we peeked raised an exception which means
                // that it was not added as token to the tokenizer, and we don't have access to it directly. We know
                // though that whatever caused this exception is the current character at tokenizer's buffer.
                throw new UnexpectedCharacterException("Unexpected character: " + this.tokenizer.getCurrentElement() + " Expected: ':' to separate name-value");
            }
            nextToken = this.tokenizer.consume();
            if (nextToken == null || !nextToken.getType().equals(TokenizerTokenType.COLON)) {
                throw new UnexpectedCharacterException("Unexpected character: " + this.tokenizer.getCurrentElement() + " Expected: ':' to separate name-value");
            }
            addToken(nextToken.getStartIndex(), nextToken.getEndIndex(), ParserTokenType.NAME_SEPARATOR);

            /*
                Why do we need to call peek()?

                Let's consider this case:  "key" : 1 + 2
                When we call parseValue() we parse 1 and then call consume() what happens if an exception is thrown?
                If no exception is thrown the if() will check if the token has the expected value which is either ','
                to separate object properties or '}'. But what if when we try to tokenize the next token we get an exception?

                "key" : 1 "abc -> this would lead to unterminated value exception when we tried to tokenize() "abc. We
                need to handle these exceptions with the correct error handling. In those cases, we expect a ',' or '}'
                which means that if we don't get either we need to handle the exception. It will not help on the error
                if we say "Unterminated value" in this case. For the case of "key" : 1 + 2, encountering the '+' will
                throw an UnexpectedCharacterException because JSON spec does not allow numbers to start with '+'

                If peek() does not raise an exception we can consume the token(advance the position pointer of tokenizer)
                and check if it has the expected value in the following if().
             */
            parseValue();
            try {
                this.tokenizer.peek();
            } catch (TokenizerException te) {
                // We use this.tokenizer.getCurrentElement() because the token we peeked raised an exception which means
                // that it was not added as token to the tokenizer, and we don't have access to it directly. We know
                // though that whatever caused this exception is the current character at tokenizer's buffer.
                throw new UnexpectedCharacterException("Unexpected character: " + this.tokenizer.getCurrentElement() + " Expected: ',' to separate object keys or '}' for JSON object");
            }
            nextToken = this.tokenizer.consume();
            if (nextToken == null
                    || (!nextToken.getType().equals(TokenizerTokenType.COMMA)
                    && !nextToken.getType().equals(TokenizerTokenType.RIGHT_CURLY_BRACKET))) {
                throw new UnexpectedCharacterException((nextToken == null ? "Unterminated value." : "Unexpected character:" + String.valueOf(this.tokenizer.getBuffer(), nextToken.getStartIndex(), nextToken.getEndIndex() - nextToken.getStartIndex() + 1)) + " Expected: " + ParserTokenType.NAME_SEPARATOR + " to separate object keys or " + ParserTokenType.OBJECT_END + " for JSON object");
            } else if (nextToken.getType().equals(TokenizerTokenType.COMMA)) {
                addToken(nextToken.getStartIndex(), nextToken.getEndIndex(), ParserTokenType.VALUE_SEPARATOR);
                nextToken = this.tokenizer.consume();
            } else {
                addToken(nextToken.getStartIndex(), nextToken.getEndIndex(), ParserTokenType.OBJECT_END);
                this.stack.pop();
                break;
            }
        }
    }

    // remember this has to be a valid json value
    private void parseValue() {
        TokenizerToken token = this.tokenizer.consume();
        if (token == null) {
            return;
        }

        switch (token.getType()) {
            case NUMBER -> parseNumber(token);
            case NULL -> parseNull(token);
            case BOOLEAN -> parseBoolean(token);
            case STRING -> parseString(token);
            case LEFT_CURLY_BRACKET -> parseObject(token);
            case LEFT_SQUARE_BRACKET -> parserArray(token);
            default ->
                    throw new UnexpectedCharacterException("Unexpected character: '" + String.valueOf(this.tokenizer.getBuffer(), token.getStartIndex(), token.getEndIndex() - token.getStartIndex() + 1) + "' Expected JSON value");
        }
    }

    private void addToken(int startIndex, int endIndex, ParserTokenType type) {
        this.tokens.add(new ParserToken(startIndex, endIndex, type));
    }

    private void parseBoolean(TokenizerToken token) {
        ParserTokenType type;

        if (this.stack.isEmpty()) {
            type = ParserTokenType.BOOLEAN;
        } else if (this.stack.peek() == '[') {
            type = ParserTokenType.ARRAY_VALUE_BOOLEAN;
        } else {
            type = ParserTokenType.OBJECT_PROPERTY_VALUE_BOOLEAN;
        }
        addToken(token.getStartIndex(), token.getEndIndex(), type);
    }

    private void parseNull(TokenizerToken token) {
        ParserTokenType type;

        if (this.stack.isEmpty()) {
            type = ParserTokenType.NULL;
        } else if (this.stack.peek() == '[') {
            type = ParserTokenType.ARRAY_VALUE_NULL;
        } else {
            type = ParserTokenType.OBJECT_PROPERTY_VALUE_NULL;
        }
        addToken(token.getStartIndex(), token.getEndIndex(), type);
    }

    private void parseNumber(TokenizerToken token) {
        ParserTokenType type;
        if (this.stack.isEmpty()) {
            type = ParserTokenType.NUMBER;
        } else if (this.stack.peek() == '[') {
            type = ParserTokenType.ARRAY_VALUE_NUMBER;
        } else {
            type = ParserTokenType.OBJECT_PROPERTY_VALUE_NUMBER;
        }
        addToken(token.getStartIndex(), token.getEndIndex(), type);
    }

    private void parseString(TokenizerToken token) {
        ParserTokenType type;

        if (this.stack.isEmpty()) {
            type = ParserTokenType.STRING;
        } else if (this.stack.peek() == '[') {
            type = ParserTokenType.ARRAY_VALUE_STRING;
        } else {
            type = ParserTokenType.OBJECT_PROPERTY_VALUE_STRING;
        }
        addToken(token.getStartIndex(), token.getEndIndex(), type);
    }
}