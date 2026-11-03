package com.houkiang.ordersettlement.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.houkiang.ordersettlement.application.OrderApplicationService;
import com.houkiang.ordersettlement.order.service.OrderIdGenerator;
import com.houkiang.ordersettlement.order.service.OrderRequestAssembler.ItemSelection;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.RegionType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:checkout-rollback;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class CheckoutRollbackTest {

    @Autowired
    OrderApplicationService orders;
    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    OrderIdGenerator ids;

    @Test
    void failedOrderInsertRollsBackStockReservationAndIdempotencyClaim() {
        when(ids.generate()).thenReturn("x".repeat(65));
        assertThrows(DataIntegrityViolationException.class, () -> orders.createWithIdempotency(
                List.of(new ItemSelection("P001", 2)),
                new Address("Test", "123", "Test Road", RegionType.NORMAL),
                DeliveryType.STANDARD, List.of(), "rollback-request-0001", "a".repeat(64)));
        assertEquals(20, jdbc.queryForObject("SELECT stock FROM products WHERE product_id = 'P001'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM settlement_orders", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_requests", Integer.class));
    }
}
