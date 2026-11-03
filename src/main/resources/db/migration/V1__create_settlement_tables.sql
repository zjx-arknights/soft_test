CREATE TABLE products (
    product_id VARCHAR(64) ${identifierCollation} NOT NULL PRIMARY KEY,
    product_name VARCHAR(200) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL CHECK (unit_price >= 0),
    stock INT NOT NULL CHECK (stock >= 0)
) ${tableOptions};

CREATE TABLE coupons (
    coupon_code VARCHAR(64) ${identifierCollation} NOT NULL PRIMARY KEY,
    coupon_type VARCHAR(32) NOT NULL CHECK (coupon_type IN ('FULL_REDUCTION', 'DISCOUNT', 'FIXED_AMOUNT')),
    threshold_amount DECIMAL(19, 2) NOT NULL CHECK (threshold_amount >= 0),
    reduction_amount DECIMAL(19, 2),
    discount_rate DECIMAL(9, 8),
    stackable BOOLEAN NOT NULL,
    CONSTRAINT ck_coupon_rule CHECK (
        (coupon_type = 'DISCOUNT' AND discount_rate IS NOT NULL AND discount_rate > 0
            AND discount_rate <= 1 AND reduction_amount IS NULL)
        OR (coupon_type IN ('FULL_REDUCTION', 'FIXED_AMOUNT') AND reduction_amount IS NOT NULL
            AND reduction_amount > 0 AND discount_rate IS NULL)
    )
) ${tableOptions};

CREATE TABLE settlement_orders (
    order_id VARCHAR(64) ${identifierCollation} NOT NULL PRIMARY KEY,
    recipient_name VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    detail_address VARCHAR(500) NOT NULL,
    region_type VARCHAR(16) NOT NULL CHECK (region_type IN ('NORMAL', 'REMOTE')),
    delivery_type VARCHAR(16) NOT NULL CHECK (delivery_type IN ('STANDARD', 'EXPRESS', 'SELF_PICKUP')),
    item_amount DECIMAL(19, 2) NOT NULL CHECK (item_amount >= 0),
    discount_amount DECIMAL(19, 2) NOT NULL CHECK (discount_amount >= 0),
    shipping_fee DECIMAL(19, 2) NOT NULL CHECK (shipping_fee >= 0),
    payable_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_order_discount CHECK (discount_amount <= item_amount),
    CONSTRAINT ck_order_payable CHECK (payable_amount = item_amount - discount_amount + shipping_fee)
) ${tableOptions};

-- Historical snapshots deliberately do not reference the live product/coupon tables.
CREATE TABLE order_items (
    order_id VARCHAR(64) ${identifierCollation} NOT NULL,
    line_number INT NOT NULL CHECK (line_number >= 0),
    product_id VARCHAR(64) ${identifierCollation} NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL CHECK (unit_price >= 0),
    stock_snapshot INT NOT NULL CHECK (stock_snapshot >= 0),
    quantity INT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, line_number),
    CONSTRAINT uq_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES settlement_orders(order_id)
) ${tableOptions};

CREATE TABLE order_coupons (
    order_id VARCHAR(64) ${identifierCollation} NOT NULL,
    coupon_position INT NOT NULL CHECK (coupon_position >= 0),
    coupon_code VARCHAR(64) ${identifierCollation} NOT NULL,
    coupon_type VARCHAR(32) NOT NULL,
    threshold_amount DECIMAL(19, 2) NOT NULL,
    reduction_amount DECIMAL(19, 2),
    discount_rate DECIMAL(9, 8),
    stackable BOOLEAN NOT NULL,
    PRIMARY KEY (order_id, coupon_position),
    CONSTRAINT uq_order_coupon UNIQUE (order_id, coupon_code),
    CONSTRAINT fk_coupon_order FOREIGN KEY (order_id) REFERENCES settlement_orders(order_id)
) ${tableOptions};
