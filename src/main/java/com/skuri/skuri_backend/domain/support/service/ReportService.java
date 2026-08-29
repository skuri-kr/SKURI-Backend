package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.exception.CommentNotFoundException;
import com.skuri.skuri_backend.domain.board.exception.PostNotFoundException;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.app.exception.AppNoticeCommentNotFoundException;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentRepository;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.exception.ChatMessageNotFoundException;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.image.policy.ChatImageAsset;
import com.skuri.skuri_backend.domain.image.policy.ChatImageAssetPolicy;
import com.skuri.skuri_backend.domain.image.service.MediaCleanupTaskService;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notice.exception.NoticeCommentNotFoundException;
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentRepository;
import com.skuri.skuri_backend.domain.support.dto.request.CreateReportRequest;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateReportStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminReportResponse;
import com.skuri.skuri_backend.domain.support.dto.response.ReportCreateResponse;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.exception.ReportNotFoundException;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import com.skuri.skuri_backend.domain.support.model.ChatMessageReportSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import com.skuri.skuri_backend.infra.admin.list.AdminPageRequestPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NoticeCommentRepository noticeCommentRepository;
    private final AppNoticeCommentRepository appNoticeCommentRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final PartyRepository partyRepository;
    private final StorageRepository storageRepository;
    private final MediaCleanupTaskService mediaCleanupTaskService;

    @Transactional
    public ReportCreateResponse createReport(String reporterId, CreateReportRequest request) {
        String normalizedTargetId = normalizeRequired(request.targetId());
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, request.targetType(), normalizedTargetId)) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_SUBMITTED);
        }

        ReportTarget target = resolveReportTarget(request.targetType(), normalizedTargetId);
        if (reporterId.equals(target.authorId())) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_YOURSELF);
        }
        if (target.chatImageAsset() != null) {
            mediaCleanupTaskService.retain(target.chatImageAsset().cleanupPaths());
        }

        try {
            Report report = reportRepository.saveAndFlush(Report.create(
                    request.targetType(),
                    normalizedTargetId,
                    target.authorId(),
                    target.snapshot(),
                    target.chatImageAsset() == null
                            ? ChatImageAssetPolicy.NO_MANAGED_ASSET_KEY
                            : target.chatImageAsset().familyKey(),
                    normalizeCode(request.category()),
                    normalizeRequired(request.reason()),
                    reporterId
            ));
            return new ReportCreateResponse(report.getId(), report.getStatus(), report.getCreatedAt());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_SUBMITTED);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReportResponse> getAdminReports(ReportStatus status, ReportTargetType targetType, int page, int size) {
        Page<AdminReportResponse> reportPage = reportRepository.search(status, targetType, resolvePageable(page, size))
                .map(this::toAdminResponse);
        return PageResponse.from(reportPage);
    }

    @Transactional
    public AdminReportResponse updateReportStatus(String reportId, UpdateReportStatusRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(ReportNotFoundException::new);
        report.updateReview(request.status(), normalizeOptionalCode(request.action()), trimToNull(request.memo()));
        reportRepository.saveAndFlush(report);
        return toAdminResponse(report);
    }

    private ReportTarget resolveReportTarget(ReportTargetType targetType, String targetId) {
        return switch (targetType) {
            case POST -> new ReportTarget(postRepository.findByIdAndDeletedFalse(targetId)
                    .orElseThrow(PostNotFoundException::new)
                    .getAuthorId());
            case COMMENT -> new ReportTarget(commentRepository.findActiveById(targetId)
                    .orElseThrow(CommentNotFoundException::new)
                    .getAuthorId());
            case NOTICE_COMMENT -> new ReportTarget(noticeCommentRepository.findByIdAndDeletedFalse(targetId)
                    .orElseThrow(NoticeCommentNotFoundException::new)
                    .getUserId());
            case APP_NOTICE_COMMENT -> new ReportTarget(appNoticeCommentRepository.findByIdAndDeletedFalse(targetId)
                    .orElseThrow(AppNoticeCommentNotFoundException::new)
                    .getUserId());
            case MEMBER -> new ReportTarget(memberRepository.findById(targetId)
                    .orElseThrow(MemberNotFoundException::new)
                    .getId());
            case CHAT_MESSAGE -> resolveChatMessageTarget(targetId);
            case CHAT_ROOM -> resolveChatRoomAuthorId(targetId);
            case TAXI_PARTY -> new ReportTarget(partyRepository.findById(targetId)
                    .orElseThrow(PartyNotFoundException::new)
                    .getLeaderId());
        };
    }

    private ReportTarget resolveChatMessageTarget(String targetId) {
        ChatMessage message = chatMessageRepository.findByIdForUpdate(targetId)
                .orElseThrow(ChatMessageNotFoundException::new);
        if (message.isDeleted()) {
            throw new ChatMessageNotFoundException();
        }
        String imageUrl = message.getType() == ChatMessageType.IMAGE ? message.getText() : null;
        String text = message.getType() == ChatMessageType.IMAGE ? null : message.getText();
        ChatImageAsset chatImageAsset = resolveManagedChatImageAsset(imageUrl).orElse(null);
        return new ReportTarget(
                message.getSenderId(),
                new ChatMessageReportSnapshot(
                        message.getId(),
                        message.getChatRoomId(),
                        message.getSenderId(),
                        message.getSenderName(),
                        message.getType(),
                        text,
                        imageUrl,
                        message.getAccountData(),
                        message.getDirection(),
                        message.getSource(),
                        message.getCreatedAt(),
                        message.getEditedAt()
                ),
                chatImageAsset
        );
    }

    private Optional<ChatImageAsset> resolveManagedChatImageAsset(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return Optional.empty();
        }
        return storageRepository.resolveRelativePath(imageUrl)
                .flatMap(ChatImageAssetPolicy::resolve);
    }

    private ReportTarget resolveChatRoomAuthorId(String targetId) {
        ChatRoom room = chatRoomRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (room.getType() == ChatRoomType.PARTY) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        return new ReportTarget(room.getCreatedBy());
    }

    private AdminReportResponse toAdminResponse(Report report) {
        return new AdminReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getTargetAuthorId(),
                report.getTargetSnapshot(),
                report.getCategory(),
                report.getReason(),
                report.getStatus(),
                report.getAction(),
                report.getAdminMemo(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private Pageable resolvePageable(int page, int size) {
        return AdminPageRequestPolicy.of(page, size);
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeCode(String value) {
        return normalizeRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record ReportTarget(
            String authorId,
            ChatMessageReportSnapshot snapshot,
            ChatImageAsset chatImageAsset
    ) {

        private ReportTarget(String authorId) {
            this(authorId, null, null);
        }
    }
}
