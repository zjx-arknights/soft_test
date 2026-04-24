package com.houkiang.ordersettlement.order.service;

import com.houkiang.ordersettlement.cart.domain.CartItem;
import com.houkiang.ordersettlement.cart.domain.Product;
import com.houkiang.ordersettlement.cart.domain.ShoppingCart;
import com.houkiang.ordersettlement.cart.service.ShoppingCartService;
import com.houkiang.ordersettlement.order.domain.*;
import com.houkiang.ordersettlement.order.exception.*;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import com.houkiang.ordersettlement.promotion.domain.PromotionCalculationResult;
import com.houkiang.ordersettlement.promotion.service.PromotionService;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.ShippingCalculationResult;
import com.houkiang.ordersettlement.shipping.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderValidator 单元测试
 */
class OrderValidatorTest {

    private OrderValidator validator = new OrderValidator();

    @Test
    void validateCartNotEmpty_ShouldPass_WhenCartIsNotEmpty() {
        ShoppingCart cart = new ShoppingCart();
        Product product = new Product("P001", "Apple", new BigDecimal("5.00"), 10);
        cart.addItem(new CartItem(product, 1));
        assertDoesNotThrow(() -> validator.validateCartNotEmpty(cart));
    }

    @Test
    void validateCartNotEmpty_ShouldThrowEmptyCartException_WhenCartIsEmpty() {
        ShoppingCart emptyCart = new ShoppingCart();
        assertThrows(EmptyCartException.class, () -> validator.validateCartNotEmpty(emptyCart));
    }

    @Test
    void validateCartNotEmpty_ShouldThrowNullPointerException_WhenCartIsNull() {
        assertThrows(NullPointerException.class, () -> validator.validateCartNotEmpty(null));
    }

    @Test
    void validateStock_ShouldPass_WhenStockSufficient() {
        ShoppingCart cart = new ShoppingCart();
        Product product = new Product("P001", "Apple", new BigDecimal("5.00"), 10);
        cart.addItem(new CartItem(product, 5));
        assertDoesNotThrow(() -> validator.validateStock(cart));
    }

    @Test
    void validateStock_ShouldPass_WhenQuantityEqualsStock() {
        ShoppingCart cart = new ShoppingCart();
        Product product = new Product("P001", "Apple", new BigDecimal("5.00"), 10);
        cart.addItem(new CartItem(product, 10));
        assertDoesNotThrow(() -> validator.validateStock(cart));
    }

    @Test
    void validateStock_ShouldThrowInsufficientStockException_WhenQuantityExceedsStock() {
        ShoppingCart cart = new ShoppingCart();
        Product product = new Product("P001", "Apple", new BigDecimal("5.00"), 10);
        cart.addItem(new CartItem(product, 15));
        assertThrows(InsufficientStockException.class, () -> validator.validateStock(cart));
    }

    @Test
    void validateStock_ShouldThrowNullPointerException_WhenCartIsNull() {
        assertThrows(NullPointerException.class, () -> validator.validateStock(null));
    }
}

/**
 * OrderStatusService 单元测试
 */
class OrderStatusServiceTest {

    private OrderStatusService service = new OrderStatusService();

    @Test
    void canTransition_ShouldReturnTrue_ForValidTransitions() {
        assertTrue(service.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID));
        assertTrue(service.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        assertTrue(service.canTransition(OrderStatus.PAID, OrderStatus.SHIPPED));
        assertTrue(service.canTransition(OrderStatus.PAID, OrderStatus.CANCELLED));
        assertTrue(service.canTransition(OrderStatus.SHIPPED, OrderStatus.COMPLETED));
    }

