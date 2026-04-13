package com.houkiang.ordersettlement.shipping.domain;

import java.math.BigDecimal;

public class ShippingCalculationResult {

    private final DeliveryType deliveryType;
    private final BigDecimal shippingFee;
    private final boolean freeShippingApplied;

    public ShippingCalculationResult(DeliveryType deliveryType, BigDecimal shippingFee, boolean freeShippingApplied) {
        this.deliveryType = deliveryType;
        this.shippingFee = shippingFee;
        this.freeShippingApplied = freeShippingApplied;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public boolean isFreeShippingApplied() {
        return freeShippingApplied;
    }
}
