package org.example.tokenizer;

import org.example.AbstractToken;

public final class TokenizerToken extends AbstractToken {
    private TokenizerTokenType type;

    public TokenizerToken(int startIndex, int endIndex, TokenizerTokenType type) {
        super(startIndex, endIndex);
        this.type = type;
    }

    public TokenizerTokenType getType() {
        return type;
    }
}
