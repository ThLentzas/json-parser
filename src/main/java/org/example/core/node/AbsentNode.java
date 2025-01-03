package org.example.core.node;

import java.util.Collections;

// Singleton
public final class AbsentNode extends Node {
    private static final AbsentNode INSTANCE = new AbsentNode();

    private AbsentNode() {
        super(Collections.emptyList(), new char[0], null);
    }

    @Override
    public Object value() {
        return null;
    }

    @Override
    public NodeType type() {
        return NodeType.ABSENT;
    }

    @Override
    public Node path(String name) {
        return INSTANCE;
    }

    @Override
    public Node path(int index) {
        return INSTANCE;
    }

    public static AbsentNode instance() {
        return INSTANCE;
    }
}
