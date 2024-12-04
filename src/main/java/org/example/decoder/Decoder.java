package org.example.decoder;

import java.nio.charset.StandardCharsets;

public final class Decoder {

    /**
     * According to rfc for JSON: JSON text exchanged between systems MUST be encoded using UTF-8. When we decode the
     * byte[] we will use UTF_8. We do not try to find the encoding schema based on the BOM bytes. We could do that by
     * extracting the BOM bytes from the byte[] and check against the known BOM values for UTF_8, UTF_16BE, UTF_16LE.
     * <br>
     * <br>
     * An implementation of the above logic can be found here:<a href="https://github.com/FasterXML/jackson-core/blob/2.19/src/main/java/com/fasterxml/jackson/core/json/ByteSourceJsonBootstrapper.java">...</a>
     * <br>
     * byte[] bytes = {34, 72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 34};
     * The above bytes are encoded using UTF-8. If the provided encoding schema does not match the one used to encode
     * those bytes it is not our responsibility to verify that. It was an error from the user's side. When we decode
     * the above sequence with UTF_16BE(wrong encoding schema provided by the user) the decoded string is "≈敬汯⁷潲汤�"
     * and we are going to work with that.
     *
     * @param bytes the byte array to decode
     * @return the decoded string known as Json text according to RFC 8259
     */
    public String decode(byte[] bytes) {
        /*
            BOM handling
            Initially i had the following code, but the specification is not strict on how the parser should handle the
            signature at the start of the byte[]. If provided it will result in UnrecognizedTokenException because it
            will be treated as separate characters. 0xEF = ï, 0xBB = », 0xBF = ¿

                if (bytes.length < 3) {
                    return new String(bytes, StandardCharsets.UTF_8);
                }

                if (bytes[0] == (byte) 0xEF
                        && bytes[1] == (byte) 0xBB
                        && bytes[2] == (byte) 0xBF) {
                    return bytes.length == 3 ? new String(new byte[]{}, StandardCharsets.UTF_8)
                            : new String(Arrays.copyOfRange(bytes, 3, bytes.length), StandardCharsets.UTF_8);
                }
         */
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
