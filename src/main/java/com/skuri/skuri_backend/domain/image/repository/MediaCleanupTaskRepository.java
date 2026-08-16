package com.skuri.skuri_backend.domain.image.repository;

import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTask;
import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MediaCleanupTaskRepository extends JpaRepository<MediaCleanupTask, String> {

    Optional<MediaCleanupTask> findByRelativePath(String relativePath);

    List<MediaCleanupTask> findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            MediaCleanupTaskStatus status,
            LocalDateTime nextAttemptAt
    );
}
