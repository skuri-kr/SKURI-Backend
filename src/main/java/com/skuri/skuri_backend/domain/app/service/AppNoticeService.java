package com.skuri.skuri_backend.domain.app.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.common.util.AnonymousCommentIdGenerator;
import com.skuri.skuri_backend.domain.app.dto.request.CreateAppNoticeRequest;
import com.skuri.skuri_backend.domain.app.dto.request.UpdateAppNoticeRequest;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeCreateResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeReadResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeUnreadCountResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeComment;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCommentLike;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeLike;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeReadStatus;
import com.skuri.skuri_backend.domain.app.exception.AppNoticeCommentNotFoundException;
import com.skuri.skuri_backend.domain.app.exception.AppNoticeNotFoundException;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentLikeRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeLikeRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeReadStatusRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberWithdrawalSanitizer;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notice.dto.request.CreateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.request.UpdateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeLikeResponse;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppNoticeService {

    private final AppNoticeRepository appNoticeRepository;
    private final AppNoticeReadStatusRepository appNoticeReadStatusRepository;
    private final AppNoticeLikeRepository appNoticeLikeRepository;
    private final AppNoticeCommentRepository appNoticeCommentRepository;
    private final AppNoticeCommentLikeRepository appNoticeCommentLikeRepository;
    private final MemberRepository memberRepository;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<AppNoticeResponse> getPublishedNotices() {
        return appNoticeRepository.findPublished(LocalDateTime.now()).stream()
                .map(appNotice -> toResponse(appNotice, false))
                .toList();
    }

    @Transactional
    public AppNoticeResponse getPublishedNotice(String memberId, String appNoticeId) {
        LocalDateTime now = LocalDateTime.now();
        if (appNoticeRepository.incrementPublishedViewCount(appNoticeId, now) == 0) {
            throw new AppNoticeNotFoundException();
        }
        AppNotice appNotice = findPublishedNoticeOrThrow(appNoticeId);
        boolean liked = StringUtils.hasText(memberId)
                && appNoticeLikeRepository.existsById_UserIdAndId_AppNoticeId(memberId, appNoticeId);
        return toResponse(appNotice, liked);
    }

    public AppNoticeResponse getPublishedNotice(String appNoticeId) {
        return getPublishedNotice(null, appNoticeId);
    }

    @Transactional(readOnly = true)
    public AppNoticeUnreadCountResponse getUnreadCount(String memberId) {
        return new AppNoticeUnreadCountResponse(appNoticeRepository.countPublishedUnread(memberId, LocalDateTime.now()));
    }

    @Transactional
    public AppNoticeReadResponse markRead(String memberId, String appNoticeId) {
        AppNotice appNotice = findPublishedNoticeOrThrow(appNoticeId);
        return appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId(memberId, appNoticeId)
                .map(this::toReadResponse)
                .orElseGet(() -> createReadStatus(memberId, appNotice));
    }

    @Transactional(readOnly = true)
    public List<NoticeCommentResponse> getComments(String memberId, String appNoticeId) {
        findPublishedNoticeOrThrow(appNoticeId);
        List<AppNoticeComment> comments = appNoticeCommentRepository.findByAppNoticeIdOrderByCreatedAtAsc(appNoticeId);
        return flattenComments(comments, memberId, resolveLikedCommentIds(memberId, comments));
    }

    @Transactional
    public NoticeCommentResponse createComment(String memberId, String appNoticeId, CreateNoticeCommentRequest request) {
        Member author = findMemberForUpdateOrThrow(memberId);
        AppNotice appNotice = findPublishedNoticeForUpdateOrThrow(appNoticeId);
        AppNoticeComment parent = null;
        if (request.parentId() != null) {
            parent = appNoticeCommentRepository.findByIdAndAppNoticeId(request.parentId(), appNoticeId)
                    .orElseThrow(AppNoticeCommentNotFoundException::new);
            if (parent.isDeleted()) {
                throw new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED);
            }
        }
        AnonymousMetadata metadata = resolveAnonymousMetadata(appNoticeId, memberId, request.isAnonymous());
        AppNoticeComment saved = appNoticeCommentRepository.save(AppNoticeComment.create(
                appNotice, memberId, resolveDisplayName(author), request.content().trim(), request.isAnonymous(),
                metadata.anonId, metadata.anonymousOrder, parent
        ));
        appNotice.increaseCommentCount(1);
        eventPublisher.publish(new NotificationDomainEvent.AppNoticeCommentCreated(saved.getId()));
        return toCommentResponse(saved, memberId, resolveDepth(saved), false);
    }

    @Transactional
    public NoticeCommentResponse updateComment(String memberId, String commentId, UpdateNoticeCommentRequest request) {
        AppNoticeComment comment = findCommentForAggregateWriteOrThrow(commentId);
        requireCommentAuthor(comment, memberId);
        boolean targetAnonymous = request.isAnonymous() != null ? request.isAnonymous() : comment.isAnonymous();
        AnonymousMetadata metadata = resolveUpdatedAnonymousMetadata(comment, memberId, request.isAnonymous());
        comment.update(request.content().trim(), targetAnonymous, metadata.anonId, metadata.anonymousOrder);
        return toCommentResponse(comment, memberId, resolveDepth(comment), resolveCommentIsLiked(memberId, commentId));
    }

    @Transactional
    public void deleteComment(String memberId, String commentId) {
        AppNoticeComment comment = findCommentForAggregateWriteOrThrow(commentId);
        requireCommentAuthor(comment, memberId);
        AppNotice appNotice = comment.getAppNotice();
        comment.softDelete();
        appNotice.increaseCommentCount(-1);
    }

    @Transactional
    public NoticeLikeResponse likeNotice(String memberId, String appNoticeId) {
        findMemberForUpdateOrThrow(memberId);
        AppNotice appNotice = findPublishedNoticeForUpdateOrThrow(appNoticeId);
        if (appNoticeLikeRepository.existsById_UserIdAndId_AppNoticeId(memberId, appNoticeId)) {
            return new NoticeLikeResponse(true, appNotice.getLikeCount());
        }
        appNoticeLikeRepository.save(AppNoticeLike.create(appNotice, memberId));
        appNotice.increaseLikeCount(1);
        return new NoticeLikeResponse(true, appNotice.getLikeCount());
    }

    @Transactional
    public NoticeLikeResponse unlikeNotice(String memberId, String appNoticeId) {
        AppNotice appNotice = findPublishedNoticeForUpdateOrThrow(appNoticeId);
        appNoticeLikeRepository.findById_UserIdAndId_AppNoticeId(memberId, appNoticeId).ifPresent(like -> {
            appNoticeLikeRepository.delete(like);
            appNotice.increaseLikeCount(-1);
        });
        return new NoticeLikeResponse(false, appNotice.getLikeCount());
    }

    @Transactional
    public NoticeCommentLikeResponse likeComment(String memberId, String commentId) {
        findMemberForUpdateOrThrow(memberId);
        AppNoticeComment comment = findCommentForAggregateWriteOrThrow(commentId);
        if (appNoticeCommentLikeRepository.existsById_UserIdAndId_CommentId(memberId, commentId)) {
            return new NoticeCommentLikeResponse(commentId, true, comment.getLikeCount());
        }
        appNoticeCommentLikeRepository.save(AppNoticeCommentLike.create(comment, memberId));
        comment.increaseLikeCount(1);
        return new NoticeCommentLikeResponse(commentId, true, comment.getLikeCount());
    }

    @Transactional
    public NoticeCommentLikeResponse unlikeComment(String memberId, String commentId) {
        AppNoticeComment comment = findCommentForAggregateWriteOrThrow(commentId);
        appNoticeCommentLikeRepository.findById_UserIdAndId_CommentId(memberId, commentId).ifPresent(like -> {
            appNoticeCommentLikeRepository.delete(like);
            comment.increaseLikeCount(-1);
        });
        return new NoticeCommentLikeResponse(commentId, false, comment.getLikeCount());
    }

    @Transactional
    public AppNoticeCreateResponse createAppNotice(CreateAppNoticeRequest request) {
        ActionFields action = normalizeAction(request.actionUrl(), request.actionLabel());
        AppNotice appNotice = appNoticeRepository.save(AppNotice.create(
                request.title().trim(), request.content().trim(), request.category(), request.priority(),
                normalizeImageUrls(request.imageUrls()), action.url, action.label, request.publishedAt()
        ));
        eventPublisher.publish(new NotificationDomainEvent.AppNoticeCreated(appNotice.getId()));
        return new AppNoticeCreateResponse(appNotice.getId(), appNotice.getTitle(), appNotice.getCreatedAt());
    }

    @Transactional
    public AppNoticeResponse updateAppNotice(String appNoticeId, UpdateAppNoticeRequest request) {
        AppNotice appNotice = findAppNoticeForUpdateOrThrow(appNoticeId);
        appNotice.update(
                trimToNull(request.title()), trimToNull(request.content()), request.category(), request.priority(),
                request.imageUrls() == null ? null : normalizeImageUrls(request.imageUrls()), request.publishedAt()
        );
        if (request.actionUrl() != null) {
            String actionLabel = trimToNull(request.actionUrl()) == null
                    ? null
                    : request.actionLabel() == null ? appNotice.getActionLabel() : request.actionLabel();
            ActionFields action = normalizeAction(request.actionUrl(), actionLabel);
            appNotice.updateAction(action.url, action.label);
        } else if (request.actionLabel() != null) {
            if (appNotice.getActionUrl() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "actionLabel은 actionUrl과 함께 사용할 수 있습니다.");
            }
            appNotice.updateAction(appNotice.getActionUrl(), trimToNull(request.actionLabel()));
        }
        return toResponse(appNotice, false);
    }

    @Transactional
    public void deleteAppNotice(String appNoticeId) {
        AppNotice appNotice = findAppNoticeForUpdateOrThrow(appNoticeId);
        List<AppNoticeComment> comments = appNoticeCommentRepository.findByAppNoticeIdOrderByCreatedAtAsc(appNoticeId);
        List<AppNoticeCommentLike> commentLikes = appNoticeCommentLikeRepository.findByAppNoticeId(appNoticeId);
        if (!commentLikes.isEmpty()) {
            appNoticeCommentLikeRepository.deleteAllInBatch(commentLikes);
        }
        if (!comments.isEmpty()) {
            comments.forEach(AppNoticeComment::detachParent);
            appNoticeCommentRepository.flush();
            appNoticeCommentRepository.deleteAllInBatch(comments);
        }
        List<AppNoticeLike> likes = appNoticeLikeRepository.findById_AppNoticeId(appNoticeId);
        if (!likes.isEmpty()) {
            appNoticeLikeRepository.deleteAllInBatch(likes);
        }
        appNoticeReadStatusRepository.deleteById_AppNoticeId(appNoticeId);
        appNoticeRepository.delete(appNotice);
    }

    @Transactional
    public void handleMemberWithdrawal(String memberId) {
        appNoticeCommentRepository.findByUserId(memberId).forEach(AppNoticeComment::anonymizeAuthor);
        List<AppNoticeCommentLike> commentLikes = appNoticeCommentLikeRepository.findById_UserId(memberId);
        if (!commentLikes.isEmpty()) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            commentLikes.forEach(like -> counts.merge(like.getId().getCommentId(), 1, Integer::sum));
            counts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> appNoticeCommentRepository.decrementLikeCountAtomically(
                            entry.getKey(), entry.getValue()));
            appNoticeCommentLikeRepository.deleteAllInBatch(commentLikes);
        }
        List<AppNoticeLike> likes = appNoticeLikeRepository.findById_UserId(memberId);
        if (!likes.isEmpty()) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            likes.forEach(like -> counts.merge(like.getId().getAppNoticeId(), 1, Integer::sum));
            counts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> appNoticeRepository.decrementLikeCountAtomically(
                            entry.getKey(), entry.getValue()));
            appNoticeLikeRepository.deleteAllInBatch(likes);
        }
        appNoticeReadStatusRepository.deleteById_UserId(memberId);
    }

    @Transactional
    public void deleteAllReadStatusesByUserId(String memberId) {
        appNoticeReadStatusRepository.deleteById_UserId(memberId);
    }

    private AppNoticeResponse toResponse(AppNotice appNotice, boolean isLiked) {
        return new AppNoticeResponse(
                appNotice.getId(), appNotice.getTitle(), appNotice.getContent(), appNotice.getCategory(),
                appNotice.getPriority(), List.copyOf(appNotice.getImageUrls()), appNotice.getActionUrl(),
                appNotice.getActionLabel(), appNotice.getViewCount(), appNotice.getLikeCount(),
                appNotice.getCommentCount(), isLiked, appNotice.getPublishedAt(), appNotice.getCreatedAt(),
                appNotice.getUpdatedAt()
        );
    }

    private List<NoticeCommentResponse> flattenComments(List<AppNoticeComment> comments, String memberId, Set<String> likedIds) {
        Map<String, CurrentAuthorProfile> authors = resolveCurrentAuthors(comments.stream().map(AppNoticeComment::getUserId).toList());
        Map<String, List<AppNoticeComment>> children = new LinkedHashMap<>();
        List<AppNoticeComment> roots = new ArrayList<>();
        for (AppNoticeComment comment : comments) {
            if (comment.hasParent()) {
                children.computeIfAbsent(comment.getParent().getId(), ignored -> new ArrayList<>()).add(comment);
            } else {
                roots.add(comment);
            }
        }
        List<NoticeCommentResponse> result = new ArrayList<>();
        roots.forEach(root -> appendCommentTree(result, root, 0, memberId, likedIds, children, authors));
        return result;
    }

    private void appendCommentTree(
            List<NoticeCommentResponse> result, AppNoticeComment comment, int depth, String memberId,
            Set<String> likedIds, Map<String, List<AppNoticeComment>> children,
            Map<String, CurrentAuthorProfile> authors
    ) {
        result.add(toCommentResponse(comment, memberId, depth, likedIds.contains(comment.getId()), authors));
        children.getOrDefault(comment.getId(), List.of()).forEach(child ->
                appendCommentTree(result, child, depth + 1, memberId, likedIds, children, authors));
    }

    private NoticeCommentResponse toCommentResponse(AppNoticeComment comment, String memberId, int depth, boolean isLiked) {
        return toCommentResponse(comment, memberId, depth, isLiked,
                resolveCurrentAuthors(Collections.singletonList(comment.getUserId())));
    }

    private NoticeCommentResponse toCommentResponse(
            AppNoticeComment comment, String memberId, int depth, boolean isLiked,
            Map<String, CurrentAuthorProfile> authors
    ) {
        boolean deleted = comment.isDeleted();
        AuthorView author = resolveAuthorView(
                comment.isAnonymous(), deleted, comment.getUserId(), comment.getUserDisplayName(),
                authors.get(comment.getUserId()), comment.getAnonymousOrder());
        return new NoticeCommentResponse(
                comment.getId(), comment.hasParent() ? comment.getParent().getId() : null, depth,
                comment.getContent(), author.id, author.name, author.photoUrl, author.admin,
                !deleted && comment.isAnonymous(), deleted ? null : comment.getAnonymousOrder(),
                !deleted && comment.isAuthor(memberId), comment.getLikeCount(), !deleted && isLiked,
                deleted, comment.getCreatedAt(), comment.getUpdatedAt()
        );
    }

    private Map<String, CurrentAuthorProfile> resolveCurrentAuthors(List<String> authorIds) {
        Set<String> ids = authorIds.stream().filter(StringUtils::hasText).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        Map<String, CurrentAuthorProfile> result = new HashMap<>();
        memberRepository.findAllActiveByIdIn(ids).forEach(member ->
                result.put(member.getId(), new CurrentAuthorProfile(member.getPhotoUrl(), member.isAdmin())));
        return result;
    }

    private AuthorView resolveAuthorView(
            boolean anonymous, boolean deleted, String authorId, String authorName,
            CurrentAuthorProfile current, Integer anonymousOrder
    ) {
        if (deleted) return new AuthorView(null, null, null, false);
        if (MemberWithdrawalSanitizer.isWithdrawnAuthorId(authorId)) {
            return new AuthorView(null, authorName, null, false);
        }
        if (!anonymous) {
            return new AuthorView(authorId, authorName, current == null ? null : current.photoUrl,
                    current != null && current.admin);
        }
        return new AuthorView(null, anonymousOrder == null ? "익명" : "익명" + anonymousOrder, null, false);
    }

    private Set<String> resolveLikedCommentIds(String memberId, List<AppNoticeComment> comments) {
        if (!StringUtils.hasText(memberId) || comments.isEmpty()) return Set.of();
        return Set.copyOf(appNoticeCommentLikeRepository.findLikedCommentIds(
                memberId, comments.stream().map(AppNoticeComment::getId).toList()));
    }

    private boolean resolveCommentIsLiked(String memberId, String commentId) {
        return StringUtils.hasText(memberId)
                && appNoticeCommentLikeRepository.existsById_UserIdAndId_CommentId(memberId, commentId);
    }

    private int resolveDepth(AppNoticeComment comment) {
        int depth = 0;
        AppNoticeComment current = comment;
        while (current.hasParent()) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private AnonymousMetadata resolveAnonymousMetadata(String appNoticeId, String memberId, boolean anonymous) {
        if (!anonymous) return new AnonymousMetadata(null, null);
        String generated = AnonymousCommentIdGenerator.generate(appNoticeId, memberId);
        AppNoticeComment existing = appNoticeCommentRepository
                .findFirstByAppNotice_IdAndUserIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(appNoticeId, memberId)
                .orElse(null);
        if (existing != null) {
            return new AnonymousMetadata(StringUtils.hasText(existing.getAnonId()) ? existing.getAnonId() : generated,
                    existing.getAnonymousOrder());
        }
        return new AnonymousMetadata(generated,
                appNoticeCommentRepository.findMaxAnonymousOrderByAppNoticeId(appNoticeId) + 1);
    }

    private AnonymousMetadata resolveUpdatedAnonymousMetadata(
            AppNoticeComment comment, String memberId, Boolean requestedAnonymous
    ) {
        if (requestedAnonymous == null || requestedAnonymous == comment.isAnonymous()) {
            return new AnonymousMetadata(comment.getAnonId(), comment.getAnonymousOrder());
        }
        if (!requestedAnonymous) return new AnonymousMetadata(null, null);
        findAppNoticeForUpdateOrThrow(comment.getAppNotice().getId());
        return resolveAnonymousMetadata(comment.getAppNotice().getId(), memberId, true);
    }

    private ActionFields normalizeAction(String actionUrl, String actionLabel) {
        String url = trimToNull(actionUrl);
        String label = trimToNull(actionLabel);
        if (url == null) {
            if (label != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "actionLabel은 actionUrl과 함께 사용할 수 있습니다.");
            }
            return new ActionFields(null, null);
        }
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "actionUrl은 유효한 HTTPS URL이어야 합니다.");
            }
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "actionUrl은 유효한 HTTPS URL이어야 합니다.");
        }
        return new ActionFields(url, label);
    }

    private void requireCommentAuthor(AppNoticeComment comment, String memberId) {
        if (!comment.isAuthor(memberId)) {
            throw new BusinessException(ErrorCode.NOT_APP_NOTICE_COMMENT_AUTHOR);
        }
    }

    private AppNoticeComment findCommentForAggregateWriteOrThrow(String commentId) {
        String appNoticeId = appNoticeCommentRepository.findAppNoticeIdById(commentId)
                .orElseThrow(AppNoticeCommentNotFoundException::new);
        findAppNoticeForUpdateOrThrow(appNoticeId);
        AppNoticeComment comment = appNoticeCommentRepository.findByIdForUpdate(commentId)
                .orElseThrow(AppNoticeCommentNotFoundException::new);
        if (comment.isDeleted()) throw new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED);
        return comment;
    }

    private AppNotice findPublishedNoticeOrThrow(String appNoticeId) {
        return appNoticeRepository.findPublishedById(appNoticeId, LocalDateTime.now())
                .orElseThrow(AppNoticeNotFoundException::new);
    }

    private AppNotice findPublishedNoticeForUpdateOrThrow(String appNoticeId) {
        AppNotice appNotice = findAppNoticeForUpdateOrThrow(appNoticeId);
        if (appNotice.getPublishedAt().isAfter(LocalDateTime.now())) throw new AppNoticeNotFoundException();
        return appNotice;
    }

    private AppNotice findAppNoticeForUpdateOrThrow(String appNoticeId) {
        return appNoticeRepository.findByIdForUpdate(appNoticeId).orElseThrow(AppNoticeNotFoundException::new);
    }

    private Member findMemberForUpdateOrThrow(String memberId) {
        return memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private String resolveDisplayName(Member member) {
        if (StringUtils.hasText(member.getNickname())) return member.getNickname().trim();
        if (StringUtils.hasText(member.getRealname())) return member.getRealname().trim();
        return member.getId();
    }

    private AppNoticeReadResponse toReadResponse(AppNoticeReadStatus status) {
        return new AppNoticeReadResponse(status.getId().getAppNoticeId(), true, status.getReadAt());
    }

    private AppNoticeReadResponse createReadStatus(String memberId, AppNotice appNotice) {
        LocalDateTime readAt = LocalDateTime.now();
        try {
            return toReadResponse(appNoticeReadStatusRepository.saveAndFlush(
                    AppNoticeReadStatus.create(appNotice, memberId, readAt)));
        } catch (DataIntegrityViolationException e) {
            return appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId(memberId, appNotice.getId())
                    .map(this::toReadResponse).orElseThrow(() -> e);
        }
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) return List.of();
        return imageUrls.stream().map(this::trimToNull).filter(value -> value != null).toList();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ActionFields(String url, String label) {}
    private record AnonymousMetadata(String anonId, Integer anonymousOrder) {}
    private record CurrentAuthorProfile(String photoUrl, boolean admin) {}
    private record AuthorView(String id, String name, String photoUrl, boolean admin) {}
}
