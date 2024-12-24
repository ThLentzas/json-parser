package org.example.tokenizer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.example.exception.IllegalControlCharacterException;
import org.example.exception.UnexpectedCharacterException;
import org.example.exception.UnrecognizedTokenException;
import org.example.exception.UnterminatedValueException;

public final class Tokenizer {
    private char[] buffer;
    private int position;
    /*
       Supportive Index for Improved Error Context

       Consider the input: "{123a : null}"
       After reading '{', the parser expects a double-quoted key. Instead, it encounters "123a".
       The tokenizer tries to interpret "123a" as a number and fails upon reading 'a', which is not valid in a number.
       Without additional context, the tokenizer might throw an exception like:

       "Position 4: Unexpected character: 'a'"

       However, this message does not convey the actual parsing context: the parser expected a string key,  not a
       malformed number. To improve error clarity, we use a supportive index that records the start position of the
       current token. This index allows the parser to catch tokenizer exceptions and rethrow them as more meaningful
       parser-level errors, for example:

       "Position 1: Unexpected character: '1' Expected: double-quoted value for object name"

       This way, we can show error messages that accurately reflect the parser's expectations at the point where the
       token began, rather than where the tokenizer finally failed.

       Note: To handle the above cases we would wrap the call to nextToken() in a try/catch block and throw our Malformed
       structure exception. I had this logic in this commit: 1d6e0de4fcbab3368ebaaeb763b0b2575b7d7ef2. I ended up doing
       it only for the cases of trailing characters.
    */
    private int initialPosition;
    private final List<TokenizerToken> tokens;
    /*
        https://www.baeldung.com/java-deque-vs-stack

        The use of stack helps us when tokenizing numbers like 2, 3] 3}. The characters ',', ']' and '}' are only allowed
        after a number when there is an array or an object. Look at tokenizeNumberHelper()
     */
    private final Deque<Character> stack;

    public Tokenizer(char[] buffer) {
        this.buffer = buffer;
        /*
            Why do we use a LinkedList instead of an ArrayList?

            LinkedLists have the problem with caching as we know, but we only iterate though the list once when we navigate
            Adding tokens to the list is efficient because we never have to resize
            ArrayList may require resizing and copying to a larger array as it grows, which can be inefficient for a
            large number of tokens
         */
        this.tokens = new LinkedList<>();
        stack = new ArrayDeque<>();
    }

    public TokenizerToken nextToken() {
        if (this.position >= this.buffer.length) {
            return null;
        }

        /*
            When peek() is called by parser's assertNoTrailingCharacters() we need to consider the case 3 mentioned in
            that method.
            "{}\t\b\r\n   " Any whitespace characters after structural characters are considered insignificant.

            Without if:
                1. After parsing {}, the tokenizer's position is right after }.
                2. The input still contains whitespace. The tokenizer calls tokenize(), which skips the whitespace and
                reaches the end of the input without producing any new tokens.
                3. peek() then tries to revert the position and mark peeked = true, but since no new token was produced,
                 it ends up returning the last token '}' incorrectly. Control returns to assertNoTrailingCharacters()
                 and despite not having a new token peek() returns '}'. For assertNoTrailingCharacters() means that
                 a token was found and as a trailing character, and it considers it invalid.
            By checking the count before and after in this case we make sure that we handle this case gracefully
         */
        int tokenCount = this.tokens.size();
        tokenize();
        if (tokenCount == this.tokens.size()) {
            return null;
        }
        return this.tokens.get(this.tokens.size() - 1);
    }

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
        skipWhiteSpace();
        if (this.position == this.buffer.length) {
            return;
        }

