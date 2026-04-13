package com.houkiang.ordersettlement.cart.domain;

import com.houkiang.ordersettlement.cart.exception.InvalidQuantityException;
import java.util.Objects;

public final class CartItem {

    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = Objects.requireNonNull(product, "product must not be null");
        setQuantity(quantity);
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero");
        }
        this.quantity = quantity;
    }

    public void increaseQuantity(int increment) {
        if (increment <= 0) {
            throw new InvalidQuantityException("Quantity increment must be greater than zero");
        }
        this.quantity += increment;
    }
}
