package decoder;

import org.example.decoder.Decoder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecoderTest {

    // This is not a test to test that Java's String constructor decodes the byte array into a String but that the
    // arguments are passed in our decode method correctly.
    @Test
    void shouldDecode() {
        Decoder decoder = new Decoder();
        byte[] bytes = {72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100};
        String actual = decoder.decode(bytes);

        assertThat(actual).isEqualTo("Hello World");
    }
}
