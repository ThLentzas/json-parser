package parser;

import org.example.exception.DuplicateObjectNameException;
import org.example.exception.MalformedStructureException;
import org.example.exception.UnrecognizedTokenException;
import org.example.parser.Parser;
import org.example.parser.ParserToken;
import org.example.parser.ParserTokenType;
import org.example.tokenizer.Tokenizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.assertj.core.groups.Tuple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.stream.Stream;

/*
    .withMessage("Unexpected character: '"
        + tokenizer.getBuffer()[tokenizer.getErrorPosition()]
        + "' at position: " + (tokenizer.getErrorPosition() + 1)
        + ". Expected: ',' to separate Array values");
    We avoid asserting like this because if tokenizer.getBuffer()[tokenizer.getErrorPosition()] is incorrect, the
    assertion will test against an incorrect value. Instead, we directly pass the expected values

    All our errors' position is 1-indexed.
 */
class ParserTest {

    // This and the test below might seem the same, but they are testing different parts of the code
    @Test
    void shouldThrowMalformedStructureExceptionExceptionForUnterminatedEmptyArray() {
        Tokenizer tokenizer = new Tokenizer("[".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 0. Unterminated value. Expected: ']' for Array");
    }

    @Test
    void shouldThrowMalformedStructureExceptionExceptionForUnterminatedNonEmptyArray() {
        Tokenizer tokenizer = new Tokenizer("[116, 943, 234, 38793".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 20. Unterminated value. Expected: ']' for Array");
    }

    /*
        We have a recursive call which returns after processing the empty array and then for the initial parseArray()
        we try to look for the next token which is null
     */
    @Test
    void shouldThrowMalformedStructureExceptionExceptionForUnterminatedNestedArray() {
        Tokenizer tokenizer = new Tokenizer("[[]".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 2. Unterminated value. Expected: ']' for Array");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForMismatchClosingBracketInArray() {
        Tokenizer tokenizer = new Tokenizer("[2, 1, 5}".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 8. Unexpected character: '}'. Expected: ']' for Array");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForUnexpectedCommaInArray() {
        Tokenizer tokenizer = new Tokenizer("[,]".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 1. Unexpected character: ',' Expected JSON value");
    }

    // trailing characters, invalid and valid token. The logic is the same for both arrays and objects so we don't have
    // test again for "{}}" and "{}0001"
    @Test
    void shouldThrowMalformedStructureExceptionForTrailingInvalidToken() {
        Tokenizer tokenizer = new Tokenizer("[]]".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 2. Unexpected character: ']'");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForTrailingValidToken() {
        Tokenizer tokenizer = new Tokenizer("[]123".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 2. Unexpected character: '1'");
    }

    // toDo: test dynamic type arrays like [123, []] is considered valid, also test the double condition for the if in assertColon
    @Test
    void shouldMalformedStructureExceptionForMissingCommaBetweenArrayValues() {
        Tokenizer tokenizer = new Tokenizer("[116 943]".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 5. Unexpected character: '9'. Expected: comma to separate Array values");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForInvalidArrayValue() {
        Tokenizer tokenizer = new Tokenizer("['']".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(UnrecognizedTokenException.class).isThrownBy(parser::parse)
                .withMessage("Position: 1. Unrecognized token: '''. Expected: a valid JSON value");
    }

    @Test
    void shouldIgnoreInsignificantWhitespacesBeforeAndAfterStructuralArrayCharacters() {
        Tokenizer tokenizer = new Tokenizer("[\"foo\" \t \n    ]\r".toCharArray());
        Parser parser = new Parser(tokenizer);
        parser.parse();

        //'[', "foo", ']'
        assertThat(parser.getTokens()).hasSize(3)
                .extracting(ParserToken::getStartIndex, ParserToken::getEndIndex, ParserToken::getType)
                .containsExactly(
                        tuple(0, 0, ParserTokenType.ARRAY_START),
                        tuple(1, 5, ParserTokenType.ARRAY_VALUE_STRING),
                        tuple(14, 14, ParserTokenType.ARRAY_END)
                );
    }

    @Test
    void shouldThrowMalformedStructureExceptionForInvalidObjectKey() {
        Tokenizer tokenizer = new Tokenizer("{ null : 1}".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character: 'n' at position: 3. Expected: double-quoted value for object name");
    }

    @Test
    void shouldThrowMalformedStructureExceptionWhenObjectKeyIsMissing() {
        Tokenizer tokenizer = new Tokenizer("{ : \"value\"}".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character: ':' at position: 3. Expected: double-quoted value for object name");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForTrailingCommaInObject() {
        Tokenizer tokenizer = new Tokenizer("{ \"key\" : \"value\",, \"key2\" : \"value2\" }".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character: ',' at position: 19. Expected: double-quoted value for object name");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForUnterminatedObject() {
        String jsonText = "{ \"key\" : \"value\"";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 16. Unterminated value. Expected: '}' for Object");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForUnterminatedNestedObject() {
        String jsonText = "{\"foo\" : {}";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 10. Unterminated value. Expected: '}' for Object");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForIllegalExpression() {
        String jsonText = "{ \"key\" : 1 + 2 }";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character: '+' at position: 13' Expected: ',' to separate object keys or '}' for JSON object");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForMissingColon() {
        String jsonText = "{ \"key\" \"0012 \"value\" }";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character: '\"' at position: 9. Expected: ':' to separate name-value");
    }

    @Test
    void shouldIgnoreInsignificantWhitespacesBeforeAndAfterStructuralObjectCharacters() {
        String jsonText = """
                {
                    "foo" : \t"bar" \t \r \n    
                }
                """;
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        parser.parse();

        //'{', "foo", ':', "bar", '}'
        assertThat(parser.getTokens()).hasSize(5)
                .extracting(ParserToken::getStartIndex, ParserToken::getEndIndex, ParserToken::getType)
                .containsExactly(
                        tuple(0, 0, ParserTokenType.OBJECT_START),
                        tuple(6, 10, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(12, 12, ParserTokenType.NAME_SEPARATOR),
                        tuple(15, 19, ParserTokenType.OBJECT_PROPERTY_VALUE_STRING),
                        tuple(27, 27, ParserTokenType.OBJECT_END)
                );
    }

    @ParameterizedTest
    @MethodSource("provideValidLeadingZeroCases")
    void shouldParseValidLeadingZeroCases(String jsonText, List<Tuple> expectedTokens) {
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        parser.parse();

        assertThat(parser.getTokens())
                .extracting(ParserToken::getStartIndex, ParserToken::getEndIndex, ParserToken::getType)
                .containsExactlyElementsOf(expectedTokens);
    }

    @Test
    void shouldThrowDuplicateObjectNameExceptionForDuplicateObjectKeys() {
        String jsonText = "{\"foo\" : \"bar\", \"foo\" : \"baz\"}";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(DuplicateObjectNameException.class).isThrownBy(parser::parse)
                .withMessage("Duplicate object name: foo");
    }

    private static Stream<Arguments> provideValidLeadingZeroCases() {
        return Stream.of(
                // Case 1: Array Value with Leading Zero
                Arguments.of("[-0, 1, 2]", List.of(
                        tuple(0, 0, ParserTokenType.ARRAY_START),
                        tuple(1, 2, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(3, 3, ParserTokenType.VALUE_SEPARATOR),
                        tuple(5, 5, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(6, 6, ParserTokenType.VALUE_SEPARATOR),
                        tuple(8, 8, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(9, 9, ParserTokenType.ARRAY_END)
                )),
                // Case 2: Last Value of the Array
                Arguments.of("[1, 2, -0]", List.of(
                        tuple(0, 0, ParserTokenType.ARRAY_START),
                        tuple(1, 1, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(2, 2, ParserTokenType.VALUE_SEPARATOR),
                        tuple(4, 4, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(5, 5, ParserTokenType.VALUE_SEPARATOR),
                        tuple(7, 8, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(9, 9, ParserTokenType.ARRAY_END)
                )),
                // Case 3: Array Value Followed by RFC Whitespace
                Arguments.of("[1, 0 \t \n , 2]", List.of(
                        tuple(0, 0, ParserTokenType.ARRAY_START),
                        tuple(1, 1, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(2, 2, ParserTokenType.VALUE_SEPARATOR),
                        tuple(4, 4, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(10, 10, ParserTokenType.VALUE_SEPARATOR),
                        tuple(12, 12, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(13, 13, ParserTokenType.ARRAY_END)
                )),
                // Case 4: Object Value with Leading Zero
                Arguments.of("{\"key\" : 0}", List.of(
                        tuple(0, 0, ParserTokenType.OBJECT_START),
                        tuple(1, 5, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(7, 7, ParserTokenType.NAME_SEPARATOR),
                        tuple(9, 9, ParserTokenType.OBJECT_PROPERTY_VALUE_NUMBER),
                        tuple(10, 10, ParserTokenType.OBJECT_END)
                ))
        );
    }

}
