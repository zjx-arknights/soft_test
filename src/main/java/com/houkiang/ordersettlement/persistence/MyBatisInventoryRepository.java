package com.houkiang.ordersettlement.persistence;

import com.houkiang.ordersettlement.cart.repository.InventoryRepository;
import com.houkiang.ordersettlement.persistence.mapper.CatalogMapper;
import java.util.Objects;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisInventoryRepository implements InventoryRepository {
    private final CatalogMapper mapper;

    public MyBatisInventoryRepository(CatalogMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public boolean reserve(String productId, int quantity) {
        return mapper.reserveStock(productId, quantity) == 1;
    }

    @Override
    public boolean release(String productId, int quantity) {
        return mapper.releaseStock(productId, quantity) == 1;
    }
}
