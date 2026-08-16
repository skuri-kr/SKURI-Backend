package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.seed.entity.SeedMigration;
import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.image.policy.ChatImageAssetPolicy;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import com.skuri.skuri_backend.domain.support.entity.Report;
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

import java.util.List;

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
            updatedMessageCount += batch.messageCount();
            updatedReportCount += batch.reportCount();
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
        messages.forEach(message -> message.markImageAssetKey(resolveImageAssetKey(message.getText())));
        chatMessageRepository.saveAll(messages);

        List<Report> reports = reportRepository.findByTargetImageAssetKeyIsNull(PageRequest.of(0, BATCH_SIZE));
        reports.forEach(report -> report.markTargetImageAssetKey(resolveImageAssetKey(report.getTargetSnapshot())));
        reportRepository.saveAll(reports);

        return new BackfillBatch(messages.size(), reports.size());
    }

    private String resolveImageAssetKey(ChatMessageReportSnapshot snapshot) {
        return snapshot == null
                ? ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY
                : resolveImageAssetKey(snapshot.imageUrl());
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

    private record BackfillBatch(int messageCount, int reportCount) {

        private boolean isEmpty() {
            return messageCount == 0 && reportCount == 0;
        }
    }
}
