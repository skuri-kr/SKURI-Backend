package com.skuri.skuri_backend.common.config;

import com.skuri.skuri_backend.common.time.ApplicationTimeZone;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "seoulDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider seoulDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(ApplicationTimeZone.SEOUL));
    }
}
