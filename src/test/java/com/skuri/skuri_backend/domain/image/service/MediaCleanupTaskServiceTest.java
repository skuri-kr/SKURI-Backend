package com.skuri.skuri_backend.domain.image.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTask;
import com.skuri.skuri_backend.domain.image.entity.MediaCleanupTaskStatus;
import com.skuri.skuri_backend.domain.image.repository.MediaCleanupTaskRepository;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaCleanupTaskServiceTest {

    @Mock
    private MediaCleanupTaskRepository mediaCleanupTaskRepository;

    @Mock
    private StorageRepository storageRepository;

    private MediaCleanupTaskService mediaCleanupTaskService;

    @BeforeEach
    void setUp() {
        mediaCleanupTaskService = new MediaCleanupTaskService(mediaCleanupTaskRepository, storageRepository);
    }

    @Test
    void enqueue_같은경로는하나의작업으로재사용한다() {
        MediaCleanupTask task = task("task-1", "chat/2026/08/image.jpg");
        when(mediaCleanupTaskRepository.findByRelativePathForUpdate("chat/2026/08/image.jpg"))
                .thenReturn(Optional.of(task));

        List<String> taskIds = mediaCleanupTaskService.enqueue(List.of(
                "chat/2026/08/image.jpg",
                "chat/2026/08/image.jpg"
        ));

        assertEquals(List.of("task-1"), taskIds);
        assertEquals(MediaCleanupTaskStatus.PENDING, task.getStatus());
    }

    @Test
    void processNow_스토리지삭제성공이면완료처리한다() {
        MediaCleanupTask task = task("task-1", "chat/2026/08/image.jpg");
        when(mediaCleanupTaskRepository.findByIdForUpdate("task-1")).thenReturn(Optional.of(task));

        mediaCleanupTaskService.processNow("task-1");

        verify(storageRepository).delete("chat/2026/08/image.jpg");
        assertEquals(MediaCleanupTaskStatus.COMPLETED, task.getStatus());
        assertEquals(0, task.getAttemptCount());
    }

    @Test
    void processNow_삭제실패면지수백오프재시도를예약한다() {
        MediaCleanupTask task = task("task-1", "chat/2026/08/image.jpg");
        LocalDateTime before = LocalDateTime.now();
        when(mediaCleanupTaskRepository.findByIdForUpdate("task-1")).thenReturn(Optional.of(task));
        doThrow(new IllegalStateException("storage unavailable"))
                .when(storageRepository)
                .delete("chat/2026/08/image.jpg");

        mediaCleanupTaskService.processNow("task-1");

        assertEquals(MediaCleanupTaskStatus.PENDING, task.getStatus());
        assertEquals(1, task.getAttemptCount());
        assertEquals("storage unavailable", task.getLastError());
        assertTrue(task.getNextAttemptAt().isAfter(before));
    }

    @Test
    void retain_대기중인정리작업을활성참조로되돌린다() {
        MediaCleanupTask task = task("task-1", "chat/2026/08/image.jpg");
        when(mediaCleanupTaskRepository.findByRelativePathForUpdate("chat/2026/08/image.jpg"))
                .thenReturn(Optional.of(task));

        mediaCleanupTaskService.retain(List.of("chat/2026/08/image.jpg"));

        assertEquals(MediaCleanupTaskStatus.ACTIVE, task.getStatus());
    }

    @Test
    void retain_이미정리된파일은새메시지참조를거절한다() {
        MediaCleanupTask task = task("task-1", "chat/2026/08/image.jpg");
        task.markCompleted(LocalDateTime.now());
        when(mediaCleanupTaskRepository.findByRelativePathForUpdate("chat/2026/08/image.jpg"))
                .thenReturn(Optional.of(task));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> mediaCleanupTaskService.retain(List.of("chat/2026/08/image.jpg"))
        );

        assertEquals(ErrorCode.CHAT_IMAGE_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void processNow_다시참조된이미지는삭제하지않는다() {
        MediaCleanupTask task = task("task-1", "chat/2026/08/image.jpg");
        task.markActive(LocalDateTime.now());
        when(mediaCleanupTaskRepository.findByIdForUpdate("task-1")).thenReturn(Optional.of(task));

        mediaCleanupTaskService.processNow("task-1");

        verify(storageRepository, never()).delete(any());
        assertEquals(MediaCleanupTaskStatus.ACTIVE, task.getStatus());
    }

    private MediaCleanupTask task(String id, String relativePath) {
        MediaCleanupTask task = MediaCleanupTask.create(relativePath, LocalDateTime.now());
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
