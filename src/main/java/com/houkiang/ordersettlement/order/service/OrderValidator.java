package com.houkiang.ordersettlement.order.service;

import com.houkiang.ordersettlement.cart.domain.CartItem;
import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.order.exception.EmptyCartException;
import com.houkiang.ordersettlement.order.exception.InsufficientStockException;
import java.util.Objects;

public class OrderValidator {

    public void validateCartNotEmpty(ShoppingCart cart) {
        Objects.requireNonNull(cart, "cart must not be null");
        if (cart.isEmpty()) {
            throw new EmptyCartException("Cart must not be empty");
        }
    }

    public void validateStock(ShoppingCart cart) {
        Objects.requireNonNull(cart, "cart must not be null");
        for (CartItem item : cart.getItems()) {
            if (item.getQuantity() > item.getProduct().getStock()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + item.getProduct().getProductId());
            }
        }
    }
}
