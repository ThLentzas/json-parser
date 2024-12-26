package node.number;

import org.example.node.NumberNode;
import org.example.parser.Parser;
import org.example.tokenizer.Tokenizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NumberNodeTest {

    @Test
    void shouldHaveTypeAndRootToken() {
        Tokenizer tokenizer = new Tokenizer("3.1415".toCharArray());
        Parser parser = new Parser(tokenizer);
        NumberNode node = (NumberNode) parser.parse();

        NumberNodeAssert.assertThat(node)
                .hasType()
                .hasRootToken();
    }

    @ParameterizedTest
    @ValueSource(ints = {12, -231})
    void shouldHaveIntValue(int number) {
        Tokenizer tokenizer = new Tokenizer(String.valueOf(number).toCharArray());
        Parser parser = new Parser(tokenizer);
        NumberNode node = (NumberNode) parser.parse();

        assertThat(node.intValue()).isEqualTo(number);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.3", "1e-2"})
    void shouldHaveDoubleValue(String number) {
        Tokenizer tokenizer = new Tokenizer(number.toCharArray());
        Parser parser = new Parser(tokenizer);
        NumberNode node = (NumberNode) parser.parse();

        assertThat(node.doubleValue()).isEqualTo(new BigDecimal(number));
    }

    @Test
    void shouldHaveLongValue() {
        Tokenizer tokenizer = new Tokenizer("9113372036854775807".toCharArray());
        Parser parser = new Parser(tokenizer);
        NumberNode node = (NumberNode) parser.parse();

       assertThat(node.longValue()).isEqualTo(9113372036854775807L);
    }

    @Test
    void shouldReturnTrueWhenNumberIsInteger() {
        Tokenizer tokenizer = new Tokenizer("1235769".toCharArray());
        Parser parser = new Parser(tokenizer);
        NumberNode node = (NumberNode) parser.parse();

        assertThat(node.isInteger(node.value())).isTrue();
    }

    @Test
    void shouldReturnTrueWhenNumberIsDouble() {
        Tokenizer tokenizer = new Tokenizer("1235769.451".toCharArray());
        Parser parser = new Parser(tokenizer);
        NumberNode node = (NumberNode) parser.parse();

        assertThat(node.isDouble(node.value())).isTrue();
    }

    @Test
    void shouldReturnTrueWhenNumberIsLong() {
        Tokenizer tokenizer = new Tokenizer("123456789123".toCharArray());
        Parser parser = new Parser(tokenizer);
        NumberNode node = (NumberNode) parser.parse();

        assertThat(node.isLong(node.value())).isTrue();
    }
}
