package com.houkiang.ordersettlement.persistence.model;

import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.RegionType;
import java.math.BigDecimal;

public record OrderRow(
        String orderId, String recipientName, String phone, String detailAddress,
        RegionType regionType, DeliveryType deliveryType, BigDecimal itemAmount,
        BigDecimal discountAmount, BigDecimal shippingFee, BigDecimal payableAmount, OrderStatus status) {

    public static OrderRow from(Order order) {
        return new OrderRow(order.getOrderId(), order.getAddress().getRecipientName(),
                order.getAddress().getPhone(), order.getAddress().getDetailAddress(),
                order.getAddress().getRegionType(), order.getDeliveryType(), order.getItemAmount(),
                order.getDiscountAmount(), order.getShippingFee(), order.getPayableAmount(), order.getStatus());
    }
}
