package com.houkiang.ordersettlement.cart.repository;

public interface InventoryRepository {
    boolean reserve(String productId, int quantity);
    boolean release(String productId, int quantity);
}
