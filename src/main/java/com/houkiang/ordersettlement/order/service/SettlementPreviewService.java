package com.houkiang.ordersettlement.order.service;

import com.houkiang.ordersettlement.order.domain.OrderSummary;
import com.houkiang.ordersettlement.order.service.OrderRequestAssembler.ItemSelection;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import java.util.List;
import java.util.Objects;

public class SettlementPreviewService {

    private final OrderRequestAssembler assembler;
    private final OrderPricingService orderPricingService;

    public SettlementPreviewService(OrderRequestAssembler assembler, OrderPricingService orderPricingService) {
        this.assembler = Objects.requireNonNull(assembler, "assembler must not be null");
        this.orderPricingService = Objects.requireNonNull(orderPricingService, "orderPricingService must not be null");
    }

    public OrderSummary preview(
            List<ItemSelection> items, Address address, DeliveryType deliveryType, List<String> couponCodes) {
        return orderPricingService.calculateOrderSummary(assembler.assemble(items, address, deliveryType, couponCodes));
    }
}
