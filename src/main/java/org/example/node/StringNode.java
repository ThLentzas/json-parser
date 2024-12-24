package org.example.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.example.exception.SubsequenceIndexViolationException;
import org.example.parser.ParserToken;

public final class StringNode extends Node {

    public StringNode(List<ParserToken> tokens, char[] buffer, Node parent) {
        super(tokens, buffer, parent);
    }

    @Override
    public NodeType type() {
        return NodeType.STRING;
    }

    // plain text
    @Override
    public String value() {
        // We remove the enclosing quotations marks
        int startIndex = this.tokens.get(this.tokenIndex).getStartIndex() + 1;
        int endIndex = this.tokens.get(this.tokenIndex).getEndIndex() - 1;
        return new String(this.buffer, startIndex, endIndex - startIndex + 1);
    }

    @Override
    public String toString() {
        return value();
    }

    // Classic leetcode problem
    public boolean isSubsequence(String subsequence) {
        if(subsequence.isEmpty()) {
            return true;
        }

        int index = 0;
        String text = value();
        for(int i = 0; i < text.length(); i++) {
            if(text.charAt(i) == subsequence.charAt(index)) {
                index++;
            }
            if(index == subsequence.length()) {
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
     * @throws IndexOutOfBoundsException if any index is negative or exceeds the length of the string.
     * @throws SubsequenceIndexViolationException if the indices are not in strict ascending order.
     */
    public String subsequence(List<Integer> indices) {
        if(indices.isEmpty()) {
            return "";
        }

        StringBuilder sequence = new StringBuilder();
        String text = value();
        int previous = -1;
        for(int index : indices) {
            if(index < 0 || index >= text.length()) {
                throw new IndexOutOfBoundsException("index: " + index + ", length: " + text.length());
            }
            // indices are allowed only in ascending order
            if(index <= previous) {
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

    public double doubleValue() {
        return convertStringToNumber().doubleValue();
    }

    public long longValue() {
        return convertStringToNumber().longValue();
    }

    public BigDecimal bigDecimalValue() {
        return convertStringToNumber();
    }

    public BigInteger bigIntegerValue() {
        return convertStringToNumber().toBigIntegerExact();
    }

    // Same logic as the convertNumber(), we just add a check where the string might have an invalid number to return 0. e.g. "123q"
    private BigDecimal convertStringToNumber() {
        int digit;
        //Empty string
        if (this.tokens.get(this.tokenIndex).getStartIndex() + 1 == this.tokens.get(this.tokenIndex).getEndIndex()) {
            return BigDecimal.ZERO;
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
                return BigDecimal.ZERO;
            }
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
                if (!(this.buffer[i] >= '0' && this.buffer[i] <= '9')) {
                    return BigDecimal.ZERO;
                }
                digit = this.buffer[i] - 48;
                fractionalPart = fractionalPart.multiply(BigInteger.valueOf(10)).add(BigInteger.valueOf(digit));
                fractionalLength++;
                i++;
            }
        }

        // scaleByPowerOfTen() throws Arithmetic exception when exponent is outside the range of a 32-bit integer.
        // Pointless to change the type to anything else
        int exponent = 0;
        if (i <= endIndex && (this.buffer[i] == 'e' || this.buffer[i] == 'E')) {
            i++;
            if (!(this.buffer[i] >= '0' && this.buffer[i] <= '9')) {
                return BigDecimal.ZERO;
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

        return isNegative ? number.negate() : number;
    }
}
