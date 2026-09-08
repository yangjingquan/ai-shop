package com.shop.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ResourceReservationMigrationTest {
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesReservationStateMachineAndIdempotencyKeys() {
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = 'resource_reservation'
                """, Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'order' AND column_name = 'client_request_id'
                """, Integer.class));
        assertEquals(4, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'resource_reservation'
                  AND index_name = 'uk_resource_reservation'
                """, Integer.class));
        assertEquals(3, jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'order'
                  AND index_name = 'uk_order_client_request'
                """, Integer.class));
    }
}
