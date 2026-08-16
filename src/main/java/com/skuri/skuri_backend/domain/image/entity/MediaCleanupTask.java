package com.skuri.skuri_backend.domain.image.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "media_cleanup_tasks",
        indexes = {
                @Index(name = "idx_media_cleanup_tasks_due", columnList = "status, next_attempt_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaCleanupTask extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "relative_path", nullable = false, unique = true, length = 500)
    private String relativePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaCleanupTaskStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    private MediaCleanupTask(String relativePath, LocalDateTime now) {
        this.relativePath = relativePath;
        this.status = MediaCleanupTaskStatus.PENDING;
        this.nextAttemptAt = now;
    }

    public static MediaCleanupTask create(String relativePath, LocalDateTime now) {
        return new MediaCleanupTask(relativePath, now);
    }

    public boolean isPending() {
        return status == MediaCleanupTaskStatus.PENDING;
    }

    public void markCompleted(LocalDateTime completedAt) {
        this.status = MediaCleanupTaskStatus.COMPLETED;
        this.completedAt = completedAt;
        this.nextAttemptAt = completedAt;
        this.lastError = null;
    }

    public void scheduleRetry(LocalDateTime now, String errorMessage) {
        this.attemptCount += 1;
        long delaySeconds = Math.min(60L * 60L, 1L << Math.min(attemptCount, 12));
        this.nextAttemptAt = now.plusSeconds(delaySeconds);
        this.lastError = errorMessage == null
                ? "미디어 삭제에 실패했습니다."
                : errorMessage.substring(0, Math.min(errorMessage.length(), 500));
    }
}
