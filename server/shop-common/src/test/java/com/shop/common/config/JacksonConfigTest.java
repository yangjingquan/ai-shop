package com.shop.common.config;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonConfigTest {
    private final JacksonConfig config = new JacksonConfig();

    @Test
    void acceptsSpaceAndIsoLocalDateTimeFormats() throws Exception {
        assertEquals(LocalDateTime.of(2026, 9, 5, 0, 0),
                config.objectMapper().readValue("\"2026-09-05 00:00:00\"", LocalDateTime.class));
        assertEquals(LocalDateTime.of(2026, 9, 5, 0, 0),
                config.objectMapper().readValue("\"2026-09-05T00:00:00\"", LocalDateTime.class));
    }
}
