package com.houkiang.ordersettlement.persistence;

import java.net.URI;
import java.util.Objects;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles(value = "mysql", inheritProfiles = false)
class MySqlPersistenceIT extends OrderPersistenceTest {

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        String url = Objects.requireNonNull(System.getenv("MYSQL_TEST_URL"), "MYSQL_TEST_URL is required");
        if (!url.startsWith("jdbc:mysql://")
                || !URI.create(url.substring(5)).getPath().matches("/[A-Za-z0-9_]+_test")) {
            throw new IllegalArgumentException("MySQL integration tests require a dedicated database ending in _test");
        }
        String username = Objects.requireNonNull(System.getenv("MYSQL_TEST_USERNAME"), "MYSQL_TEST_USERNAME is required");
        String password = Objects.requireNonNull(System.getenv("MYSQL_TEST_PASSWORD"), "MYSQL_TEST_PASSWORD is required");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
    }
}
