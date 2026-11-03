package com.houkiang.ordersettlement.promotion.service;

import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.promotion.domain.CouponType;
import com.houkiang.ordersettlement.promotion.domain.PromotionCalculationResult;
import com.houkiang.ordersettlement.promotion.exception.CouponNotApplicableException;
import com.houkiang.ordersettlement.promotion.exception.InvalidCouponException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromotionServiceTest {

    private final PromotionService promotionService = new PromotionService();

    @Test
    @DisplayName("场景测试：满减优惠券叠加折扣券")
    void testApplyMultipleCoupons() {
        // 1. 准备数据：订单金额 100 元
        BigDecimal orderAmount = new BigDecimal("100.00");

        // 2. 准备优惠券：满100减20元，以及一个9折券
        Coupon fullReduction = Coupon.fullReduction("C01", new BigDecimal("100.00"), new BigDecimal("20.00"));
        Coupon discount = Coupon.discount("C02", BigDecimal.ZERO, new BigDecimal("0.90"));

        // 3. 执行：按顺序应用优惠券
        PromotionCalculationResult result = promotionService.applyCoupons(orderAmount, List.of(fullReduction, discount));

        // 4. 断言：(100 - 20) * 0.9 = 72.00
        // 注意：金额比较建议使用 compareTo 避免精度导致的 equals 失败
        assertEquals(0, new BigDecimal("72.00").compareTo(result.getFinalAmount()), "最终金额应为 72.00");
        assertEquals(List.of("C01", "C02"), result.getAppliedCouponCodes());
    }

    @Test
    @DisplayName("异常测试：优惠券门槛为负数")
    void testInvalidThreshold() {
        Coupon coupon = new Coupon("C01", CouponType.FULL_REDUCTION, new BigDecimal("-1.00"), new BigDecimal("10.00"), null, true);
        assertThrows(InvalidCouponException.class, () -> {
            new CouponValidator().validateCoupon(coupon, new BigDecimal("100.00"));
        });
    }

    @Test
    @DisplayName("异常测试：减免金额非法")
    void testInvalidReduction() {
        Coupon coupon = Coupon.fullReduction("C02", new BigDecimal("100.00"), new BigDecimal("0.00"));
        assertThrows(InvalidCouponException.class, () -> {
            new CouponValidator().validateCoupon(coupon, new BigDecimal("100.00"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.10", "0", "1.10"})
    @DisplayName("异常测试：折扣率大于1或小于等于0")
    void testInvalidDiscountRate(String rate) {
        Coupon coupon = Coupon.discount("C03", BigDecimal.ZERO, new BigDecimal(rate));
        assertThrows(InvalidCouponException.class, () -> {
            new CouponValidator().validateCoupon(coupon, new BigDecimal("100.00"));
        });
    }

    @Test
    @DisplayName("防御性测试：Coupon 对象属性为 null")
    void testNullAttributesInCoupon() {
        CouponValidator validator = new CouponValidator();
        // 缺少优惠门槛。
        Coupon nullThreshold = new Coupon("C_NULL", CouponType.FULL_REDUCTION, null, new BigDecimal("10.00"), null, true);
        assertThrows(InvalidCouponException.class, () -> validator.validateCoupon(nullThreshold, new BigDecimal("100")));

        // 缺少减免金额。
        Coupon nullReduction = new Coupon("C_NULL", CouponType.FULL_REDUCTION, new BigDecimal("100.00"), null, null, true);
        assertThrows(InvalidCouponException.class, () -> validator.validateCoupon(nullReduction, new BigDecimal("100")));
    }

    @Test
    @DisplayName("覆盖 validateStackable 的 null 分支")
    void testValidateStackableNulls() {
        CouponValidator validator = new CouponValidator();
        // 券列表为空引用。
        assertThrows(NullPointerException.class, () -> validator.validateStackable(null));

        // 券列表包含空元素。
        List<Coupon> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        assertThrows(NullPointerException.class, () -> validator.validateStackable(listWithNull));
    }

    @Test
    @DisplayName("覆盖 validateCoupon 自身的 null 校验")
    void testValidateCouponNulls() {
        CouponValidator validator = new CouponValidator();
        assertThrows(NullPointerException.class, () -> validator.validateCoupon(null, BigDecimal.TEN));
    }

    @ParameterizedTest
    @CsvSource({"99.99, false", "100.00, true", "100.01, true"})
    @DisplayName("边界测试：满减门槛前、等于门槛和门槛后")
    void testFullReductionThreshold(BigDecimal amount, boolean applicable) {
        Coupon coupon = Coupon.fullReduction("THRESHOLD", new BigDecimal("100.00"), new BigDecimal("20.00"));
        if (applicable) {
            assertEquals(new BigDecimal("20.00"), promotionService.calculateDiscount(amount, coupon));
        } else {
            assertThrows(CouponNotApplicableException.class,
                    () -> promotionService.calculateDiscount(amount, coupon));
        }
    }

    @Test
    void testApplyEmptyCouponList() {
        // 传入 Collections.emptyList()，验证返回结果是否就是原价
        PromotionCalculationResult result = promotionService.applyCoupons(new BigDecimal("100.00"), List.of());
        assertEquals(0, new BigDecimal("100.00").compareTo(result.getFinalAmount()));
    }

    @Test
    @DisplayName("兼容性测试：null 优惠券列表按未使用优惠处理")
    void testNullCouponList() {
        BigDecimal amount = new BigDecimal("100.00");
        var result = promotionService.applyCoupons(amount, null);
        assertEquals(amount, result.getOriginalAmount());
        assertEquals(amount, result.getFinalAmount());
        assertEquals(new BigDecimal("0.00"), result.getDiscountAmount());
        assertEquals(List.of(), result.getAppliedCouponCodes());
    }

    @Test
    @DisplayName("分支补全：优惠金额超过原价需归零")
    void testAmountDiscountToZero() {
        BigDecimal amount = new BigDecimal("50.00");
        // 减100的固定券
        Coupon bigCoupon = Coupon.fixedAmount("BIG", BigDecimal.ZERO, new BigDecimal("100.00"));
        var result = promotionService.applyCoupons(amount, List.of(bigCoupon));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getFinalAmount()));
    }

    @Test
    @DisplayName("异常测试：折扣率缺失")
    void testNullDiscountRate() {
        Coupon coupon = Coupon.discount("NULL_RATE", BigDecimal.ZERO, null);
        assertThrows(InvalidCouponException.class,
                () -> promotionService.calculateDiscount(BigDecimal.TEN, coupon));
    }

    @Test
    @DisplayName("边界测试：折扣率为 1 时不减免金额")
    void testDiscountRateOne() {
        Coupon coupon = Coupon.discount("NO_DISCOUNT", BigDecimal.ZERO, BigDecimal.ONE);
        var result = promotionService.applyCoupons(new BigDecimal("100.00"), List.of(coupon));
        assertEquals(new BigDecimal("0.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("100.00"), result.getFinalAmount());
        assertEquals(List.of("NO_DISCOUNT"), result.getAppliedCouponCodes());
    }
}

