package com.houkiang.ordersettlement.order.domain;

import java.math.BigDecimal;

public class OrderSummary {

    private final BigDecimal itemAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal shippingFee;
    private final BigDecimal payableAmount;

    public OrderSummary(
            BigDecimal itemAmount,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            BigDecimal payableAmount) {
        this.itemAmount = itemAmount;
        this.discountAmount = discountAmount;
        this.shippingFee = shippingFee;
        this.payableAmount = payableAmount;
    }

    public BigDecimal getItemAmount() {
        return itemAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }
}
