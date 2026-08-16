package com.skuri.skuri_backend.domain.image.repository;

import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTask;
import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MediaCleanupTaskRepository extends JpaRepository<MediaCleanupTask, String> {

    Optional<MediaCleanupTask> findByRelativePath(String relativePath);

    @Modifying
    @Query(value = """
            insert ignore into media_cleanup_tasks (
                id,
                relative_path,
                status,
                attempt_count,
                next_attempt_at,
                created_at,
                updated_at
            ) values (
                UUID(),
                :relativePath,
                'ACTIVE',
                0,
                :now,
                :now,
                :now
            )
            """, nativeQuery = true)
    int insertActiveIfAbsent(
            @Param("relativePath") String relativePath,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task
            from MediaCleanupTask task
            where task.relativePath = :relativePath
            """)
    Optional<MediaCleanupTask> findByRelativePathForUpdate(@Param("relativePath") String relativePath);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task
            from MediaCleanupTask task
            where task.id = :taskId
            """)
    Optional<MediaCleanupTask> findByIdForUpdate(@Param("taskId") String taskId);

    @Query("""
            select task.id
            from MediaCleanupTask task
            where task.status = :status
              and task.nextAttemptAt <= :nextAttemptAt
            order by task.nextAttemptAt asc
            """)
    List<String> findDueTaskIds(
            @Param("status") MediaCleanupTaskStatus status,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            Pageable pageable
    );
}
