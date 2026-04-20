package com.houkiang.ordersettlement.test_common_shipping.common;

import com.houkiang.ordersettlement.common.util.MoneyUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MoneyUtils 工具类测试")
class MoneyUtilsTest {

    // ─────────────────────────────────────────────────────────────
    // requireNonNegative
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("requireNonNegative")
    class RequireNonNegative {

        @Test
        @DisplayName("输入 0 → 返回 0.00")
        void zero_returnsZero() {
            BigDecimal result = MoneyUtils.requireNonNegative(BigDecimal.ZERO, "amount");
            assertEquals(new BigDecimal("0.00"), result);
        }

        @Test
        @DisplayName("输入正数 12.3 → 返回 12.30（两位小数）")
        void positive_returnsScaled() {
            BigDecimal result = MoneyUtils.requireNonNegative(new BigDecimal("12.3"), "amount");
            assertEquals(new BigDecimal("12.30"), result);
        }

        @Test
        @DisplayName("输入多位小数 9.999 → 按 HALF_UP 保留两位 10.00")
        void multipleDecimal_roundsHalfUp() {
            BigDecimal result = MoneyUtils.requireNonNegative(new BigDecimal("9.999"), "amount");
            assertEquals(new BigDecimal("10.00"), result);
        }

        @Test
        @DisplayName("输入负数 → 抛出 IllegalArgumentException")
        void negative_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> MoneyUtils.requireNonNegative(new BigDecimal("-0.01"), "amount"));
        }

        @Test
        @DisplayName("输入 null → 抛出 NullPointerException")
        void nullInput_throwsNullPointer() {
            assertThrows(NullPointerException.class,
                    () -> MoneyUtils.requireNonNegative(null, "amount"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // requirePositive
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("requirePositive")
    class RequirePositive {

        @Test
        @DisplayName("输入最小正数 0.01 → 返回 0.01")
        void minPositive_returnsValue() {
            BigDecimal result = MoneyUtils.requirePositive(new BigDecimal("0.01"), "price");
            assertEquals(new BigDecimal("0.01"), result);
        }

        @Test
        @DisplayName("输入正数 100 → 返回 100.00")
        void largePositive_returnsScaled() {
            BigDecimal result = MoneyUtils.requirePositive(new BigDecimal("100"), "price");
            assertEquals(new BigDecimal("100.00"), result);
        }

        @Test
        @DisplayName("输入 0 → 抛出 IllegalArgumentException")
        void zero_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> MoneyUtils.requirePositive(BigDecimal.ZERO, "price"));
        }

        @Test
        @DisplayName("输入负数 → 抛出 IllegalArgumentException")
        void negative_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> MoneyUtils.requirePositive(new BigDecimal("-1"), "price"));
        }

        @Test
        @DisplayName("输入 null → 抛出 NullPointerException")
        void nullInput_throwsNullPointer() {
            assertThrows(NullPointerException.class,
                    () -> MoneyUtils.requirePositive(null, "price"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // minZero
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("minZero")
    class MinZero {

        @Test
        @DisplayName("输入负数 → 返回 0.00")
        void negative_returnsZero() {
            BigDecimal result = MoneyUtils.minZero(new BigDecimal("-99.99"));
            assertEquals(new BigDecimal("0.00"), result);
        }

        @Test
        @DisplayName("输入 0 → 返回 0.00")
        void zero_returnsZero() {
            BigDecimal result = MoneyUtils.minZero(BigDecimal.ZERO);
            assertEquals(new BigDecimal("0.00"), result);
        }

        @Test
        @DisplayName("输入正数 1.235 → 按 HALF_UP 返回 1.24")
        void positive_roundsHalfUp() {
            BigDecimal result = MoneyUtils.minZero(new BigDecimal("1.235"));
            assertEquals(new BigDecimal("1.24"), result);
        }

        @Test
        @DisplayName("输入正数 50.50 → 返回 50.50")
        void positiveExact_returnsSame() {
            BigDecimal result = MoneyUtils.minZero(new BigDecimal("50.50"));
            assertEquals(new BigDecimal("50.50"), result);
        }

        @Test
        @DisplayName("输入 null → 抛出 NullPointerException")
        void nullInput_throwsNullPointer() {
            assertThrows(NullPointerException.class,
                    () -> MoneyUtils.minZero(null));
        }
    }
}
