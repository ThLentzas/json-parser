package org.example.tokenizer;

import org.example.exception.UnexpectedCharacterException;
import org.example.exception.UnrecognizedTokenException;
import org.example.exception.UnterminatedValueException;

import java.util.ArrayList;
import java.util.List;

public final class Tokenizer {
    private char[] buffer;
    private int position;
    private List<TokenizerToken> tokens;
    private int tokenIndex;

    public Tokenizer(char[] buffer) {
        this.buffer = buffer;
        this.tokens = new ArrayList<>();
    }

    public TokenizerToken nextToken() {
        tokenize();
        return this.tokens.isEmpty() ? null : this.tokens.get(this.tokenIndex - 1);
    }

    // toDO: Explain why we conditionally add number, string, boolean, null and not the structural characters. Also why
    //  we dont check for unicode code points for anything else other than string, review all the comments
    /*
        Lexical errors occur when individual tokens in the input do not conform to the rules of valid tokens in the
        JSON specification. It is the responsibility of the tokenizer to handle those errors. These errors will be handled
        by the tokenizer, the structural ones will be handled by the parser

            Invalid boolean: felse
            Unterminated string: "John Doe
            Invalid number format: 30.(a number cannot end with a decimal point)
            Unrecognized Character: our default case
     */
    private void tokenize() {
        if (this.buffer.length == 0) {
            return;
        }

        switch (buffer[this.position]) {
            // We don't use isWhiteSpace() because Java and JSON RFC do not consider the same characters as whitespace
            case ' ', '\n', '\r', '\t' -> this.position++;
            case '{' ->
                    this.tokens.add(new TokenizerToken(this.position, this.position++, TokenizerTokenType.LEFT_CURLY_BRACKET));
            case '}' ->
                    this.tokens.add(new TokenizerToken(this.position, this.position++, TokenizerTokenType.RIGHT_CURLY_BRACKET));
            case '[' ->
                    this.tokens.add(new TokenizerToken(this.position, this.position++, TokenizerTokenType.LEFT_SQUARE_BRACKET));
            case ']' ->
                    this.tokens.add(new TokenizerToken(this.position, this.position++, TokenizerTokenType.RIGHT_SQUARE_BRACKET));
            case ':' -> this.tokens.add(new TokenizerToken(this.position, this.position++, TokenizerTokenType.COLON));
            case ',' -> this.tokens.add(new TokenizerToken(this.position, this.position++, TokenizerTokenType.COMMA));
            // signs, decimal point and exponential notation
            case '-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', 'e', 'E' -> {
                int initialPosition = this.position;
                tokenizeNumber();
                this.tokens.add(new TokenizerToken(initialPosition, this.position - 1, TokenizerTokenType.NUMBER));
            }
            case '"' -> {
                int initialPosition = this.position;
                tokenizeString();
                this.tokens.add(new TokenizerToken(initialPosition, this.position - 1, TokenizerTokenType.STRING));
            }
            case 'f', 't' -> {
                int initialPosition = this.position;
                tokenizeBoolean();
                this.tokens.add(new TokenizerToken(initialPosition, this.position, TokenizerTokenType.BOOLEAN));
            }
            case 'n' -> {
                int initialPosition = this.position;
                tokenizeNull();
                this.tokens.add(new TokenizerToken(initialPosition, this.position, TokenizerTokenType.NULL));
            }
            default -> throw new UnrecognizedTokenException("Unrecognized token: " + this.buffer[this.position]);
        }
        this.tokenIndex++;
    }

