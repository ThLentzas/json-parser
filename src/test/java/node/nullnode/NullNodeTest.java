package node.nullnode;

import org.example.core.node.NullNode;
import org.example.core.parser.Parser;
import org.example.core.tokenizer.Tokenizer;
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
