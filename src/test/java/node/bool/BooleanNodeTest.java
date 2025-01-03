package node.bool;

import org.example.core.node.BooleanNode;
import org.example.core.parser.Parser;
import org.example.core.tokenizer.Tokenizer;
import org.junit.jupiter.api.Test;

class BooleanNodeTest {

    @Test
    void shouldHaveValue() {
        Tokenizer tokenizer = new Tokenizer("false".toCharArray());
        Parser parser = new Parser(tokenizer);
        BooleanNode node = (BooleanNode) parser.parse();

        BooleanNodeAssert.assertThat(node)
                .hasType()
                .hasRootToken()
                .hasValue(false);
    }

    @Test
    void shouldHaveNumericValue() {
        Tokenizer tokenizer = new Tokenizer("true".toCharArray());
        Parser parser = new Parser(tokenizer);
        BooleanNode node = (BooleanNode) parser.parse();

        BooleanNodeAssert.assertThat(node)
                .hasNumericValue(1);
    }
}
