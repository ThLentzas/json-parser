package node.number;

import org.assertj.core.api.AbstractAssert;
import org.example.node.NodeType;
import org.example.node.NumberNode;
import org.example.parser.ParserTokenType;

import java.math.BigDecimal;

class NumberNodeAssert extends AbstractAssert<NumberNodeAssert, NumberNode> {

    NumberNodeAssert(NumberNode actual) {
        super(actual, NumberNodeAssert.class);
    }

    // This method is used to initiate the assertion chain.
    static NumberNodeAssert assertThat(NumberNode actual) {
        return new NumberNodeAssert(actual);
    }

    NumberNodeAssert hasValue(BigDecimal value) {
        isNotNull();
        BigDecimal actualValue = actual.value();
        if (actualValue.compareTo(value) != 0) {
            failWithMessage("Expected value to be <%s> but was <%s>", value, actualValue);
        }
        return this;
    }

    NumberNodeAssert hasType() {
        isNotNull();
        if (!actual.type().equals(NodeType.NUMBER)) {
            failWithMessage("Expected type to be <%s> but was <%s>", NodeType.NUMBER, actual.type());
        }
        return this;
    }

    NumberNodeAssert hasRootToken() {
        isNotNull();
        if (!actual.rootToken().getType().equals(ParserTokenType.NUMBER)) {
            failWithMessage("Expected root token to be <%s> but was <%s>", ParserTokenType.NUMBER, actual.type());
        }
        return this;
    }
}
