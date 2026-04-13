package com.houkiang.ordersettlement.order.domain;

import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class Order {

    private final String orderId;
    private final ShoppingCart shoppingCart;
    private final Address address;
    private final DeliveryType deliveryType;
    private final List<Coupon> coupons;
    private final BigDecimal itemAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal shippingFee;
    private final BigDecimal payableAmount;
    private OrderStatus status;

    public Order(
            String orderId,
            ShoppingCart shoppingCart,
            Address address,
            DeliveryType deliveryType,
            List<Coupon> coupons,
            OrderSummary summary,
            OrderStatus status) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        this.orderId = orderId;
        this.shoppingCart = Objects.requireNonNull(shoppingCart, "shoppingCart must not be null");
        this.address = Objects.requireNonNull(address, "address must not be null");
        this.deliveryType = Objects.requireNonNull(deliveryType, "deliveryType must not be null");
        this.coupons = List.copyOf(Objects.requireNonNull(coupons, "coupons must not be null"));
        Objects.requireNonNull(summary, "summary must not be null");
        this.itemAmount = summary.getItemAmount();
        this.discountAmount = summary.getDiscountAmount();
        this.shippingFee = summary.getShippingFee();
        this.payableAmount = summary.getPayableAmount();
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public String getOrderId() {
        return orderId;
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCart;
    }

    public Address getAddress() {
        return address;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public List<Coupon> getCoupons() {
        return coupons;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }
}
