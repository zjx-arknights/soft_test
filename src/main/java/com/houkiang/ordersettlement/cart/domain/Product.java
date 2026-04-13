package com.houkiang.ordersettlement.cart.domain;

import com.houkiang.ordersettlement.cart.exception.InvalidPriceException;
import java.math.BigDecimal;
import java.util.Objects;

public final class Product {

    private final String productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int stock;

    public Product(String productId, String productName, BigDecimal unitPrice, int stock) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName must not be blank");
        }
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPriceException("Product price must not be negative");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.stock = stock;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getStock() {
        return stock;
    }
}
