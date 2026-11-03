package com.houkiang.ordersettlement.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.houkiang.ordersettlement.cart.domain.Product;
import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.cart.service.ShoppingCartService;
import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderCreateRequest;
import com.houkiang.ordersettlement.order.repository.OrderRepository;
import com.houkiang.ordersettlement.order.service.OrderService;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.RegionType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderAtomicityTest {

    @Autowired
    OrderRepository repository;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void rollsBackHeaderWhenItemInsertFails() {
        assertRolledBack(order("x".repeat(201), List.of()));
    }

    @Test
    void rollsBackHeaderAndItemsWhenCouponInsertFails() {
        assertRolledBack(order("Valid product",
                List.of(Coupon.fixedAmount("x".repeat(65), BigDecimal.ZERO, BigDecimal.ONE))));
    }

    private void assertRolledBack(Order order) {
        // No enclosing test transaction: the repository must perform the rollback itself.
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(order));
        assertTrue(repository.findById(order.getOrderId()).isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM order_items WHERE order_id = ?",
                Integer.class, order.getOrderId()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM order_coupons WHERE order_id = ?",
                Integer.class, order.getOrderId()));
    }

    private Order order(String productName, List<Coupon> coupons) {
        ShoppingCart cart = new ShoppingCart();
        new ShoppingCartService().addItem(cart, new Product("P001", productName, new BigDecimal("50.00"), 20), 2);
        return new OrderService().createOrder(new OrderCreateRequest(cart,
                new Address("Test", "123", "Test Road", RegionType.NORMAL), DeliveryType.STANDARD, coupons));
    }
}
