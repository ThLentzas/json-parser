package org.example.core.node;

import org.example.core.exception.UnrecognizedTokenException;
import org.example.core.parser.ParserToken;
import org.example.core.parser.ParserTokenType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public abstract class ContainerNode extends Node {

    protected ContainerNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
    }

    protected Node buildValue() {
        switch (this.tokens.get(this.tokenIndex).getType()) {
            case OBJECT_PROPERTY_VALUE_NUMBER, ARRAY_VALUE_NUMBER, NUMBER -> {
                return new NumberNode(List.of(this.tokens.get(this.tokenIndex)), this.buffer, this);
            }
            case OBJECT_PROPERTY_VALUE_STRING, ARRAY_VALUE_STRING, STRING -> {
                return new StringNode(List.of(this.tokens.get(this.tokenIndex)), this.buffer, this);
            }
            case OBJECT_PROPERTY_VALUE_BOOLEAN, ARRAY_VALUE_BOOLEAN, BOOLEAN -> {
                return new BooleanNode(List.of(this.tokens.get(this.tokenIndex)), this.buffer, this);
            }
            case OBJECT_PROPERTY_VALUE_NULL, ARRAY_VALUE_NULL, NULL -> {
                return new NullNode(List.of(this.tokens.get(this.tokenIndex)), this.buffer, this);
            }
            case OBJECT_START -> {
                int startIndex = this.tokenIndex;
                findMatchingEndIndex(ParserTokenType.OBJECT_START, ParserTokenType.OBJECT_END);
                return new ObjectNode(this.tokens.subList(startIndex, this.tokenIndex + 1), this.buffer, this);
            }
            case ARRAY_START -> {
                int startIndex = this.tokenIndex;
                findMatchingEndIndex(ParserTokenType.ARRAY_START, ParserTokenType.ARRAY_END);
                return new ArrayNode(this.tokens.subList(startIndex, this.tokenIndex + 1), this.buffer, this);
            }
            // This only happens if the current token is not a valid json value which means that we have an internal problem on the logic
            // of the  method that calls buildValue() on a non-value token like '}' or ':' etc
            default ->
                    throw new UnrecognizedTokenException("Unexpected token type: " + this.tokens.get(this.tokenIndex).getType() + "Expected a valid JSON Value");
        }
    }

    protected boolean hasNext() {
        return this.tokenIndex <= this.tokens.size();
    }

    protected ParserToken next() {
        return this.tokens.get(this.tokenIndex++);
    }

    /*
        This method finds the matching ending index for a container node, either an object or an array

        In the case of a nested object, we can not pass the initial list of tokens to that Node. Doing so, the nested
        object will have access to tokens outside its own scope. We would have to set an upper bound and always keep checking
        that we don't cross that boundary. To avoid that, we can search for its matching end index '}' or ']' and create
        a sublist that will include only the tokens that belong to that node and not tokens that belong to the parent.

        Given the JSON structure:

     *  {
     *     "key1": [1, 2, {"nestedKey": "value"}],
     *     "key2": "value2"
        }

        If parsing starts at `[`, this method will find the matching `]`.
     *  If parsing starts at `{`, this method will find the matching `}`.

        This way, we ensure nested objects or arrays don't access tokens beyond their scope

        When parsing an array ('[') or an object ('{'), simply creating a sublist from the remaining tokens would cause
        nested arrays or objects to include tokens that belong to their parent node.  By finding this matching end
        index, we create a sublist of tokens specific to the current node's scope. In the above example we don't want
        the nested object to contain all the remaining tokens that belong to its parent '[' and its grandparen the outer
        object
     */
    private void findMatchingEndIndex(ParserTokenType start, ParserTokenType end) {
        Deque<ParserTokenType> stack = new ArrayDeque<>();
        stack.push(start);

        while (this.tokenIndex < this.tokens.size()) {
            this.tokenIndex++;
            if (this.tokens.get(this.tokenIndex).getType().equals(start)) {
                stack.push(start);
            }
            if (this.tokens.get(this.tokenIndex).getType().equals(end)) {
                stack.pop();
            }
            if (stack.isEmpty()) {
                return;
            }
        }
    }
}
