package tokenizer;

import org.example.exception.UTF8DecoderException;
import org.example.exception.UnexpectedCharacterException;
import org.example.exception.UnrecognizedTokenException;
import org.example.exception.UnterminatedValueException;
import org.example.tokenizer.Tokenizer;
import org.example.tokenizer.TokenizerToken;
import org.example.tokenizer.TokenizerTokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/*
    In every test bellow as i have already explained we can not pass escape sequences as our json text input instead
    we pass each character individually and the parser will convert the sequence to the corresponding character. (comment
    above main)
    We can not pass "\"\n\"", we pass '\n' as an escape character we need to pass each character individually
    "\"\\n\"" We escape the backslash which converts it to a backslash literal at compile time and then the character n
    The parser will map correctly the escape character/sequence.

    Same logic applies for this as well "\"A\\uD83D\\uDE00Bé\"". These 2 sequences are a surrogate pair. We escape the
    backlash after A to treat the remaining characters as part of a unicode escape sequence. The parser will read
    the characters \uD83D and map them accordingly. This has to do with java
 */
class TokenizerTest {
    /*
        We use @MethodSource because if we used @ValueSource we wouldn't be able to assert on the start and end index.
        @ValueSource(strings = {
            // Positive integers
            "1", "12345", "0",
            // Negative integers
            "-1", "-12345", "-0",
            // Simple decimals
            "1.0", "123.45", "0.1", "0.12345", "-1.0", "-123.45", "-0.1", "-0.12345",
            // Exponential notation (positive base)
            "1e2", "1E2", "123e+5", "123E+5", "123e-5", "123E-5",
            // Exponential notation (negative base)
            "-1e2", "-1E2", "-123e+5", "-123E+5", "-123e-5", "-123E-5",
            // Exponential notation with decimals
            "1.2e3", "123.45E6", "0.1e-2", "0.123e+2",
            "-1.2e3", "-123.45E6", "-0.1e-2", "-0.123e+2"
        })
     */
    @ParameterizedTest
    @MethodSource("provideNumberTestCases")
    void shouldTokenizeNumber(String jsonText, int startIndex, int endIndex) {
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        TokenizerToken token = tokenizer.nextToken();

        TokenizerTokenAssert.assertThat(token)
                .hasStartIndex(startIndex)
                .hasEndIndex(endIndex)
                .hasTokenType(TokenizerTokenType.NUMBER);
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForMinusSignWithoutDigit() {
        String jsonText = "-";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 0. A valid numeric value requires a digit (0-9) after the minus sign");
    }

    // '-*' where * is any character that is not a number
    @Test
    void shouldThrowUnexpectedCharacterExceptionForMinusSignFollowedByInvalidCharacter() {
        String jsonText = "-a";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 0. Unexpected character: 'a'. Expected a digit (0-9) after the minus sign");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForNumberWithLeadingPositiveSign() {
        Tokenizer tokenizer = new Tokenizer("+123".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 0. JSON specification prohibits numbers from being prefixed with a plus sign");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForNumberWithLeadingZeros() {
        Tokenizer tokenizer = new Tokenizer("-01".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 1. Leading zeros are not allowed");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForMultipleDecimalPoints() {
        Tokenizer tokenizer = new Tokenizer("0.1.2".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 3. Only one decimal point is allowed");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionWhenDecimalPointIsFoundAfterExponentialNotation() {
        Tokenizer tokenizer = new Tokenizer("-12e5.3".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 5. Decimal point is not allowed after exponential notation");
    }

    // The cases for the 3rd if in handleDecimalPoint
    @ParameterizedTest
    @ValueSource(strings = {"-12.", "-12.e5"})
    void shouldThrowUnexpectedCharacterExceptionWhenDecimalPointIsNotFollowedByDigit() {
        Tokenizer tokenizer = new Tokenizer("-12.e5".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 3. Decimal point must be followed by a digit");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForMultipleExponentialNotations() {
        Tokenizer tokenizer = new Tokenizer("1e2e3".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 3. Only one exponential notation('e' or 'E') is allowed");
    }

    // 2nd if in the handleExponentialNotation
    @ParameterizedTest
    @ValueSource(strings = {"1.1e", "246Ef"})
    void shouldThrowUnexpectedCharacterExceptionWhenExponentialNotationIsNotFollowedByADigit(String jsonText) {
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 3. Exponential notation must be followed by a digit");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForSignNotFollowingExponentialNotation() {
        Tokenizer tokenizer = new Tokenizer("12+3".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 2. Sign ('+' or '-') is only allowed as part of exponential notation after 'e' or 'E'");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForSignWithoutDigitAfterExponentialNotation() {
        Tokenizer tokenizer = new Tokenizer("1e+".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 2. Exponential notation must be followed by a digit");
    }

    // The following characters in this case are invalid at the top level but valid in an array or an object. The 2nd part
    // is tested in the parser. We can pass control characters directly here because we don't have a Json String but a
    // Number instead. These characters are allowed to be passed as raw bytes. Read more in tokenizeString()
    @ParameterizedTest
    @ValueSource(strings = {"2a", "3,", "4]", "5}", "6 ", "7\n", "8\t", "9\r"})
    void shouldThrowUnexpectedCharacterExceptionForNumberFollowedByInvalidCharacter(String jsonText) {
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 1. Unexpected character: '" + jsonText.charAt(1) + "'");
    }

    @Test
    void shouldTokenizeJsonTrue() {
        Tokenizer tokenizer = new Tokenizer("true".toCharArray());
        TokenizerToken token = tokenizer.nextToken();

        TokenizerTokenAssert.assertThat(token)
                .hasStartIndex(0)
                .hasEndIndex(3)
                .hasTokenType(TokenizerTokenType.BOOLEAN);
    }

    @Test
    void shouldTokenizeJsonFalse() {
        Tokenizer tokenizer = new Tokenizer("false".toCharArray());
        TokenizerToken token = tokenizer.nextToken();

        TokenizerTokenAssert.assertThat(token)
                .hasStartIndex(0)
                .hasEndIndex(4)
                .hasTokenType(TokenizerTokenType.BOOLEAN);
    }

    @Test
    void shouldTokenizeJsonNull() {
        Tokenizer tokenizer = new Tokenizer("null".toCharArray());
        TokenizerToken token = tokenizer.nextToken();

        TokenizerTokenAssert.assertThat(token)
                .hasStartIndex(0)
                .hasEndIndex(3)
                .hasTokenType(TokenizerTokenType.NULL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"trfe", "tre", "falst", "fals", "nulf", "nul"})
    void shouldThrowUnrecognizedTokenExceptionWhenJsonTextIsInvalidForJsonBooleanAndNull(String jsonText) {
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        assertThatExceptionOfType(UnrecognizedTokenException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Unrecognized token: '" + jsonText + "'. Expected a valid JSON value");
    }

    // Always read the comment on top of the class to understand the values passed
    @Test
    void shouldTokenizeJsonString() {
        Tokenizer tokenizer = new Tokenizer("\"abc\"".toCharArray());
        TokenizerToken token = tokenizer.nextToken();

        TokenizerTokenAssert.assertThat(token)
                .hasStartIndex(0)
                .hasEndIndex(4)
                .hasTokenType(TokenizerTokenType.STRING);
    }

    // The 1st test covers the case where we have more characters after a string. In the test below, the string is followed
    // ']' a structural character which could be a valid in a case like ["abc"] but it is in the wrong context because
    // currently we don't have an array
    @Test
    void shouldThrowUnrecognizedTokenExceptionForValidStringFollowedByNonStructuralCharacter() {
        Tokenizer tokenizer = new Tokenizer("\"abc\"123".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 5. Unexpected character: '1'");
    }

    @Test
    void shouldThrowUnrecognizedTokenExceptionForValidStringFollowedByStructuralCharacter() {
        Tokenizer tokenizer = new Tokenizer("\"abc\"]".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 5. Unexpected character: ']'");
    }

    @Test
    void shouldTokenizeJsonStringWithSurrogatePair() {
        Tokenizer tokenizer = new Tokenizer("\"A\\uD83D\\uDE00Bé\"".toCharArray());
        TokenizerToken token = tokenizer.nextToken();

        TokenizerTokenAssert.assertThat(token)
                .hasStartIndex(0)
                .hasEndIndex(6)
                .hasTokenType(TokenizerTokenType.STRING);
    }

    // Always read the comment on top of the class to understand the values passed
    @Test
    void shouldThrowUTF8DecoderExceptionFor2ConsecutiveHighSurrogates() {
        Tokenizer tokenizer = new Tokenizer("\"A\\uD83D\\uD83DBé\"".toCharArray());

        assertThatExceptionOfType(UTF8DecoderException.class).isThrownBy(tokenizer::nextToken);
    }

    @Test
    void shouldThrowUTF8DecoderExceptionForUnpairedHighSurrogate() {
        Tokenizer tokenizer = new Tokenizer("\"A\\uD83Dé\"".toCharArray());

        assertThatExceptionOfType(UTF8DecoderException.class).isThrownBy(tokenizer::nextToken);
    }

    @Test
    void shouldThrowUTF8DecoderExceptionForUnpairedLowSurrogate() {
        Tokenizer tokenizer = new Tokenizer("\"A\\uDE00".toCharArray());

        assertThatExceptionOfType(UTF8DecoderException.class).isThrownBy(tokenizer::nextToken);
    }

    /*
        Always read the comment on top of the class to understand the values passed

        We have to consider 2 cases based on the number of consecutive backslashes:
            Even: For an even number of backslashes we will have n / 2 backslash literals

        At runtime when the escape sequence is converted by the compiler we have ['"', '\', '\', 'q' "] We tokenize the
        array into ['"', '\', 'q', "] -> consecutive backslashes result in a backslash literal

        For detailed explanation on the logic look at handleEscapeCharacter()
     */
    @Test
    void shouldTokenizeJsonStringWithEvenNumberOfBackslashes() {
        Tokenizer tokenizer = new Tokenizer("\"\\\\q\"".toCharArray());
        TokenizerToken token = tokenizer.nextToken();

        TokenizerTokenAssert.assertThat(token)
                .hasStartIndex(0)
                .hasEndIndex(3)
                .hasTokenType(TokenizerTokenType.STRING);
    }

    /*
        At runtime when the escape sequence is converted by the compiler we have ['"', '\', '\', '\', 'n'"] We tokenize
        the array into ['"', '\', '\n', "]. We have odd number of backslashes the above input array will consider the 1st
        two backslashes as a pair and the remaining backslash MUST be followed by a character that if combined must lead
        to a valid escape character. In our case, the next character is 'n'. If we concatenate those into 1 we have as
        final array ['"', '\', '\n', "]. This and the above test result in the same output due to the way we handle
        the number of backslashes.

        For detailed explanation on the logic look at tokenizeString()
     */
    @Test
    void shouldTokenizeJsonStringWithOddNumberOfBackslashes() {
        Tokenizer tokenizer = new Tokenizer("\"\\\\\\n\"".toCharArray());
        TokenizerToken token = tokenizer.nextToken();

        TokenizerTokenAssert.assertThat(token)
                .hasStartIndex(0)
                .hasEndIndex(3)
                .hasTokenType(TokenizerTokenType.STRING);
    }

    /*
        Always read the comment on top of the class to understand the values passed

        We have a unicode escape sequence '\u00E9' which represents the character 'é'. This character is part of the
        BMP, and it will convert the unicode sequence to the corresponding character. The output is ['"', 'é', '"']
     */
    @Test
    void shouldTokenizeJsonStringWithCodePointInBasicMultilingualPlane() {
        Tokenizer tokenizer = new Tokenizer("\"\\u00E9\"".toCharArray());
        TokenizerToken token = tokenizer.nextToken();

        TokenizerTokenAssert.assertThat(token)
                .hasStartIndex(0)
                .hasEndIndex(2)
                .hasTokenType(TokenizerTokenType.STRING);
    }

    /*
        Case: ['"', '\', '"']
        For any odd number of backslashes where the unpaired backslash is followed by '"' and there isn't any closing
        quotation mark '"' in the remaining characters of the string we must throw an exception, it is not a valid string.
        Read mapEscapeCharacter()
     */
    @Test
    void shouldThrowUnexpectedCharacterExceptionForInputEndingWithEscapedQuote() {
        Tokenizer tokenizer = new Tokenizer("\"\\\"".toCharArray());

        assertThatExceptionOfType(UnterminatedValueException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 2. Unterminated value for JSON String");
    }

    @Test
    void shouldThrowUnterminatedValueExceptionForUnterminatedString() {
        Tokenizer tokenizer = new Tokenizer("\"abc".toCharArray());

        assertThatExceptionOfType(UnterminatedValueException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 4. Unterminated value for JSON String");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionWhenEscapeCharacterIsNotValid() {
        Tokenizer tokenizer = new Tokenizer("\"\\q".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 2. Unexpected escape character: q");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForIncompleteCharacterEscapeSequence() {
        Tokenizer tokenizer = new Tokenizer("\"\\".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 1. Incomplete character escape sequence");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForIncompleteUnicodeCharacterSequence() {
        Tokenizer tokenizer = new Tokenizer("\"\\u00E".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 5. Unexpected end of input for unicode escape sequence");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForInvalidCharactersInUnicodeSequence() {
        Tokenizer tokenizer = new Tokenizer("\"\\u00E!\"".toCharArray());

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 6. Unexpected character: '!'. A hex-digit was expected in the character escape sequence");
    }


    @Test
    void shouldThrowUnrecognizedTokenExceptionForInvalidCharacters() {
        Tokenizer tokenizer = new Tokenizer("@".toCharArray());

        assertThatExceptionOfType(UnrecognizedTokenException.class).isThrownBy(tokenizer::nextToken)
                .withMessage("Position: 0. Unrecognized token: '@'. Expected: a valid JSON value");
    }

    static Stream<Arguments> provideNumberTestCases() {
        return Stream.of(
                Arguments.of("1", 0, 0),
                Arguments.of("12345", 0, 4),
                Arguments.of("-1", 0, 1),
                Arguments.of("-0", 0, 1),
                Arguments.of("-12345", 0, 5),
                Arguments.of("1.0", 0, 2),
                Arguments.of("123.45", 0, 5),
                Arguments.of("0.1", 0, 2),
                Arguments.of("0.12345", 0, 6),
                Arguments.of("-1.0", 0, 3),
                Arguments.of("-123.45", 0, 6),
                Arguments.of("1e2", 0, 2),
                Arguments.of("123e+5", 0, 5),
                Arguments.of("123e-5", 0, 5),
                Arguments.of("-1e2", 0, 3),
                Arguments.of("-123e+5", 0, 6),
                Arguments.of("1.2e3", 0, 4),
                Arguments.of("123.45E6", 0, 7)
        );
    }
}
