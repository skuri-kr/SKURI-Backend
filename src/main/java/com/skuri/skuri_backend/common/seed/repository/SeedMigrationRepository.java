package com.skuri.skuri_backend.common.seed.repository;

import com.skuri.skuri_backend.common.seed.entity.SeedMigration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SeedMigrationRepository extends JpaRepository<SeedMigration, String> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert ignore into seed_migrations (migration_key, applied_at)
            values (:migrationKey, :appliedAt)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("migrationKey") String migrationKey,
            @Param("appliedAt") LocalDateTime appliedAt
    );
}
