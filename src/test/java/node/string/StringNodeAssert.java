package node.string;

import org.assertj.core.api.AbstractAssert;
import org.example.core.node.NodeType;
import org.example.core.node.StringNode;
import org.example.core.parser.ParserTokenType;

import java.math.BigDecimal;
import java.util.List;

class StringNodeAssert extends AbstractAssert<StringNodeAssert, StringNode> {

    StringNodeAssert(StringNode actual) {
        super(actual, StringNodeAssert.class);
    }

    static StringNodeAssert assertThat(StringNode actual) {
        return new StringNodeAssert(actual);
    }

    StringNodeAssert hasValue(String value) {
        isNotNull();
        if(!actual.value().equals(value)) {
            failWithMessage("Expected value to be <%s> but was <%s>", value, actual.value());
        }

        return this;
    }

    StringNodeAssert hasType() {
        isNotNull();
        if(!actual.type().equals(NodeType.STRING)) {
            failWithMessage("Expected type to be <%s> but was <%s>", NodeType.STRING, actual.value());
        }

        return this;
    }

    StringNodeAssert hasRootToken() {
        isNotNull();
        if(!actual.rootToken().getType().equals(ParserTokenType.STRING)) {
            failWithMessage("Expected root token to be <%s> but was <%s>", ParserTokenType.STRING, actual.value());
        }

        return this;
    }

    StringNodeAssert hasSubsequence(List<Integer> indices, String subsequence) {
        isNotNull();
        String actualSubsequence = actual.subsequence(indices);
        if(!actualSubsequence.equals(subsequence)) {
            failWithMessage("Expected subsequence to be <%s> but was <%s>", subsequence, actualSubsequence);
        }
        return this;
    }

    StringNodeAssert hasIntValue(int value) {
        isNotNull();

        int actualValue = actual.intValue();
        if(actualValue != value) {
            failWithMessage("Expected value to be <%s> but was <%s>", value, actualValue);
        }
        return this;
    }

    StringNodeAssert hasDoubleValue(double value) {
        isNotNull();

        BigDecimal actualValue = actual.doubleValue();
        if(actualValue.compareTo(new BigDecimal(String.valueOf(value))) != 0) {
            failWithMessage("Expected value to be <%s> but was <%s>", value, actualValue);
        }
        return this;
    }

    StringNodeAssert hasLongValue(double value) {
        isNotNull();

        long actualValue = actual.longValue();
        if(actualValue != value) {
            failWithMessage("Expected value to be <%s> but was <%s>", value, actualValue);
        }
        return this;
    }
}
