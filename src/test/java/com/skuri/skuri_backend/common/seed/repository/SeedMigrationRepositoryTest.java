package com.skuri.skuri_backend.common.seed.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:seed-migration;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class SeedMigrationRepositoryTest {

    @Autowired
    private SeedMigrationRepository seedMigrationRepository;

    @Test
    void 마커가이미있으면_적용시각을덮어쓰지않고_선점에실패한다() {
        LocalDateTime firstAppliedAt = LocalDateTime.of(2026, 8, 30, 10, 0);
        LocalDateTime secondAppliedAt = firstAppliedAt.plusHours(1);

        int firstResult = seedMigrationRepository.insertIfAbsent("migration-key", firstAppliedAt);
        int secondResult = seedMigrationRepository.insertIfAbsent("migration-key", secondAppliedAt);

        assertThat(firstResult).isEqualTo(1);
        assertThat(secondResult).isZero();
        assertThat(seedMigrationRepository.findById("migration-key"))
                .get()
                .extracting(migration -> migration.getAppliedAt())
                .isEqualTo(firstAppliedAt);
    }
}