    /*
        We include the scientific notation as part of the number representation. Both E+ and E indicate a positive
        exponent in the number, while E- a negative one. (times 10 to the power of)

        -123.45E-6 and -123.45e-6 are the same number: −0.00012345 -> times 10 to the power of -6
        -123.45E+6, -123.45E6, -123.45e+6 and -123.45e6 are the same number: −123450000
        Same logic applies for positive ones.

        If we have something like "1E3" which is an integer we can not call Integer.parseInt() because parenInt() is
        strictly for integer values, and the input string must represent a plain integer without any fractional
        or scientific notation. We work with Double.

        String text = "123.45e-6         ";
        This is not considered a valid number because there are whitespaces that are not considered insignificant.
        Insignificant whites spaces according to rfc are the ones before and/or after the 6 structural characters({}[],:)

        NaN and -+Infinity are not valid values for JSON Number according to rfc
    */
    private void tokenizeNumber() {
        if (buffer[this.position] == '+') {
            throw new UnexpectedCharacterException("JSON specification prohibits numbers from being prefixed with a plus sign");
        }

        if (buffer[this.position] == '-') {
            tokenizeNegative();
            return;
        }
        tokenizePositive();
    }

    private void tokenizeNegative() {
        // After the '-' sign we need a digit
        if (this.position + 1 == this.buffer.length) {
            throw new UnexpectedCharacterException("A valid numeric value requires a digit (0-9) after the minus sign");
        }

        if (!(this.buffer[this.position + 1] >= '0' && buffer[this.position + 1] <= '9')) {
            throw new UnexpectedCharacterException("A valid numeric value requires a digit (0-9) after the minus sign");
        }

        // The only valid number that we can have that starts with -0 is the one followed by '.'(-0.3). Any other case is
        // considered leading zero.
        if (this.buffer[this.position + 1] == '0'
                && this.position + 2 != this.buffer.length
                && this.buffer[this.position + 2] != '.') {
            throw new UnexpectedCharacterException("Leading zeros are not allowed");
        }

        this.position++;
        tokenizerNumberHelper();
    }

    private void tokenizePositive() {
        // If the next character after 0 is not '.'(0.3) it is considered a leading zero
        if (this.buffer[this.position] == '0'
                && this.position + 1 != buffer.length
                && this.buffer[this.position + 1] != '.') {
            throw new UnexpectedCharacterException("Leading zeros are not allowed");
        }

        // .3 not valid
        if (this.buffer[this.position] == '.') {
            throw new UnexpectedCharacterException("A leading digit is required before a decimal point");
        }

        tokenizerNumberHelper();
    }

    // toDo: precision

    /**
     * After either {@link #tokenizeNegative()} or {@link #tokenizePositive()} confirmed that the number has a valid
     * initial structure, this method verifies the remaining characters to ensure they adhere to the JSON Number
     * specification.
     *
     * @throws UnexpectedCharacterException 1) The decimal point is not followed by a digit 2) The exponential notation
     *                                      is not followed by a digit 3) We encountered an expected character for JSON Number
     */
    private void tokenizerNumberHelper() {
        boolean endOfNumber = false;
        while (!endOfNumber && this.position < this.buffer.length) {
            switch (this.buffer[this.position]) {
                // We can still encounter '-', '+' as part of the exponential notation
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+' -> this.position++;
                case '.' -> {
                    // After '.' only digits are valid(12.e5 is not valid)
                    if (!(this.buffer[this.position + 1] >= '0' && this.buffer[this.position + 1] <= '9')) {
                        throw new UnexpectedCharacterException("Decimal point must be followed by a digit");
                    }
                    this.position++;
                }
                case 'e', 'E' -> {
                    /*
                        e4, e+4, e-5 are not valid without a leading digit
                        if (this.position == 0) {
                            throw new UnexpectedCharacterException("A leading digit is required before exponent notation");
                        }
                        Initially i had the above logic, but just e or E do not indicate a number starting with the
                        exponential notation, we will consider that case as unrecognized character
                     */
                    // When e or E is the last character of the buffer including e-, e+, E-, E+  and are not followed by
                    // a digit
                    if (this.position + 1 == this.buffer.length
                            || (this.position + 2 == this.buffer.length
                            && (this.buffer[this.position + 1] == '+' || this.buffer[this.position + 1] == '-'))) {
                        throw new UnexpectedCharacterException("Exponential notation must be followed by a digit");
                    }
                    this.position++;
                }
                // We encountered an unexpected character
                default -> endOfNumber = true;
            }
        }
        // A number can not end in a decimal point
        if (this.buffer[this.position - 1] == '.') {
            throw new UnexpectedCharacterException("Decimal point must be followed by a digit");
        }

        // If endOfNumber is true before we are done iterating over the buffer case we encountered an unexpected
        // character. e.g. 1234a
        if (endOfNumber) {
            throw new UnexpectedCharacterException("Unexpected character " + this.buffer[this.position] + " found at " + this.position);
        }
    }

