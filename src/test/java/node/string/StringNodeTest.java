package node.string;

import org.example.core.exception.SubsequenceIndexViolationException;
import org.example.core.node.StringNode;
import org.example.core.parser.Parser;
import org.example.core.tokenizer.Tokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class StringNodeTest {

    @Test
    void shouldHaveValue() {
        Tokenizer tokenizer = new Tokenizer("\"json\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        StringNodeAssert.assertThat(node)
                .hasType()
                .hasRootToken()
                .hasValue("json");
    }

    @Test
    void shouldReturnFalseWhenSubsequenceIsNotPresent() {
        Tokenizer tokenizer = new Tokenizer("\"ahbgdc\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        assertThat(node.isSubsequence("axc")).isFalse();
    }

    @Test
    void shouldReturnTrueWhenSubsequenceIsValid() {
        Tokenizer tokenizer = new Tokenizer("\"ahbgdc\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        assertThat(node.isSubsequence("abc")).isTrue();
    }

    @Test
    void shouldReturnSubsequence() {
        Tokenizer tokenizer = new Tokenizer("\"ahbgdc\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        StringNodeAssert.assertThat(node)
                .hasSubsequence(List.of(2, 4, 5), "bdc");
    }

    @Test
    void shouldThrowIndexOutOfBoundsExceptionWhenAtLeastOneIndexIsNegative() {
        Tokenizer tokenizer = new Tokenizer("\"ahbgdc\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> node.subsequence(List.of(-1, 2)))
                .withMessage("index: -1, length: 6");
    }

    @Test
    void shouldThrowSubsequenceExceptionWhenAtLeastOneIndexIsNegative() {
        Tokenizer tokenizer = new Tokenizer("\"ahbgdc\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        assertThatExceptionOfType(SubsequenceIndexViolationException.class).isThrownBy(() -> node.subsequence(List.of(3, 2)))
                .withMessage("Indices must be in strict ascending order. Found: 2 after 3");
    }

    @Test
    void shouldReturnIntValue() {
        Tokenizer tokenizer = new Tokenizer("\"1345136\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        StringNodeAssert.assertThat(node)
                .hasIntValue(1345136);
    }

    @Test
    void shouldReturnDoubleValue() {
        Tokenizer tokenizer = new Tokenizer("\"-1.23E1\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        StringNodeAssert.assertThat(node)
                .hasDoubleValue(-12.3);
    }

    @Test
    void shouldReturnLongValue() {
        Tokenizer tokenizer = new Tokenizer("\"9223372036854775807\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        StringNodeAssert.assertThat(node)
                .hasLongValue(9223372036854775807L);
    }

    @Test
    void shouldReturnZeroWhenNumericStringContainsInvalidCharacter() {
        Tokenizer tokenizer = new Tokenizer("\"1345136q\"".toCharArray());
        Parser parser = new Parser(tokenizer);
        StringNode node = (StringNode) parser.parse();

        StringNodeAssert.assertThat(node)
                .hasIntValue(0);
    }
}
