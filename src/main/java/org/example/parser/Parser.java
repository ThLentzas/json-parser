package org.example.parser;

import org.example.exception.DuplicateObjectNameException;
import org.example.exception.MalformedStructureException;
import org.example.exception.TokenizerException;
import org.example.tokenizer.Tokenizer;
import org.example.tokenizer.TokenizerToken;
import org.example.tokenizer.TokenizerTokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
    Why we don't have a char[] buffer, and we access the elements from the Tokenizer?
        As tokenizer progresses through its buffer it will replace the content in case of an escape sequence and then
        the 2 buffers will not be the same anymore. Doing in the parser's constructor this.buffer = this.tokenizer.getBuffer()
        is simply not enough.
 */
public final class Parser {
    private List<ParserToken> tokens;
    private Tokenizer tokenizer;
    private int position;
    private Deque<Character> stack;
    private boolean isKey;

    public Parser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.tokens = new ArrayList<>();
        this.stack = new ArrayDeque<>();
    }

    public void parse() {
        TokenizerToken token = this.tokenizer.nextToken();
        parseValue(token);
    }

    /*
        When we encounter '[', we begin parsing an array by calling parseArray(). The next step is to determine if the
        array is empty by consuming the following token:
            If the next token is null, it means we’ve reached the end of the input unexpectedly, resulting in an
            unterminated array error.
            If the next token is ']', it indicates an empty array.
            If the token is neither null nor ], we assume it should be the start of a value.
     */
    private void parserArray(TokenizerToken token) {
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.ARRAY_START));
        this.stack.push('[');
        this.tokenizer.advance();
        TokenizerToken nextToken;

        nextToken = this.tokenizer.nextToken();
        if (nextToken == null) {
            throw new MalformedStructureException(buildStructuralErrorMessage("Expected: ']' for Array"));
        }

        if (nextToken.getType().equals(TokenizerTokenType.RIGHT_SQUARE_BRACKET)) {
            assertNoTrailingCharacters(nextToken, ParserTokenType.ARRAY_END);
            return;
        }

        parseValue(nextToken);
        nextToken = this.tokenizer.nextToken();
        while (nextToken != null && nextToken.getType().equals(TokenizerTokenType.COMMA)) {
            this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.VALUE_SEPARATOR));
            this.tokenizer.advance();
            nextToken = this.tokenizer.nextToken();
            parseValue(nextToken);
            nextToken = this.tokenizer.nextToken();
        }

        if (nextToken == null) {
            throw new MalformedStructureException(buildStructuralErrorMessage("Expected: ']' for Array"));
        }

        // We need a way to separate what was the type of token that was not comma and exited the loop
        // Case 1: Some value -> we were expecting comma
        // Case 2: Anything else -> we were expecting ']' to terminate the array
        if (!nextToken.getType().equals(TokenizerTokenType.RIGHT_SQUARE_BRACKET)) {
            if (isValue(nextToken.getType())) {
                throw new MalformedStructureException(buildStructuralErrorMessage(nextToken, "Expected: comma to separate Array values"));
            } else {
                throw new MalformedStructureException(buildStructuralErrorMessage(nextToken, "Expected: ']' for Array"));
            }
        }
        assertNoTrailingCharacters(nextToken, ParserTokenType.ARRAY_END);
    }

    private void parseObject(TokenizerToken token) {
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.OBJECT_START));
        this.stack.push('{');
        this.tokenizer.advance();
        TokenizerToken nextToken;

        nextToken = this.tokenizer.nextToken();
        if (nextToken == null) {
            throw new MalformedStructureException(buildStructuralErrorMessage("Expected: '}' for Object"));
        }

        if (nextToken.getType().equals(TokenizerTokenType.RIGHT_CURLY_BRACKET)) {
            assertNoTrailingCharacters(nextToken, ParserTokenType.OBJECT_END);
            this.tokenizer.advance();
            return;
        }

        Set<String> names = new HashSet<>();
        while (true) {
            // toDo: fix this NPE that happens after the 2nd to last if
            assertStringKey(nextToken, names);
            this.tokenizer.advance();
            nextToken = this.tokenizer.nextToken();
            assertColon(nextToken);
            this.tokenizer.advance();
            nextToken = this.tokenizer.nextToken();
            parseValue(nextToken);

            nextToken = this.tokenizer.nextToken();
            if (nextToken == null) {
                throw new MalformedStructureException(buildStructuralErrorMessage("Expected: '}' for Object"));
            }

            if (!nextToken.getType().equals(TokenizerTokenType.COMMA)
                    && !nextToken.getType().equals(TokenizerTokenType.RIGHT_CURLY_BRACKET)) {
                throw new MalformedStructureException(buildStructuralErrorMessage(nextToken, "Expected: '}' for object"));
            }

            if (nextToken.getType().equals(TokenizerTokenType.COMMA)) {
                addToken(nextToken.getStartIndex(), nextToken.getEndIndex(), ParserTokenType.VALUE_SEPARATOR);
                this.tokenizer.advance();
                nextToken = this.tokenizer.nextToken();
            } else {
                assertNoTrailingCharacters(nextToken, ParserTokenType.OBJECT_END);
                this.tokenizer.advance();
                break;
            }
        }
    }

    private void assertStringKey(TokenizerToken token, Set<String> names) {
        if (!token.getType().equals(TokenizerTokenType.STRING)) {
            throw new MalformedStructureException("Unexpected character: '"
                    + this.tokenizer.getBuffer()[token.getStartIndex()]
                    + "' at position: " + (token.getStartIndex() + 1)
                    + ". Expected: double-quoted value for object name");
        }
        /*
            We remove the mandatory quotation marks for the string name.
            "abcd" We start from the token.getStartIndex() + 1 which is the 1st character after then opening quotation
            mark and include, token.getEndIndex() - token.getStartIndex() - 1 to skip the closing quotation marks
         */
        String name = new String(this.tokenizer.getBuffer(), token.getStartIndex() + 1, token.getEndIndex() - token.getStartIndex() - 1);
        if (names.contains(name)) {
            throw new DuplicateObjectNameException("Duplicate object name: " + name);
        }
        names.add(name);
        isKey = true;
        parseString(token);
    }

    private void assertColon(TokenizerToken token) {
        if (token == null || !token.getType().equals(TokenizerTokenType.COLON)) {
            throw new MalformedStructureException("Unexpected character: '"
                    + this.tokenizer.getBuffer()[this.tokenizer.getInitialPosition()]
                    + "' at position: " + (this.tokenizer.getInitialPosition() + 1)
                    + ". Expected: ':' to separate name-value");
        }
        addToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.NAME_SEPARATOR);
    }

    /*
        We need peek() for the same reason as any other case, to ensure the next token is valid. Let's consider this case
        [6, '']. We parse 6 valid json value, but then tokenizer tries to parse ' which is an unrecognized token and
        it throws. It is not that ' is not considered a JSON value it is that is invalid token. Valid tokens like '}' are
        considered invalid JSON values.
     */
    private void parseValue(TokenizerToken token) {
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
                    throw new MalformedStructureException("Position: " + token.getStartIndex() + ". Unexpected character: '" + String.valueOf(this.tokenizer.getBuffer(), token.getStartIndex(), token.getEndIndex() - token.getStartIndex() + 1) + "' Expected JSON value");
        }
        this.tokenizer.advance();
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
            /*
                We have an edge case to consider for object. When parseString() is called it can be for both a string value
                but also for key name, and we need a way to distinguish the 2.
             */
            type = isKey ? ParserTokenType.OBJECT_PROPERTY_NAME : ParserTokenType.OBJECT_PROPERTY_VALUE_STRING;
            isKey = false;
        }
        addToken(token.getStartIndex(), token.getEndIndex(), type);
    }

    /*
        For trailing characters we have to consider 3 cases:

            1. Trailing characters that would lead to invalid token: []001. When the tokenizer tries to tokenize 001 it
            would throw an exception because leading zeros are not allowed we catch that TokenizerException and throw
            accordingly to reflect the context, that we got an unexpected character
            2. Trailing characters that would lead to valid token: []1. The tokenizer will create a token when peek()
            is called which means the return value of peek() will be a non-null token. In this case, since stack is empty
            we don't have any nesting, we have trailing characters and throw an exception
            3. Insignificant trailing characters(trailing whitespaces): {}\n\t\r When the tokenizer checks if there are
            characters after '}' and finds whitespace characters they are considered insignificant according to rfc and
            should be ignored. peek() returns the last token or null for cases like this or when we go out of bounds of
            the array.
     */

    private void assertNoTrailingCharacters(TokenizerToken token, ParserTokenType type) {
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), type));
        this.stack.pop();

        this.tokenizer.advance();
        if (this.stack.isEmpty()) {
            try {
                token = this.tokenizer.nextToken();
                /*
                    We have more characters after an empty array and no nested objects
                    Let's consider this example: jsonText = "{}}";

                    First we call consume(), stack = '{', this.position = 1. We can have an early return
                    if the object is empty and valid. We peek() in the next token,
                    we encounter '}', we remove from the stack, we have a valid pair, and this.position is increased to 2. Control
                    returns to parseObject() which checks if the token was '}' and if so it consumes it.
                        We have 3 cases where we can have tokens after a closing square bracket.
                            1. {
                                 "outerKey": {
                                   "innerKey": "innerValue"
                                 }
                               } Nested Objects
                            2. [
                                  { "name": "Alice" },
                                  { "name": "Bob" }
                                ] // Array of objects
                            3.  {
                                   "outerKey": {
                                     "innerKey": "innerValue"
                                   },
                                   "anotherKey": "anotherValue"
                                 } Nested object followed by key property of the outer object
                            4. Empty object {}
                         Note that 2, 3 are the same because '}' is followed by comma.

                       We peek() in the next token, we encounter '}' and our stack is empty which means neither a nested
                       object nor a value in an object array. Tokenizer will return a non-null nextToken while our stack
                       is empty, so we throw a MalformedStructureException. This will be the case for any non-null value
                       while our stack is empty. To consider those trailing characters as invalid the stack must be
                       empty because it will mean we have no nested arrays/objects so trailing values after '}' are not
                       allowed
                 */
                if (token != null) {
                    throw new MalformedStructureException("Position: " + token.getStartIndex() + ". Unexpected character: '" + this.tokenizer.getBuffer()[token.getStartIndex()] + "'");
                }
            } catch (TokenizerException te) {
                throw new MalformedStructureException("Position: " + this.tokenizer.getInitialPosition() + ". Unexpected character: '" + this.tokenizer.getBuffer()[this.tokenizer.getInitialPosition()] + "'");
            }
        }
    }

    private String buildLexicalErrorMessage(String message) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Position: ")
                .append(this.tokenizer.getInitialPosition())
                .append(". Unexpected character: '")
                .append(this.tokenizer.getBuffer()[this.tokenizer.getInitialPosition()])
                .append("'. ")
                .append(message);
        return errorMessage.toString();
    }

    private String buildStructuralErrorMessage(String message) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Position: ")
                .append(this.tokenizer.getBuffer().length - 1)
                .append(". Unterminated value. ")
                .append(message);
        return errorMessage.toString();
    }

    private String buildStructuralErrorMessage(TokenizerToken token, String message) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Position: ")
                .append(token.getStartIndex())
                .append(". Unexpected character: '")
                .append(this.tokenizer.getBuffer()[token.getStartIndex()])
                .append("'. ")
                .append(message);
        return errorMessage.toString();
    }

    private boolean isValue(TokenizerTokenType type) {
        return type == TokenizerTokenType.STRING
                || type == TokenizerTokenType.NUMBER
                || type == TokenizerTokenType.BOOLEAN
                || type == TokenizerTokenType.NULL
                || type == TokenizerTokenType.LEFT_CURLY_BRACKET
                || type == TokenizerTokenType.LEFT_SQUARE_BRACKET;
    }

    public List<ParserToken> getTokens() {
        return tokens;
    }
}