package node.object;

import org.example.node.ObjectNode;
import org.example.parser.Parser;
import org.example.tokenizer.Tokenizer;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class ObjectNodeTest {

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

    // Returns the map values
    @Test
    void shouldHaveMapValues() {
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
        List<Object> values = new ArrayList<>(4);
        values.add("SGML");
        values.add("SGML");
        values.add("Standard Generalized Markup Language");
        values.add("SGML");

        ObjectNodeAssert.assertThat(node)
                .hasValues(values);
    }

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

    @Test
    void shouldThrowNoSuchElementExceptionWhenKeyDoesNotExist() {
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

       assertThatExceptionOfType(NoSuchElementException.class)
               .isThrownBy(() -> node.key("src"))
               .withMessage("Key 'src' not found");
    }
}
