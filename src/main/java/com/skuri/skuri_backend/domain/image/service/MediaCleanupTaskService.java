package com.skuri.skuri_backend.domain.image.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTask;
import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTaskStatus;
import com.skuri.skuri_backend.domain.image.repository.MediaCleanupTaskRepository;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

    @Transactional
    public void retain(Collection<String> relativePaths) {
        List<MediaCleanupTask> tasks = lockTasks(relativePaths, LocalDateTime.now());
        if (tasks.stream().anyMatch(MediaCleanupTask::isCompleted)) {
            throw new BusinessException(ErrorCode.CHAT_IMAGE_UNAVAILABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        tasks.forEach(task -> task.markActive(now));
    }

    @Transactional
    public void lock(Collection<String> relativePaths) {
        lockTasks(relativePaths, LocalDateTime.now());
    }

    @Transactional
    public List<String> enqueue(Collection<String> relativePaths) {
        LocalDateTime now = LocalDateTime.now();
        return lockTasks(relativePaths, now).stream()
                .peek(task -> task.markPending(now))
                .map(MediaCleanupTask::getId)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNow(String taskId) {
        mediaCleanupTaskRepository.findByIdForUpdate(taskId)
                .filter(MediaCleanupTask::isPending)
                .ifPresent(this::processTask);
    }

    @Scheduled(
            fixedDelayString = "${media.cleanup.fixed-delay-ms:60000}",
            initialDelayString = "${media.cleanup.initial-delay-ms:60000}"
    )
    @Transactional
    public void processDueTasks() {
        List<String> taskIds = mediaCleanupTaskRepository.findDueTaskIds(
                        MediaCleanupTaskStatus.PENDING,
                        LocalDateTime.now(),
                        PageRequest.of(0, 20)
                );
        taskIds.forEach(this::processPendingTask);
    }

    private void processPendingTask(String taskId) {
        mediaCleanupTaskRepository.findByIdForUpdate(taskId)
                .filter(MediaCleanupTask::isPending)
                .ifPresent(this::processTask);
    }

    private List<MediaCleanupTask> lockTasks(Collection<String> relativePaths, LocalDateTime now) {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return List.of();
        }

        return relativePaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .map(path -> lockTask(path, now))
                .toList();
    }

    private MediaCleanupTask lockTask(String relativePath, LocalDateTime now) {
        mediaCleanupTaskRepository.insertActiveIfAbsent(relativePath, now);
        return mediaCleanupTaskRepository.findByRelativePathForUpdate(relativePath)
                .orElseThrow(() -> new IllegalStateException("미디어 정리 작업 잠금에 실패했습니다."));
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
