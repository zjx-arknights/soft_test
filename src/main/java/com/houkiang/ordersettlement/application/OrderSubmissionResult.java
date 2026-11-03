package com.houkiang.ordersettlement.application;

import com.houkiang.ordersettlement.web.dto.OrderResponse;
import java.util.Objects;

public record OrderSubmissionResult(OrderResponse order, boolean created) {
    public OrderSubmissionResult {
        Objects.requireNonNull(order, "order must not be null");
    }
}
