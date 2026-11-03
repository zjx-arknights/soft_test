-- Existing orders predate inventory reservations and must not be restocked on cancellation.
ALTER TABLE settlement_orders ADD COLUMN stock_reserved BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE idempotency_requests (
    request_key VARCHAR(128) ${identifierCollation} NOT NULL PRIMARY KEY,
    request_fingerprint CHAR(64) ${identifierCollation} NOT NULL,
    order_id VARCHAR(64) ${identifierCollation},
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_idempotency_order UNIQUE (order_id),
    CONSTRAINT fk_idempotency_order FOREIGN KEY (order_id) REFERENCES settlement_orders(order_id)
) ${tableOptions};
