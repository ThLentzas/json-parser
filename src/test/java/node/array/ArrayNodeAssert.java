package node.array;

import org.assertj.core.api.AbstractAssert;
import org.example.node.ArrayNode;
import org.example.node.NodeType;
import org.example.parser.ParserTokenType;

import java.util.Arrays;

class ArrayNodeAssert extends AbstractAssert<ArrayNodeAssert, ArrayNode> {

    ArrayNodeAssert(ArrayNode actual) {
        super(actual, ArrayNodeAssert.class);
    }

    // This method is used to initiate the assertion chain.
    static ArrayNodeAssert assertThat(ArrayNode actual) {
        return new ArrayNodeAssert(actual);
    }

    ArrayNodeAssert hasValue(Object[] nodes) {
        isNotNull();
        if (!Arrays.equals(actual.value(), nodes)) {
            failWithMessage("Expected array values to be <%s> but was <%s>", Arrays.toString(nodes), Arrays.toString(actual.value()));
        }
        return this;
    }

    ArrayNodeAssert hasType() {
        isNotNull();
        if (!actual.type().equals(NodeType.ARRAY)) {
            failWithMessage("Expected type to be <%s> but was <%s>", NodeType.ARRAY, actual.type());
        }
        return this;
    }

    ArrayNodeAssert hasRootToken() {
        isNotNull();
        if (!actual.rootToken().getType().equals(ParserTokenType.ARRAY_START)) {
            failWithMessage("Expected type to be <%s> but was <%s>", NodeType.ARRAY, actual.type());
        }
        return this;
    }
}
