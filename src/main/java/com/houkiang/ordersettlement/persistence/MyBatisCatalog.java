package com.houkiang.ordersettlement.persistence;

import com.houkiang.ordersettlement.cart.domain.Product;
import com.houkiang.ordersettlement.catalog.CouponCatalog;
import com.houkiang.ordersettlement.catalog.ProductCatalog;
import com.houkiang.ordersettlement.persistence.mapper.CatalogMapper;
import com.houkiang.ordersettlement.promotion.domain.Coupon;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisCatalog implements ProductCatalog, CouponCatalog {

    private final CatalogMapper mapper;

    public MyBatisCatalog(CatalogMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Optional<Product> findProduct(String productId) {
        return Optional.ofNullable(mapper.findProduct(productId));
    }

    @Override
    public List<Product> listProducts() {
        return List.copyOf(mapper.listProducts());
    }

    @Override
    public Optional<Coupon> findCoupon(String couponCode) {
        return Optional.ofNullable(mapper.findCoupon(couponCode));
    }

    @Override
    public List<Coupon> listCoupons() {
        return List.copyOf(mapper.listCoupons());
    }
}
