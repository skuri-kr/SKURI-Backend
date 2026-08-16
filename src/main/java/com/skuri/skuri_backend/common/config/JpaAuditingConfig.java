package com.skuri.skuri_backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "seoulDateTimeProvider")
public class JpaAuditingConfig {

    private static final ZoneId SEOUL_TIME_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public DateTimeProvider seoulDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(SEOUL_TIME_ZONE));
    }
}
