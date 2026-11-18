package com.minibank.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money comes in and goes out as pounds (e.g. 12.50), but inside the bank it is always a whole number of pence
 * stored in a {@code long}. Floating point can't represent 0.10 exactly, so it never touches a balance.
 */
public final class Money {

    private Money() {
    }

    /** 12.50 -> 1250. Throws if there are more than two decimal places, rather than silently rounding. */
    public static long toPence(BigDecimal pounds) {
        try {
            return pounds.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new InvalidRequestException("Amounts can have at most 2 decimal places and must fit in the account");
        }
    }

    /** 1250 -> 12.50 */
    public static BigDecimal toPounds(long pence) {
        return BigDecimal.valueOf(pence, 2);
    }
}
