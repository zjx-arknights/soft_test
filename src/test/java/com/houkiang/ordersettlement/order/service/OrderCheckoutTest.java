package com.houkiang.ordersettlement.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.houkiang.ordersettlement.cart.domain.Product;
import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.cart.service.ShoppingCartService;
import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderCreateRequest;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.order.exception.InvalidOrderStatusTransitionException;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.RegionType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderCheckoutTest {

    @Test
    @DisplayName("真实服务协作：三类券顺序叠加、原价包邮及订单完成流程")
    void checkout_ShouldApplyCouponsAndFreeShippingBeforeCompletingOrder() {
        ShoppingCart cart = new ShoppingCart();
        new ShoppingCartService().addItem(
                cart, new Product("P001", "Book", new BigDecimal("50.00"), 2), 2);
        List<Coupon> coupons = List.of(
                Coupon.fullReduction("FULL", new BigDecimal("100.00"), new BigDecimal("20.00")),
                Coupon.discount("RATE", BigDecimal.ZERO, new BigDecimal("0.90")),
                Coupon.fixedAmount("FIXED", BigDecimal.ZERO, new BigDecimal("10.00")));
        OrderCreateRequest request = new OrderCreateRequest(
                cart, new Address("张三", "13800138000", "北京市朝阳区测试路1号", RegionType.NORMAL),
                DeliveryType.STANDARD, coupons);
        OrderService service = new OrderService(
                new OrderPricingService(), new OrderStatusService(), () -> "ORD-CHECKOUT");

        Order order = service.createOrder(request);

        assertEquals("ORD-CHECKOUT", order.getOrderId());
        assertEquals(new BigDecimal("100.00"), order.getItemAmount());
        assertEquals(new BigDecimal("38.00"), order.getDiscountAmount());
        // 包邮按优惠前商品金额判断，优惠后 62 元仍然包邮。
        assertEquals(new BigDecimal("0.00"), order.getShippingFee());
        assertEquals(new BigDecimal("62.00"), order.getPayableAmount());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());

        service.transitionStatus(order, OrderStatus.PAID);
        service.transitionStatus(order, OrderStatus.SHIPPED);
        service.transitionStatus(order, OrderStatus.COMPLETED);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> service.transitionStatus(order, OrderStatus.CANCELLED));
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }
}
