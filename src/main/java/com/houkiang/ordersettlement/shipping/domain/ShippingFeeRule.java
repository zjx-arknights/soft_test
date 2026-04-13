package com.houkiang.ordersettlement.shipping.domain;

import java.math.BigDecimal;
import java.util.Objects;

public class ShippingFeeRule {

    private final DeliveryType deliveryType;
    private final RegionType regionType;
    private final BigDecimal baseFee;
    private final BigDecimal freeShippingThreshold;

    public ShippingFeeRule(
            DeliveryType deliveryType,
            RegionType regionType,
            BigDecimal baseFee,
            BigDecimal freeShippingThreshold) {
        this.deliveryType = Objects.requireNonNull(deliveryType, "deliveryType must not be null");
        this.regionType = Objects.requireNonNull(regionType, "regionType must not be null");
        this.baseFee = Objects.requireNonNull(baseFee, "baseFee must not be null");
        this.freeShippingThreshold = freeShippingThreshold;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public RegionType getRegionType() {
        return regionType;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public BigDecimal getFreeShippingThreshold() {
        return freeShippingThreshold;
    }
}
