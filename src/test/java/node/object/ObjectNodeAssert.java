package node.object;

import org.assertj.core.api.AbstractAssert;
import org.example.node.NodeType;
import org.example.node.ObjectNode;
import org.example.parser.ParserTokenType;

import java.util.List;
import java.util.Map;

class ObjectNodeAssert extends AbstractAssert<ObjectNodeAssert, ObjectNode> {
    ObjectNodeAssert(ObjectNode actual) {
        super(actual, ObjectNodeAssert.class);
    }

    // This method is used to initiate the assertion chain.
    static ObjectNodeAssert assertThat(ObjectNode actual) {
        return new ObjectNodeAssert(actual);
    }

    ObjectNodeAssert hasValue(Map<String, Object> nodes) {
        isNotNull();
        if (!actual.value().equals(nodes)) {
            failWithMessage("Expected value to be <%s> but was <%s>", nodes, actual.value());
        }
        return this;
    }

    ObjectNodeAssert hasKey(String key, Object value) {
        isNotNull();
        if (!actual.key(key).equals(value)) {
            failWithMessage("Expected key value to be <%s> but was <%s>", value, actual.key(key));
        }
        return this;
    }


    ObjectNodeAssert hasValues(List<Object> values) {
        isNotNull();
        if (!actual.values().equals(values)) {
            failWithMessage("Expected map values to be <%s> but was <%s>", values, actual.values());
        }
        return this;
    }

    ObjectNodeAssert hasType() {
        isNotNull();
        if (!actual.type().equals(NodeType.OBJECT)) {
            failWithMessage("Expected type to be <%s> but was <%s>", NodeType.OBJECT, actual.type());
        }
        return this;
    }

    ObjectNodeAssert hasRootToken() {
        isNotNull();
        if (!actual.rootToken().getType().equals(ParserTokenType.OBJECT_START)) {
            failWithMessage("Expected type to be <%s> but was <%s>", NodeType.ARRAY, actual.type());
        }
        return this;
    }
}
