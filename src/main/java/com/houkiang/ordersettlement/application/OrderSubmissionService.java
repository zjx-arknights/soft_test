package com.houkiang.ordersettlement.application;

import com.houkiang.ordersettlement.order.exception.IdempotencyConflictException;
import com.houkiang.ordersettlement.order.exception.InvalidIdempotencyKeyException;
import com.houkiang.ordersettlement.order.repository.IdempotencyRepository;
import com.houkiang.ordersettlement.order.repository.IdempotencyRepository.Claim;
import com.houkiang.ordersettlement.order.service.OrderRequestAssembler.ItemSelection;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.web.dto.SettlementPreviewRequest;
import com.houkiang.ordersettlement.web.dto.OrderResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class OrderSubmissionService {

    private final OrderApplicationService orders;
    private final IdempotencyRepository idempotency;

    public OrderSubmissionService(OrderApplicationService orders, IdempotencyRepository idempotency) {
        this.orders = Objects.requireNonNull(orders, "orders must not be null");
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency must not be null");
    }

    public OrderSubmissionResult submit(SettlementPreviewRequest request, String key) {
        SettlementPreviewRequest.AddressRequest address = request.address();
        var selections = request.items().stream()
                .map(item -> new ItemSelection(item.productId(), item.quantity())).toList();
        Address destination = new Address(address.recipientName(), address.phone(),
                address.detailAddress(), address.regionType());
        if (key == null) {
            return new OrderSubmissionResult(
                    OrderResponse.from(createWithRetry(selections, destination, request, null, null)), true);
        }
        if (!key.matches("[A-Za-z0-9_-]{16,128}")) {
            throw new InvalidIdempotencyKeyException();
        }
        String fingerprint = fingerprint(request);
        try {
            return new OrderSubmissionResult(
                    OrderResponse.from(createWithRetry(selections, destination, request, key, fingerprint)), true);
        } catch (DuplicateKeyException exception) {
            Claim previous = idempotency.findByKey(key).orElseThrow(() -> exception);
            if (!previous.requestFingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException();
            }
            if (previous.orderId() == null) {
                throw new IllegalStateException("Completed idempotency claim has no order");
            }
            return new OrderSubmissionResult(OrderResponse.from(orders.find(previous.orderId())), false);
        }
    }

    private com.houkiang.ordersettlement.order.domain.Order createWithRetry(List<ItemSelection> selections,
            Address destination, SettlementPreviewRequest request, String key, String fingerprint) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                if (key == null) {
                    return orders.create(selections, destination, request.deliveryType(), request.couponCodes());
                }
                return orders.createWithIdempotency(selections, destination,
                        request.deliveryType(), request.couponCodes(), key, fingerprint);
            } catch (CannotAcquireLockException exception) {
                if (attempt == 2) {
                    throw exception;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(25L * (attempt + 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("Checkout retry loop exited without a result");
    }

    private String fingerprint(SettlementPreviewRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateInt(digest, request.items().size());
            for (SettlementPreviewRequest.ItemRequest item : request.items()) {
                updateString(digest, item.productId());
                updateInt(digest, item.quantity());
            }
            SettlementPreviewRequest.AddressRequest address = request.address();
            updateString(digest, address.recipientName());
            updateString(digest, address.phone());
            updateString(digest, address.detailAddress());
            updateString(digest, address.regionType().name());
            updateString(digest, request.deliveryType().name());
            updateInt(digest, request.couponCodes().size());
            for (String coupon : request.couponCodes()) {
                updateString(digest, coupon);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void updateString(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateInt(digest, bytes.length);
        digest.update(bytes);
    }

    private void updateInt(MessageDigest digest, int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }
}
