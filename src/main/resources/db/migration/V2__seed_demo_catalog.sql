INSERT INTO products (product_id, product_name, unit_price, stock) VALUES
    ('P001', 'Java 入门', 50.00, 20),
    ('P002', '软件测试实践', 38.00, 10),
    ('P003', '机械键盘', 100.00, 0);

INSERT INTO coupons (coupon_code, coupon_type, threshold_amount, reduction_amount, discount_rate, stackable) VALUES
    ('FULL100_20', 'FULL_REDUCTION', 100.00, 20.00, NULL, TRUE),
    ('DISCOUNT90', 'DISCOUNT', 0.00, NULL, 0.90, TRUE),
    ('FIXED10', 'FIXED_AMOUNT', 0.00, 10.00, NULL, TRUE);
