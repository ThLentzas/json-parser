package org.example.core.decoder;

import org.example.core.exception.MalformedStructureException;
import org.example.core.exception.UTF8DecoderException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public final class Decoder {

    /*
     *  According to the spec:
     *
     *  8.2.  Unicode Characters

     *    When all the strings represented in a JSON text are composed entirely of Unicode characters [UNICODE]
     *    (however escaped), then that JSON text is interoperable in the sense that all software implementations
     *    that parse it will agree on the contents of names and of string values in objects and arrays.

     *    However, the ABNF in this specification allows member names and string values to contain bit sequences that
     *    cannot encode Unicode characters; for example, "\uDEAD" (a single unpaired UTF-16 surrogate).  The behavior
     *    of software that receives JSON texts containing such values is unpredictable; for example, implementations
     *    might return different values for the length of a string value or even suffer fatal runtime exceptions.

     *  We are going to implement a strict json parser. The previous implementation would do something like
     *  new String(bytes, StandardCharsets.UTF_8). The constructor we used does not have much control over the decoding
     *  process. Whenever the byte array contains unmappable character sequences, it replaces those characters with the
     *  default replacement character �. This is problematic because since we have a strict json parser where EVERY
     *  BYTE OR SEQUENCE OF UTF8 BYTES MUST MAP TO A CHARACTER. Invalid bytes/sequences lead to unmappable characters
     *  and violate that property.
     *  https://www.baeldung.com/java-utf-8-validation
     *  https://stackoverflow.com/questions/59094164/verifying-a-string-is-utf-8-encoded-in-java
     *
     *  Which byte values are considered valid?
     *      1. Single‐byte ASCII character in the range 0x00–0x7F
            2. Leading byte in the specific ranges for multi‐byte sequences:

                0xC2–0xDF for 2‐byte sequences(0xC0 and 0xC1 are invalid, they would form “overlong” representations of ASCII),
                0xE0–0xEF for 3‐byte sequences,
                0xF0–0xF4 for 4‐byte sequences,

            3. Continuation bytes (0x80–0xBF) that correctly follow the leading byte count

     *  Any byte that violates these rules, either by being out of range or appearing in the wrong place is considered
     *  invalid in UTF‐8.
     *
     *  It is important to remember that by implementing a strict parser we will not have any unmappable characters in
     *  the decoded string, we should not encounter '�' anywhere if our rules are correct.
     *
     *  Note: Unpaired surrogates like : [123, 34, 92, 117, 68, 70, 65, 65, 34, 58, 48, 125] {"\uDFAA":0} have a valid
     *  4byte representation (68, 70, 65, 65) and the decoder will not throw. We handle them in the Tokenizer as unpaired
     *  surrogates
     */
    public String decode(byte[] bytes) {
        // Exactly one top level (Object, Array...)
        if(bytes == null || bytes.length == 0) {
            throw new MalformedStructureException("Empty input is not valid JSON");
        }

        try {
            /*
                By default, the decoder is strict meaning it will reject any byte or byte sequence that is invalid and
                unmappable. .onMalformedInput(CodingErrorAction.REPORT) .onUnmappableCharacter(CodingErrorAction.REPORT);

                If we wanted a lenient parser we could use
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                which will replace the unmappable character with the replacement symbol '�'
             */
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            CharBuffer decoded = decoder.decode(ByteBuffer.wrap(bytes));
            return String.valueOf(decoded);
        } catch (CharacterCodingException cce) {
            throw new UTF8DecoderException("Invalid UTF-8 byte sequence");
        }
    }
}
