package org.example.parser;

import org.example.tokenizer.Tokenizer;
import org.example.tokenizer.TokenizerToken;

import java.util.ArrayList;
import java.util.List;

public final class Parser {
    private List<ParserToken> tokens;
    private Tokenizer tokenizer;
    private int position;

    public Parser(Tokenizer tokenizer) {
        tokens = new ArrayList<>();
        this.tokenizer = tokenizer;
    }

    public void parse() {
        TokenizerToken token = this.tokenizer.nextToken();
        if(token == null) {
            return;
        }

        switch (token.getType()) {
            case NUMBER -> parseNumber(token);
            case NULL -> parseNull(token);
            case BOOLEAN -> parseBoolean(token);
        }
    }

    private void parseBoolean(TokenizerToken token) {
        addParserToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.BOOLEAN);
    }

    private void parseNull(TokenizerToken token) {
        addParserToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.NULL);
    }

    private void parseNumber(TokenizerToken token) {
        addParserToken(token.getStartIndex(), token.getEndIndex(), ParserTokenType.NUMBER);
    }

    private void addParserToken(int startIndex, int endIndex, ParserTokenType type) {
        this.tokens.add(new ParserToken(startIndex, endIndex, type));
    }

    public List<ParserToken> getTokens() {
        return tokens;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }
}
