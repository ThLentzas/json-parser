package org.example;

public abstract class AbstractToken {
    protected int startIndex;
    protected int endIndex;

    protected AbstractToken(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }
}
