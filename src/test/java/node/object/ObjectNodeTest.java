package node.object;

import org.example.core.node.Node;
import org.example.core.node.NodeType;
import org.example.core.node.ObjectNode;
import org.example.core.parser.Parser;
import org.example.core.tokenizer.Tokenizer;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectNodeTest {

    @Test
    void shouldHaveKey() {
        String jsonText = """
              {
                "ID": "SGML",
                "SortAs": "SGML",
                "GlossTerm": "Standard Generalized Markup Language",
                "Acronym": "SGML"
              }
           """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        ObjectNode node = (ObjectNode) parser.parse();

        ObjectNodeAssert.assertThat(node)
                .hasKey("SortAs", "SGML");
    }

    // Returns a Map that represents the value of the node
    @Test
    void shouldHaveValue() {
        String jsonText = """
              {
                "ID": "SGML",
                "SortAs": "SGML",
                "GlossTerm": "Standard Generalized Markup Language",
                "Acronym": "SGML"
              }
           """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        ObjectNode node = (ObjectNode) parser.parse();

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("ID", "SGML");
        fields.put("SortAs", "SGML");
        fields.put("GlossTerm", "Standard Generalized Markup Language");
        fields.put("Acronym", "SGML");

        ObjectNodeAssert.assertThat(node)
                .hasType()
                .hasRootToken()
                .hasValue(fields);
    }

    @Test
    void shouldHaveKeys() {
        String jsonText = """
              {
                "ID": "SGML",
                "SortAs": "SGML",
                "GlossTerm": "Standard Generalized Markup Language",
                "Acronym": "SGML"
              }
           """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        ObjectNode node = (ObjectNode) parser.parse();

        ObjectNodeAssert.assertThat(node)
                .hasKeys(Set.of("ID", "SortAs", "GlossTerm", "Acronym"));
    }

    // Returns the map values
    @Test
    void shouldHaveValues() {
        String jsonText = """
              {
                "ID": "SGML",
                "SortAs": "SGML",
                "GlossTerm": "Standard Generalized Markup Language",
                "Acronym": "SGML"
              }
           """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        ObjectNode node = (ObjectNode) parser.parse();
        Object[] values = {"SGML", "SGML", "Standard Generalized Markup Language", "SGML"};

        ObjectNodeAssert.assertThat(node)
                .hasValues(values);
    }

    @Test
    void shouldReturnNodeFromPath() {
        String jsonText = """
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
           """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        ObjectNode node = (ObjectNode) parser.parse();
        Node child = node.path("address")
                .path("metadata")
                .path("locationCode");

        assertThat(child.value()).isEqualTo("WL-XYZ");
    }

    @Test
    void shouldReturnAbsentNodeFromPath() {
        String jsonText = """
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
           """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        ObjectNode node = (ObjectNode) parser.parse();
        Node child = node.path("address")
                .path("zipCode");

        assertThat(child.type()).isEqualTo(NodeType.ABSENT);
    }
}
