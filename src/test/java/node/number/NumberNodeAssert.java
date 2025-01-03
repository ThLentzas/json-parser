package node.number;

import org.assertj.core.api.AbstractAssert;
import org.example.core.node.NodeType;
import org.example.core.node.NumberNode;
import org.example.core.parser.ParserTokenType;

class NumberNodeAssert extends AbstractAssert<NumberNodeAssert, NumberNode> {

    NumberNodeAssert(NumberNode actual) {
        super(actual, NumberNodeAssert.class);
    }

    // This method is used to initiate the assertion chain.
    static NumberNodeAssert assertThat(NumberNode actual) {
        return new NumberNodeAssert(actual);
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
