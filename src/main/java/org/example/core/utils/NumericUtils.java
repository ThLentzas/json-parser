package org.example.core.utils;

import java.math.BigDecimal;

public final class NumericUtils {
    /*
        According to rfc:
            This specification allows implementations to set limits on the range and precision of numbers accepted.
            Since software that implements IEEE 754 binary64 (double precision) numbers is generally available and
            widely used, good interoperability can be achieved by implementations that expect no more precision or range
            than these provide, in the sense that implementations will approximate JSON numbers within the expected precision.
     */
    public static final BigDecimal MAX_RANGE = new BigDecimal("1.7976931348623157e+308");
    public static final BigDecimal MIN_RANGE = new BigDecimal("-1.7976931348623157e+308");

    private NumericUtils() {
        throw new UnsupportedOperationException("NumericUtils is a utility class and cannot be instantiated");
    }

    // 1e-2 when unwrapped will have a decimal point, so we can't just look for '.'
    public static boolean isInteger(Number number) {
        BigDecimal bd = new BigDecimal(String.valueOf(number));
        return bd.stripTrailingZeros().scale() <= 0
                && bd.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) >= 0
                && bd.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0;
    }

    /*
        Why we use -Double.MAX_VALUE for the lower bound instead of Double.MIN_VALUE?
            Double.MIN_VALUE represents the smallest possible number in Double precision floating number representation
            It is 4.94 * 10^−324 which is a positive really close to zero number but not negative. It is the minimum value
            of the subnormal numbers.
            If we do bd.compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) >= 0 && bd.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) <= 0
            We only look at all the positive numbers we can represent for both normal and subnormal[1.80 * 10^308, 4.94 * 10^−324].
            We do not include any negative numbers.

       The only difference between the positive and negative IEEE 754 representation (whether normal or subnormal) is
       the sign bit. The exponent and mantissa will be identical for +x and −x. Only the sign bit flips. All we have to
       do is flip the sign of the MAX_VALUE.
       This way we represent the largest negative number in double-precision floating-point representation,
       approximately -1.80 × 10^308. The resulting range is [-1.80 × 10^308, 1.80 × 10^308], covers all representable
       double-precision numbers.
     */
    public static boolean isDouble(Number number) {
        BigDecimal bd = new BigDecimal(String.valueOf(number));
        return bd.stripTrailingZeros().scale() > 0  // We need at least 1 decimal point that is not 124.0
                && bd.compareTo(BigDecimal.valueOf(-Double.MAX_VALUE)) >= 0
                && bd.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) <= 0;
    }

    public static boolean isLong(Number number) {
        BigDecimal bd = new BigDecimal(String.valueOf(number));
        return bd.stripTrailingZeros().scale() <= 0
                && bd.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0
                && bd.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0;
    }
}
