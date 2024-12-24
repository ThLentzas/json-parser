package org.example.node;

import org.example.parser.ParserToken;

import java.util.List;

public final class BooleanNode extends Node {

    public BooleanNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
    }

    public int numericValue() {
        return this.buffer[this.tokens.get(this.tokenIndex).getStartIndex()] == 'f' ? 0 : 1;
    }

    @Override
    public Boolean value() {
        return this.buffer[this.tokens.get(this.tokenIndex).getStartIndex()] != 'f';
    }

    @Override
    public NodeType type() {
        return NodeType.BOOLEAN;
    }

    @Override
    public String toString() {
        return this.buffer[this.tokens.get(this.tokenIndex).getStartIndex()] == 'f' ? "false" : "true";
    }
}
