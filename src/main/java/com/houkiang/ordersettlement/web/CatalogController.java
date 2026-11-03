package com.houkiang.ordersettlement.web;

import com.houkiang.ordersettlement.catalog.CouponCatalog;
import com.houkiang.ordersettlement.catalog.ProductCatalog;
import com.houkiang.ordersettlement.web.dto.CouponResponse;
import com.houkiang.ordersettlement.web.dto.ProductResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final ProductCatalog productCatalog;
    private final CouponCatalog couponCatalog;

    public CatalogController(ProductCatalog productCatalog, CouponCatalog couponCatalog) {
        this.productCatalog = productCatalog;
        this.couponCatalog = couponCatalog;
    }

    @GetMapping("/products")
    public List<ProductResponse> products() {
        return productCatalog.listProducts().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/coupons")
    public List<CouponResponse> coupons() {
        return couponCatalog.listCoupons().stream().map(CouponResponse::from).toList();
    }
}
