package org.example.parser;

import org.example.AbstractToken;

public final class ParserToken extends AbstractToken {
    private ParserTokenType type;

    public ParserToken(int startIndex, int endIndex, ParserTokenType type) {
        super(startIndex, endIndex);
        this.type = type;
    }

    public ParserTokenType getType() {
        return type;
    }
}