    /**
     * Rfc string structure: string = quotation-mark *char quotation-mark. It is mandatory to start and end with '"' the
     * character literal and not the corresponding unicode code point(\u0022) quotation-mark *char quotation-mar
     * (*char means zero or more characters)
     * <br>
     * <br>
     * JSON mandates that strings to be enclosed in double quotes. "Hello World" is a valid JSON string, but to pass
     * this string as a parameter we must escape the double quotes. String jsonText = "\"Hello World\""; When we iterate
     * though the characters of the string our tokenizer will look for " that will indicate the start of the string but
     * if we pass just "Hello World" then it would be just the characters Hello World and the tokenizer will not know
     * what to do.
     *
     * @throws UnexpectedCharacterException if the strings does not end with quotation mark '"'
     */
    private void tokenizeString() {
        this.position++;
        while (this.position < this.buffer.length) {
            // Check if the current character is a backslash ('\'), which indicates the start of a potential unicode escape sequence.
            // We use '\\' because this how we represent a literal backslash.
            if (this.buffer[this.position] == '\\') {
                handleEscapeCharacter();
            }
            this.position++;
        }

        // Case: "(1 character), any string that does not end with "
        if (this.buffer.length == 1 || this.buffer[this.position - 1] != '"') {
            throw new UnterminatedValueException("Unterminated value for: " + TokenizerTokenType.STRING);
        }
    }

