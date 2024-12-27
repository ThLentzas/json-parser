package node.object;

import org.assertj.core.api.AbstractAssert;
import org.example.node.Node;
import org.example.node.NodeType;
import org.example.node.ObjectNode;
import org.example.parser.ParserTokenType;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class ObjectNodeAssert extends AbstractAssert<ObjectNodeAssert, ObjectNode> {
    ObjectNodeAssert(ObjectNode actual) {
        super(actual, ObjectNodeAssert.class);
    }

    static ObjectNodeAssert assertThat(ObjectNode actual) {
        return new ObjectNodeAssert(actual);
    }

    ObjectNodeAssert hasKey(String keyName, Object value) {
        isNotNull();
        if (!Objects.equals(actual.key(keyName).value(), value)) {
            failWithMessage("Expected key value to be <%s> but was <%s>", value, value);
        }
        return this;
    }

    // value() as ObjectNode
    ObjectNodeAssert hasValue(Map<String, Object> nodes) {
        isNotNull();
        if (!actual.value().equals(nodes)) {
            failWithMessage("Expected value to be <%s> but was <%s>", nodes, actual.value());
        }
        return this;
    }

    ObjectNodeAssert hasKeys(Set<String> keys) {
        isNotNull();
        if (!actual.keys().equals(keys)) {
            failWithMessage("Expected key value to be <%s> but was <%s>", keys, actual.keys());
        }
        return this;
    }

    ObjectNodeAssert hasValues(Object[] values) {
        isNotNull();
        if (!Arrays.equals(values, actual.values())) {
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
