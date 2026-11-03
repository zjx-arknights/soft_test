package com.houkiang.ordersettlement.order.repository;

import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(String orderId);

    boolean updateStatus(String orderId, OrderStatus expectedStatus, OrderStatus targetStatus);

    boolean wasStockReserved(String orderId);
}
