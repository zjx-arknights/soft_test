package com.houkiang.ordersettlement.order.service;

import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderCreateRequest;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.order.domain.OrderSummary;
import com.houkiang.ordersettlement.order.exception.OrderCreationException;
import java.util.Objects;

public class OrderService {

    private final OrderPricingService orderPricingService;
    private final OrderStatusService orderStatusService;
    private final OrderIdGenerator orderIdGenerator;

    public OrderService(
            OrderPricingService orderPricingService,
            OrderStatusService orderStatusService,
            OrderIdGenerator orderIdGenerator) {
        this.orderPricingService = Objects.requireNonNull(orderPricingService, "orderPricingService must not be null");
        this.orderStatusService = Objects.requireNonNull(orderStatusService, "orderStatusService must not be null");
        this.orderIdGenerator = Objects.requireNonNull(orderIdGenerator, "orderIdGenerator must not be null");
    }

    public OrderService() {
        this(new OrderPricingService(), new OrderStatusService(), new DefaultOrderIdGenerator());
    }

    public Order createOrder(OrderCreateRequest request) {
        if (request == null) {
            throw new OrderCreationException("Order create request must not be null");
        }
        OrderSummary summary = orderPricingService.calculateOrderSummary(request);
        String orderId = orderIdGenerator.generate();
        if (orderId == null || orderId.isBlank()) {
            throw new OrderCreationException("Generated order id must not be blank");
        }
        return new Order(
                orderId,
                request.getShoppingCart(),
                request.getAddress(),
                request.getDeliveryType(),
                request.getCoupons(),
                summary,
                OrderStatus.PENDING_PAYMENT);
    }

    public void transitionStatus(Order order, OrderStatus targetStatus) {
        orderStatusService.transitionStatus(order, targetStatus);
    }
}
