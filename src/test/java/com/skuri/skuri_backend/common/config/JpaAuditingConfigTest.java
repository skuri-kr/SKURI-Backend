package com.skuri.skuri_backend.common.config;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;

class JpaAuditingConfigTest {

    private static final ZoneId SEOUL_TIME_ZONE = ZoneId.of("Asia/Seoul");

    @Test
    void auditingTime_서울시간대로생성한다() {
        LocalDateTime before = LocalDateTime.now(SEOUL_TIME_ZONE);
        LocalDateTime auditedAt = (LocalDateTime) new JpaAuditingConfig()
                .seoulDateTimeProvider()
                .getNow()
                .orElseThrow();
        LocalDateTime after = LocalDateTime.now(SEOUL_TIME_ZONE);

        assertFalse(auditedAt.isBefore(before));
        assertFalse(auditedAt.isAfter(after));
    }
}
