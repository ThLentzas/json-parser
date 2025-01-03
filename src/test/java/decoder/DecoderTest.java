package decoder;

import org.example.core.decoder.Decoder;
import org.example.core.exception.MalformedStructureException;
import org.example.core.exception.UTF8DecoderException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DecoderTest {

    // works for null as well
    @Test
    void shouldThrowMalformedStructureExceptionForEmptyInputArray() {
        Decoder decoder = new Decoder();
        byte[] bytes = {};
        assertThatExceptionOfType(MalformedStructureException.class).isThrownBy(() -> decoder.decode(bytes))
                .withMessage("Empty input is not valid JSON");
    }

    @Test
    void shouldThrowUTF8DecoderExceptionForLeadingByteOutOfValidRange() {
        Decoder decoder = new Decoder();
        // In UTF‐8, bytes in the range 0x80–0xBF can only appear as continuation bytes. 0x80 is used as a leading byte,
        // which is invalid
        byte[] bytes = {(byte) 0x80}; //128
        assertThatExceptionOfType(UTF8DecoderException.class).isThrownBy(() -> decoder.decode(bytes));
    }

    @Test
    void shouldThrowUTF8DecoderExceptionForOverlongEncoding() {
        Decoder decoder = new Decoder();
        // 0xC0 and 0xC1 are invalid leading bytes
        byte[] bytes = {(byte) 0xC0, (byte) 0xA0}; // 192, 160
        assertThatExceptionOfType(UTF8DecoderException.class).isThrownBy(() -> decoder.decode(bytes));
    }

    @Test
    void shouldThrowUTF8DecoderExceptionForLeadingByteOutOfMaxUTF8Range() {
        Decoder decoder = new Decoder();
        // Bytes 0xF5–0xFF as the first byte would indicate code points beyond U+10FFFF, which UTF‐8 no longer allows
        byte[] bytes = {(byte) 0xF6}; // 246
        assertThatExceptionOfType(UTF8DecoderException.class).isThrownBy(() -> decoder.decode(bytes));
    }

    @Test
    void shouldThrowUTF8DecoderExceptionForContinuationByteOutOfRange() {
        Decoder decoder = new Decoder();
        // 0xC2 indicates a 2‐byte sequence, so the next byte must be 0x80–0xBF. 0x7F is outside that range
        byte[] bytes = {(byte) 0xC2, (byte) 0x7F}; // 194, 127
        assertThatExceptionOfType(UTF8DecoderException.class).isThrownBy(() -> decoder.decode(bytes));
    }

    @Test
    void shouldThrowUTF8DecoderExceptionWhenThereAreNotEnoughContinuationBytes() {
        Decoder decoder = new Decoder();
        // 0xE0 indicates a 3‐byte sequence. We only provide one continuation byte (0x80)
        byte[] bytes = {(byte) 0xE0, (byte) 0x80 }; // 224, 128
        assertThatExceptionOfType(UTF8DecoderException.class).isThrownBy(() -> decoder.decode(bytes));
    }

    @Test
    void shouldThrowUTF8DecoderExceptionWhenThereAreTooManyContinuationBytes() {
        Decoder decoder = new Decoder();
        // 0xC2 indicates a 2‐byte sequence, one continuation is needed. We gave two
        byte[] bytes = {(byte) 0xC2, (byte) 0x80, (byte) 0x80}; // 194, 128, 128
        assertThatExceptionOfType(UTF8DecoderException.class).isThrownBy(() -> decoder.decode(bytes));
    }
}
