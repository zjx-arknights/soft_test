package com.houkiang.ordersettlement.web.dto;

import com.houkiang.ordersettlement.cart.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(String productId, String productName, BigDecimal unitPrice, int stock) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getProductId(), product.getProductName(),
                product.getUnitPrice(), product.getStock());
    }
}