    @Test
    void canTransition_ShouldReturnFalse_ForInvalidTransitions() {
        assertFalse(service.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.SHIPPED));
        assertFalse(service.canTransition(OrderStatus.PAID, OrderStatus.COMPLETED));
        assertFalse(service.canTransition(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        assertFalse(service.canTransition(OrderStatus.COMPLETED, OrderStatus.PAID));
        assertFalse(service.canTransition(OrderStatus.CANCELLED, OrderStatus.PENDING_PAYMENT));
    }

    @Test
    void canTransition_ShouldReturnFalse_ForTerminalStates() {
        assertFalse(service.canTransition(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        assertFalse(service.canTransition(OrderStatus.CANCELLED, OrderStatus.PAID));
    }

    @Test
    void transitionStatus_ShouldUpdateOrderStatus_WhenTransitionValid() {
        Order order = createOrderWithStatus(OrderStatus.PENDING_PAYMENT);
        service.transitionStatus(order, OrderStatus.PAID);
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void transitionStatus_ShouldThrowInvalidOrderStatusTransitionException_WhenTransitionInvalid() {
        Order order = createOrderWithStatus(OrderStatus.PAID);
        assertThrows(InvalidOrderStatusTransitionException.class,
                () -> service.transitionStatus(order, OrderStatus.PENDING_PAYMENT));
    }

    private Order createOrderWithStatus(OrderStatus status) {
        ShoppingCart cart = new ShoppingCart();
        Product product = new Product("P001", "Apple", new BigDecimal("5.00"), 10);
        cart.addItem(new CartItem(product, 1));
        OrderSummary summary = new OrderSummary(BigDecimal.valueOf(5), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(5));
        return new Order("ORD-1", cart, new Address("John Doe", "1234567890", "123 Main St", com.houkiang.ordersettlement.shipping.domain.RegionType.NORMAL), DeliveryType.STANDARD, List.of(), summary, status);
    }
}

/**
 * OrderPricingService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderPricingServiceTest {

    @Mock
    private ShoppingCartService shoppingCartService;

    @Mock
    private PromotionService promotionService;

    @Mock
    private ShippingService shippingService;

    @Mock
    private OrderValidator orderValidator;

    @InjectMocks
    private OrderPricingService orderPricingService;

    private ShoppingCart cart;
    private Address address;
    private List<Coupon> coupons;
    private OrderCreateRequest request;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart();
        Product product = new Product("P001", "Apple", new BigDecimal("10.00"), 10);
        cart.addItem(new CartItem(product, 2));
        address = new Address("John Doe", "1234567890", "123 Main St", com.houkiang.ordersettlement.shipping.domain.RegionType.NORMAL);
        coupons = List.of();
        request = new OrderCreateRequest(cart, address, DeliveryType.STANDARD, coupons);
    }

    @Test
    void calculateOrderSummary_ShouldReturnCorrectSummary_WhenNoPromotionAndStandardShipping() {
        BigDecimal itemAmount = new BigDecimal("20.00");
        when(shoppingCartService.calculateCartSubtotal(cart)).thenReturn(itemAmount);
        when(promotionService.applyCoupons(eq(itemAmount), eq(coupons)))
                .thenReturn(new PromotionCalculationResult(itemAmount, BigDecimal.ZERO, itemAmount, List.of()));
        when(shippingService.calculateShippingFee(eq(itemAmount), eq(address), eq(DeliveryType.STANDARD)))
                .thenReturn(new ShippingCalculationResult(DeliveryType.STANDARD, new BigDecimal("5.00"), false));
        doNothing().when(orderValidator).validateCartNotEmpty(cart);
        doNothing().when(orderValidator).validateStock(cart);
        doNothing().when(shippingService).validateAddress(address);

        OrderSummary summary = orderPricingService.calculateOrderSummary(request);

        assertEquals(itemAmount, summary.getItemAmount());
        assertEquals(BigDecimal.ZERO, summary.getDiscountAmount());
        assertEquals(new BigDecimal("5.00"), summary.getShippingFee());
        assertEquals(new BigDecimal("25.00"), summary.getPayableAmount());
    }

    @Test
    void calculateOrderSummary_ShouldApplyDiscountAndShipping_WhenPromotionExists() {
        BigDecimal itemAmount = new BigDecimal("100.00");
        BigDecimal discount = new BigDecimal("20.00");
        BigDecimal finalAmount = new BigDecimal("80.00");
        BigDecimal shipping = new BigDecimal("10.00");

        when(shoppingCartService.calculateCartSubtotal(cart)).thenReturn(itemAmount);
        when(promotionService.applyCoupons(eq(itemAmount), eq(coupons)))
                .thenReturn(new PromotionCalculationResult(itemAmount, discount, finalAmount, List.of()));
        when(shippingService.calculateShippingFee(any(BigDecimal.class), any(Address.class), any(DeliveryType.class)))
                .thenReturn(new ShippingCalculationResult(DeliveryType.STANDARD, shipping, false));
        doNothing().when(orderValidator).validateCartNotEmpty(cart);
        doNothing().when(orderValidator).validateStock(cart);
        doNothing().when(shippingService).validateAddress(address);

        OrderSummary summary = orderPricingService.calculateOrderSummary(request);

        assertEquals(itemAmount, summary.getItemAmount());
        assertEquals(discount, summary.getDiscountAmount());
        assertEquals(shipping, summary.getShippingFee());
        assertEquals(finalAmount.add(shipping), summary.getPayableAmount());
    }

    @Test
    void calculateOrderSummary_ShouldEnsurePayableAmountNotNegative_WhenDiscountExceedsItemAmount() {
        BigDecimal itemAmount = new BigDecimal("30.00");
        BigDecimal discount = new BigDecimal("40.00");
        BigDecimal finalAmount = new BigDecimal("-10.00");
        BigDecimal shipping = new BigDecimal("5.00");

        when(shoppingCartService.calculateCartSubtotal(cart)).thenReturn(itemAmount);
        when(promotionService.applyCoupons(eq(itemAmount), eq(coupons)))
                .thenReturn(new PromotionCalculationResult(itemAmount, discount, finalAmount, List.of()));
        when(shippingService.calculateShippingFee(any(BigDecimal.class), any(Address.class), any(DeliveryType.class)))
                .thenReturn(new ShippingCalculationResult(DeliveryType.STANDARD, shipping, false));
        doNothing().when(orderValidator).validateCartNotEmpty(cart);
        doNothing().when(orderValidator).validateStock(cart);
        doNothing().when(shippingService).validateAddress(address);

        OrderSummary summary = orderPricingService.calculateOrderSummary(request);

        // MoneyUtils.minZero 应确保 payableAmount 不低于 0
        assertEquals(0, summary.getPayableAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateOrderSummary_ShouldValidateCartNotEmpty() {
        doThrow(new EmptyCartException("Cart empty")).when(orderValidator).validateCartNotEmpty(cart);
        assertThrows(EmptyCartException.class, () -> orderPricingService.calculateOrderSummary(request));
        verify(orderValidator, times(1)).validateCartNotEmpty(cart);
        verifyNoInteractions(shoppingCartService, promotionService, shippingService);
    }

    @Test
    void calculateOrderSummary_ShouldValidateStock() {
        doThrow(new InsufficientStockException("Stock insufficient")).when(orderValidator).validateStock(cart);
        assertThrows(InsufficientStockException.class, () -> orderPricingService.calculateOrderSummary(request));
        verify(orderValidator, times(1)).validateStock(cart);
    }

    @Test
    void calculateOrderSummary_ShouldValidateAddress() {
        doThrow(new IllegalArgumentException("Invalid address")).when(shippingService).validateAddress(address);
        assertThrows(IllegalArgumentException.class, () -> orderPricingService.calculateOrderSummary(request));
        verify(shippingService, times(1)).validateAddress(address);
    }

    @Test
    void calculateOrderSummary_ShouldThrowOrderCreationException_WhenRequestIsNull() {
        assertThrows(OrderCreationException.class, () -> orderPricingService.calculateOrderSummary(null));
    }
}

/**
 * OrderService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderPricingService orderPricingService;

    @Mock
    private OrderStatusService orderStatusService;

    @Mock
    private OrderIdGenerator orderIdGenerator;

    @InjectMocks
    private OrderService orderService;

    private ShoppingCart cart;
    private Address address;
    private List<Coupon> coupons;
    private OrderCreateRequest request;
    private OrderSummary summary;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart();
        Product product = new Product("P001", "Apple", new BigDecimal("10.00"), 10);
        cart.addItem(new CartItem(product, 2));
        address = new Address("John Doe", "1234567890", "123 Main St", com.houkiang.ordersettlement.shipping.domain.RegionType.NORMAL);
        coupons = List.of();
        request = new OrderCreateRequest(cart, address, DeliveryType.STANDARD, coupons);
        summary = new OrderSummary(new BigDecimal("20.00"), BigDecimal.ZERO, new BigDecimal("5.00"), new BigDecimal("25.00"));
    }

    @Test
    void createOrder_ShouldReturnOrderWithPendingPaymentStatus_WhenRequestValid() {
        when(orderPricingService.calculateOrderSummary(request)).thenReturn(summary);
        when(orderIdGenerator.generate()).thenReturn("ORD-12345");

        Order order = orderService.createOrder(request);

        assertNotNull(order);
        assertEquals("ORD-12345", order.getOrderId());
        assertEquals(cart, order.getShoppingCart());
        assertEquals(address, order.getAddress());
        assertEquals(DeliveryType.STANDARD, order.getDeliveryType());
        assertEquals(coupons, order.getCoupons());
        assertEquals(summary.getItemAmount(), order.getItemAmount());
        assertEquals(summary.getDiscountAmount(), order.getDiscountAmount());
        assertEquals(summary.getShippingFee(), order.getShippingFee());
        assertEquals(summary.getPayableAmount(), order.getPayableAmount());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }

    @Test
    void createOrder_ShouldThrowOrderCreationException_WhenRequestIsNull() {
        assertThrows(OrderCreationException.class, () -> orderService.createOrder(null));
    }

    @Test
    void createOrder_ShouldThrowOrderCreationException_WhenGeneratedOrderIdIsBlank() {
        when(orderPricingService.calculateOrderSummary(request)).thenReturn(summary);
        when(orderIdGenerator.generate()).thenReturn("   ");
        assertThrows(OrderCreationException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_ShouldThrowOrderCreationException_WhenGeneratedOrderIdIsNull() {
        when(orderPricingService.calculateOrderSummary(request)).thenReturn(summary);
        when(orderIdGenerator.generate()).thenReturn(null);
        assertThrows(OrderCreationException.class, () -> orderService.createOrder(request));
    }

    @Test
    void transitionStatus_ShouldDelegateToOrderStatusService() {
        Order order = createDummyOrder();
        orderService.transitionStatus(order, OrderStatus.PAID);
        verify(orderStatusService, times(1)).transitionStatus(order, OrderStatus.PAID);
    }

    private Order createDummyOrder() {
        return new Order("ORD-1", cart, address, DeliveryType.STANDARD, coupons, summary, OrderStatus.PENDING_PAYMENT);
    }
}