    /*
        jsonText = "a\\b" indicates that the second backslash is treated as a literal backslash ('\'), rather than the
        start of an escape sequence. As a result, the parsed string becomes "a\b". Since the backslash is escaped, the
        sequence "\b" is not interpreted as the escape character for backspace, but instead as a backslash followed by
        the character 'b'.

        jsonText = "a\u005Cb" It is the same logic as above, '\' indicates the start of a sequence and u indicates the
        start of unicode character which in this case is '\'. As a result we also have "a\\b" and then we follow the same
        logic as above. Our parser will convert the unicode sequence \u005C to its corresponding character '\' and replace
        it into the string
     */
    private void handleEscapeCharacter() {
        // Even number of '\' is only allowed if the next character is 'u' which indicates the potential of unicode code sequence
        int count = findConsecutiveBackslashes();
        // We advanced the pointer, and now we need to check if there are still enough characters. In valid strings that
        // end with '"' this condition will never be true, but on an invalid string it will. str = "a\\\\ -> invalid,
        // str = "a\\\\" -> valid
        if (this.position == this.buffer.length) {
            throw new UnexpectedCharacterException("Incomplete character escape sequence");
        }
        /*
            Handling consecutive backslashes:
            Parsers will treat 2 consecutive backslashes as an escape character for backslash and will convert those
            into a single backslash literal.

                Even: If the number of consecutive backslashes is even it means we have count / 2 pairs of backslashes.
                We will replace every pair of backslashes with a backslash literal("\\\\", "\\"). This means that since
                the last backslash is treated as backslash literal the next character can be anything.
                ['"', '\', '\', 'q' '"'] will be transformed ['"', '\', 'q', '"']. This is valid despite the fact that after
                the backslash we have 'q' because in this case the backslash is treated as a literal and not as an escape
                character

                Odd: If the number of consecutive backslashes is even it means we have count / 2 pairs of backslashes + 1
                Now that we have 1 backslash left that is not considered a backslash literal we need the next character
                of the buffer to be a valid escape character
                ['"', '\', '\', '\', 'n' '"'] will be transformed ['"', '\', (unpaired '\'), 'n', '"']. This will only
                be valid if the next character is a valid escape character. In any other case we will throw. If the
                character is valid then we pair the 2 and overwrite the value '\' and 'n' will be mapped to a single
                character '\n'

            Resetting index after replacing backlashes:

            After successfully replacing every 2 backslashes with a backslash literal we need to reset the position in
            the array. What we need to remember is that since we count consecutive backslashes, the number of backslashes
            (n) and the interval of the starting index and the ending index of this.position will be the same.
                Even: ['"', '\', '\', 'q' '"'], this.position = 1, (1st index we found a backslash). We traverse until
                we find a non-backslash character. We stop at 'q', this position = 3. We advanced this.position by n, now since
                we are replacing n with n / 2 we also need to adjust this.position. In the case of even, we know that
                we don't have a remaining unpaired backslash. ['"', '\', 'q', '"'] after halving this.position, it will
                be at index 1 which is '\'. This is the intended behaviour because then the flow will return to tokenizeString()
                and this.position++; will be executed moving this.position to the next character.

                Odd: ['"', '\', '\', '\', 'n' '"'], this.position = 1, (1st index we found a backslash). We traverse until
                we find a non-backslash character. We stop at 'n', this position = 4. We advanced this.position by n, now since
                we are replacing n with n / 2 we also need to adjust this.position. ['"', '\', (unpaired '\'), 'n', '"']
                First since the count is odd, we check the character that this.position points to because we know we have
                an unpaired backslash and the next character must be a valid escape character.
                Once we know that the next character is a valid escape character we reduced the number of backslashes from n
                to n / 2 + 1(unpaired). We mimic this behaviour for this.position. this.position = this.position / 2 + 1
                this.position = 3. Position now is at index 3, the unpaired backslash. At this point we need to check
                the next character, we already know that this character is a valid escape character, so we advance the
                index one more time. Overall, this.position = this.position / 2 + 1(unpaired) + 1(next character)
         */
        if (count % 2 != 0) {
            if (!isEscapeCharacter(this.buffer[this.position])) {
                throw new UnexpectedCharacterException("Unexpected escape character: " + this.buffer[this.position]);
            }

            /*
                Case: ['"', '\', '"']
                For any odd number of backslashes where the unpaired backslash is followed by '"' and the '"' is
                the last character of the string we must throw an exception it is not a valid string.

                What happens if we don't consider this case?
                When we try to merge '\' and '"' as explained in mapEscapeCharacter(), we get '\"' which is '"' as a
                character not as the start/end of a string. Our iteration ends ['"', '"'] we look at the last character
                of the string to see if it is '"' to verify that we have enclosing quotation mark and it passes. This is
                not the expected behaviour the above input is not valid, it is a string that starts with '"', valid so
                far then it has an escaped character '"' but not enclosing quotation mark. If the escaped character
                that was found is '"' and there are no more characters in the string to potentially have a valid string
                we need to throw
             */
            if (this.buffer[this.position] == '"' && this.position + 1 == this.buffer.length) {
                throw new UnexpectedCharacterException("Unexpected escape character: " + this.buffer[this.position]);
            }
            /*
                Case count = 1: When count = 1 there is no need to replace backslashes there is only 1 consecutive
                backslash, and we don't need to adjust the position of the index. It points to the character that broke
                the sequence which is the one after the single backslash
             */
            if (count != 1) {
                this.buffer = String.valueOf(this.buffer).replace("\\\\", "\\").toCharArray();
                this.position = this.position / 2 + 2;
            }
            if (this.buffer[this.position] == 'u') {
                handleUnicodeEscapeSequence();
                return;
            }
            mapEscapeCharacter();
            return;
        }
        this.buffer = String.valueOf(this.buffer).replace("\\\\", "\\").toCharArray();
        this.position /= 2;
    }

