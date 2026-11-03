package com.houkiang.ordersettlement.persistence.model;

import com.houkiang.ordersettlement.cart.domain.CartItem;
import com.houkiang.ordersettlement.cart.domain.Product;
import java.math.BigDecimal;

public record OrderItemRow(
        String orderId, int lineNumber, String productId, String productName,
        BigDecimal unitPrice, int stockSnapshot, int quantity) {

    public static OrderItemRow from(String orderId, int lineNumber, CartItem item) {
        Product product = item.getProduct();
        return new OrderItemRow(orderId, lineNumber, product.getProductId(), product.getProductName(),
                product.getUnitPrice(), product.getStock(), item.getQuantity());
    }

    public CartItem toCartItem() {
        return new CartItem(new Product(productId, productName, unitPrice, stockSnapshot), quantity);
    }
}
