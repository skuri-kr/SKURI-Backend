package com.skuri.skuri_backend.common.config;

import com.skuri.skuri_backend.common.time.ApplicationTimeZone;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JpaAuditingConfigTest {

    @Test
    void auditingTime_서울시간대로생성한다() {
        LocalDateTime before = LocalDateTime.now(ApplicationTimeZone.SEOUL);
        LocalDateTime auditedAt = (LocalDateTime) new JpaAuditingConfig()
                .seoulDateTimeProvider()
                .getNow()
                .orElseThrow();
        LocalDateTime after = LocalDateTime.now(ApplicationTimeZone.SEOUL);

        assertFalse(auditedAt.isBefore(before));
        assertFalse(auditedAt.isAfter(after));
    }
}
