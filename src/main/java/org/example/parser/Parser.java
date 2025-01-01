package org.example.parser;

import org.example.exception.DuplicateObjectNameException;
import org.example.exception.LexicalException;
import org.example.exception.MalformedStructureException;
import org.example.exception.MaxNestingLevelExceededException;
import org.example.exception.UnexpectedTokenException;
import org.example.node.ArrayNode;
import org.example.node.BooleanNode;
import org.example.node.Node;
import org.example.node.NullNode;
import org.example.node.NumberNode;
import org.example.node.ObjectNode;
import org.example.node.StringNode;
import org.example.tokenizer.Tokenizer;
import org.example.tokenizer.TokenizerToken;
import org.example.tokenizer.TokenizerTokenType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/*
    Why we don't have a char[] buffer, and we access the elements from the Tokenizer?
        As tokenizer progresses through its buffer it will replace the content in case of an escape sequence and then
        the 2 buffers will not be the same anymore. Doing in the parser's constructor this.buffer = this.tokenizer.getBuffer()
        is simply not enough.
 */
public final class Parser {
    private final List<ParserToken> tokens;
    private final Tokenizer tokenizer;
    private int depth;
    /*
        Every time we encounter '[' or '{' we increase the current depth by 1 and everytime we encounter ']' or '}' we
        decrement by 1. Example: [{}, {}, [[]]] 1 -> 2 -> 1 ... 3 -> 2 -> 1
     */
    private static final int MAX_NESTING_DEPTH = 256;
    /*
        The use of stack helps us to keep track of trailing characters and nesting. e.g. "[[]]" ']' is valid after the 1st ']'
        because there is nesting while the 2nd ']' in "[]]" is an invalid trailing character.
        Look at assertNoTrailingCharacters()
     */
    private final Deque<Character> stack;
    private boolean isKey;

