package com.kntrel.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class Numbers {
    private Numbers() {}


    //UTIL
    public static boolean equalish(Number a, Number b) {
        return compare(a, b) == 0;
    }
    public static boolean lessThan(Number a, Number b) {
        return compare(a, b) < 0;
    }
    public static boolean lessThanOrEqual(Number a, Number b) {
        return compare(a, b) <= 0;
    }
    public static boolean greaterThan(Number a, Number b) {
        return compare(a, b) > 0;
    }
    public static boolean greaterThanOrEqual(Number a, Number b) {
        return compare(a, b) >= 0;
    }
    /**
     * Returns:
     *  -1 if a < b
     *   0 if a == b (numeric value)
     *   1 if a > b
     */
    public static int compare(Number a, Number b) {
        if (a == null || b == null) {
            throw new NullPointerException("Numbers.compare does not accept null");
        }

        // NaN policy: reject (recommended for ordering)
        if (isNaN(a) || isNaN(b)) {
            throw new IllegalArgumentException("Cannot order NaN values");
        }

        // Infinity policy
        if (isInfinite(a) || isInfinite(b)) {
            return Double.compare(a.doubleValue(), b.doubleValue());
        }

        return toBigDecimal(a).compareTo(toBigDecimal(b));
    }
    public static boolean hasDecimals(Number n) {
        if (n == null) return false;

        double d = n.doubleValue();
        return !Double.isNaN(d)
                && !Double.isInfinite(d)
                && d != Math.floor(d);
    }


    //HELPERS
    private static boolean isNaN(Number n) {
        return (n instanceof Double d && Double.isNaN(d))
                || (n instanceof Float f && Float.isNaN(f));
    }

    private static boolean isInfinite(Number n) {
        return (n instanceof Double d && Double.isInfinite(d))
                || (n instanceof Float f && Float.isInfinite(f));
    }

    private static BigDecimal toBigDecimal(Number n) {
        if (n instanceof BigDecimal bd) return bd;
        if (n instanceof BigInteger bi) return new BigDecimal(bi);

        if (n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long) {
            return BigDecimal.valueOf(n.longValue());
        }

        if (n instanceof Float || n instanceof Double) {
            // Better than new BigDecimal(doubleValue())
            return new BigDecimal(n.toString());
        }

        // Fallback for custom Number subclasses
        return new BigDecimal(n.toString());
    }
}