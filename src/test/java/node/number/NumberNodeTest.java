package node.number;

import org.example.node.NumberNode;
import org.example.parser.Parser;
import org.example.tokenizer.Tokenizer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

class NumberNodeTest {

    @ParameterizedTest
    @MethodSource("provideNumberTestCases")
    void shouldHaveValue(String jsonText, BigDecimal value) {
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        NumberNode node = (NumberNode) parser.parse();

        NumberNodeAssert.assertThat(node)
                .hasType()
                .hasRootToken()
                .hasValue(value);
    }

    static Stream<Arguments> provideNumberTestCases() {
        return Stream.of(
                Arguments.of("12", new BigDecimal(12)),
                //unwrapping
                Arguments.of("1e-2", new BigDecimal("0.01")),
                Arguments.of("-231", new BigDecimal(-231)),
                Arguments.of("2147483647.12345", new BigDecimal("2147483647.12345")),
                Arguments.of("-2147483649.12345", new BigDecimal("-2147483649.12345")),
                Arguments.of("9113372036854775807", new BigDecimal("9113372036854775807")),
                Arguments.of("-8113355036854775807", new BigDecimal("-8113355036854775807")),
                Arguments.of("12345.6789012345678901234567890123456789", new BigDecimal("12345.6789012345678901234567890123456789"))
        );
    }
}
