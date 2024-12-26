package tokenizer;

import org.assertj.core.api.AbstractAssert;
import org.example.tokenizer.TokenizerToken;
import org.example.tokenizer.TokenizerTokenType;

class TokenizerTokenAssert extends AbstractAssert<TokenizerTokenAssert, TokenizerToken> {
    TokenizerTokenAssert(TokenizerToken actual) {
        super(actual, TokenizerTokenAssert.class);
    }

    static TokenizerTokenAssert assertThat(TokenizerToken actual) {
        return new TokenizerTokenAssert(actual);
    }

    TokenizerTokenAssert hasStartIndex(int startIndex) {
        isNotNull();
        if (actual.getStartIndex() != startIndex) {
            failWithMessage("Expected start index to be <%s> but was <%s>", startIndex, actual.getStartIndex());
        }
        return this;
    }

    TokenizerTokenAssert hasEndIndex(int endIndex) {
        isNotNull();
        if (actual.getEndIndex() != endIndex) {
            failWithMessage("Expected end index to be <%s> but was <%s>", endIndex, actual.getEndIndex());
        }
        return this;
    }

    TokenizerTokenAssert hasTokenType(TokenizerTokenType tokenType) {
        isNotNull();
        if (actual.getType() != tokenType) {
            failWithMessage("Expected toke type to be <%s> but was <%s>", tokenType, actual.getType());
        }
        return this;
    }
}
