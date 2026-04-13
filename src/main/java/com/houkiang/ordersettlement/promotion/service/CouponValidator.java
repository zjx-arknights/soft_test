package com.houkiang.ordersettlement.promotion.service;

import com.houkiang.ordersettlement.common.util.MoneyUtils;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.promotion.exception.CouponNotApplicableException;
import com.houkiang.ordersettlement.promotion.exception.InvalidCouponException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class CouponValidator {

    public void validateCoupon(Coupon coupon, BigDecimal orderAmount) {
        Objects.requireNonNull(coupon, "coupon must not be null");
        MoneyUtils.requireNonNegative(orderAmount, "orderAmount");
        validateThreshold(coupon);

        if (orderAmount.compareTo(coupon.getThresholdAmount()) < 0) {
            throw new CouponNotApplicableException("Order amount does not meet coupon threshold");
        }

        switch (coupon.getCouponType()) {
            case FULL_REDUCTION, FIXED_AMOUNT -> validateReductionAmount(coupon);
            case DISCOUNT -> validateDiscountRate(coupon);
            default -> throw new InvalidCouponException("Unsupported coupon type");
        }
    }

    public void validateStackable(List<Coupon> coupons) {
        Objects.requireNonNull(coupons, "coupons must not be null");
        for (Coupon coupon : coupons) {
            Objects.requireNonNull(coupon, "coupon in list must not be null");
        }
    }

    private void validateThreshold(Coupon coupon) {
        BigDecimal threshold = coupon.getThresholdAmount();
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidCouponException("Coupon threshold must not be negative");
        }
    }

    private void validateReductionAmount(Coupon coupon) {
        BigDecimal reductionAmount = coupon.getReductionAmount();
        if (reductionAmount == null || reductionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCouponException("Coupon reduction amount must be greater than zero");
        }
    }

    private void validateDiscountRate(Coupon coupon) {
        BigDecimal discountRate = coupon.getDiscountRate();
        if (discountRate == null
                || discountRate.compareTo(BigDecimal.ZERO) <= 0
                || discountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidCouponException("Discount rate must be greater than 0 and less than or equal to 1");
        }
    }
}
