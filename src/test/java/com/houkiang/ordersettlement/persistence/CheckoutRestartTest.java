package com.houkiang.ordersettlement.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.houkiang.ordersettlement.OrderSettlementApplication;
import com.houkiang.ordersettlement.application.OrderSubmissionResult;
import com.houkiang.ordersettlement.application.OrderSubmissionService;
import com.houkiang.ordersettlement.catalog.ProductCatalog;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.RegionType;
import com.houkiang.ordersettlement.web.dto.SettlementPreviewRequest;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class CheckoutRestartTest {

    @TempDir
    Path directory;

    @Test
    void retryAfterRestartReturnsOriginalOrderAndDoesNotDeductStockAgain() {
        String url = "jdbc:h2:file:" + directory.resolve("checkout").toAbsolutePath().toString().replace('\\', '/')
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE";
        SettlementPreviewRequest request = new SettlementPreviewRequest(
                List.of(new SettlementPreviewRequest.ItemRequest("P001", 2)),
                new SettlementPreviewRequest.AddressRequest("Test", "123", "Test Road", RegionType.NORMAL),
                DeliveryType.STANDARD, List.of());
        String key = "restart-request-0001";
        String id;
        try (ConfigurableApplicationContext context = start(url)) {
            OrderSubmissionResult created = context.getBean(OrderSubmissionService.class).submit(request, key);
            assertTrue(created.created());
            id = created.order().orderId();
            assertEquals(18, stock(context));
        }
        try (ConfigurableApplicationContext context = start(url)) {
            OrderSubmissionResult replay = context.getBean(OrderSubmissionService.class).submit(request, key);
            assertFalse(replay.created());
            assertEquals(id, replay.order().orderId());
            assertEquals(18, stock(context));
        }
    }

    private ConfigurableApplicationContext start(String url) {
        return new SpringApplicationBuilder(OrderSettlementApplication.class).web(WebApplicationType.NONE)
                .profiles("local").run("--spring.datasource.url=" + url, "--spring.main.banner-mode=off");
    }

    private int stock(ConfigurableApplicationContext context) {
        return context.getBean(ProductCatalog.class).findProduct("P001").orElseThrow().getStock();
    }
}