    /*
        For input ['"', '\', 'b', '"'] or any odd number of backslashes where we have an unpaired/unescaped backslash
        we need to 'merge' it with the following escape character.
        Output ['"', '\b', '"']
        this.position is at the escape character, we overwrite the value at this.position - 1 with the escape merged
        escape character.
     */
    private void mapEscapeCharacter() {
        char c;
        switch (this.buffer[this.position]) {
            case 'b' -> c = '\b';
            case 't' -> c = '\t';
            case 'n' -> c = '\n';
            case 'f' -> c = '\f';
            case 'r' -> c = '\r';
            // The 2 possible remaining cases, remain the same (", /)
            default -> c = this.buffer[this.position];
        }
        this.buffer[this.position - 1] = c;
        // To correctly insert the escape character, we create a substring from the start of the array until the index
        // of the backslash(exclusive), then we append the new merged escape character and then the remaining part of
        // the array. this.position is at the index of the character(- 1 is the backslash, + 1 the remaining array)
        this.buffer = (String.valueOf(this.buffer).substring(0, this.position - 1)
                + c
                + String.valueOf(this.buffer).substring(this.position + 1)).toCharArray();
        /*
            Since we merged the 2 characters we need to reset the index.
            ['"', '\', 'b', '"'] Initially this.position is at 'b' after merging ['"', '\b', '"'] this.position would
            be at '"' index 2 in both cases. If we don't reset the index, the flow returns back to tokenizeString() which
            increments the position. If we don't reset the index we will miss on 1 character.
         */
        this.position--;
    }

    /*
        The 1st if check is a bit tricky. When we have this input buffer = ['"', 'A', '\', 'u', '0', '0', 'E', '"'], the
        check this.position + 4 >= this.buffer.length is actually false because after 'u' there are 4 characters left
        that could potentially form a hex sequence ('0', '0', 'E', '"'). It is the 2nd if() that will catch this error
        by checking isHexCharacter('"'). The 1st if will catch input buffers like ['"', 'A', '\', 'u', '0', '0', 'E']
     */
    private void handleUnicodeEscapeSequence() {
        // There are not enough characters left in the string to represent a valid unicode escape sequence
        if (this.position + 4 >= this.buffer.length) {
            throw new UnexpectedCharacterException("A hex-digit was expected in the character escape sequence");
        }

        if (isHexCharacter(this.buffer[this.position + 1])
                && isHexCharacter(this.buffer[this.position + 2])
                && isHexCharacter(this.buffer[this.position + 3])
                && isHexCharacter(this.buffer[this.position + 4])) {
            String hexSequence = "" + this.buffer[this.position + 1]
                    + this.buffer[this.position + 2]
                    + this.buffer[this.position + 3]
                    + this.buffer[this.position + 4];
            /*
                Why this narrowing conversion is allowed here without any data loss? (char) codePoint

                We know that our unicode sequence will have 4 hex digits and the maximum value we can have with 4 digits
                is FFFF which results in 65535. char is an unsigned 16-bit value, with a valid range of 0 to 65535 (0x0000 to 0xFFFF).
                Since the maximum value of a 4-digit hexadecimal sequence (FFFF) fits within the range of char, there is
                no risk of data loss during this narrowing conversion.

                Assigning an int to a char gives us the character representation of the int's Unicode value
                codepoint = 65 -> c = 'A'. If the int value is in the range of surrogate pairs -> c = '�' It can also have
                unicode escape sequence as value because they represent a single character c = '\uD838'
                char c = (char) codePoint;
             */
            int codePoint = Integer.parseInt(hexSequence, 16);
            /*
                For the hexSequence we just formed we can get the numeric value hexSequence = "D83D" -> codepoint = 55357
                Valid surrogate pairs always have the same order (High - Low)

                Case: High Surrogate -> call handleSurrogatePair()
                Case: Low Surrogate or character in BMP, convert the hex sequence to the corresponding character. For
                low surrogates since they don't represent any character by themselves we get '�'
             */
            if (Character.isSurrogate((char) codePoint) && (Character.isHighSurrogate((char) codePoint))) {
                handleSurrogatePair(codePoint);
                return;
            }
            /*
                At this point we either have a low surrogate or BMP character. In either case we need to replace the
                unicode escape sequence with the corresponding character. This is vital to follow the rfc string comparison
                section where "a\\b" and "a\u005Cb" must be equal.

                We need to replace \u005C with '\', this.position is at 'u'. To correctly insert the converted character,
                we create a substring from the start of the array until this.position - 1 which includes all the characters
                until the start of the escape sequence, then we append the new converted character and then the remaining
                part of the array which starts at this.position + 5 to not include the unicode sequence u005C. Note that
                we don't overwrite the values of the buffer, but we create a new char[] instead.

                Resetting the position:
                What we did was to keep all the characters before the escape sequence, then add the new character at
                the position of '\' and then keep all the characters after the sequence. Our index is currently at
                wherever 'u' was. We need this.position to point to the new converted character so when the flow returns
                to tokenizeString() to increment this.position, so we can keep iterating over the array. Since this.position
                is at the index where 'u' previously was, and we inserted the new character at the position of '\' we just
                need to decrement this.position by 1 to point to the new character

                "a\u005Cb" this.position = 2 -> replace the sequence \u005C -> "a\b" -> position must point to the new
                character, position - 1
             */
            this.buffer = (String.valueOf(this.buffer).substring(0, this.position - 1)
                    + String.valueOf(Character.toChars(codePoint))
                    + String.valueOf(this.buffer).substring(this.position + 5)).toCharArray();
            this.position--;
            // Not a valid hex number
        } else {
            throw new UnexpectedCharacterException("A hex-digit was expected in the character escape sequence");
        }
    }