        this.initialPosition = this.position;
        switch (this.buffer[this.position]) {
            case '{' -> {
                this.stack.push(this.buffer[this.position]);
                this.tokens.add(new TokenizerToken(this.position, this.position, TokenizerTokenType.LEFT_CURLY_BRACKET));
            }
            case '}' -> {
                if (stack.isEmpty()) {
                    throw new UnexpectedCharacterException("Position: " + this.position + ". Unexpected character: '}'");
                }
                this.stack.pop();
                this.tokens.add(new TokenizerToken(this.position, this.position, TokenizerTokenType.RIGHT_CURLY_BRACKET));
            }
            case '[' -> {
                this.stack.push(this.buffer[this.position]);
                this.tokens.add(new TokenizerToken(this.position, this.position, TokenizerTokenType.LEFT_SQUARE_BRACKET));
            }
            case ']' -> {
                if (stack.isEmpty()) {
                    throw new UnexpectedCharacterException("Position: " + this.position + ". Unexpected character: ']'");
                }
                this.stack.pop();
                this.tokens.add(new TokenizerToken(this.position, this.position, TokenizerTokenType.RIGHT_SQUARE_BRACKET));
            }
            case ':' -> this.tokens.add(new TokenizerToken(this.position, this.position, TokenizerTokenType.COLON));
            case ',' -> this.tokens.add(new TokenizerToken(this.position, this.position, TokenizerTokenType.COMMA));
            // signs, decimal point and exponential notation
            case '-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                tokenizeNumber();
                this.tokens.add(new TokenizerToken(initialPosition, this.position, TokenizerTokenType.NUMBER));
            }
            case '"' -> {
                tokenizeString();
                this.tokens.add(new TokenizerToken(initialPosition, this.position, TokenizerTokenType.STRING));
            }
            case 'f', 't' -> {
                tokenizeBoolean();
                this.tokens.add(new TokenizerToken(initialPosition, this.position, TokenizerTokenType.BOOLEAN));
            }
            case 'n' -> {
                tokenizeNull();
                this.tokens.add(new TokenizerToken(initialPosition, this.position, TokenizerTokenType.NULL));
            }
            default ->
                    throw new UnrecognizedTokenException("Position: " + this.position + ". Unrecognized token: '" + this.buffer[this.position] + "'. Expected: a valid JSON value");
        }
    }

    /*
        We include the scientific notation as part of the number representation. Both E+ and E indicate a positive
        exponent in the number, while E- a negative one. (times 10 to the power of)

        -123.45E-6 and -123.45e-6 are the same number: −0.00012345 -> times 10 to the power of -6
        -123.45E+6, -123.45E6, -123.45e+6 and -123.45e6 are the same number: −123450000
        Same logic applies for positive ones. When we encounter exponential notation it means take the number before
        times 10 to the number after 12e50 is 12 * 10 ^ 50

        NaN and -+Infinity are not valid values for JSON Number according to rfc
        Octal and Hex are not allowed as of RFC 8259
    */
    private void tokenizeNumber() {
        if (this.buffer[this.position] == '-') {
            tokenizeNegative();
            return;
        }
        tokenizePositive();
    }

    private void tokenizeNegative() {
        // After the minus sign we need a digit
        if (this.position + 1 == this.buffer.length) {
            throw new UnexpectedCharacterException("Position: " + this.position + ". A valid numeric value requires a digit (0-9) after the minus sign");
        }

        if (!(this.buffer[this.position + 1] >= '0' && buffer[this.position + 1] <= '9')) {
            throw new UnexpectedCharacterException("Position: " + this.position + ". Unexpected character: '" + this.buffer[this.position + 1] + "'. Expected a digit (0-9) after the minus sign");
        }

        // position + 1 is a digit, advance the pointer
        this.position++;
        tokenizerNumberHelper();
    }

    private void tokenizePositive() {
        if (this.buffer[this.position] == '+') {
            throw new UnexpectedCharacterException("Position: " + this.position + ". JSON specification prohibits numbers from being prefixed with a plus sign");
        }

        tokenizerNumberHelper();
    }

    /**
     * After either {@link #tokenizeNegative()} or {@link #tokenizePositive()} confirmed that the number has a valid
     * initial structure, this method verifies the remaining characters to ensure they adhere to the JSON Number
     * specification. Note: hex values are not allowed.
     *
     * @throws UnexpectedCharacterException 1) The decimal point is not followed by a digit 2) The exponential notation
     *                                      is not followed by a digit 3) We encountered an expected character for JSON Number
     */
    private void tokenizerNumberHelper() {
        // Handle leading zeros e.g. -01
        // Only characters allowed after 0
        //  '.': 0.1
        //  'e', 'E': 0e3, 0E3
        // We want to handle all cases that are in the form 0x where x is a number. If we get 0y where y is any other
        // character we would treat that character as Unrecognized token: 'y'
        if (this.buffer[this.position] == '0'
                && this.position + 1 != this.buffer.length
                && this.buffer[this.position + 1] != '.'
                && this.buffer[this.position + 1] != 'e'
                && this.buffer[this.position + 1] != 'E'
                && this.buffer[this.position + 1] >= '0'
                && this.buffer[this.position + 1] <= '9') {
            throw new UnexpectedCharacterException("Position: " + this.position + ". Leading zeros are not allowed");
        }

        boolean endOfNumber = false;
        boolean hasExponentialNotation = false;
        boolean hasDecimalPoint = false;
        // until we encounter a character or traversed the entire array
        while (!endOfNumber && this.position < this.buffer.length) {
            switch (this.buffer[this.position]) {
                // We can still encounter '-', '+' as part of the exponential notation
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> this.position++;
                case '.' -> {
                    handleDecimalPoint(hasDecimalPoint, hasExponentialNotation);
                    this.position++;
                    hasDecimalPoint = true;
                }
                case 'e', 'E' -> {
                    handleExponentialNotation(hasExponentialNotation);
                    this.position++;
                    hasExponentialNotation = true;
                }

                case '-', '+' -> {
                    handleSign();
                    this.position++;
                }

                // We encountered an unexpected character
                default -> endOfNumber = true;
            }
        }

        /*
            If endOfNumber is true, it means we encountered a number before the end of the input buffer. We need to
            consider 1 valid case and everything else will be considered invalid. If the stack that we keep track of the
            current element is not empty,(we either have '[' or '{' as the top element in the stack) we will let the call
            to the corresponding method parseArray() or parseObject() determine if the next character is valid or not
            It is explained below above the if condition. In any other case, including RFC whitespace characters is
            considered invalid. 5\n is invalid

            e.g. 3, -> Valid as array value or as value in an object that has more keys, invalid at the top level
                 4] -> Valid in an array, invalid at the top level
                 5} -> Valid in an object, invalid at the top level
                 6(space)  -> Valid whitespace in an array or object, invalid at the top level
                 7\n -> Valid whitespace in an array or object, invalid at the top level
                 8\t -> Valid whitespace in an array or object, invalid at the top level
                 9\r -> Valid whitespace in an array or object, invalid at the top level

           The above whitespaces between structural characters are considered insignificant so [5\n \t \r] is considered
           valid

           Any other case is considered invalid. e.g. 3a

           Why we need the condition: this.stack.isEmpty()?
               When our stack is not empty we have either an array or an object, which means that we can provide a message
               that reflects the context. For 2a: "Position: 1, Unexpected character: 'a'" is enough but for a case
               like [3[4]]: "Position: 2, Unexpected character: '['" is not enough. What we do instead is we don't check
               the next character, we tokenize it and when the logic of parseArray() expects after a value a comma and
               gets ']' it will provide a more meaningful message. Similar for object
         */
        if (endOfNumber && this.stack.isEmpty()) {
            checkForUnexpectedCharacter(this.position);
        }
        // At this point we either encountered an unexpected character or we reached the end of the array. In either case,
        // the end index of the number is the current - 1. The parser will move the index to the next token, either the
        // character or the end of the array
        this.position--;
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
     * @throws UnterminatedValueException if the strings does not end with quotation mark '"'
     */
    private void tokenizeString() {
        // Skip the opening quotation mark
        this.position++;
        while (this.position < this.buffer.length && this.buffer[this.position] != '"') {
            /*
                According to the spec for JSON String only(anywhere else can be used, for example as insignificant whitespaces
                between structural characters, look at shouldIgnoreInsignificantWhitespacesBeforeAndAfterStructuralArrayCharacters() ParserTest):

                All Unicode characters may be placed within the quotation marks, except for the characters that MUST be
                escaped: quotation mark, reverse solidus, and the control characters (U+0000 through U+001F).

                This means that this byte sequence is invalid [34, 10, 34] because the control character is passed as
                raw byte, and it is unescaped but [34, 92, 110, 34] should be considered valid as a new line character

                Invalid JSON:
                    "Hello
                    World"
                The newline character (U+000A) is raw and unescaped, making the JSON invalid.

                However, [34, 92, 110, 34] should be considered valid as the newline character is escaped:

                Valid JSON:
                "Hello\nWorld"
                Here, the newline character is represented using its escape sequence `\n`.
                REMEMBER as mentioned above main this is the raw json payload no matter the language

                In our escape character logic, when we encounter a backslash(92) we look for the next character to be
                a valid escape character. Not all control characters have a text representation. They can still be
                represented using their Unicode escape sequence (e.g., `\u000A`).
             */
            if (this.buffer[this.position] < 0x20) {
                throw new IllegalControlCharacterException("Position: " + this.position + ". Illegal control character. Control characters must be escaped");
            }
            // Start of a potential escape character/sequence.
            if (this.buffer[this.position] == '\\') {
                handleEscapeCharacter();
            }
            this.position++;
        }

        // We did not find closing quotation mark
        if (this.buffer.length == this.position) {
            throw new UnterminatedValueException("Position: " + this.position + ". Unterminated value for JSON String");
        }
        /*
            Why we need the condition: this.stack.isEmpty()?

            When our stack is not empty we have either an array or an object, which means that we can provide a message
            that reflects the context. For "str"a: "Position: 5, Unexpected character: 'a'" is enough but for a case
            like ["str"[4]]: "Position: 6, Unexpected character: '['" is not enough. What we do instead is we don't check
            the next character, we tokenize it and when the logic of parseArray() expects after a value a comma and
            gets ']' it will provide a more meaningful message. Similar for object
         */
        if (this.stack.isEmpty()) {
            checkForUnexpectedCharacter(this.position + 1);
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
        int count = findConsecutiveBackslashes();
        // We advanced the pointer, and now we need to check if there are still enough characters. In valid strings that
        // end with '"' this condition will never be true, but on an invalid string it will. str = "a\\\\ -> invalid,
        // str = "a\\\\" -> valid
        if (this.position == this.buffer.length) {
            throw new UnexpectedCharacterException("Position: " + (this.position - 1) + ". Incomplete character escape sequence");
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
                Now that we have 1 backslash left that is not considered a backslash literal. We need the next character
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

                Odd: ['"', '\', '\', '\', 'r' '"'], this.position = 1, (1st index we found a backslash). We traverse until
                we find a non-backslash character. We stop at 'r', this position = 4. We advanced this.position by n, now since
                we are replacing n with n / 2 we also need to adjust this.position. ['"', '\', (unpaired '\'), 'r', '"']
                First since the count is odd, we check the character that this.position points to because we know we have
                an unpaired backslash and the next character must be a valid escape character.
                Once we know that the next character is a valid escape character we reduced the number of backslashes from n
                to n / 2 + 1(unpaired). We mimic this behaviour for this.position. this.position = this.position / 2 + 1
                this.position = 3. Position now is at index 3, the escaped character
         */
        if (count % 2 != 0) {
            if (!isEscapeCharacter(this.buffer[this.position])) {
                throw new UnexpectedCharacterException("Position: " + this.position + ". Unexpected escape character: " + this.buffer[this.position]);
            }

            /*
                Case count = 1: When count = 1 there is no need to replace backslashes there is only 1 consecutive
                backslash, and we don't need to adjust the position of the index. It points to the character that broke
                the sequence which is the one after the single backslash
             */
            if (count != 1) {
                this.buffer = String.valueOf(this.buffer).replace("\\\\", "\\").toCharArray();
                this.position = this.position / 2 + 1;
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
        // To correctly insert the escaped character, we create a substring from the start of the array until the index
        // of the backslash(exclusive), then we append the new merged escape character and then the remaining part of
        // the array. this.position is at the index of the character(- 1 is the backslash, + 1 the remaining array)
        this.buffer = (String.valueOf(this.buffer).substring(0, this.position - 1)
                + c
                + String.valueOf(this.buffer).substring(this.position + 1)).toCharArray();
        /*
            After merging the escaped character, we must reset the position to avoid skipping any characters.
            For example, consider the initial sequence: ['"', '\', 'b', '"'].
            Before merging, this.position points to 'b'. After merging, the sequence becomes ['"', '\b', '"'],
            and the position that previously pointed to 'b' now points to the second '"' (at index 2).
            If we don’t decrement the position, when control returns to tokenizeString(), the position will
            be incremented again, causing us to skip a character.

            It is very important to not visit the character we just merged. In the case of, ['"', '\', '"'], we merge it
            into ['"', '\"'] which is eventually this ['"', '"'].
            Initially looks like  a valid empty string BUT it is not because the 2nd quotation mark was an escape
            character and not a closing quotation mark for the string to end. By decreasing the position,
            this.position = 1(which is the merged character) we will 'skip' that character. The new merged character is
            at position - 1 from the current position. Decrement the position now moves the index to the new merged character
            BUT  since the control returns to tokenizeString() after handleEscapeCharacter() returns, the position is
            incremented. If there are no more characters, this.position == this.buffer.length is true which means invalid
            string or if there are more characters to traverse and none of them is a closing quotation mark at some point
            this.position == this.buffer.length will also be true which will also lead to an invalid string
         */
        this.position--;
    }

    /*
        For a valid unicode sequence we have the following cases:
            1. High Surrogate
            2. Low Surrogate
            3. BMP

        High Surrogate: We look ahead in the array to see if there is a following sequence that could potentially form a
        valid surrogate pair. When looking at the next character after the valid sequence it can be the start of unicode
        sequence, so we have the 3 options we had above or a non-escaped character. The way we handle those cases is that
        we look at the next sequence only if the current is a high surrogate. If the next sequence is a valid high/low
        surrogate or BMP we convert both the sequences into the corresponding characters.
        If after the high surrogate we do not encounter an escape sequence we just convert that into its corresponding
        character.
        Low Surrogate/BMP: We do not look ahead in the array, surrogate pairs are always in the form of (high - low). We
        convert the sequence to the corresponding character

        The 1st if check is a bit tricky. When we have this input buffer = ['"', 'A', '\', 'u', '0', '0', 'E', '"'], the
        check this.position + 4 >= this.buffer.length is actually false because after 'u' there are 4 characters left
        that could potentially form a hex sequence ('0', '0', 'E', '"'). It is the 2nd if() that will catch this error
        by checking isHexCharacter('"'). The 1st if will catch input buffers like ['"', 'A', '\', 'u', '0', '0', 'E']
     */
    private void handleUnicodeEscapeSequence() {
        /*
            There are not enough characters left in the string to represent a valid unicode escape sequence. We need at
            least 4 more characters left from the current position. This is the reason why we need >= and not just >
            If we are at index 0 for an array of size 4, 0 + 4 > 4 is false, and then we will try to access the 4th element
            after our current index which will be out of bounds. this.buffer[4] is out of bounds for an array of size 4
         */
        if (this.position + 4 >= this.buffer.length) {
            throw new UnexpectedCharacterException("Position: " + (this.buffer.length - 1) + ". Unexpected end of input for unicode escape sequence");
        }

        validateHexCharacter(this.position + 1);
        validateHexCharacter(this.position + 2);
        validateHexCharacter(this.position + 3);
        validateHexCharacter(this.position + 4);

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
            validateHexCharacter(this.position + 7);
            validateHexCharacter(this.position + 8);
            validateHexCharacter(this.position + 9);
            validateHexCharacter(this.position + 10);
            String hexSequence = "" + this.buffer[this.position + 7]
                    + this.buffer[this.position + 8]
                    + this.buffer[this.position + 9]
                    + this.buffer[this.position + 10];

            int codePoint = Integer.parseInt(hexSequence, 16);
            // Case 1: Surrogate pair
            // Case 2: 2 High surrogates in a row
            // Case 3: HighSurrogate into BMP
            String str;
            /*
                We have 2 codepoints, the highSurrogate which is passed from handleUnicodeEscapeSequence() and the
                one we just compute for the next unicode sequence we found. The following process is explained in detail in
                the method above handleUnicodeEscapeSequence() when we were converting a unicode sequence to a BMP
                character.
                While previously we only converted 1 character, now we have to handle 2. If it is a surrogate
                pair or 2 high surrogates in a row, the array will have '�', '�' because in UTF_8 surrogates by themselves
                don't correspond to any character. When we decode them in the case of a surrogate pair we will get
                the correct character. (�, � under the hood hold the unicode values, so they are encoded correctly)
                e.g. 2 high surrogates: "A\uD83D\uD83DBé" -> [", A, �, �, B, é, "] = [34, 65, '\uD83D'(55357), '\uD83D'(55357), 66, 233, 34]

                Note: Previously we reset the index(this.position) now we don't because if we have a surrogate pair
                or 2 surrogate highs we insert 2 characters in the array. We know that this.position is at 'u' and
                we insert (�,  �) in the positions of ('\' previous of position) and ('u') so this.position points
                to the correct character, control returns to tokenizeString() and this.position is incremented.
            */
            if (Character.isLowSurrogate((char) codePoint)) {
                int surrogatePair = Character.toCodePoint((char) highSurrogate, (char) codePoint);
                str = String.valueOf(this.buffer).substring(0, this.position - 1)
                        + String.valueOf(Character.toChars(surrogatePair))
                        + String.valueOf(this.buffer).substring(this.position + 11);
            } else {
                // high surrogate or BMP
                str = String.valueOf(this.buffer).substring(0, this.position - 1)
                        + String.valueOf(Character.toChars(highSurrogate))
                        + String.valueOf(Character.toChars(codePoint))
                        + String.valueOf(this.buffer).substring(this.position + 11);
            }
            this.buffer = str.toCharArray();
            return;
        }
        /*
            This is the case where we had the previous high surrogate, but it was not followed by a unicode sequence or
            they were not enough characters left to form a sequence.
            We simply convert the high surrogate to (�) as explained in detail handleUnicodeEscapeSequence() for the
            BMP/low surrogate character case including why we need to reset the position.
         */
        this.buffer = (String.valueOf(this.buffer).substring(0, this.position - 1)
                + String.valueOf(Character.toChars(highSurrogate))
                + String.valueOf(this.buffer).substring(this.position + 5)).toCharArray();
        this.position--;
    }

    /**
     * According to the JSON rfc: The hexadecimal letters A through F can be uppercase or lowercase. This method could
     * have been a boolean isHexCharacter() but we wouldn't have clear error handling. We would call the method 4 times
     * in an if(), once for every character, and if one returned false we wouldn't know which to provide a clear message
     * to the user
     *
     * @throws UnexpectedCharacterException if the character is not a valid hex digit
     */
    private void validateHexCharacter(int position) {
        char c = this.buffer[position];
        if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
            throw new UnexpectedCharacterException("Position: " + position + ". Unexpected character: '" + this.buffer[position] + "'. A hex-digit was expected in the character escape sequence");
        }
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

    private void tokenizeFalse() {
        if (this.position + 4 >= this.buffer.length) {
            throw new UnrecognizedTokenException("Unrecognized token: '" + new String(this.buffer, this.position, this.buffer.length - this.position) + "'. Expected a valid JSON value");
        }

        if (this.buffer[++this.position] != 'a'
                || this.buffer[++this.position] != 'l'
                || this.buffer[++this.position] != 's'
                || this.buffer[++this.position] != 'e') {
            throw new UnrecognizedTokenException("Unrecognized token: '" + new String(this.buffer, this.initialPosition, 5) + "'. Expected a valid JSON value");
        }

        /*
            Why we need the condition: this.stack.isEmpty()?
               When our stack is not empty we have either an array or an object, which means that we can provide a message
               that reflects the context. For falsea: "Position: 5, Unexpected character: 'a'" is enough but for a case
               like [false[4]]: "Position: 6, Unexpected character: '['" is not enough. What we do instead is we don't check
               the next character, we tokenize it and when the logic of parseArray() expects after a value a comma and
               gets ']' it will provide a more meaningful message. Similar for object.
               The same logic applies for true and null
         */
        if (this.stack.isEmpty()) {
            checkForUnexpectedCharacter(this.position + 1);
        }
    }

    private void tokenizeTrue() {
        if (this.position + 3 >= this.buffer.length) {
            throw new UnrecognizedTokenException("Unrecognized token: '" + new String(this.buffer, this.position, this.buffer.length - this.position) + "'. Expected a valid JSON value");
        }

        if (this.buffer[++this.position] != 'r'
                || this.buffer[++this.position] != 'u'
                || this.buffer[++this.position] != 'e') {
            throw new UnrecognizedTokenException("Unrecognized token: '" + new String(this.buffer, this.initialPosition, 4) + "'. Expected a valid JSON value");
        }
        if (this.stack.isEmpty()) {
            checkForUnexpectedCharacter(this.position + 1);
        }
    }

    private void tokenizeNull() {
        if (this.position + 3 >= this.buffer.length) {
            throw new UnrecognizedTokenException("Unrecognized token: '" + new String(this.buffer, this.position, this.buffer.length - this.position) + "'. Expected a valid JSON value");
        }

        if (this.buffer[++this.position] != 'u'
                || this.buffer[++this.position] != 'l'
                || this.buffer[++this.position] != 'l') {
            throw new UnrecognizedTokenException("Unrecognized token: '" + new String(this.buffer, this.initialPosition, 4) + "'. Expected a valid JSON value");
        }
        if (this.stack.isEmpty()) {
            checkForUnexpectedCharacter(this.position + 1);
        }
    }

    private void checkForUnexpectedCharacter(int charPosition) {
        if (charPosition < this.buffer.length) {
            throw new UnexpectedCharacterException("Position: " + charPosition + ". Unexpected character: '" + this.buffer[charPosition] + "'");
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

    private void skipWhiteSpace() {
        boolean isWhiteSpace = true;

        while (isWhiteSpace && this.position < this.buffer.length) {
            // We don't use isWhiteSpace() because Java and JSON RFC do not consider the same characters as whitespace
            if (isRFCWhiteSpace(this.buffer[this.position])) {
                this.position++;
            } else {
                isWhiteSpace = false;
            }
        }
    }

    private void handleDecimalPoint(boolean hasDecimalPoint, boolean hasExponentialNotation) {
        // 0.1.2 is not allowed
        if (hasDecimalPoint) {
            throw new UnexpectedCharacterException("Position: " + this.position + ". Only one decimal point is allowed");
        }

        // Decimal point is not allowed after 'e' or 'E'
        if (hasExponentialNotation) {
            throw new UnexpectedCharacterException("Position: " + this.position + ". Decimal point is not allowed after exponential notation");
        }
        /*
            After '.' only digits are valid(12.e5 is not valid) x. where x is any number is also invalid because there
            are not enough characters left
        */
        if (this.position + 1 == this.buffer.length || !(this.buffer[this.position + 1] >= '0' && this.buffer[this.position + 1] <= '9')) {
            throw new UnexpectedCharacterException("Position: " + this.position + ". Decimal point must be followed by a digit");
        }
    }

    private void handleExponentialNotation(boolean hasExponentialNotation) {
        // 1e2e3 not allowed
        if (hasExponentialNotation) {
            throw new UnexpectedCharacterException("Position: " + this.position + ". Only one exponential notation('e' or 'E') is allowed");
        }
        // When e or E is the last character of the buffer or the next character is neither a digit nor a sign
        if (this.position + 1 == this.buffer.length
                || !(this.buffer[this.position + 1] >= '0'
                && this.buffer[this.position + 1] <= '9')
                && this.buffer[this.position + 1] != '-'
                && this.buffer[this.position + 1] != '+') {
            throw new UnexpectedCharacterException("Position: " + this.position + ". Exponential notation must be followed by a digit");
        }
    }

    private void handleSign() {
        // 12+3 not allowed
        if (this.buffer[this.position - 1] != 'E' && this.buffer[this.position - 1] != 'e') {
            throw new UnexpectedCharacterException("Position: " + this.position + ". Sign ('+' or '-') is only allowed as part of exponential notation after 'e' or 'E'");
        }

        //1e+ not allowed, 1e+q not allowed
        if (this.position + 1 == this.buffer.length || !(this.buffer[this.position + 1] >= '0' && this.buffer[this.position + 1] <= '9')) {
            throw new UnexpectedCharacterException("Position: " + this.position + ". Exponential notation must be followed by a digit");
        }
    }

    private boolean isRFCWhiteSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    public char[] getBuffer() {
        return this.buffer;
    }

    public int getInitialPosition() {
        return this.initialPosition;
    }

    public void advance() {
        this.position++;
    }

}
