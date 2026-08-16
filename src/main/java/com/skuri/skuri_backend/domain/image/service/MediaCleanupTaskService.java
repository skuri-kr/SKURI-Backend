package com.skuri.skuri_backend.domain.image.service;

import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTask;
import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTaskStatus;
import com.skuri.skuri_backend.domain.image.repository.MediaCleanupTaskRepository;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaCleanupTaskService {

    private final MediaCleanupTaskRepository mediaCleanupTaskRepository;
    private final StorageRepository storageRepository;

    public List<String> enqueue(Collection<String> relativePaths) {
        LocalDateTime now = LocalDateTime.now();
        return relativePaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .distinct()
                .map(path -> mediaCleanupTaskRepository.findByRelativePath(path)
                        .orElseGet(() -> mediaCleanupTaskRepository.save(MediaCleanupTask.create(path, now))))
                .map(MediaCleanupTask::getId)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNow(String taskId) {
        mediaCleanupTaskRepository.findById(taskId)
                .filter(MediaCleanupTask::isPending)
                .ifPresent(this::processTask);
    }

    @Scheduled(
            fixedDelayString = "${media.cleanup.fixed-delay-ms:60000}",
            initialDelayString = "${media.cleanup.initial-delay-ms:60000}"
    )
    @Transactional
    public void processDueTasks() {
        List<MediaCleanupTask> tasks = mediaCleanupTaskRepository
                .findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        MediaCleanupTaskStatus.PENDING,
                        LocalDateTime.now()
                );
        tasks.forEach(this::processTask);
    }

    private void processTask(MediaCleanupTask task) {
        try {
            storageRepository.delete(task.getRelativePath());
            task.markCompleted(LocalDateTime.now());
        } catch (RuntimeException e) {
            task.scheduleRetry(LocalDateTime.now(), e.getMessage());
            log.warn(
                    "미디어 정리 재시도 예약: taskId={}, relativePath={}, attemptCount={}",
                    task.getId(),
                    task.getRelativePath(),
                    task.getAttemptCount(),
                    e
            );
        }
    }
}
