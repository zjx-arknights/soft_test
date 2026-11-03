package com.houkiang.ordersettlement.order.repository;

import java.util.Optional;

public interface IdempotencyRepository {
    void claim(String key, String fingerprint);
    void complete(String key, String fingerprint, String orderId);
    Optional<Claim> findByKey(String key);

    record Claim(String requestKey, String requestFingerprint, String orderId) {
    }
}
