package com.houkiang.ordersettlement.promotion.service;

import com.houkiang.ordersettlement.common.util.MoneyUtils;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.promotion.domain.PromotionCalculationResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PromotionService {

    private final CouponValidator couponValidator;

    public PromotionService(CouponValidator couponValidator) {
        this.couponValidator = Objects.requireNonNull(couponValidator, "couponValidator must not be null");
    }

    public PromotionService() {
        this(new CouponValidator());
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount, Coupon coupon) {
        BigDecimal amount = MoneyUtils.requireNonNegative(orderAmount, "orderAmount");
        couponValidator.validateCoupon(coupon, amount);
        BigDecimal discount = switch (coupon.getCouponType()) {
            case FULL_REDUCTION, FIXED_AMOUNT -> coupon.getReductionAmount();
            case DISCOUNT -> amount.multiply(BigDecimal.ONE.subtract(coupon.getDiscountRate()));
        };
        return discount.min(amount).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public PromotionCalculationResult applyCoupons(BigDecimal orderAmount, List<Coupon> coupons) {
        BigDecimal originalAmount = MoneyUtils.requireNonNegative(orderAmount, "orderAmount");
        List<Coupon> safeCoupons = coupons == null ? Collections.emptyList() : coupons;
        couponValidator.validateStackable(safeCoupons);

        BigDecimal currentAmount = originalAmount;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<String> appliedCodes = new ArrayList<>();

        for (Coupon coupon : safeCoupons) {
            BigDecimal discount = calculateDiscount(currentAmount, coupon);
            totalDiscount = totalDiscount.add(discount);
            currentAmount = MoneyUtils.minZero(currentAmount.subtract(discount));
            appliedCodes.add(coupon.getCouponCode());
        }

        return new PromotionCalculationResult(
                originalAmount,
                totalDiscount.setScale(2, java.math.RoundingMode.HALF_UP),
                currentAmount,
                appliedCodes);
    }
}