    public Parser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        /*
            Why do we use a LinkedList instead of an ArrayList?

            LinkedLists have the problem with caching as we know, but we only iterate though the list once when we navigate
            Adding tokens to the list is efficient because we never have to resize
            ArrayList may require resizing and copying to a larger array as it grows, which can be inefficient for a
            large number of tokens
         */
        this.tokens = new LinkedList<>();
        this.stack = new ArrayDeque<>();
    }

    public Node parse() {
        // The 1st token can't be null because if it was that would mean we have an empty input array which we already checked in the decoder
        TokenizerToken token = this.tokenizer.nextToken();
        parseValue(token);

        switch (this.tokens.get(0).getType()) {
            case OBJECT_START -> {
                return new ObjectNode(this.tokens, this.tokenizer.getBuffer(), null);
            }
            case ARRAY_START -> {
                return new ArrayNode(this.tokens, this.tokenizer.getBuffer(), null);
            }
            case NUMBER -> {
                return new NumberNode(this.tokens, this.tokenizer.getBuffer(), null);
            }
            case STRING -> {
                return new StringNode(this.tokens, this.tokenizer.getBuffer(), null);
            }
            case BOOLEAN -> {
                return new BooleanNode(this.tokens, this.tokenizer.getBuffer(), null);
            }
            case NULL -> {
                return new NullNode(this.tokens, this.tokenizer.getBuffer(), null);
            }
            // This only happens if the 1st token of the Node is not a valid json value which means there is a problem on our part
            default -> throw new UnexpectedTokenException("Unexpected token type: " + this.tokens.get(0).getType());
        }
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
        if(this.depth > 256) {
            throw new MaxNestingLevelExceededException("Maximum nesting depth exceeded: " + MAX_NESTING_DEPTH);
        }
        this.stack.push('[');
        this.depth++;
        this.tokenizer.advance();
        TokenizerToken nextToken;

        nextToken = this.tokenizer.nextToken();
        if (nextToken == null) {
            throw new MalformedStructureException(buildErrorMessage("Expected: ']' for Array"));
        }

        if (nextToken.getType().equals(TokenizerTokenType.RIGHT_SQUARE_BRACKET)) {
            assertNoTrailingCharacters(nextToken, ParserTokenType.ARRAY_END);
            return;
        }

        parseValue(nextToken);
        nextToken = this.tokenizer.nextToken();
        while (nextToken != null && nextToken.getType().equals(TokenizerTokenType.COMMA)) {
            this.tokens.add(new ParserToken(nextToken.getStartIndex(), nextToken.getEndIndex(), ParserTokenType.VALUE_SEPARATOR));
            this.tokenizer.advance();
            nextToken = this.tokenizer.nextToken();
            // [1,
            if (nextToken == null) {
                throw new MalformedStructureException("Position: " + (this.tokenizer.getBuffer().length - 1) + ". Unexpected end of array. Expected a valid JSON value after comma");
            }
            parseValue(nextToken);
            nextToken = this.tokenizer.nextToken();
        }

        if (nextToken == null) {
            throw new MalformedStructureException(buildErrorMessage("Expected: ']' for Array"));
        }

        if (!nextToken.getType().equals(TokenizerTokenType.RIGHT_SQUARE_BRACKET)) {
            throw new MalformedStructureException(buildErrorMessage(nextToken, "Expected: comma to separate Array values"));
        }
        assertNoTrailingCharacters(nextToken, ParserTokenType.ARRAY_END);
    }

    /*
        This is the structure of a JSON Object according to rfc:
            object = begin-object [ member *( value-separator member ) ] end-object
            member = string name-separator value
        The * symbol means zero or more repetitions of the content that follows, zero or more members separated by comma

        We keep track of the opening curly bracket. We need it for assertNoTrailingCharacters() it is explained in the
        stack property above.

        We check early for an empty object, otherwise we make sure it follows the structure describe by the rfc.
            1. peek -> check for key -> advance
            2. peek -> check for colon -> advance
            3. peek -> parse value -> advance
            4. peek -> if comma it means we should expect more members, the loop continues -> advance token
                       if '}' we need to assert no trailing characters
     */
    private void parseObject(TokenizerToken token) {
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.OBJECT_START));
        this.stack.push('{');
        if(this.depth > 256) {
            throw new MaxNestingLevelExceededException("Maximum nesting depth exceeded: " + MAX_NESTING_DEPTH);
        }
        this.depth++;
        this.tokenizer.advance();
        TokenizerToken nextToken;

        nextToken = this.tokenizer.nextToken();
        if (nextToken == null) {
            throw new MalformedStructureException(buildErrorMessage("Expected: '}' for Object"));
        }

        // Check for empty object early
        if (nextToken.getType().equals(TokenizerTokenType.RIGHT_CURLY_BRACKET)) {
            assertNoTrailingCharacters(nextToken, ParserTokenType.OBJECT_END);
            return;
        }

        Set<String> names = new HashSet<>();
        while (true) {
            assertStringKey(nextToken, names);
            this.tokenizer.advance();
            nextToken = this.tokenizer.nextToken();
            assertColon(nextToken);
            this.tokenizer.advance();
            nextToken = this.tokenizer.nextToken();
            parseValue(nextToken);

            nextToken = this.tokenizer.nextToken();
            if (nextToken == null) {
                throw new MalformedStructureException(buildErrorMessage("Expected: '}' for Object"));
            }

            if (!nextToken.getType().equals(TokenizerTokenType.COMMA) && !nextToken.getType().equals(TokenizerTokenType.RIGHT_CURLY_BRACKET)) {
                throw new MalformedStructureException(buildErrorMessage(nextToken, "Expected: '}' or ',' to separate fields"));
            }

            if (nextToken.getType().equals(TokenizerTokenType.COMMA)) {
                this.tokens.add(new ParserToken(nextToken.getStartIndex(), nextToken.getEndIndex(), ParserTokenType.VALUE_SEPARATOR));
                this.tokenizer.advance();
                nextToken = this.tokenizer.nextToken();
            } else {
                assertNoTrailingCharacters(nextToken, ParserTokenType.OBJECT_END);
                break;
            }
        }
    }

    // The 1st time assertStringKey() is called the token can't be null because we already checked for an empty object
    // In subsequence calls, after we encountered comma the token can be null, so we need this check
    // Case: {"x": true, -> token after comma is null
    private void assertStringKey(TokenizerToken token, Set<String> names) {
        if (token == null) {
            throw new MalformedStructureException("Position: " + (this.tokenizer.getBuffer().length - 1) + ". Unexpected end of object. Expected: double-quoted value for object name");
        }

        if (!token.getType().equals(TokenizerTokenType.STRING)) {
            throw new MalformedStructureException(buildErrorMessage(token, "Expected: double-quoted value for object name"));
        }

        /*
            We remove the mandatory quotation marks for the string name.
            "abcd" We start from the token.getStartIndex() + 1 which is the 1st character after then opening quotation
            mark and include, token.getEndIndex() - token.getStartIndex() - 1 to skip the closing quotation marks
         */
        String name = new String(this.tokenizer.getBuffer(), token.getStartIndex() + 1, token.getEndIndex() - token.getStartIndex() - 1);
        if (names.contains(name)) {
            throw new DuplicateObjectNameException("Duplicate object name: '" + name + "'");
        }
        names.add(name);
        isKey = true;
        parseString(token);
    }

    private void assertColon(TokenizerToken token) {
        if (token == null) {
            throw new MalformedStructureException(buildErrorMessage("Expected: ':' to separate name-value"));
        }

        if (!token.getType().equals(TokenizerTokenType.COLON)) {
            throw new MalformedStructureException(buildErrorMessage(token, "Expected: ':' to separate name-value"));
        }
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.NAME_SEPARATOR));
    }

    private void parseValue(TokenizerToken token) {
        if (token == null) {
            throw new MalformedStructureException(buildErrorMessage("Expected: a valid JSON value"));
        }

        switch (token.getType()) {
            case NUMBER -> parseNumber(token);
            case NULL -> parseNull(token);
            case BOOLEAN -> parseBoolean(token);
            case STRING -> parseString(token);
            case LEFT_CURLY_BRACKET -> parseObject(token);
            case LEFT_SQUARE_BRACKET -> parserArray(token);
            default ->
                    throw new MalformedStructureException("Position: " + token.getStartIndex() + ". Unexpected character: '" + String.valueOf(this.tokenizer.getBuffer(), token.getStartIndex(), token.getEndIndex() - token.getStartIndex() + 1) + "'. Expected a valid JSON value");
        }
        this.tokenizer.advance();
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
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), type));
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
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), type));
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
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), type));
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
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), type));
    }

    /*
        For trailing characters we have to consider 3 cases:

            1. Trailing characters that would lead to invalid token: []001. When the tokenizer tries to tokenize 001 it
            would throw an exception because leading zeros are not allowed we catch that LexicalException and throw
            accordingly to reflect the context, that we got an unexpected character
            2. Trailing characters that would lead to valid token: []1. The tokenizer will create a token when nextToken()
            is called. Not null token and stack is empty we don't have any nesting, we have trailing characters and
            throw an exception
            3. Insignificant trailing characters(trailing whitespaces): {}\n\t\r When the tokenizer checks if there are
            characters after '}' and finds whitespace characters they are considered insignificant according to rfc and
            should be ignored. nextToken() returns the last token or null for cases like this or when we go out of bounds of
            the array.
     */
    private void assertNoTrailingCharacters(TokenizerToken token, ParserTokenType type) {
        this.tokens.add(new ParserToken(token.getStartIndex(), token.getEndIndex(), type));
        this.stack.pop();
        this.depth--;

        if (this.stack.isEmpty()) {
            try {
                /*
                    Why we don't reset the position?

                    When assertNoTrailingCharacters() tokenizer's position index is at '}' or ']' and we need to look ahead if
                    there are any invalid tokens for the given context.

                    Case 1: jsonText = "[[]]" For the 1st ']' we have a trailing character ']' which in the given context is valid
                    Case 2: jsonText = "[]123" The trailing character is invalid for this context
                    Case 3: jsonText = "[]" No trailing characters

                    In order to check for any trailing characters we need to advance the tokenizer's position to peek at the next
                    token.
                        If the token is null -> no trailing characters
                        If the token is not null and the stack is empty -> invalid trailing characters
                        If the token is not null and the stack is not empty -> we either had an object as a property of another
                        object, an array of arrays or an array of objects, we have some form of nesting, we consider that as a
                        valid trailing character.

                    We need to make sure that if the stack is empty the next token we peek is null, otherwise we have trailing
                    characters. Stack being empty means there is no nesting, so we can't have Case 1. If the stack is empty
                    we peek into the next character, and we have to consider 3 cases
                        Case 1: Valid token []123
                        Case 2: Invalid token -> []001(leading zeros are not allowed)
                        Case 3: Token is null

                    Case 1: Tokenizer correctly tokenizes the 123 as a JSON number, empty stack with trailing character invalid
                    Case 2: Tokenizer throws because leading zeros are not allowed, we catch this exception, and we throw our
                    MalformedStructureException to reflect the context and instead of leading zeros are not allowed as a message
                    we inform them that we encounter an unexpected character.
                    Case 3: Null token = no trailing characters

                    Despite advancing the position we don't need to reset because if the token is null it means we have no more
                    tokens in the tokenizer and if it is not null we have invalid trailing characters, so we throw.
                 */
                this.tokenizer.advance();
                token = this.tokenizer.nextToken();
                if (token != null) {
                    throw new MalformedStructureException("Position: " + token.getStartIndex() + ". Unexpected character: '" + this.tokenizer.getBuffer()[token.getStartIndex()] + "'");
                }
            } catch (LexicalException le) {
                throw new MalformedStructureException("Position: " + this.tokenizer.getInitialPosition() + ". Unexpected character: '" + this.tokenizer.getBuffer()[this.tokenizer.getInitialPosition()] + "'");
            }
        }
    }

    private String buildErrorMessage(String message) {
        return new StringBuilder().append("Position: ")
                .append(this.tokenizer.getBuffer().length - 1)
                .append(". Unterminated value. ")
                .append(message).toString();
    }

    private String buildErrorMessage(TokenizerToken token, String message) {
        return new StringBuilder().append("Position: ")
                .append(token.getStartIndex())
                .append(". Unexpected character: '")
                .append(this.tokenizer.getBuffer()[token.getStartIndex()])
                .append("'. ")
                .append(message).toString();
    }

    public List<ParserToken> getTokens() {
        return tokens;
    }
}