    private void handleSurrogatePair(int highSurrogate) {
        // There at least enough characters to potentially form a pair
        if (this.position + 10 < this.buffer.length
                && this.buffer[this.position + 5] == '\\'
                && this.buffer[this.position + 6] == 'u') {
            /*
                From the call to handleUnicodeEscapeSequence() we know that since this.position is at 'u' we looked at
                the next 4 characters and created a valid unicode escape sequence which was a high surrogate. We have
                4 choices for the next 6 characters
                    1. Valid high surrogate
                    2. Valid low surrogate
                    3. Valid BMP
                    4. Invalid unicode sequence

                To form the following sequence we know that if this.position is at 'u',  the next sequence will start
                from this.position + 5, as we skip u + the 4 hex digits, this.position + 5 is the backslash to indicate
                the start of the next sequence, this.position + 6 will be the new u, and in 7, 8, 9, 10 will be the
                4 new hex digits.
             */
            if (isHexCharacter(this.buffer[this.position + 7])
                    && isHexCharacter(this.buffer[this.position + 8])
                    && isHexCharacter(this.buffer[this.position + 9])
                    && isHexCharacter(this.buffer[this.position + 10])) {
                String hexSequence = "" + this.buffer[this.position + 7]
                        + this.buffer[this.position + 8]
                        + this.buffer[this.position + 9]
                        + this.buffer[this.position + 10];

                int codePoint = Integer.parseInt(hexSequence, 16);
                // Case 1: Surrogate pair
                // Case 2: 2 High surrogates in a row
                String str;
                /*
                    We have 2 codepoints, the highSurrogate which is passed from handleUnicodeEscapeSequence() and the
                    one we just compute for the next surrogate we found. The following process is explained in detail in
                    the method above handleUnicodeEscapeSequence() when we were converting a unicode sequence to a BMP
                    character.
                    While previously we only converted 1 character, now we have to handle 2. If it is a surrogate
                    pair or 2 high surrogates in a row, the array will have ' �', ' �' because in UTF_8 surrogates by themselves
                    don't correspond to any character. When we decode them in the case of a surrogate pair we will get
                    the correct character. (�, � under the hood hold the unicode values, so they are encoded correctly)
                    e.g. 2 high surrogates: "A\uD83D\uD83DBé" -> [", A, �, �, B, é, "] = [34, 65, '\uD83D'(55357), '\uD83D'(55357), 66, 233, 34]

                    Note: Previously we reset the index(this.position) now we don't because if we have surrogate pair
                    or 2 surrogate highs we insert 2 characters in the array. We know that this.position is at 'u' and
                    we insert (�,  �) in the positions of ('\' previous of position) and ('u') so this.position points
                    to the correct character
                 */
                if (Character.isLowSurrogate((char) codePoint)) {
                    int surrogatePair = Character.toCodePoint((char) highSurrogate, (char) codePoint);
                    str = String.valueOf(this.buffer).substring(0, this.position - 1)
                            + String.valueOf(Character.toChars(surrogatePair))
                            + String.valueOf(this.buffer).substring(this.position + 11);
                } else {
                    // BMP
                    str = String.valueOf(this.buffer).substring(0, this.position - 1)
                            + String.valueOf(Character.toChars(highSurrogate))
                            + String.valueOf(Character.toChars(codePoint))
                            + String.valueOf(this.buffer).substring(this.position + 11);
                }
                this.buffer = str.toCharArray();
                return;
            } else {
                throw new UnexpectedCharacterException("Unexpected character. A hexadecimal digit was expected as part of the character escape sequence");
            }
        }
        /*
            This is the case where we had the previous high surrogate, but it was not followed by a unicode sequence.
            We simply convert the high surrogate to (�) as explained in detail handleUnicodeEscapeSequence() for the
            BMP/low surrogate character case including why we need to reset the position.
         */
        this.buffer = (String.valueOf(this.buffer).substring(0, this.position - 1)
                + String.valueOf(Character.toChars(highSurrogate))
                + String.valueOf(this.buffer).substring(this.position + 5)).toCharArray();
        this.position--;
    }

