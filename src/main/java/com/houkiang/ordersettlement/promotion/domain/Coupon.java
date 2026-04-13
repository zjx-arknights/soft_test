package com.houkiang.ordersettlement.promotion.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class Coupon {

    private final String couponCode;
    private final CouponType couponType;
    private final BigDecimal thresholdAmount;
    private final BigDecimal reductionAmount;
    private final BigDecimal discountRate;
    private final boolean stackable;

    public Coupon(
            String couponCode,
            CouponType couponType,
            BigDecimal thresholdAmount,
            BigDecimal reductionAmount,
            BigDecimal discountRate,
            boolean stackable) {
        if (couponCode == null || couponCode.isBlank()) {
            throw new IllegalArgumentException("couponCode must not be blank");
        }
        this.couponCode = couponCode;
        this.couponType = Objects.requireNonNull(couponType, "couponType must not be null");
        this.thresholdAmount = thresholdAmount;
        this.reductionAmount = reductionAmount;
        this.discountRate = discountRate;
        this.stackable = stackable;
    }

    public static Coupon fullReduction(String code, BigDecimal thresholdAmount, BigDecimal reductionAmount) {
        return new Coupon(code, CouponType.FULL_REDUCTION, thresholdAmount, reductionAmount, null, true);
    }

    public static Coupon discount(String code, BigDecimal thresholdAmount, BigDecimal discountRate) {
        return new Coupon(code, CouponType.DISCOUNT, thresholdAmount, null, discountRate, true);
    }

    public static Coupon fixedAmount(String code, BigDecimal thresholdAmount, BigDecimal reductionAmount) {
        return new Coupon(code, CouponType.FIXED_AMOUNT, thresholdAmount, reductionAmount, null, true);
    }

    public String getCouponCode() {
        return couponCode;
    }

    public CouponType getCouponType() {
        return couponType;
    }

    public BigDecimal getThresholdAmount() {
        return thresholdAmount;
    }

    public BigDecimal getReductionAmount() {
        return reductionAmount;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public boolean isStackable() {
        return stackable;
    }
}
