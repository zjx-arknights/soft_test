package com.houkiang.ordersettlement.promotion.domain;

import java.math.BigDecimal;
import java.util.List;

public class PromotionCalculationResult {

    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal finalAmount;
    private final List<String> appliedCouponCodes;

    public PromotionCalculationResult(
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            List<String> appliedCouponCodes) {
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.appliedCouponCodes = List.copyOf(appliedCouponCodes);
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public List<String> getAppliedCouponCodes() {
        return appliedCouponCodes;
    }
}
