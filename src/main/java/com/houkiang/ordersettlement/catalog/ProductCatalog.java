package com.houkiang.ordersettlement.catalog;

import com.houkiang.ordersettlement.cart.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductCatalog {

    Optional<Product> findProduct(String productId);

    List<Product> listProducts();
}
