package org.example.utils;

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

    public static boolean isDouble(Number number) {
        BigDecimal bd = new BigDecimal(String.valueOf(number));
        return bd.stripTrailingZeros().scale() > 0  // We need at least 1 decimal point that is not 124.0
                // toDo: explain this
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
