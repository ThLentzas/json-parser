package org.example.node;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
        We can't just do: return hasKey(key) ? this.map.get(key).value : null

        We need to distinguish between they key not existing and the key existing with null value. In the above code
        if we return null it is ambiguous
     */
    public Object key(String key) {
        if (!hasKey(key)) {
            throw new NoSuchElementException("Key '" + key + "' not found");
        }
        return this.map.get(key).value();
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

    // We never check if the tokens have the expected structure because if they didn't the parser would have already
    // handled it
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
