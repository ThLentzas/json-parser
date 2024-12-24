package node.array;

import org.example.node.ArrayNode;
import org.example.node.Node;
import org.example.parser.Parser;
import org.example.tokenizer.Tokenizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ArrayNodeTest {

    @Test
    void shouldHaveValue() {
        String jsonText = """
              [
                {
                    "value": "New",
                    "onclick": "CreateNewDoc()"
                },
                123,
                "Hello"
              ]
              """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        ArrayNode node = (ArrayNode) parser.parse();

        Map<String, String> obj = new LinkedHashMap<>();
        obj.put("value", "New");
        obj.put("onclick", "CreateNewDoc()");

        Object[] nodes = {obj, new BigDecimal(123), "Hello"};
        /*
            The value() method returns an Object[] where each element is the return value of its node's value() call.
            In this case, we have 1 ObjectNode node and ObjectNode's value() returns a LinedHashMap, 1 NumberNode,
            its value() return the numeric value of the Node and a StringNode whose value() returns a String value as
            text excluding the opening and closing quotation marks
         */
        ArrayNodeAssert.assertThat(node)
                .hasType()
                .hasRootToken()
                .hasValue(nodes);
    }

    @Test
    void shouldGetNodeAtIndex() {
        String jsonText = """
                 ["veniam","id", "exercitation", "ad"]
                """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        ArrayNode node = (ArrayNode) parser.parse();
        Node expected = node.get(2);

        assertThat(node.value()).contains(expected.value());
    }

    @Test
    void shouldThrowArrayIndexOutOfBoundsExceptionWhenAccessingInvalidIndex() {
        String jsonText = """
                 ["veniam","id", "exercitation", "ad"]
                """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        ArrayNode node = (ArrayNode) parser.parse();

        assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class).isThrownBy(() -> node.get(10))
                .withMessage("index: 10, length: 4");
    }
}
