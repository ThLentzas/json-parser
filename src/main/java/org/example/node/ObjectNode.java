package org.example.node;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.parser.ParserToken;
import org.example.parser.ParserTokenType;

public final class ObjectNode extends ContainerNode {
    private final Map<String, Node> map;

    public ObjectNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
        this.map = build();
    }

    // Our Map is of type String, Node and for each node we return its value
    @Override
    public Map<String, Object> value() {
        Map<String, Object> fields = new LinkedHashMap<>();

        for (Map.Entry<String, Node> entry : map.entrySet()) {
            fields.put(entry.getKey(), entry.getValue().value());
        }

        return fields;
    }

    /*
        When the key has as value null the return type will be a NullNode. When the key does not exist it will return null
        {"key": null} Calling key("key") will return a NullNode the key exists with null as value.
        {"anotherKey": 123} Calling key("key") will return null.

        We would need a way to distinguish the two, only if we return the value of the node and not the node itself.
        The value of the NullNode is null, and we couldn't distinguish between the key not existing or existing and its
        value is null
     */
    public Node key(String name) {
        return hasKey(name) ? this.map.get(name) : null;
    }

    public Set<String> keys() {
        return this.map.keySet();
    }

    // Return the values for each key. The value of the map is a Node and for each of those we call their value(). We don't
    // return the Node itself
    public Object[] values() {
        return this.map.values()
                .stream()
                .map(Node::value)
                .toArray();
    }

    public boolean hasKey(String name) {
        return this.map.containsKey(name);
    }

    public NodeType type() {
        return NodeType.OBJECT;
    }

    /*
        Safe accessor for child nodes. Instead of throwing an exception when a field does not exist, we return an
        “absent node” that indicates no such field. We allow traversing nested nodes without having to constantly check
        for existence. It is the same logic as field() but it allows to chain for nested object values without throwing
        NPE if field is called in a null value.

        {
          "firstName": "Alice",
          "lastName": "Anderson",
          "address": {
            "street": "123 Maple Street",
            "city": "Wonderland",
            "metadata": {
              "locationCode": "WL-XYZ",
              "verified": true
            }
          },
          "emails": [
            { "type": "home", "email": "alice@home.com" },
            { "type": "work", "email": "alice@company.com" }
          ]
        }

        path("firstName") would return a StringNode whose value is "Alice"
        path("address").path("city") would return a StringNode whose value is "Wonderland"
        path("address").path("metadata").path("locationCode") would return a StringNode whose value is "WL-XYZ"
        path("address").path("zipCode") would return an absent node (rather than throwing an exception)

        Note: This method will have an actual implementation for container nodes. Any other node's implementation will
        return an AbsentNode.
     */
    @Override
    public Node path(String name) {
        Node child = this.map.get(name);
        return child == null ? AbsentNode.instance() : child;
    }

    @Override
    public Node path(int index) {
        return AbsentNode.instance();
    }

    // We never check if the tokens have the expected structure because if they didn't the parser would have already handled it
    private Map<String, Node> build() {
        Map<String, Node> object = new LinkedHashMap<>();
        // Skip opening '{'
        this.tokenIndex++;

        ParserToken nextToken = next();
        if (nextToken.getType().equals(ParserTokenType.OBJECT_END)) {
            return object;
        }

        while (hasNext()) {
            String key = new String(this.buffer, nextToken.getStartIndex() + 1, nextToken.getEndIndex() - nextToken.getStartIndex() - 1);
            // Skip colon
            this.tokenIndex++;
            Node value = buildValue();
            object.put(key, value);
            this.tokenIndex++;
            nextToken = next();
            if (nextToken.getType().equals(ParserTokenType.OBJECT_END)) {
                break;
            }
            // If the previous token was not '}' it must have been a comma, and we skip to move to the next value
            nextToken = next();
        }
        return object;
    }

    private String indent(int indentLevel) {
        // 2 spaces per indent level
        char[] spaces = new char[indentLevel * 2];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        int indent = 1;

        if (this.map.isEmpty()) {
            return "{}";
        }
        int count = 0;
        for (Map.Entry<String, Node> entry : this.map.entrySet()) {
            String key = entry.getKey();
            Node value = entry.getValue();
            sb.append(indent(indent));
            sb.append("\"").append(key).append("\":");

            if (value instanceof ObjectNode) {
                sb.append(" ").append(value.toString().replace("\n", "\n" + indent(indent)));
            } else {
                sb.append(" ").append(value.toString());
            }

            count++;
            if (count < this.map.size()) {
                // Comma if there are more fields
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append(indent(0)).append("}");

        return sb.toString();
    }
}
