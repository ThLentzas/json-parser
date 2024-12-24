package org.example.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.example.parser.ParserToken;

public final class NumberNode extends Node {

    public NumberNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
    }

    /*
        Note: For input: 123123e100000 the output will be 1.23123E+100005
        Explanation: It is the same number. We shift the decimal point five places to the left and increase the exponent
 	    by the same number. Shifting the decimal point in the coefficient changes the exponent to maintain the same value.
 	    Scientific notation  requires the coefficient to be between 1 and 10. For 123123 we need to move the decimal
 	    point five places to the left
     */
    @Override
    public BigDecimal value() {
        return convertNumber();
    }

    public int intValue() {
        return convertNumber().intValue();
    }

    public double doubleValue() {
        return convertNumber().doubleValue();
    }

    public long longValue() {
        return convertNumber().longValue();
    }

    @Override
    public NodeType type() {
        return NodeType.NUMBER;
    }

    @Override
    public String toString() {
        int startIndex = this.tokens.get(this.tokenIndex).getStartIndex();
        int endIndex = this.tokens.get(this.tokenIndex).getEndIndex();
        return new String(this.buffer, startIndex, endIndex - startIndex + 1);
    }

    // 1e-2 when unwrapped will have a decimal point, so we can't just look for '.'
    private boolean isInteger(BigDecimal number) {
        return number.stripTrailingZeros().scale() <= 0
                && number.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0
                && number.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0;
    }

    private boolean isDouble(BigDecimal number) {
        // We need at least 1 decimal point that is not 124.0
        return number.stripTrailingZeros().scale() > 0
                && number.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) >= 0
                && number.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) <= 0;
    }

    private boolean isLong(BigDecimal number) {
        return number.stripTrailingZeros().scale() <= 0
                && number.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0
                && number.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0;
    }

    private boolean isBigInteger(BigDecimal number) {
        try {
            number.toBigIntegerExact();
            return true;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    //toDo: explain overflow/underflow
    /*
        12.56 this number is the same as 12 + 0.56. To get 0.56 we count the number of digits after . and divide the
        number after . with 10 to the power of count. 56 -> 2 digits -> 56 / 10 ^ 2 = 0.56

        The formula: (integralPart + fractionalPart / 10^(fractionalLength)) * 10^(exponent)

        We build the number in 3 parts:
            1. integral part
            2. fractional part while counting its length
            3. exponent

        The "hardest" case we have to consider is: 1.257e1
        1. We keep traversing the buffer until we encounter '.', 'e' or 'E'. From start to that index we build the integral
        part just like we do in the leetcode problems.
            - Get the numeric value of the character(- '0' would also work)
            - We multiply the number by 10, before we add the next digit. Each digit is correctly positioned. We shift
              the existing digits one place to the left, making room for the new least significant digit.
              e.g. ['1', '2'] -> 0 * 10 + 1 = 1 -> 1 * 10 + 2 = 12. This is the same logic in all 3 parts
        2. If we encountered a decimal point we need to find the fractional part which could be until the end of the
        buffer or until we find 'e' or 'E'. As we build the fractional part we also keep track of its length.
        e.g. ['1', '2', '.', '5', '6'] A way to write 12.56 is to add 0.56 to our integral part. We currently have the
        fractional part which is 56 and its length 2. To convert to 0.56 we can divide the fractional part with the 10
        to the power of its length. 56 / 10 ^ 2 = 0.56
        3. The presence of the exponential notation means multiple everything before 'e' or 'E' with everything after
        We follow the same logic and compute the exponent after exponential notation

        We compute the number based on the above formula

        If there is no fractional part, we add to the integral part (0 / 10 ^ 0) which is 0, so it does not change the
        outcome.
        If there is no exponent part, multiplying the previous number with 10 ^ 0 also does not affect the number

        The time complexity is still linear, we make a single scan of the tokens.
     */
    private BigDecimal convertNumber() {
        int digit;
        int startIndex = this.tokens.get(this.tokenIndex).getStartIndex();
        int endIndex = this.tokens.get(this.tokenIndex).getEndIndex();
        boolean isNegative = false;

        if (this.buffer[startIndex] == '-') {
            startIndex++;
            isNegative = true;
        }

        BigInteger integralPart = BigInteger.ZERO;
        int i = startIndex;
        while (i <= endIndex && this.buffer[i] != '.' && this.buffer[i] != 'e' && this.buffer[i] != 'E') {
            digit = this.buffer[i] - 48;
            integralPart = integralPart.multiply(BigInteger.valueOf(10)).add(BigInteger.valueOf(digit));
            i++;
        }

        BigInteger fractionalPart = BigInteger.ZERO;
        // scaleByPowerOfTen() throws Arithmetic exception when exponent is outside the range of a 32-bit integer.
        // Pointless to change the type to anything else
        int fractionalLength = 0;
        if (i <= endIndex && this.buffer[i] == '.') {
            i++;
            while (i <= endIndex && this.buffer[i] != 'e' && this.buffer[i] != 'E') {
                digit = this.buffer[i] - 48;
                fractionalPart = fractionalPart.multiply(BigInteger.valueOf(10)).add(BigInteger.valueOf(digit));
                fractionalLength++;
                i++;
            }
        }

        // scaleByPowerOfTen() throws Arithmetic exception when exponent is outside the range of a 32-bit integer.
        // Pointless to change the type to anything else
        // Remember for cases like this:
        // [0.4e00669999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999969999999006]
        // The exponent is everything after 'e' and it will overflow and Java will wrap around to 64770077. This is the
        // expected behaviour. Output: 4E+64770077
        int exponent = 0;
        if (i <= endIndex && (this.buffer[i] == 'e' || this.buffer[i] == 'E')) {
            i++;
            boolean isNegativeExponent = false;
            if (this.buffer[i] == '-') {
                isNegativeExponent = true;
                i++;
            }
            if (this.buffer[i] == '+') {
                i++;
            }

            while (i <= endIndex) {
                digit = this.buffer[i] - 48;
                exponent = exponent * 10 + digit;
                i++;
            }
            if (isNegativeExponent) {
                exponent = -exponent;
            }
        }

        BigDecimal bdIntegral = new BigDecimal(integralPart);
        BigDecimal bdFraction = new BigDecimal(fractionalPart).scaleByPowerOfTen(-fractionalLength);
        BigDecimal number = bdIntegral.add(bdFraction);
        number = number.scaleByPowerOfTen(exponent);

        return isNegative ? number.negate() : number;
    }
}
