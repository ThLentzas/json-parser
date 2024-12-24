package org.example.node;

import org.example.parser.ParserToken;

import java.util.List;

public abstract class Node {
    protected List<ParserToken> tokens;
    protected char[] buffer;
    protected int tokenIndex;
    protected Node parent;

    protected Node(List<ParserToken> tokens, char[] buffer, Node parent) {
        this.tokens = tokens;
        this.buffer = buffer;
        this.parent = parent;
    }

    public abstract Object value();

    public abstract NodeType type();

    public ParserToken rootToken() {
        return this.tokens.get(0);
    }

    public Node parent() {
        return this.parent;
    }
}
