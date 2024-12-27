package org.example.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.example.exception.OutOfRangeException;
import org.example.parser.ParserToken;
import org.example.utils.NumericUtils;

public final class NumberNode extends Node {
    private int index;
    private final Number value;

    public NumberNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
        this.value = convertNumber();
    }

    /*
        Note: For input: 123123e100000 the output will be 1.23123E+100005
        Explanation: It is the same number. We shift the decimal point five places to the left and increase the exponent
 	    by the same number. Shifting the decimal point in the coefficient changes the exponent to maintain the same value.
 	    Scientific notation  requires the coefficient to be between 1 and 10. For 123123 we need to move the decimal
 	    point five places to the left
     */
    @Override
    public Number value() {
        return this.value;
    }

    public int intValue() {
        return this.value.intValue();
    }

    public BigDecimal doubleValue() {
        return (BigDecimal) value();
    }

    public long longValue() {
        return this.value.longValue();
    }

    @Override
    public NodeType type() {
        return NodeType.NUMBER;
    }

    @Override
    public Node path(String name) {
        return AbsentNode.instance();
    }

    @Override
    public Node path(int index) {
        return AbsentNode.instance();
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    public boolean isDouble(Number number) {
        return NumericUtils.isDouble(number);
    }

    public boolean isInteger(Number number) {
        return NumericUtils.isInteger(number);
    }

    public boolean isLong(Number number) {
        return NumericUtils.isLong(number);
    }

    /*
        12.56 this number is the same as 12 + 0.56. To get 0.56 we count the number of digits after . and divide the
        number after . with 10 to the power of count. 56 -> 2 digits -> 56 / 10 ^ 2 = 0.56

        The formula: (integralPart + fractionalPart / 10^(fractionalLength)) * 10^(exponent)

        We build the number in 3 parts:
            1. integral part
            2. fractional part while counting its length
            3. exponent

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

    private Number convertNumber() {
        int startIndex = this.tokens.get(this.tokenIndex).getStartIndex();
        int endIndex = this.tokens.get(this.tokenIndex).getEndIndex();
        boolean isNegative = false;

        if (this.buffer[startIndex] == '-') {
            startIndex++;
            isNegative = true;
        }

        this.index = startIndex;
        BigDecimal number = integralPart(endIndex).add(fractionalPart(endIndex)).scaleByPowerOfTen(exponent(endIndex));

        number = isNegative ? number.negate() : number;
        if(NumericUtils.MAX_RANGE.compareTo(number) < 0) {
            throw new OutOfRangeException("Number exceeds the maximum allowed range of 1E308.");
        }
        if(NumericUtils.MIN_RANGE.compareTo(number) > 0) {
            throw new OutOfRangeException("Number exceeds the minimum allowed range of -1E308.");
        }

        if(isInteger(number)) {
            return number.intValue();
        }

        if(isLong(number)) {
            return number.longValue();
        }

        return number;
    }

    private BigDecimal integralPart(int endIndex) {
        BigInteger integralPart = BigInteger.ZERO;
        int digit;
        while (this.index <= endIndex && this.buffer[this.index] != '.' && this.buffer[this.index] != 'e' && this.buffer[this.index] != 'E') {
            digit = this.buffer[this.index] - 48;
            integralPart = integralPart.multiply(BigInteger.valueOf(10)).add(BigInteger.valueOf(digit));
            this.index++;
        }
        return new BigDecimal(integralPart);
    }

    private BigDecimal fractionalPart(int endIndex) {
        BigInteger fractionalPart = BigInteger.ZERO;
        int digit;
        // scaleByPowerOfTen() throws Arithmetic exception when exponent is outside the range of a 32-bit integer.
        // Pointless to change the type to anything else
        int fractionalLength = 0;
        if (this.index <= endIndex && this.buffer[this.index] == '.') {
            this.index++; // Skip decimal
            while (this.index <= endIndex && this.buffer[this.index] != 'e' && this.buffer[this.index] != 'E') {
                digit = this.buffer[this.index] - 48;
                fractionalPart = fractionalPart.multiply(BigInteger.valueOf(10)).add(BigInteger.valueOf(digit));
                fractionalLength++;
                this.index++;
            }
        }

        return new BigDecimal(fractionalPart).scaleByPowerOfTen(-fractionalLength);
    }

    private int exponent(int endIndex) {
        // scaleByPowerOfTen() throws Arithmetic exception when exponent is outside the range of a 32-bit integer.
        // Pointless to change the type to anything else
        // Remember for cases like this:
        // [0.4e00669999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999969999999006]
        // The exponent is everything after 'e' and it will overflow and Java will wrap around to 64770077. This is the
        // expected behaviour. Output: 4E+64770077
        int exponent = 0;
        int digit;
        if (this.index <= endIndex && (this.buffer[this.index] == 'e' || this.buffer[this.index] == 'E')) {
            this.index++;
            boolean isNegativeExponent = false;
            if (this.buffer[this.index] == '-') {
                isNegativeExponent = true;
                this.index++;
            }
            if (this.buffer[this.index] == '+') {
                this.index++;
            }

            while (this.index <= endIndex) {
                digit = this.buffer[this.index] - 48;
                exponent = exponent * 10 + digit;
                this.index++;
            }
            if (isNegativeExponent) {
                exponent = -exponent;
            }
        }
        return exponent;
    }
}
