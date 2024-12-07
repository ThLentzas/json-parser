package parser;

import org.example.exception.MalformedStructureException;
import org.example.exception.UnexpectedCharacterException;
import org.example.exception.UnterminatedValueException;
import org.example.parser.Parser;
import org.example.tokenizer.Tokenizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

// toDo: look the cases {}""". This is allowed []    (whitespaces)
class ParserTest {

    @Test
    void shouldThrowUnterminatedValueExceptionForUnterminatedArray() {
        String jsonText = "[116, 943, 234, 38793";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(UnterminatedValueException.class).isThrownBy(parser::parse)
                .withMessage("Unterminated value. Expected: ']' for JSON array");
    }

    @ParameterizedTest
    @ValueSource(strings = {"500", "null"})
    void shouldThrowUnexpectedCharacterExceptionForInvalidObjectKey(String key) {
        String jsonText = String.format("""
                {
                    %s : "value"
                }
                """, key);
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character. Expected: double-quoted value for object name");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionForUnexpectedCommaInArray() {
        String jsonText = "[,]";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character: ',' Expected JSON value");
    }

    @Test
    void  shouldThrowUnexpectedCharacterExceptionForExtraClosingSquareBracket() {
        String jsonText = "[]]";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character after: ']'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{ \"key\" : \"value\",, \"key2\" : \"value2\" }", "{ \"key\" : \"value\",, }"})
    void  shouldThrowUnexpectedCharacterExceptionForTrailingCommasInObject(String jsonText) {
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character. Expected: double-quoted value for object name");
    }

    // look at the comment of the parseObject above the peek() call
    @Test
    void shouldThrowUnexpectedCharacterExceptionWhenInvalidCharacterFollowsObjectValue() {
        String jsonText = "{ \"key\" : 1 + 2 }";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character: + Expected: ',' to separate object keys or '}' for JSON object");
    }

    @Test
    void shouldThrowUnexpectedCharacterExceptionWhenNameValueSeparatorIsMissing() {
        String jsonText = "{ \"key\" \"0012 \"value\" }";
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());
        Parser parser = new Parser(tokenizer);

        assertThatExceptionOfType(UnexpectedCharacterException.class).isThrownBy(parser::parse)
                .withMessage("Unexpected character: '\"' + Expected: ':' to separate key-value");
    }
}
