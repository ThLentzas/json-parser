package org.example.core.node;

import org.example.core.parser.ParserToken;

import java.util.List;

public final class BooleanNode extends Node {
    private final boolean value;

    public BooleanNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
        this.value = this.buffer[this.tokens.get(this.tokenIndex).getStartIndex()] != 'f';
    }

    public int numericValue() {
        return this.value ? 1 : 0;
    }

    @Override
    public Boolean value() {
        return this.value;
    }

    @Override
    public NodeType type() {
        return NodeType.BOOLEAN;
    }

    @Override
    public Node path(String name) {
        return AbsentNode.instance();
    }

    @Override
    public Node path(int index) {
        return AbsentNode.instance();
    }

    @Override
    public String toString() {
        return this.buffer[this.tokens.get(this.tokenIndex).getStartIndex()] == 'f' ? "false" : "true";
    }
}
