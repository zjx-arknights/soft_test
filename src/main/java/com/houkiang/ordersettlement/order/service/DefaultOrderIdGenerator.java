package com.houkiang.ordersettlement.order.service;

import java.util.UUID;

public class DefaultOrderIdGenerator implements OrderIdGenerator {

    @Override
    public String generate() {
        return "ORD-" + UUID.randomUUID();
    }
}
