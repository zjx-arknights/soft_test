package com.houkiang.ordersettlement.persistence.model;

import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.promotion.domain.CouponType;
import java.math.BigDecimal;

public record OrderCouponRow(
        String orderId, int couponPosition, String couponCode, CouponType couponType,
        BigDecimal thresholdAmount, BigDecimal reductionAmount, BigDecimal discountRate, boolean stackable) {

    public static OrderCouponRow from(String orderId, int position, Coupon coupon) {
        return new OrderCouponRow(orderId, position, coupon.getCouponCode(), coupon.getCouponType(),
                coupon.getThresholdAmount(), coupon.getReductionAmount(), coupon.getDiscountRate(), coupon.isStackable());
    }

    public Coupon toCoupon() {
        return new Coupon(couponCode, couponType, thresholdAmount, reductionAmount, discountRate, stackable);
    }
}
