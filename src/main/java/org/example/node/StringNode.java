package org.example.node;

import org.example.exception.OutOfRangeException;
import org.example.exception.SubsequenceIndexViolationException;
import org.example.parser.ParserToken;
import org.example.utils.NumericUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public final class StringNode extends Node {
    private final String value;

    public StringNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
        this.value = new String(this.buffer, this.tokens.get(this.tokenIndex).getStartIndex() + 1, this.tokens.get(this.tokenIndex).getEndIndex() - this.tokens.get(this.tokenIndex).getStartIndex() - 1);
    }

    @Override
    public NodeType type() {
        return NodeType.STRING;
    }

    @Override
    public Node path(String name) {
        return AbsentNode.instance();
    }

    @Override
    public Node path(int index) {
        return AbsentNode.instance();
    }

    // plain text
    @Override
    public String value() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    // Classic leetcode problem
    public boolean isSubsequence(String subsequence) {
        if (subsequence.isEmpty()) {
            return true;
        }

        int index = 0;
        String text = value();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == subsequence.charAt(index)) {
                index++;
            }
            if (index == subsequence.length()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts a subsequence from the given indices of the string represented by this node.
     * The indices must be in strict ascending order.
     *
     * @param indices a list of indices specifying the characters to include in the subsequence.
     * @return a string formed by concatenating the characters of the original string at the specified indices.
     * @throws IndexOutOfBoundsException          if any index is negative or exceeds the length of the string.
     * @throws SubsequenceIndexViolationException if the indices are not in strict ascending order.
     */
    public String subsequence(List<Integer> indices) {
        if (indices.isEmpty()) {
            return "";
        }

        StringBuilder sequence = new StringBuilder();
        String text = value();
        int previous = -1;
        for (int index : indices) {
            if (index < 0 || index >= text.length()) {
                throw new IndexOutOfBoundsException("index: " + index + ", length: " + text.length());
            }
            // indices are allowed only in ascending order
            if (index <= previous) {
                throw new SubsequenceIndexViolationException("Indices must be in strict ascending order. Found: " + index + " after " + previous);
            }
            sequence.append(text.charAt(index));
            previous = index;
        }
        return sequence.toString();
    }

    public int intValue() {
        return convertStringToNumber().intValue();
    }

    public BigDecimal doubleValue() {
        return (BigDecimal) convertStringToNumber();
    }

    public long longValue() {
        return convertStringToNumber().longValue();
    }

    // Same logic as the convertNumber(), we just add a check where the string might have an invalid number to return 0.
    // e.g. "123q"
    private Number convertStringToNumber() {
        int digit;
        //Empty string
        if (this.tokens.get(this.tokenIndex).getStartIndex() + 1 == this.tokens.get(this.tokenIndex).getEndIndex()) {
            return 0;
        }

        int startIndex = this.tokens.get(tokenIndex).getStartIndex() + 1;
        int endIndex = this.tokens.get(tokenIndex).getEndIndex() - 1;
        boolean isNegative = false;

        if (this.buffer[startIndex] == '-') {
            startIndex++;
            isNegative = true;
        }

        BigInteger integralPart = BigInteger.ZERO;
        int i = startIndex;
        while (i <= endIndex && this.buffer[i] != '.' && this.buffer[i] != 'e' && this.buffer[i] != 'E') {
            if (!(this.buffer[i] >= '0' && this.buffer[i] <= '9')) {
                return 0;
            }
            digit = this.buffer[i] - 48;
            integralPart = integralPart.multiply(BigInteger.valueOf(10)).add(BigInteger.valueOf(digit));
            i++;
        }

        BigInteger fractionalPart = BigInteger.ZERO;
        int fractionalLength = 0;
        if (i <= endIndex && this.buffer[i] == '.') {
            i++;
            while (i <= endIndex && this.buffer[i] != 'e' && this.buffer[i] != 'E') {
                if (!(this.buffer[i] >= '0' && this.buffer[i] <= '9')) {
                    return 0;
                }
                digit = this.buffer[i] - 48;
                fractionalPart = fractionalPart.multiply(BigInteger.valueOf(10)).add(BigInteger.valueOf(digit));
                fractionalLength++;
                i++;
            }
        }

        int exponent = 0;
        if (i <= endIndex && (this.buffer[i] == 'e' || this.buffer[i] == 'E')) {
            i++;
            if (!(this.buffer[i] >= '0' && this.buffer[i] <= '9')) {
                return 0;
            }

            boolean isNegativeExponent = false;
            if (this.buffer[i] == '-') {
                isNegativeExponent = true;
                i++;
            }
            if (this.buffer[i] == '+') {
                i++;
            }

            while (i <= endIndex) {
                if (!(this.buffer[i] >= '0' && this.buffer[i] <= '9')) {
                    return BigDecimal.ZERO;
                }
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

        number = isNegative ? number.negate() : number;
        // The numeric value of the string must be within the allowed range
        if (NumericUtils.MAX_RANGE.compareTo(number) < 0) {
            throw new OutOfRangeException("Number exceeds the maximum allowed range of 1E308.");
        }
        if (NumericUtils.MIN_RANGE.compareTo(number) > 0) {
            throw new OutOfRangeException("Number exceeds the minimum allowed range of -1E308.");
        }

        if (NumericUtils.isInteger(number)) {
            return number.intValue();
        }

        if (NumericUtils.isLong(number)) {
            return number.longValue();
        }

        return number;
    }
}
