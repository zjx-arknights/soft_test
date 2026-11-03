package com.houkiang.ordersettlement.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.houkiang.ordersettlement.OrderSettlementApplication;
import com.houkiang.ordersettlement.application.OrderApplicationService;
import com.houkiang.ordersettlement.order.domain.OrderStatus;
import java.nio.file.Path;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class CheckoutMigrationTest {

    @TempDir
    Path directory;

    @Test
    void migratingAnOldOrderDoesNotTreatItsStockAsReserved() {
        String url = "jdbc:h2:file:" + directory.resolve("legacy").toAbsolutePath().toString().replace('\\', '/')
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration")
                .placeholders(Map.of("identifierCollation", "", "tableOptions", ""))
                .target(MigrationVersion.fromVersion("2")).load().migrate();

        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        jdbc.update("""
                INSERT INTO settlement_orders (order_id, recipient_name, phone, detail_address,
                    region_type, delivery_type, item_amount, discount_amount, shipping_fee, payable_amount, status)
                VALUES ('ORD-legacy', 'Test', '123', 'Test Road', 'NORMAL', 'SELF_PICKUP',
                    100.00, 0.00, 0.00, 100.00, 'PENDING_PAYMENT')
                """);
        jdbc.update("""
                INSERT INTO order_items (order_id, line_number, product_id, product_name,
                    unit_price, stock_snapshot, quantity)
                VALUES ('ORD-legacy', 0, 'P001', 'Java 入门', 50.00, 20, 2)
                """);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(OrderSettlementApplication.class)
                .web(WebApplicationType.NONE).profiles("local")
                .run("--spring.datasource.url=" + url, "--spring.main.banner-mode=off")) {
            JdbcTemplate migrated = context.getBean(JdbcTemplate.class);
            assertFalse(migrated.queryForObject(
                    "SELECT stock_reserved FROM settlement_orders WHERE order_id = 'ORD-legacy'", Boolean.class));
            assertEquals(OrderStatus.CANCELLED,
                    context.getBean(OrderApplicationService.class)
                            .transition("ORD-legacy", OrderStatus.CANCELLED).getStatus());
            assertEquals(20, migrated.queryForObject(
                    "SELECT stock FROM products WHERE product_id = 'P001'", Integer.class));
        }
    }
}
