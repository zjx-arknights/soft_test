package com.houkiang.ordersettlement.persistence;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles(value = "mysql", inheritProfiles = false)
class MySqlAtomicityIT extends OrderAtomicityTest {

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        MySqlPersistenceIT.mysql(registry);
    }
}
