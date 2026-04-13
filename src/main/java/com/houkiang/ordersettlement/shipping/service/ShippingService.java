package com.houkiang.ordersettlement.shipping.service;

import com.houkiang.ordersettlement.common.util.MoneyUtils;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.RegionType;
import com.houkiang.ordersettlement.shipping.domain.ShippingCalculationResult;
import com.houkiang.ordersettlement.shipping.domain.ShippingFeeRule;
import com.houkiang.ordersettlement.shipping.exception.InvalidAddressException;
import com.houkiang.ordersettlement.shipping.exception.InvalidShippingRuleException;
import com.houkiang.ordersettlement.shipping.exception.UnsupportedDeliveryTypeException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

public class ShippingService {

    private static final BigDecimal STANDARD_NORMAL_FEE = new BigDecimal("8.00");
    private static final BigDecimal STANDARD_REMOTE_FEE = new BigDecimal("15.00");
    private static final BigDecimal EXPRESS_NORMAL_FEE = new BigDecimal("15.00");
    private static final BigDecimal STANDARD_FREE_THRESHOLD = new BigDecimal("88.00");
    private static final BigDecimal EXPRESS_FREE_THRESHOLD = new BigDecimal("188.00");

    private final Map<String, ShippingFeeRule> rules;

    public ShippingService() {
        this.rules = Map.of(
                key(DeliveryType.STANDARD, RegionType.NORMAL),
                new ShippingFeeRule(DeliveryType.STANDARD, RegionType.NORMAL,
                        STANDARD_NORMAL_FEE, STANDARD_FREE_THRESHOLD),
                key(DeliveryType.STANDARD, RegionType.REMOTE),
                new ShippingFeeRule(DeliveryType.STANDARD, RegionType.REMOTE,
                        STANDARD_REMOTE_FEE, null),
                key(DeliveryType.EXPRESS, RegionType.NORMAL),
                new ShippingFeeRule(DeliveryType.EXPRESS, RegionType.NORMAL,
                        EXPRESS_NORMAL_FEE, EXPRESS_FREE_THRESHOLD),
                key(DeliveryType.SELF_PICKUP, RegionType.NORMAL),
                new ShippingFeeRule(DeliveryType.SELF_PICKUP, RegionType.NORMAL,
                        MoneyUtils.ZERO, BigDecimal.ZERO),
                key(DeliveryType.SELF_PICKUP, RegionType.REMOTE),
                new ShippingFeeRule(DeliveryType.SELF_PICKUP, RegionType.REMOTE,
                        MoneyUtils.ZERO, BigDecimal.ZERO));
    }

    public ShippingService(Map<String, ShippingFeeRule> rules) {
        this.rules = Map.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
    }

    public void validateAddress(Address address) {
        if (address == null) {
            throw new InvalidAddressException("Address must not be null");
        }
        if (isBlank(address.getRecipientName())) {
            throw new InvalidAddressException("Recipient name must not be blank");
        }
        if (isBlank(address.getPhone())) {
            throw new InvalidAddressException("Phone must not be blank");
        }
        if (isBlank(address.getDetailAddress())) {
            throw new InvalidAddressException("Detail address must not be blank");
        }
        if (address.getRegionType() == null) {
            throw new InvalidAddressException("Region type must not be null");
        }
    }

    public void validateDeliveryType(DeliveryType deliveryType, RegionType regionType) {
        Objects.requireNonNull(deliveryType, "deliveryType must not be null");
        Objects.requireNonNull(regionType, "regionType must not be null");
        if (deliveryType == DeliveryType.EXPRESS && regionType == RegionType.REMOTE) {
            throw new UnsupportedDeliveryTypeException("Express delivery is not supported in remote regions");
        }
        if (!rules.containsKey(key(deliveryType, regionType))) {
            throw new UnsupportedDeliveryTypeException("Unsupported delivery type or region");
        }
    }

    public ShippingCalculationResult calculateShippingFee(
            BigDecimal itemAmount,
            Address address,
            DeliveryType deliveryType) {
        BigDecimal amount = MoneyUtils.requireNonNegative(itemAmount, "itemAmount");
        validateAddress(address);
        validateDeliveryType(deliveryType, address.getRegionType());

        ShippingFeeRule rule = rules.get(key(deliveryType, address.getRegionType()));
        validateRule(rule);

        if (deliveryType == DeliveryType.SELF_PICKUP) {
            return new ShippingCalculationResult(deliveryType, MoneyUtils.ZERO, true);
        }
        if (rule.getFreeShippingThreshold() != null
                && amount.compareTo(rule.getFreeShippingThreshold()) >= 0) {
            return new ShippingCalculationResult(deliveryType, MoneyUtils.ZERO, true);
        }
        return new ShippingCalculationResult(deliveryType, MoneyUtils.requireNonNegative(rule.getBaseFee(), "baseFee"), false);
    }

    public static String key(DeliveryType deliveryType, RegionType regionType) {
        return deliveryType.name() + ":" + regionType.name();
    }

    private void validateRule(ShippingFeeRule rule) {
        if (rule == null) {
            throw new InvalidShippingRuleException("Shipping rule must not be null");
        }
        if (rule.getBaseFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidShippingRuleException("Base shipping fee must not be negative");
        }
        if (rule.getFreeShippingThreshold() != null
                && rule.getFreeShippingThreshold().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidShippingRuleException("Free shipping threshold must not be negative");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
