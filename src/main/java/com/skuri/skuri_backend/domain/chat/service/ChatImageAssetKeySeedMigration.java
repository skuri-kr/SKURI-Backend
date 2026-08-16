package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.seed.entity.SeedMigration;
import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.image.policy.ChatImageAssetPolicy;
import com.skuri.skuri_backend.domain.image.service.MediaCleanupTaskService;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.model.ChatMessageReportSnapshot;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ChatImageAssetKeySeedMigration {

    static final String MIGRATION_KEY = "chat-image-asset-key-backfill-20260817";

    private static final int BATCH_SIZE = 200;

    private final ChatMessageRepository chatMessageRepository;
    private final ReportRepository reportRepository;
    private final StorageRepository storageRepository;
    private final MediaCleanupTaskService mediaCleanupTaskService;
    private final SeedMigrationRepository seedMigrationRepository;
    private final TransactionTemplate transactionTemplate;

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        if (seedMigrationRepository.existsById(MIGRATION_KEY)) {
            return;
        }

        int updatedMessageCount = 0;
        int updatedReportCount = 0;
        while (true) {
            BackfillBatch batch = transactionTemplate.execute(status -> backfillNextBatch());
            if (batch == null || batch.isEmpty()) {
                break;
            }
            updatedMessageCount += batch.updatedMessageCount();
            updatedReportCount += batch.updatedReportCount();
        }

        try {
            seedMigrationRepository.saveAndFlush(SeedMigration.apply(MIGRATION_KEY));
            log.info(
                    "채팅 이미지 자산 키 backfill 완료: messages={}, reports={}",
                    updatedMessageCount,
                    updatedReportCount
            );
        } catch (DataIntegrityViolationException e) {
            log.info("채팅 이미지 자산 키 backfill 건너뜀: 이미 다른 인스턴스에서 적용됨");
        }
    }

    private BackfillBatch backfillNextBatch() {
        List<ChatMessage> messages = chatMessageRepository.findByTypeAndImageAssetKeyIsNull(
                ChatMessageType.IMAGE,
                PageRequest.of(0, BATCH_SIZE)
        );
        List<Report> reports = reportRepository.findByTargetImageAssetKeyIsNull(PageRequest.of(0, BATCH_SIZE));

        List<ImageAssetKeyBackfill> messageBackfills = messages.stream()
                .map(message -> new ImageAssetKeyBackfill(message.getId(), resolveImageAssetKey(message.getText())))
                .toList();
        Map<String, ChatMessage> reportTargetMessages = findLegacyReportTargetMessages(reports);
        List<ImageAssetKeyBackfill> reportBackfills = reports.stream()
                .map(report -> new ImageAssetKeyBackfill(
                        report.getId(),
                        resolveReportImageAssetKey(report, reportTargetMessages)
                ))
                .toList();

        lockManagedAssets(messageBackfills, reportBackfills);

        int updatedMessageCount = messageBackfills.stream()
                .mapToInt(backfill -> chatMessageRepository.fillMissingImageAssetKey(
                        backfill.id(),
                        backfill.assetKey()
                ))
                .sum();
        int updatedReportCount = reportBackfills.stream()
                .mapToInt(backfill -> reportRepository.fillMissingTargetImageAssetKeyById(
                        backfill.id(),
                        backfill.assetKey()
                ))
                .sum();

        return new BackfillBatch(messages.size(), reports.size(), updatedMessageCount, updatedReportCount);
    }

    private Map<String, ChatMessage> findLegacyReportTargetMessages(List<Report> reports) {
        List<String> targetIds = reports.stream()
                .filter(report -> report.getTargetType() == ReportTargetType.CHAT_MESSAGE)
                .filter(report -> report.getTargetSnapshot() == null)
                .map(Report::getTargetId)
                .distinct()
                .toList();
        if (targetIds.isEmpty()) {
            return Map.of();
        }

        Map<String, ChatMessage> targetMessages = new LinkedHashMap<>();
        chatMessageRepository.findAllById(targetIds)
                .forEach(message -> targetMessages.put(message.getId(), message));
        return targetMessages;
    }

    private String resolveReportImageAssetKey(Report report, Map<String, ChatMessage> targetMessages) {
        ChatMessageReportSnapshot snapshot = report.getTargetSnapshot();
        if (snapshot != null) {
            return resolveImageAssetKey(snapshot.imageUrl());
        }
        if (report.getTargetType() != ReportTargetType.CHAT_MESSAGE) {
            return ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY;
        }

        ChatMessage targetMessage = targetMessages.get(report.getTargetId());
        if (targetMessage == null || targetMessage.getType() != ChatMessageType.IMAGE) {
            return ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY;
        }
        if (StringUtils.hasText(targetMessage.getImageAssetKey())
                && !ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY.equals(targetMessage.getImageAssetKey())) {
            return targetMessage.getImageAssetKey();
        }
        return resolveImageAssetKey(targetMessage.getText());
    }

    private void lockManagedAssets(
            List<ImageAssetKeyBackfill> messageBackfills,
            List<ImageAssetKeyBackfill> reportBackfills
    ) {
        List<String> cleanupPaths = java.util.stream.Stream.concat(messageBackfills.stream(), reportBackfills.stream())
                .map(ImageAssetKeyBackfill::assetKey)
                .filter(assetKey -> !ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY.equals(assetKey))
                .flatMap(assetKey -> ChatImageAssetPolicy.cleanupPathsForFamilyKey(assetKey).stream())
                .distinct()
                .sorted()
                .toList();
        if (!cleanupPaths.isEmpty()) {
            mediaCleanupTaskService.lock(cleanupPaths);
        }
    }

    private String resolveImageAssetKey(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY;
        }
        return storageRepository.resolveRelativePath(imageUrl)
                .flatMap(ChatImageAssetPolicy::resolve)
                .map(imageAsset -> imageAsset.familyKey())
                .orElse(ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY);
    }

    private record ImageAssetKeyBackfill(String id, String assetKey) {
    }

    private record BackfillBatch(
            int messageCount,
            int reportCount,
            int updatedMessageCount,
            int updatedReportCount
    ) {

        private boolean isEmpty() {
            return messageCount == 0 && reportCount == 0;
        }
    }
}
