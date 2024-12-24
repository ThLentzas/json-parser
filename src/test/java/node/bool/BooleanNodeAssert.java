package node.bool;

import org.assertj.core.api.AbstractAssert;
import org.example.node.BooleanNode;
import org.example.node.NodeType;
import org.example.parser.ParserTokenType;

class BooleanNodeAssert extends AbstractAssert<BooleanNodeAssert, BooleanNode> {

    BooleanNodeAssert(BooleanNode actual) {
        super(actual, BooleanNodeAssert.class);
    }

    static BooleanNodeAssert assertThat(BooleanNode actual) {
        return new BooleanNodeAssert(actual);
    }

    BooleanNodeAssert hasValue(boolean value) {
        isNotNull();
        if(!actual.value().equals(value)) {
            failWithMessage("Expected value to be <%s> but was <%s>", value, actual.value());
        }

        return this;
    }

    BooleanNodeAssert hasType() {
        isNotNull();
        if(!actual.type().equals(NodeType.BOOLEAN)) {
            failWithMessage("Expected type to be <%s> but was <%s>", NodeType.BOOLEAN, actual.value());
        }

        return this;
    }

    BooleanNodeAssert hasRootToken() {
        isNotNull();
        if(!actual.rootToken().getType().equals(ParserTokenType.BOOLEAN)) {
            failWithMessage("Expected root token to be <%s> but was <%s>", ParserTokenType.BOOLEAN, actual.value());
        }

        return this;
    }

    BooleanNodeAssert hasNumericValue(int value) {
        isNotNull();
        if(actual.numericValue() != value) {
            failWithMessage("Expected value to be <%s> but was <%s>", value, actual.numericValue());
        }

        return this;
    }
}
