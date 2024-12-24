package node.nullnode;

import org.example.node.NullNode;
import org.example.parser.Parser;
import org.example.tokenizer.Tokenizer;
import org.junit.jupiter.api.Test;

class NullNodeTest {

    @Test
    void shouldHaveValue() {
        Tokenizer tokenizer = new Tokenizer("null".toCharArray());
        Parser parser = new Parser(tokenizer);
        NullNode node = (NullNode) parser.parse();

        NullNodeAssert.assertThat(node)
                .hasValue()
                .hasType()
                .hasRootToken();
    }
}
