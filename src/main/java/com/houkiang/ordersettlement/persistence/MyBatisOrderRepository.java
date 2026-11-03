package com.houkiang.ordersettlement.persistence;

import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.order.domain.OrderSummary;
import com.houkiang.ordersettlement.order.repository.OrderRepository;
import com.houkiang.ordersettlement.persistence.mapper.OrderMapper;
import com.houkiang.ordersettlement.persistence.model.OrderCouponRow;
import com.houkiang.ordersettlement.persistence.model.OrderItemRow;
import com.houkiang.ordersettlement.persistence.model.OrderRow;
import com.houkiang.ordersettlement.shipping.domain.Address;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.IntStream;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MyBatisOrderRepository implements OrderRepository {

    private final OrderMapper mapper;

    public MyBatisOrderRepository(OrderMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public void save(Order order) {
        mapper.insertOrder(OrderRow.from(order));
        mapper.insertItems(IntStream.range(0, order.getShoppingCart().getItems().size())
                .mapToObj(index -> OrderItemRow.from(order.getOrderId(), index,
                        order.getShoppingCart().getItems().get(index))).toList());
        if (!order.getCoupons().isEmpty()) {
            mapper.insertCoupons(IntStream.range(0, order.getCoupons().size())
                    .mapToObj(index -> OrderCouponRow.from(order.getOrderId(), index, order.getCoupons().get(index)))
                    .toList());
        }
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(mapper.findOrder(orderId)).map(row -> {
            ShoppingCart cart = new ShoppingCart();
            mapper.findItems(orderId).forEach(item -> cart.addItem(item.toCartItem()));
            return new Order(row.orderId(), cart,
                    new Address(row.recipientName(), row.phone(), row.detailAddress(), row.regionType()),
                    row.deliveryType(), mapper.findCoupons(orderId).stream().map(OrderCouponRow::toCoupon).toList(),
                    new OrderSummary(row.itemAmount(), row.discountAmount(), row.shippingFee(), row.payableAmount()),
                    row.status());
        });
    }

    @Override
    public boolean updateStatus(String orderId, OrderStatus expectedStatus, OrderStatus targetStatus) {
        return mapper.updateStatus(orderId, expectedStatus, targetStatus) == 1;
    }

    @Override
    public boolean wasStockReserved(String orderId) {
        return mapper.wasStockReserved(orderId);
    }
}
