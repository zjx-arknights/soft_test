package com.houkiang.ordersettlement.config;

import com.houkiang.ordersettlement.cart.service.ShoppingCartService;
import com.houkiang.ordersettlement.catalog.CouponCatalog;
import com.houkiang.ordersettlement.catalog.ProductCatalog;
import com.houkiang.ordersettlement.order.service.DefaultOrderIdGenerator;
import com.houkiang.ordersettlement.order.service.OrderIdGenerator;
import com.houkiang.ordersettlement.order.service.OrderPricingService;
import com.houkiang.ordersettlement.order.service.OrderRequestAssembler;
import com.houkiang.ordersettlement.order.service.OrderService;
import com.houkiang.ordersettlement.order.service.OrderStatusService;
import com.houkiang.ordersettlement.order.service.OrderValidator;
import com.houkiang.ordersettlement.order.service.SettlementPreviewService;
import com.houkiang.ordersettlement.promotion.service.CouponValidator;
import com.houkiang.ordersettlement.promotion.service.PromotionService;
import com.houkiang.ordersettlement.shipping.service.ShippingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SettlementConfiguration {

    @Bean
    public ShoppingCartService shoppingCartService() {
        return new ShoppingCartService();
    }

    @Bean
    public CouponValidator couponValidator() {
        return new CouponValidator();
    }

    @Bean
    public PromotionService promotionService(CouponValidator couponValidator) {
        return new PromotionService(couponValidator);
    }

    @Bean
    public ShippingService shippingService() {
        return new ShippingService();
    }

    @Bean
    public OrderValidator orderValidator() {
        return new OrderValidator();
    }

    @Bean
    public OrderPricingService orderPricingService(
            ShoppingCartService shoppingCartService, PromotionService promotionService,
            ShippingService shippingService, OrderValidator orderValidator) {
        return new OrderPricingService(shoppingCartService, promotionService, shippingService, orderValidator);
    }

    @Bean
    public OrderStatusService orderStatusService() {
        return new OrderStatusService();
    }

    @Bean
    public OrderIdGenerator orderIdGenerator() {
        return new DefaultOrderIdGenerator();
    }

    @Bean
    public OrderService orderService(
            OrderPricingService orderPricingService, OrderStatusService orderStatusService,
            OrderIdGenerator orderIdGenerator) {
        return new OrderService(orderPricingService, orderStatusService, orderIdGenerator);
    }

    @Bean
    public OrderRequestAssembler orderRequestAssembler(
            ProductCatalog productCatalog, CouponCatalog couponCatalog,
            ShoppingCartService shoppingCartService) {
        return new OrderRequestAssembler(productCatalog, couponCatalog, shoppingCartService);
    }

    @Bean
    public SettlementPreviewService settlementPreviewService(
            OrderRequestAssembler assembler, OrderPricingService orderPricingService) {
        return new SettlementPreviewService(assembler, orderPricingService);
    }
}
