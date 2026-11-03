package com.houkiang.ordersettlement.order.service;

import com.houkiang.ordersettlement.cart.domain.Product;
import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.cart.service.ShoppingCartService;
import com.houkiang.ordersettlement.catalog.CouponCatalog;
import com.houkiang.ordersettlement.catalog.CouponNotFoundException;
import com.houkiang.ordersettlement.catalog.ProductCatalog;
import com.houkiang.ordersettlement.catalog.ProductNotFoundException;
import com.houkiang.ordersettlement.order.domain.OrderCreateRequest;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.promotion.exception.InvalidCouponException;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class OrderRequestAssembler {

    private final ProductCatalog productCatalog;
    private final CouponCatalog couponCatalog;
    private final ShoppingCartService shoppingCartService;

    public OrderRequestAssembler(ProductCatalog productCatalog, CouponCatalog couponCatalog,
            ShoppingCartService shoppingCartService) {
        this.productCatalog = Objects.requireNonNull(productCatalog, "productCatalog must not be null");
        this.couponCatalog = Objects.requireNonNull(couponCatalog, "couponCatalog must not be null");
        this.shoppingCartService = Objects.requireNonNull(shoppingCartService, "shoppingCartService must not be null");
    }

    public OrderCreateRequest assemble(
            List<ItemSelection> items, Address address, DeliveryType deliveryType, List<String> couponCodes) {
        if (new HashSet<>(couponCodes).size() != couponCodes.size()) {
            throw new InvalidCouponException("The same coupon code cannot be applied more than once");
        }
        ShoppingCart cart = new ShoppingCart();
        for (ItemSelection item : items) {
            Product product = productCatalog.findProduct(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));
            shoppingCartService.addItem(cart, product, item.quantity());
        }
        List<Coupon> coupons = couponCodes.stream()
                .map(code -> couponCatalog.findCoupon(code).orElseThrow(() -> new CouponNotFoundException(code)))
                .toList();
        return new OrderCreateRequest(cart, address, deliveryType, coupons);
    }

    public record ItemSelection(String productId, int quantity) {
    }
}
