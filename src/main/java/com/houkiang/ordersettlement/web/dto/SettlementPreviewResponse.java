package com.houkiang.ordersettlement.web.dto;

import com.houkiang.ordersettlement.order.domain.OrderSummary;
import java.math.BigDecimal;
import java.util.List;

public record SettlementPreviewResponse(
        BigDecimal itemAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal payableAmount,
        List<String> appliedCouponCodes) {

    public SettlementPreviewResponse {
        appliedCouponCodes = List.copyOf(appliedCouponCodes);
    }

    public static SettlementPreviewResponse from(OrderSummary summary, List<String> couponCodes) {
        return new SettlementPreviewResponse(summary.getItemAmount(), summary.getDiscountAmount(),
                summary.getShippingFee(), summary.getPayableAmount(), couponCodes);
    }
}
