package org.example;

import org.example.decoder.Decoder;
import org.example.parser.Parser;
import org.example.tokenizer.Tokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/*
    THE MOST FUNDAMENTAL THING TO ALWAYS REMEMBER is to ALWAYS provide the precompiled sequence of characters to the
    parser. In Java escape character/sequences are converted by the compiler at compile time to their corresponding
    character. This is not the expected behaviour according to rfc that states that a parser must determine that those
    2 strings are equal "a\\b" and "a\u005Cb" which will not be possible for the parser since at compile the unicode code
    sequence will be converted. Instead, we sent {'a', '\', 'u', '0', '0', '5', 'c', 'b'} and we let the parser do the
    conversion. This is very important because RFC states that we can represent Json strings with unicode escape sequences

    For example, this jsonText = "\"\\\\\\\\\\\\\\\\\\\\\\\\\\u\""; consists of 26 '\' which at runtime are converted
    to 13 backslash literals and 'u'. If this is the input to the parser, then the parser will try to match every 2
    backslash characters to a backslash literal. In this case, we will have 6 pairs and left with '\' and 'u' which is
    not a valid sequence while initially the json text was valid. The expected behaviour is to send all 26 backlashes
    to the parser, let the parser interpret that to 13 backslash literals and then u.

    When we have String str = "\"A\uD83D\uDE00Bé\"" and we call getBytes(StandardCharsets.UTF_8) java will give us this
    output array bytes = [34, 65, -16, -97, -104, -128, 66, -61, -87, 34]. If we look close the 2 unicode escape sequences
    fall into the range of surrogate pairs. These sequences do not represent standalone characters but must be combined
    into a single Unicode code point. [", A, �, �, B, é, "]. The byte representation under the hood will have the values
    [34, 65, 55357, 56832, 66, 34]. The array might look like this [", A, �, �, B, é, "] but under the hood the � have
    the '\uD83D' and '\uDE00', it is just according to rfc since they are surrogate pair they can't represent any character.

    This is the expected behaviour.
    UTF-8 encoding character numbers between U+D800 and U+DFFF, which are reserved for use with the UTF-16 encoding form
    (as surrogate pairs) and do not directly represent characters. We represent this character with �

    Why those 2 arrays are the same and represent: A😀Bé
    We provide this string  A😀Bé, UTF-8 encodes it into the array [34, 65, -16, -97, -104, -128, 66, -61, -87, 34].
    We decode, we tokenize [", A, �, �, B, é, "] and as mentioned � holds under the hood its own unicode hex value.
    (valid surrogate)

    This is the array {34, 65, 92, 117, 68, 56, 51, 68, 92, 117, 68, 69, 48, 48, 66, -61, -87, 34} for the same value:
    A😀Bé, but now it was provided as "A\uD83D\uDE00Bé\" the server will convert the 2 unicode sequences to � but under
    the hood as mentioned they will hold their unicode code point(valid surrogate)

    Shortly to encode this string "A😀Bé" UTF-8 looks at 😀 and its Unicode code point U+1F600. It needs 4 bytes to
    represent this character. The process is fully explained in the text file(leading and contribution bytes), on how
    those 4 byte sequence is created [-16, -97, -104, -128]. When we want to decode, we check byte by byte we determine
    that those 4 bytes are part of the same sequence and replace with the actual character. We just look at the binary
    representation of -16 which is (256 - 16) = 240 -> 11110000 4 1s at the start 4 bytes, the rest is fully explained
    in the text file.

    This string String str = "\"A\uD83D\uDE00Bé\"" is equal to "A😀Bé". In our parser we read the unicode escape sequences
    we determine if they are high/low surrogates and then combine to corresponding Unicode code point D83D and DE00 are
    U+1F600(😀). In our byte array we process \uD83D, \uDE00, determine high/low, and combine them.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        // "\"A\\uD83D\\uDE00Bé\"" utf-8 values
        byte[] bytes = {34, 65, 92, 117, 68, 56, 51, 68, 92, 117, 68, 56, 51, 68, 66, -61, -87, 34};
        Decoder decoder = new Decoder();
        String jsonText = decoder.decode(bytes);
        jsonText = "{  \"key\" : \"value\", \"key1\" : \"value\" }";
        // toDo: for according to the string below how it is still valid and how we successfully we deal with all the whitespace added
        jsonText = """
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
                "IDs": [116, 943, 234, 38793]
              }
          }""";
        jsonText = "{\"xd\": 1}";
        jsonText = Files.readString(Path.of("src/main/java/org/example/test.json"));
        System.out.println(jsonText.substring(5380, 5500));
        Tokenizer tokenizer = new Tokenizer(jsonText.toCharArray());

        Parser parser = new Parser(tokenizer);

        parser.parse();
        System.out.println();
    }
}