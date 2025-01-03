package org.example.core.node;

import org.example.core.parser.ParserToken;

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

    /*
        This method is only "implemented" by ObjectNode. Every other method has to return an AbsentNode.
        If the returned Node from path("metadata") in the chain, path("address").path("metadata").path("locationCode")
        does not exist an AbsentNode is returned as value which ensures that subsequent calls will not cause NPE by
        trying to call path() on an null value. This is the reason why every node also implements this method, so that
        if at some point return a non-ObjectNode to be able to call path() without any exception. It is a way for "safe
        navigation" in ObjectNode

        Same logic is applied for path(index)
     */
    public abstract Node path(String name);

    public abstract Node path(int index);

    public ParserToken rootToken() {
        return this.tokens.get(0);
    }

    public Node parent() {
        return this.parent;
    }
}
