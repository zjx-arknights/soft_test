package com.houkiang.ordersettlement.web.dto;

import com.houkiang.ordersettlement.cart.domain.CartItem;
import com.houkiang.ordersettlement.common.util.MoneyUtils;
import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.web.dto.SettlementPreviewRequest.AddressRequest;
import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        String orderId, OrderStatus status, List<ItemResponse> items, AddressRequest address, DeliveryType deliveryType,
        List<CouponResponse> coupons, BigDecimal itemAmount, BigDecimal discountAmount,
        BigDecimal shippingFee, BigDecimal payableAmount) {

    public OrderResponse {
        items = List.copyOf(items);
        coupons = List.copyOf(coupons);
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getOrderId(), order.getStatus(),
                order.getShoppingCart().getItems().stream().map(ItemResponse::from).toList(),
                new AddressRequest(order.getAddress().getRecipientName(), order.getAddress().getPhone(),
                        order.getAddress().getDetailAddress(), order.getAddress().getRegionType()),
                order.getDeliveryType(), order.getCoupons().stream().map(CouponResponse::from).toList(),
                order.getItemAmount(), order.getDiscountAmount(), order.getShippingFee(), order.getPayableAmount());
    }

    public record ItemResponse(String productId, String productName, BigDecimal unitPrice,
            int quantity, BigDecimal subtotal) {

        static ItemResponse from(CartItem item) {
            return new ItemResponse(item.getProduct().getProductId(), item.getProduct().getProductName(),
                    item.getProduct().getUnitPrice(), item.getQuantity(),
                    MoneyUtils.requireNonNegative(item.getProduct().getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity())), "subtotal"));
        }
    }
}
