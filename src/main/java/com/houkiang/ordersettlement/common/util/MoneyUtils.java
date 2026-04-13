package com.houkiang.ordersettlement.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class MoneyUtils {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private MoneyUtils() {
    }

    public static BigDecimal requireNonNegative(BigDecimal amount, String fieldName) {
        Objects.requireNonNull(amount, fieldName + " must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal requirePositive(BigDecimal amount, String fieldName) {
        Objects.requireNonNull(amount, fieldName + " must not be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal minZero(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
