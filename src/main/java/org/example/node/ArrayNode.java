package org.example.node;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.example.parser.ParserToken;
import org.example.parser.ParserTokenType;

public final class ArrayNode extends ContainerNode {
    private final Node[] nodes;

    public ArrayNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
        this.nodes = build();
    }

    @Override
    public Object[] value() {
        return Arrays.stream(nodes)
                .map(Node::value)
                .toArray();
    }

    public Node get(int index) {
        if(index >= this.nodes.length) {
            throw new ArrayIndexOutOfBoundsException("index: " + index + ", length: " + this.nodes.length);
        }
        return this.nodes[index];
    }

    @Override
    public NodeType type() {
        return NodeType.ARRAY;
    }

    // We need deepToString() for deeply nested arrays
    @Override
    public String toString() {
        return Arrays.deepToString(value());
    }

    private Node[] build() {
        // Only '[' and ']'
        if (this.tokens.size() == 2) {
            return new Node[]{};
        }

        // We don't know how many node values this array will have, and we want to avoid potential resizes of the ArrayList
        List<Node> list = new LinkedList<>();
        this.tokenIndex++;
        ParserToken nextToken;
        while (hasNext()) {
            list.add(buildValue());
            this.tokenIndex++;
            nextToken = next();
            if (nextToken.getType().equals(ParserTokenType.ARRAY_END)) {
                break;
            }
        }
        return list.toArray(list.toArray(new Node[0]));
    }
}
