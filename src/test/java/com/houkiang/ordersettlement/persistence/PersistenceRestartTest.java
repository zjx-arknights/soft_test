package com.houkiang.ordersettlement.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.houkiang.ordersettlement.OrderSettlementApplication;
import com.houkiang.ordersettlement.application.OrderApplicationService;
import com.houkiang.ordersettlement.catalog.ProductCatalog;
import com.houkiang.ordersettlement.order.domain.Order;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import com.houkiang.ordersettlement.order.service.OrderRequestAssembler.ItemSelection;
import com.houkiang.ordersettlement.shipping.domain.Address;
import com.houkiang.ordersettlement.shipping.domain.DeliveryType;
import com.houkiang.ordersettlement.shipping.domain.RegionType;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

class PersistenceRestartTest {

    @TempDir
    Path directory;

    @Test
    void orderAndCatalogChangesSurviveDatabaseCloseAndApplicationRestart() {
        String url = "jdbc:h2:file:" + directory.resolve("restart").toAbsolutePath().toString().replace('\\', '/')
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE";
        String id;
        try (ConfigurableApplicationContext context = start(url)) {
            OrderApplicationService service = context.getBean(OrderApplicationService.class);
            Order order = service.create(List.of(new ItemSelection("P001", 2)),
                    new Address("张三", "123", "Test Road", RegionType.NORMAL),
                    DeliveryType.STANDARD, List.of("FULL100_20", "DISCOUNT90", "FIXED10"));
            id = order.getOrderId();
            service.transition(id, OrderStatus.PAID);
            context.getBean(JdbcTemplate.class).update("UPDATE products SET unit_price = 99.99 WHERE product_id = 'P001'");
        }
        try (ConfigurableApplicationContext context = start(url)) {
            Order order = context.getBean(OrderApplicationService.class).find(id);
            assertEquals(new BigDecimal("62.00"), order.getPayableAmount());
            assertEquals(OrderStatus.PAID, order.getStatus());
            assertEquals("张三", order.getAddress().getRecipientName());
            assertEquals(new BigDecimal("50.00"), order.getShoppingCart().getItems().get(0).getProduct().getUnitPrice());
            assertEquals(new BigDecimal("99.99"),
                    context.getBean(ProductCatalog.class).findProduct("P001").orElseThrow().getUnitPrice());
            assertEquals(3, context.getBean(ProductCatalog.class).listProducts().size());
        }
    }

    private ConfigurableApplicationContext start(String url) {
        return new SpringApplicationBuilder(OrderSettlementApplication.class).web(WebApplicationType.NONE)
                .profiles("local").run("--spring.datasource.url=" + url, "--spring.main.banner-mode=off");
    }
}
