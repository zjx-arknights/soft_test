package com.houkiang.ordersettlement.order.service;

import com.houkiang.ordersettlement.cart.service.ShoppingCartService;
import com.houkiang.ordersettlement.common.util.MoneyUtils;
import com.houkiang.ordersettlement.order.domain.OrderCreateRequest;
import com.houkiang.ordersettlement.order.domain.OrderSummary;
import com.houkiang.ordersettlement.order.exception.OrderCreationException;
import com.houkiang.ordersettlement.promotion.domain.PromotionCalculationResult;
import com.houkiang.ordersettlement.promotion.service.PromotionService;
import com.houkiang.ordersettlement.shipping.domain.ShippingCalculationResult;
import com.houkiang.ordersettlement.shipping.service.ShippingService;
import java.math.BigDecimal;
import java.util.Objects;

public class OrderPricingService {

    private final ShoppingCartService shoppingCartService;
    private final PromotionService promotionService;
    private final ShippingService shippingService;
    private final OrderValidator orderValidator;

    public OrderPricingService(
            ShoppingCartService shoppingCartService,
            PromotionService promotionService,
            ShippingService shippingService,
            OrderValidator orderValidator) {
        this.shoppingCartService = Objects.requireNonNull(shoppingCartService, "shoppingCartService must not be null");
        this.promotionService = Objects.requireNonNull(promotionService, "promotionService must not be null");
        this.shippingService = Objects.requireNonNull(shippingService, "shippingService must not be null");
        this.orderValidator = Objects.requireNonNull(orderValidator, "orderValidator must not be null");
    }

    public OrderPricingService() {
        this(new ShoppingCartService(), new PromotionService(), new ShippingService(), new OrderValidator());
    }

    public OrderSummary calculateOrderSummary(OrderCreateRequest request) {
        if (request == null) {
            throw new OrderCreationException("Order create request must not be null");
        }
        orderValidator.validateCartNotEmpty(request.getShoppingCart());
        orderValidator.validateStock(request.getShoppingCart());
        shippingService.validateAddress(request.getAddress());

        BigDecimal itemAmount = shoppingCartService.calculateCartSubtotal(request.getShoppingCart());
        PromotionCalculationResult promotionResult = promotionService.applyCoupons(itemAmount, request.getCoupons());
        ShippingCalculationResult shippingResult = shippingService.calculateShippingFee(
                itemAmount, request.getAddress(), request.getDeliveryType());
        BigDecimal payableAmount = MoneyUtils.minZero(promotionResult.getFinalAmount().add(shippingResult.getShippingFee()));

        return new OrderSummary(
                itemAmount,
                promotionResult.getDiscountAmount(),
                shippingResult.getShippingFee(),
                payableAmount);
    }
}
