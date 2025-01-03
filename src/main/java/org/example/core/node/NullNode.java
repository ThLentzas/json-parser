package org.example.core.node;

import org.example.core.parser.ParserToken;

import java.util.List;

public final class NullNode extends Node {
    private final Object value;

    public NullNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
        this.value = null;
    }

    @Override
    public Object value() {
        return this.value;
    }

    @Override
    public NodeType type() {
        return NodeType.NULL;
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
        return "null";
    }
}
