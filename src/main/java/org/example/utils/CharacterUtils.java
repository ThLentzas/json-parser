package org.example.utils;

import java.util.HashMap;
import java.util.Map;

public final class CharacterUtils {

    private static final Map<Byte, String> controlCharacterMap = new HashMap<>();

    // Since control characters don't have text representation, we can improve the error handling by mapping the byte value
    // to some relevant text
    static {
        controlCharacterMap.put((byte) 0x00, "<NUL>");
        controlCharacterMap.put((byte) 0x01, "<SOH>");
        controlCharacterMap.put((byte) 0x02, "<STX>");
        controlCharacterMap.put((byte) 0x03, "<ETX>");
        controlCharacterMap.put((byte) 0x04, "<EOT>");
        controlCharacterMap.put((byte) 0x05, "<ENQ>");
        controlCharacterMap.put((byte) 0x06, "<ACK>");
        controlCharacterMap.put((byte) 0x07, "<BEL>");
        controlCharacterMap.put((byte) 0x08, "<BS>");
        controlCharacterMap.put((byte) 0x09, "<TAB>");
        controlCharacterMap.put((byte) 0x0A, "<LF>");
        controlCharacterMap.put((byte) 0x0B, "<VT>");
        controlCharacterMap.put((byte) 0x0C, "<FF>");
        controlCharacterMap.put((byte) 0x0D, "<CR>");
        controlCharacterMap.put((byte) 0x0E, "<SO>");
        controlCharacterMap.put((byte) 0x0F, "<SI>");
        controlCharacterMap.put((byte) 0x10, "<DLE>");
        controlCharacterMap.put((byte) 0x11, "<DC1>");
        controlCharacterMap.put((byte) 0x12, "<DC2>");
        controlCharacterMap.put((byte) 0x13, "<DC3>");
        controlCharacterMap.put((byte) 0x14, "<DC4>");
        controlCharacterMap.put((byte) 0x15, "<NAK>");
        controlCharacterMap.put((byte) 0x16, "<SYN>");
        controlCharacterMap.put((byte) 0x17, "<ETB>");
        controlCharacterMap.put((byte) 0x18, "<CAN>");
        controlCharacterMap.put((byte) 0x19, "<EM>");
        controlCharacterMap.put((byte) 0x1A, "<SUB>");
        controlCharacterMap.put((byte) 0x1B, "<ESC>");
        controlCharacterMap.put((byte) 0x1C, "<FS>");
        controlCharacterMap.put((byte) 0x1D, "<GS>");
        controlCharacterMap.put((byte) 0x1E, "<RS>");
        controlCharacterMap.put((byte) 0x1F, "<US>");
        controlCharacterMap.put((byte) 0x7F, "<DEL>");
    }

    private CharacterUtils() {
        throw new UnsupportedOperationException("CharacterUtils is a utility class and cannot be instantiated");
    }

    // These are the values for control characters: 0x00 - 0x1F and 0x7F
    public static boolean isControlCharacter(char c) {
        return c <= 0x1F || c == 0x7F;
    }

    /*
        Important to note that passing {0x20, 0x09, 0x0A, 0x0D} in our initial byte array will return true when
        isRFCWhiteSpace() checks that character because under the hood it will look at the binary representation(byte value)
     */
    public static boolean isRFCWhiteSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    public static String mapToText(byte value) {
        return controlCharacterMap.getOrDefault(value, "Unknown Control Character");
    }
}
