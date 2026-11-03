package com.houkiang.ordersettlement.persistence;

import com.houkiang.ordersettlement.order.repository.IdempotencyRepository;
import com.houkiang.ordersettlement.persistence.mapper.IdempotencyMapper;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisIdempotencyRepository implements IdempotencyRepository {
    private final IdempotencyMapper mapper;

    public MyBatisIdempotencyRepository(IdempotencyMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public void claim(String key, String fingerprint) {
        mapper.claim(key, fingerprint);
    }

    @Override
    public void complete(String key, String fingerprint, String orderId) {
        if (mapper.complete(key, fingerprint, orderId) != 1) {
            throw new IllegalStateException("Idempotency claim could not be completed");
        }
    }

    @Override
    public Optional<Claim> findByKey(String key) {
        return Optional.ofNullable(mapper.findByKey(key));
    }
}
