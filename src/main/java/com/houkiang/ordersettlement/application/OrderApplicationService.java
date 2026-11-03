package com.houkiang.ordersettlement.application;

import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.order.exception.OrderNotFoundException;
import com.houkiang.ordersettlement.order.exception.OrderUpdateConflictException;
import com.houkiang.ordersettlement.order.repository.OrderRepository;
import com.houkiang.ordersettlement.order.repository.IdempotencyRepository;
import com.houkiang.ordersettlement.cart.repository.InventoryRepository;
import com.houkiang.ordersettlement.order.exception.InsufficientStockException;
import com.houkiang.ordersettlement.order.service.OrderRequestAssembler;
import com.houkiang.ordersettlement.order.service.OrderRequestAssembler.ItemSelection;
import com.houkiang.ordersettlement.order.service.OrderService;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderApplicationService {

    private final OrderRequestAssembler assembler;
    private final OrderService orderService;
    private final OrderRepository repository;
    private final InventoryRepository inventory;
    private final IdempotencyRepository idempotency;

    public OrderApplicationService(OrderRequestAssembler assembler, OrderService orderService,
            OrderRepository repository, InventoryRepository inventory, IdempotencyRepository idempotency) {
        this.assembler = Objects.requireNonNull(assembler, "assembler must not be null");
        this.orderService = Objects.requireNonNull(orderService, "orderService must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.inventory = Objects.requireNonNull(inventory, "inventory must not be null");
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency must not be null");
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Order create(List<ItemSelection> items, Address address, DeliveryType deliveryType, List<String> couponCodes) {
        return createOrder(items, address, deliveryType, couponCodes, null, null);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Order createWithIdempotency(List<ItemSelection> items, Address address, DeliveryType deliveryType,
            List<String> couponCodes, String key, String fingerprint) {
        idempotency.claim(key, fingerprint);
        return createOrder(items, address, deliveryType, couponCodes, key, fingerprint);
    }

    private Order createOrder(List<ItemSelection> items, Address address, DeliveryType deliveryType,
            List<String> couponCodes, String key, String fingerprint) {
        Order order = orderService.createOrder(assembler.assemble(items, address, deliveryType, couponCodes));
        order.getShoppingCart().getItems().stream()
                .sorted(java.util.Comparator.comparing(item -> item.getProduct().getProductId()))
                .forEach(item -> {
                    if (!inventory.reserve(item.getProduct().getProductId(), item.getQuantity())) {
                        throw new InsufficientStockException("Insufficient stock for product: "
                                + item.getProduct().getProductId());
                    }
                });
        repository.save(order);
        if (key != null) {
            idempotency.complete(key, fingerprint, order.getOrderId());
        }
        return order;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Order find(String orderId) {
        return repository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Order transition(String orderId, OrderStatus targetStatus) {
        Order order = find(orderId);
        OrderStatus expectedStatus = order.getStatus();
        orderService.transitionStatus(order, targetStatus);
        if (!repository.updateStatus(orderId, expectedStatus, targetStatus)) {
            throw new OrderUpdateConflictException(orderId);
        }
        if (targetStatus == OrderStatus.CANCELLED && repository.wasStockReserved(orderId)) {
            order.getShoppingCart().getItems().stream()
                    .sorted(java.util.Comparator.comparing(item -> item.getProduct().getProductId()))
                    .forEach(item -> inventory.release(item.getProduct().getProductId(), item.getQuantity()));
        }
        return order;
    }
}
