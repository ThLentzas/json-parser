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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/*
    .withMessage("Unexpected character: '"
        + tokenizer.getBuffer()[tokenizer.getErrorPosition()]
        + "' at position: " + (tokenizer.getErrorPosition() + 1)
        + ". Expected: ',' to separate Array values");
    We avoid asserting like this because if tokenizer.getBuffer()[tokenizer.getInitialPosition()] is incorrect, the
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

    /*
        We have a recursive call which returns after processing the empty array and then for the initial parseArray()
        we try to look for the next token which is null

        !!! This test is the same in the end, what is inside the array does not change the case we are testing
        @Test
        void shouldThrowMalformedStructureExceptionExceptionForUnterminatedNestedArray() {
            Tokenizer tokenizer = new Tokenizer("[[]".toCharArray());
            Parser parser = new Parser(tokenizer);

            assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                    .withMessage("Position: 2. Unterminated value. Expected: ']' for Array");
        }
     */
    @Test
    void shouldThrowMalformedStructureExceptionExceptionForUnterminatedNonEmptyArray() {
        Tokenizer tokenizer = new Tokenizer("[116, 943, 234, 38793".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 20. Unterminated value. Expected: ']' for Array");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForUnexpectedEndOfArrayAfterComma() {
        Tokenizer tokenizer = new Tokenizer("[1,".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 2. Unexpected end of array. Expected a valid JSON value after comma");
    }


    @Test
    void shouldThrowUnrecognizedTokenExceptionForMismatchClosingBracketInArray() {
        Tokenizer tokenizer = new Tokenizer("[2, 1, 5!".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(UnrecognizedTokenException.class).isThrownBy(parser::parse)
                .withMessage("Position: 8. Unrecognized token: '!'. Expected: a valid JSON value");
    }

    // trailing characters, invalid and valid token. The logic is the same for both arrays and objects, so we don't have
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

    @Test
    void shouldMalformedStructureExceptionForMissingCommaBetweenArrayValues() {
        Tokenizer tokenizer = new Tokenizer("[116 943]".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 5. Unexpected character: '9'. Expected: comma to separate Array values");
    }

    // We expect a value and we get an invalid token, tokenizer throws
    @Test
    void shouldThrowUnrecognizedTokenExceptionForInvalidArrayValue() {
        Tokenizer tokenizer = new Tokenizer("[']".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(UnrecognizedTokenException.class).isThrownBy(parser::parse)
                .withMessage("Position: 1. Unrecognized token: '''. Expected: a valid JSON value");
    }

    // We expect a value, and we get a valid token, but it is incorrect structurally. parseValue() throws
    // comma is a recognized token
    @Test
    void shouldThrowMalformedStructureExceptionForUnexpectedCommaInArray() {
        Tokenizer tokenizer = new Tokenizer("[,]".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 1. Unexpected character: ','. Expected a valid JSON value");
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
                .withMessage("Position: 2. Unexpected character: 'n'. Expected: double-quoted value for object name");
    }

    // After comma, we expect the next string key
    @Test
    void shouldThrowMalformedStructureExceptionWhenObjectKeyIsNull() {
        Tokenizer tokenizer = new Tokenizer("{ \"key\": \"value\",".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 16. Unterminated value. Expected: double-quoted value for object name");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForTrailingCommaInObject() {
        Tokenizer tokenizer = new Tokenizer("{ \"key\" : \"value\",, \"key2\" : \"value2\" }".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 18. Unexpected character: ','. Expected: double-quoted value for object name");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForUnterminatedObject() {
        String jsonText = "{ \"key\" : \"value\"";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 16. Unterminated value. Expected: '}' for Object");
    }

    // This and the test below might seem the same, but they are testing different parts of the code. 1st is null, 2nd is miss match
    @Test
    void shouldThrowMalformedStructureExceptionWhenColonIsNull() {
        String jsonText = "{ \"key\"";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 6. Unterminated value. Expected: ':' to separate name-value");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForMissMatchColon() {
        String jsonText = "{ \"key\" \"value\"";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 8. Unexpected character: '\"'. Expected: ':' to separate name-value");
    }

    @Test
    void shouldThrowMalformedStructureExceptionForUnexpectedTokenInObject() {
        Tokenizer tokenizer = new Tokenizer("{\"key\":\"value\" 123}".toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Position: 15. Unexpected character: '1'. Expected: '}' or ',' to separate fields");
    }

    // Control characters here can be used because they are not part of a Json String. Read more: tokenizeString()
    @Test
    void shouldIgnoreInsignificantWhitespacesBeforeAndAfterStructuralObjectCharacters() {
        String jsonText = "{ \"foo\" : \t\"bar\" \t \r \n }";

        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        parser.parse();

        //'{', "foo", ':', "bar", '}'
        assertThat(parser.getTokens()).hasSize(5)
                .extracting(ParserToken::getStartIndex, ParserToken::getEndIndex, ParserToken::getType)
                .containsExactly(
                        tuple(0, 0, ParserTokenType.OBJECT_START),
                        tuple(2, 6, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(8, 8, ParserTokenType.NAME_SEPARATOR),
                        tuple(11, 15, ParserTokenType.OBJECT_PROPERTY_VALUE_STRING),
                        tuple(23, 23, ParserTokenType.OBJECT_END)
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

    // Json Example by the RFC spec
    @Test
    void shouldParse() {
        String jsonText = """
                {
                    "Image": {
                        "Width":  800,
                        "Height": 600,
                        "Title":  "View from 15th Floor",
                        "Thumbnail": {
                            "Url":    "http://www.example.com/image/481989943",
                            "Height": 125,
                            "Width":  100
                        },
                        "Animated" : false,
                        "IDs": [116, 943, 234, 38.793]
                      }
                  }""";

        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);
        parser.parse();

        assertThat(parser.getTokens()).hasSize(49)
                .extracting(ParserToken::getStartIndex, ParserToken::getEndIndex, ParserToken::getType)
                .containsExactly(
                        tuple(0, 0, ParserTokenType.OBJECT_START),
                        // "Image": Nested object starts
                        tuple(6, 12, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(13, 13, ParserTokenType.NAME_SEPARATOR),
                        tuple(15, 15, ParserTokenType.OBJECT_START),

                        // "Width": 800
                        tuple(25, 31, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(32, 32, ParserTokenType.NAME_SEPARATOR),
                        tuple(35, 37, ParserTokenType.OBJECT_PROPERTY_VALUE_NUMBER),
                        tuple(38, 38, ParserTokenType.VALUE_SEPARATOR),

                        // "Height": 600
                        tuple(48, 55, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(56, 56, ParserTokenType.NAME_SEPARATOR),
                        tuple(58, 60, ParserTokenType.OBJECT_PROPERTY_VALUE_NUMBER),
                        tuple(61, 61, ParserTokenType.VALUE_SEPARATOR),

                        // "Title": "View from 15th Floor"
                        tuple(71, 77, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(78, 78, ParserTokenType.NAME_SEPARATOR),
                        tuple(81, 102, ParserTokenType.OBJECT_PROPERTY_VALUE_STRING),
                        tuple(103, 103, ParserTokenType.VALUE_SEPARATOR),

                        // "Thumbnail": Nested object starts
                        tuple(113, 123, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(124, 124, ParserTokenType.NAME_SEPARATOR),
                        tuple(126, 126, ParserTokenType.OBJECT_START),

                        // "Url": "http://www.example.com/image/481989943"
                        tuple(140, 144, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(145, 145, ParserTokenType.NAME_SEPARATOR),
                        tuple(150, 189, ParserTokenType.OBJECT_PROPERTY_VALUE_STRING),
                        tuple(190, 190, ParserTokenType.VALUE_SEPARATOR),

                        // "Height": 125
                        tuple(204, 211, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(212, 212, ParserTokenType.NAME_SEPARATOR),
                        tuple(214, 216, ParserTokenType.OBJECT_PROPERTY_VALUE_NUMBER),
                        tuple(217, 217, ParserTokenType.VALUE_SEPARATOR),

                        // "Width": 100
                        tuple(231, 237, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(238, 238, ParserTokenType.NAME_SEPARATOR),
                        tuple(241, 243, ParserTokenType.OBJECT_PROPERTY_VALUE_NUMBER),

                        // End of "Thumbnail" object
                        tuple(253, 253, ParserTokenType.OBJECT_END),
                        tuple(254, 254, ParserTokenType.VALUE_SEPARATOR),

                        // "Animated": false
                        tuple(264, 273, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(275, 275, ParserTokenType.NAME_SEPARATOR),
                        tuple(277, 281, ParserTokenType.OBJECT_PROPERTY_VALUE_BOOLEAN),
                        tuple(282, 282, ParserTokenType.VALUE_SEPARATOR),

                        // "IDs": Array starts
                        tuple(292, 296, ParserTokenType.OBJECT_PROPERTY_NAME),
                        tuple(297, 297, ParserTokenType.NAME_SEPARATOR),
                        tuple(299, 299, ParserTokenType.ARRAY_START),

                        // Array Values: 116, 943, 234, 38.793
                        tuple(300, 302, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(303, 303, ParserTokenType.VALUE_SEPARATOR),
                        tuple(305, 307, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(308, 308, ParserTokenType.VALUE_SEPARATOR),
                        tuple(310, 312, ParserTokenType.ARRAY_VALUE_NUMBER),
                        tuple(313, 313, ParserTokenType.VALUE_SEPARATOR),
                        tuple(315, 320, ParserTokenType.ARRAY_VALUE_NUMBER),

                        // End of Array
                        tuple(321, 321, ParserTokenType.ARRAY_END),

                        // End of "Image" object
                        tuple(329, 329, ParserTokenType.OBJECT_END),

                        // End of outermost object
                        tuple(333, 333, ParserTokenType.OBJECT_END)
                );
    }

    // https://json.org/example.html
    @ParameterizedTest
    @MethodSource("provideJsonOrgExamples")
    void shouldParseJsonOrgExamples(String jsonText) {
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatNoException().isThrownBy(parser::parse);
    }

    private static Stream<Arguments> provideJsonOrgExamples() throws IOException {
        List<String> examples = new ArrayList<>(5);

        Path dir = Paths.get("src/test/java/parser/resources");
        try (Stream<Path> paths = Files.list(dir)) {
            paths.forEach(path -> {
                try {
                    examples.add(Files.readString(path));
                } catch (IOException ioe) {
                    throw new RuntimeException("Failed to read file: " + path, ioe);
                }
            });
        }
        List<Arguments> arguments = new ArrayList<>();
        examples.forEach(example -> arguments.add(Arguments.of(example)));

        return arguments.stream();
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