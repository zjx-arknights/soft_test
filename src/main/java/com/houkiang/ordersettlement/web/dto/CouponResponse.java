package com.houkiang.ordersettlement.web.dto;

import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.promotion.domain.CouponType;
import java.math.BigDecimal;

public record CouponResponse(
        String couponCode, CouponType couponType, BigDecimal thresholdAmount,
        BigDecimal reductionAmount, BigDecimal discountRate) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(coupon.getCouponCode(), coupon.getCouponType(),
                coupon.getThresholdAmount(), coupon.getReductionAmount(), coupon.getDiscountRate());
    }
}
