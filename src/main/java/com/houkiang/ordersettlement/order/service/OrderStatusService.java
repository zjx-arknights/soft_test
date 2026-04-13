package com.houkiang.ordersettlement.order.service;

import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.order.exception.InvalidOrderStatusTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class OrderStatusService {

    private final Map<OrderStatus, Set<OrderStatus>> allowedTransitions;

    public OrderStatusService() {
        this.allowedTransitions = new EnumMap<>(OrderStatus.class);
        allowedTransitions.put(OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        allowedTransitions.put(OrderStatus.PAID, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        allowedTransitions.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.COMPLETED));
        allowedTransitions.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        allowedTransitions.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        Objects.requireNonNull(currentStatus, "currentStatus must not be null");
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        return allowedTransitions.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    public void transitionStatus(Order order, OrderStatus targetStatus) {
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        if (!canTransition(order.getStatus(), targetStatus)) {
            throw new InvalidOrderStatusTransitionException(
                    "Cannot transition order from " + order.getStatus() + " to " + targetStatus);
        }
        order.setStatus(targetStatus);
    }
}
