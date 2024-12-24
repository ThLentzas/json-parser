package node.nullnode;

import org.assertj.core.api.AbstractAssert;
import org.example.node.NodeType;
import org.example.node.NullNode;
import org.example.parser.ParserTokenType;

class NullNodeAssert extends AbstractAssert<NullNodeAssert, NullNode> {

    NullNodeAssert(NullNode actual) {
        super(actual, NullNodeAssert.class);
    }

    static NullNodeAssert assertThat(NullNode actual) {
        return new NullNodeAssert(actual);
    }

    NullNodeAssert hasValue() {
        isNotNull();
        if (actual.value() != null) {
            failWithMessage("Expected value to be <null> but was <%s>", actual.value());
        }

        return this;
    }

    NullNodeAssert hasType() {
        isNotNull();
        if (!actual.type().equals(NodeType.NULL)) {
            failWithMessage("Expected type to be <%s> but was <%s>", NodeType.NULL, actual.type());
        }

        return this;
    }

    NullNodeAssert hasRootToken() {
        isNotNull();
        if (!actual.rootToken().getType().equals(ParserTokenType.NULL)) {
            failWithMessage("Expected root token to be <%s> but was <%s>", ParserTokenType.NULL, actual.rootToken().getType());
        }

        return this;
    }
}
