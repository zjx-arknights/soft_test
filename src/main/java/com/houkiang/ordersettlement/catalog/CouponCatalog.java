package com.houkiang.ordersettlement.catalog;

import com.houkiang.ordersettlement.promotion.domain.Coupon;
import java.util.List;
import java.util.Optional;

public interface CouponCatalog {

    Optional<Coupon> findCoupon(String couponCode);

    List<Coupon> listCoupons();
}
