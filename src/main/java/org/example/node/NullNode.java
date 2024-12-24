package org.example.node;

import org.example.parser.ParserToken;

import java.util.List;

public final class NullNode extends Node {

    public NullNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
    }

    @Override
    public Object value() {
        return null;
    }

    @Override
    public NodeType type() {
        return NodeType.NULL;
    }

    @Override
    public String toString() {
        return "null";
    }
}
