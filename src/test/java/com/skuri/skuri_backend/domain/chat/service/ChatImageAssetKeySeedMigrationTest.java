package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.seed.entity.SeedMigration;
import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.image.service.MediaCleanupTaskService;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.model.ChatMessageReportSnapshot;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatImageAssetKeySeedMigrationTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private StorageRepository storageRepository;

    @Mock
    private MediaCleanupTaskService mediaCleanupTaskService;

    @Mock
    private SeedMigrationRepository seedMigrationRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ChatImageAssetKeySeedMigration migration;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void migrate_기존채팅이미지와신고증거에정규화자산키를채운다() {
        String imageUrl = "https://cdn.skuri.app/chat/2026/08/shared-image.png";
        ChatMessage message = ChatMessage.create(
                "room-1",
                "member-1",
                "홍길동",
                imageUrl,
                ChatMessageType.IMAGE,
                null,
                null
        );
        Report report = Report.create(
                ReportTargetType.CHAT_MESSAGE,
                "message-1",
                "member-1",
                new ChatMessageReportSnapshot(
                        "message-1",
                        "room-1",
                        "member-1",
                        "홍길동",
                        ChatMessageType.IMAGE,
                        null,
                        imageUrl,
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 8, 17, 12, 0),
                        null
                ),
                "SPAM",
                "광고성 이미지입니다.",
                "reporter-1"
        );
        ReflectionTestUtils.setField(report, "targetImageAssetKey", null);

        when(seedMigrationRepository.existsById(ChatImageAssetKeySeedMigration.MIGRATION_KEY)).thenReturn(false);
        when(chatMessageRepository.findByTypeAndImageAssetKeyIsNull(eq(ChatMessageType.IMAGE), any(Pageable.class)))
                .thenReturn(List.of(message), List.of());
        when(reportRepository.findByTargetImageAssetKeyIsNull(any(Pageable.class)))
                .thenReturn(List.of(report), List.of());
        when(storageRepository.resolveRelativePath(imageUrl))
                .thenReturn(Optional.of("chat/2026/08/shared-image.png"));

        migration.migrate();

        assertEquals("chat/2026/08/shared-image", message.getImageAssetKey());
        assertEquals("chat/2026/08/shared-image", report.getTargetImageAssetKey());
        InOrder backfillOrder = inOrder(mediaCleanupTaskService, chatMessageRepository, reportRepository);
        backfillOrder.verify(mediaCleanupTaskService).lock(List.of(
                "chat/2026/08/shared-image.jpg",
                "chat/2026/08/shared-image.png",
                "chat/2026/08/shared-image.webp",
                "chat/2026/08/shared-image_thumb.jpg",
                "chat/2026/08/shared-image_thumb.png",
                "chat/2026/08/shared-image_thumb.webp"
        ));
        backfillOrder.verify(chatMessageRepository).saveAll(List.of(message));
        backfillOrder.verify(reportRepository).saveAll(List.of(report));
        ArgumentCaptor<SeedMigration> migrationCaptor = ArgumentCaptor.forClass(SeedMigration.class);
        verify(seedMigrationRepository).saveAndFlush(migrationCaptor.capture());
        assertEquals(ChatImageAssetKeySeedMigration.MIGRATION_KEY, migrationCaptor.getValue().getMigrationKey());
    }

    @Test
    void migrate_snapshot이없는기존채팅신고는_대상이미지에서자산키를복원한다() {
        String imageUrl = "https://cdn.skuri.app/chat/2026/08/legacy-image.jpg";
        ChatMessage targetMessage = ChatMessage.create(
                "room-1",
                "member-1",
                "홍길동",
                imageUrl,
                ChatMessageType.IMAGE,
                null,
                null
        );
        ReflectionTestUtils.setField(targetMessage, "id", "message-legacy");
        Report report = Report.create(
                ReportTargetType.CHAT_MESSAGE,
                "message-legacy",
                "member-1",
                "SPAM",
                "기존 이미지 신고입니다.",
                "reporter-1"
        );
        ReflectionTestUtils.setField(report, "targetImageAssetKey", null);

        when(seedMigrationRepository.existsById(ChatImageAssetKeySeedMigration.MIGRATION_KEY)).thenReturn(false);
        when(chatMessageRepository.findByTypeAndImageAssetKeyIsNull(eq(ChatMessageType.IMAGE), any(Pageable.class)))
                .thenReturn(List.of(), List.of());
        when(reportRepository.findByTargetImageAssetKeyIsNull(any(Pageable.class)))
                .thenReturn(List.of(report), List.of());
        when(chatMessageRepository.findAllById(List.of("message-legacy"))).thenReturn(List.of(targetMessage));
        when(storageRepository.resolveRelativePath(imageUrl)).thenReturn(Optional.of("chat/2026/08/legacy-image.jpg"));

        migration.migrate();

        assertEquals("chat/2026/08/legacy-image", report.getTargetImageAssetKey());
        verify(mediaCleanupTaskService).lock(List.of(
                "chat/2026/08/legacy-image.jpg",
                "chat/2026/08/legacy-image.png",
                "chat/2026/08/legacy-image.webp",
                "chat/2026/08/legacy-image_thumb.jpg",
                "chat/2026/08/legacy-image_thumb.png",
                "chat/2026/08/legacy-image_thumb.webp"
        ));
    }
}
