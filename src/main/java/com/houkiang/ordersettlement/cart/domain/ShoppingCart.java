package com.houkiang.ordersettlement.cart.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ShoppingCart {

    private final List<CartItem> items = new ArrayList<>();

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Optional<CartItem> findItemByProductId(String productId) {
        return items.stream()
                .filter(item -> item.getProduct().getProductId().equals(productId))
                .findFirst();
    }

    public void addItem(CartItem item) {
        items.add(item);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }
}
