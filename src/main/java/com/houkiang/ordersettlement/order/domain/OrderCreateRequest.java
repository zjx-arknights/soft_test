package com.houkiang.ordersettlement.order.domain;

import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import java.util.Collections;
import java.util.List;

public class OrderCreateRequest {

    private final ShoppingCart shoppingCart;
    private final Address address;
    private final DeliveryType deliveryType;
    private final List<Coupon> coupons;

    public OrderCreateRequest(
            ShoppingCart shoppingCart,
            Address address,
            DeliveryType deliveryType,
            List<Coupon> coupons) {
        this.shoppingCart = shoppingCart;
        this.address = address;
        this.deliveryType = deliveryType;
        this.coupons = coupons == null ? Collections.emptyList() : List.copyOf(coupons);
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
}
