package com.skuri.skuri_backend.domain.support.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.image.policy.ChatImageAssetPolicy;
import com.skuri.skuri_backend.domain.support.entity.converter.ChatMessageReportSnapshotJsonConverter;
import com.skuri.skuri_backend.domain.support.model.ChatMessageReportSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reports_reporter_target",
                        columnNames = {"reporter_id", "target_type", "target_id"}
                )
        },
        indexes = {
                @jakarta.persistence.Index(name = "idx_reports_target", columnList = "target_type, target_id"),
                @jakarta.persistence.Index(name = "idx_reports_chat_image_asset", columnList = "target_type, target_image_asset_key")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "target_author_id", length = 36)
    private String targetAuthorId;

    @Convert(converter = ChatMessageReportSnapshotJsonConverter.class)
    @Column(name = "target_snapshot", columnDefinition = "json")
    private ChatMessageReportSnapshot targetSnapshot;

    @Column(name = "target_image_asset_key", length = 255)
    private String targetImageAssetKey;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "reporter_id", nullable = false, length = 36)
    private String reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(length = 100)
    private String action;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    private Report(
            ReportTargetType targetType,
            String targetId,
            String targetAuthorId,
            ChatMessageReportSnapshot targetSnapshot,
            String targetImageAssetKey,
            String category,
            String reason,
            String reporterId
    ) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetAuthorId = targetAuthorId;
        this.targetSnapshot = targetSnapshot;
        this.targetImageAssetKey = targetImageAssetKey;
        this.category = category;
        this.reason = reason;
        this.reporterId = reporterId;
        this.status = ReportStatus.PENDING;
    }

    public static Report create(
            ReportTargetType targetType,
            String targetId,
            String targetAuthorId,
            String category,
            String reason,
            String reporterId
    ) {
        return new Report(
                targetType,
                targetId,
                targetAuthorId,
                null,
                ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY,
                category,
                reason,
                reporterId
        );
    }

    public static Report create(
            ReportTargetType targetType,
            String targetId,
            String targetAuthorId,
            ChatMessageReportSnapshot targetSnapshot,
            String category,
            String reason,
            String reporterId
    ) {
        return create(
                targetType,
                targetId,
                targetAuthorId,
                targetSnapshot,
                ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY,
                category,
                reason,
                reporterId
        );
    }

    public static Report create(
            ReportTargetType targetType,
            String targetId,
            String targetAuthorId,
            ChatMessageReportSnapshot targetSnapshot,
            String targetImageAssetKey,
            String category,
            String reason,
            String reporterId
    ) {
        return new Report(
                targetType,
                targetId,
                targetAuthorId,
                targetSnapshot,
                targetImageAssetKey,
                category,
                reason,
                reporterId
        );
    }

    public void markTargetImageAssetKey(String targetImageAssetKey) {
        this.targetImageAssetKey = targetImageAssetKey;
    }

    public void updateReview(ReportStatus status, String action, String adminMemo) {
        if (!this.status.canTransitionTo(status)) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_STATUS_TRANSITION);
        }
        this.status = status;
        this.action = action;
        this.adminMemo = adminMemo;
    }
}