    // According to the JSON rfc: The hexadecimal letters A through F can be uppercase or lowercase.
    private boolean isHexCharacter(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F' || (c >= 'a' && c <= 'f'));
    }

    // These characters are considered valid escape characters according to rfc
    private boolean isEscapeCharacter(char c) {
        return c == '"'
                || c == '/'
                || c == 'b'
                || c == 'f'
                || c == 'n'
                || c == 'r'
                || c == 't'
                || c == 'u';
    }

    private void tokenizeBoolean() {
        if (this.buffer[this.position] == 'f') {
            tokenizeFalse();
        } else {
            tokenizeTrue();
        }
    }

    /*
        If we pass more or less characters than false they are not valid JSON Boolean values.
        For example, "falses" or "fals" or "false  "(trailing whitespaces)
     */
    private void tokenizeFalse() {
        if (this.buffer.length != 5) {
            throw new UnrecognizedTokenException("Unrecognized token: " + String.valueOf(this.buffer));
        }

        if (this.buffer[++this.position] != 'a'
                || this.buffer[++this.position] != 'l'
                || this.buffer[++this.position] != 's'
                || this.buffer[++this.position] != 'e') {
            throw new UnrecognizedTokenException("Unrecognized token: " + String.valueOf(this.buffer));
        }
    }

    /*
        If we pass more or less characters than true, they are not valid JSON Boolean values.
        For example, "truef" or "tru" or "true  "(trailing whitespaces)
     */
    private void tokenizeTrue() {
        if (this.buffer.length != 4) {
            throw new UnrecognizedTokenException("Unrecognized token: " + String.valueOf(this.buffer));
        }

        if (this.buffer[++this.position] != 'r'
                || this.buffer[++this.position] != 'u'
                || this.buffer[++this.position] != 'e') {
            throw new UnrecognizedTokenException("Unrecognized token: " + String.valueOf(this.buffer));

        }
    }

    /*
        If we pass more or less characters than null, they are not valid JSON Null.
        For example, "nullt" or "nul" or "null  "(trailing whitespaces)
     */
    private void tokenizeNull() {
        if (this.buffer.length != 4) {
            throw new UnrecognizedTokenException("Unrecognized token: " + String.valueOf(this.buffer));
        }

        if (this.buffer[++this.position] != 'u'
                || this.buffer[++this.position] != 'l'
                || this.buffer[++this.position] != 'l') {
            throw new UnrecognizedTokenException("Unrecognized token: " + String.valueOf(this.buffer));
        }
    }

    private int findConsecutiveBackslashes() {
        int count = 0;

        while (this.position < this.buffer.length && this.buffer[this.position] == '\\') {
            count++;
            this.position++;
        }
        return count;
    }
}
