package com.houkiang.ordersettlement.cart.service;

import com.houkiang.ordersettlement.cart.domain.CartItem;
import com.houkiang.ordersettlement.cart.domain.Product;
import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.cart.exception.InvalidPriceException;
import com.houkiang.ordersettlement.cart.exception.InvalidQuantityException;
import com.houkiang.ordersettlement.cart.exception.ProductNotFoundInCartException;
import java.math.BigDecimal;
import java.util.Objects;

public class ShoppingCartService {

    public void addItem(ShoppingCart cart, Product product, int quantity) {
        Objects.requireNonNull(cart, "cart must not be null");
        Objects.requireNonNull(product, "product must not be null");
        validateQuantity(quantity);
        validatePrice(product.getUnitPrice());

        cart.findItemByProductId(product.getProductId())
                .ifPresentOrElse(
                        item -> item.increaseQuantity(quantity),
                        () -> cart.addItem(new CartItem(product, quantity)));
    }

    public void removeItem(ShoppingCart cart, String productId) {
        Objects.requireNonNull(cart, "cart must not be null");
        validateProductId(productId);
        CartItem item = cart.findItemByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundInCartException("Product is not in cart: " + productId));
        cart.removeItem(item);
    }

    public void updateQuantity(ShoppingCart cart, String productId, int newQuantity) {
        Objects.requireNonNull(cart, "cart must not be null");
        validateProductId(productId);
        validateQuantity(newQuantity);
        CartItem item = cart.findItemByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundInCartException("Product is not in cart: " + productId));
        item.setQuantity(newQuantity);
    }

    public BigDecimal calculateItemSubtotal(CartItem item) {
        Objects.requireNonNull(item, "item must not be null");
        validateQuantity(item.getQuantity());
        validatePrice(item.getProduct().getUnitPrice());
        return item.getProduct().getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    public BigDecimal calculateCartSubtotal(ShoppingCart cart) {
        Objects.requireNonNull(cart, "cart must not be null");
        return cart.getItems().stream()
                .map(this::calculateItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero");
        }
    }

    private void validatePrice(BigDecimal unitPrice) {
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPriceException("Product price must not be negative");
        }
    }

    private void validateProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
    }
}
