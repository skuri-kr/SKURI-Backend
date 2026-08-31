package com.skuri.skuri_backend.domain.notice.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.common.util.AnonymousCommentIdGenerator;
import com.skuri.skuri_backend.domain.contentblock.service.ContentBlockQueryService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberWithdrawalSanitizer;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notice.dto.request.CreateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.request.UpdateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeBookmarkResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeBookmarkSummaryResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeDetailResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeReadResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeSummaryResponse;
import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.entity.NoticeBookmark;
import com.skuri.skuri_backend.domain.notice.entity.NoticeCategory;
import com.skuri.skuri_backend.domain.notice.entity.NoticeComment;
import com.skuri.skuri_backend.domain.notice.entity.NoticeCommentLike;
import com.skuri.skuri_backend.domain.notice.entity.NoticeLike;
import com.skuri.skuri_backend.domain.notice.entity.NoticeReadStatus;
import com.skuri.skuri_backend.domain.notice.repository.NoticeBookmarkRepository;
import com.skuri.skuri_backend.domain.notice.exception.NoticeCommentNotFoundException;
import com.skuri.skuri_backend.domain.notice.exception.NoticeNotFoundException;
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentLikeRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeLikeRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeReadStatusRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeSummaryProjection;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
public class NoticeService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final NoticeRepository noticeRepository;
    private final NoticeCommentRepository noticeCommentRepository;
    private final NoticeCommentLikeRepository noticeCommentLikeRepository;
    private final NoticeReadStatusRepository noticeReadStatusRepository;
    private final NoticeLikeRepository noticeLikeRepository;
    private final NoticeBookmarkRepository noticeBookmarkRepository;
    private final MemberRepository memberRepository;
    private final ContentBlockQueryService contentBlockQueryService;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> getNotices(
            String memberId,
            String category,
            String search,
            Integer page,
            Integer size
    ) {
        String resolvedCategory = resolveCategory(category);
        Pageable pageable = resolvePageable(page, size);
        Page<NoticeSummaryProjection> noticePage = noticeRepository.searchSummaries(resolvedCategory, trimToNull(search), pageable);
        List<String> noticeIds = noticePage.getContent().stream().map(NoticeSummaryProjection::id).toList();
        boolean hasMemberId = StringUtils.hasText(memberId);
        Set<String> readNoticeIds = !hasMemberId || noticeIds.isEmpty()
                ? Set.of()
                : Set.copyOf(noticeReadStatusRepository.findReadNoticeIds(memberId, noticeIds));
        Set<String> likedNoticeIds = !hasMemberId || noticeIds.isEmpty()
                ? Set.of()
                : Set.copyOf(noticeLikeRepository.findLikedNoticeIds(memberId, noticeIds));
        Set<String> bookmarkedNoticeIds = !hasMemberId || noticeIds.isEmpty()
                ? Set.of()
                : Set.copyOf(noticeBookmarkRepository.findBookmarkedNoticeIds(memberId, noticeIds));
        Set<String> commentedNoticeIds = !hasMemberId || noticeIds.isEmpty()
                ? Set.of()
                : Set.copyOf(noticeCommentRepository.findCommentedNoticeIds(memberId, noticeIds));

        return PageResponse.from(noticePage.map(notice -> toSummaryResponse(
                notice,
                readNoticeIds.contains(notice.id()),
                likedNoticeIds.contains(notice.id()),
                bookmarkedNoticeIds.contains(notice.id()),
                commentedNoticeIds.contains(notice.id())
        )));
    }

    @Transactional
    public NoticeDetailResponse getNoticeDetail(String memberId, String noticeId) {
        int updatedRows = noticeRepository.incrementViewCount(noticeId);
        if (updatedRows == 0) {
            throw new NoticeNotFoundException();
        }

        Notice notice = findNoticeOrThrow(noticeId);
        boolean isRead = noticeReadStatusRepository.existsById_UserIdAndId_NoticeIdAndReadTrue(memberId, noticeId);
        boolean isLiked = noticeLikeRepository.existsById_UserIdAndId_NoticeId(memberId, noticeId);
        boolean isBookmarked = noticeBookmarkRepository.existsById_UserIdAndId_NoticeId(memberId, noticeId);
        return toDetailResponse(notice, isRead, isLiked, isBookmarked);
    }

    @Transactional
    public NoticeReadResponse markRead(String memberId, String noticeId) {
        Notice notice = findNoticeOrThrow(noticeId);
        LocalDateTime readAt = LocalDateTime.now();
        NoticeReadStatus status = noticeReadStatusRepository.findById_UserIdAndId_NoticeId(memberId, noticeId)
                .orElseGet(() -> NoticeReadStatus.create(notice, memberId, readAt));
        status.markRead(readAt);
        noticeReadStatusRepository.save(status);
        return new NoticeReadResponse(noticeId, true, status.getReadAt());
    }

    @Transactional(readOnly = true)
    public List<NoticeCommentResponse> getComments(String memberId, String noticeId) {
        findNoticeOrThrow(noticeId);
        List<NoticeComment> comments = noticeCommentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);
        Set<String> likedCommentIds = resolveLikedCommentIds(memberId, comments);
        return flattenComments(comments, memberId, likedCommentIds);
    }

    @Transactional
    public NoticeCommentResponse createComment(String memberId, String noticeId, CreateNoticeCommentRequest request) {
        Notice notice = findNoticeForUpdateOrThrow(noticeId);
        Member author = findMemberOrThrow(memberId);

        NoticeComment parent = null;
        if (request.parentId() != null) {
            parent = noticeCommentRepository.findByIdAndNoticeId(request.parentId(), noticeId)
                    .orElseThrow(NoticeCommentNotFoundException::new);
        }

        AnonymousMetadata anonymousMetadata = resolveAnonymousMetadata(noticeId, memberId, request.isAnonymous());
        NoticeComment comment = NoticeComment.create(
                notice,
                memberId,
                resolveDisplayName(author),
                request.content().trim(),
                request.isAnonymous(),
                anonymousMetadata.anonId,
                anonymousMetadata.anonymousOrder,
                parent
        );
        NoticeComment saved = noticeCommentRepository.save(comment);
        notice.increaseCommentCount(1);
        eventPublisher.publish(new NotificationDomainEvent.NoticeCommentCreated(saved.getId()));

        return toCommentResponse(saved, memberId, resolveDepth(saved), false);
    }

    @Transactional
    public NoticeCommentResponse updateComment(String memberId, String commentId, UpdateNoticeCommentRequest request) {
        NoticeComment comment = findCommentForWriteOrThrow(commentId);
        requireCommentAuthor(comment, memberId);
        boolean targetAnonymous = request.isAnonymous() != null ? request.isAnonymous() : comment.isAnonymous();
        AnonymousMetadata anonymousMetadata = resolveUpdatedAnonymousMetadata(comment, memberId, request.isAnonymous());
        comment.update(
                request.content().trim(),
                targetAnonymous,
                anonymousMetadata.anonId,
                anonymousMetadata.anonymousOrder
        );
        return toCommentResponse(comment, memberId, resolveDepth(comment), resolveCommentIsLiked(memberId, comment.getId()));
    }

    @Transactional
    public NoticeCommentLikeResponse likeComment(String memberId, String commentId) {
        NoticeComment comment = findCommentForWriteOrThrow(commentId);
        if (noticeCommentLikeRepository.existsById_UserIdAndId_CommentId(memberId, commentId)) {
            return new NoticeCommentLikeResponse(commentId, true, comment.getLikeCount());
        }

        noticeCommentLikeRepository.save(NoticeCommentLike.create(comment, memberId));
        comment.increaseLikeCount(1);
        return new NoticeCommentLikeResponse(commentId, true, comment.getLikeCount());
    }

    @Transactional
    public NoticeCommentLikeResponse unlikeComment(String memberId, String commentId) {
        NoticeComment comment = findCommentForWriteOrThrow(commentId);
        noticeCommentLikeRepository.findById_UserIdAndId_CommentId(memberId, commentId)
                .ifPresent(commentLike -> {
                    noticeCommentLikeRepository.delete(commentLike);
                    comment.increaseLikeCount(-1);
                });
        return new NoticeCommentLikeResponse(commentId, false, comment.getLikeCount());
    }

    @Transactional
    public void deleteComment(String memberId, String commentId) {
        NoticeComment comment = findCommentForWriteOrThrow(commentId);
        requireCommentAuthor(comment, memberId);
        Notice notice = findNoticeForUpdateOrThrow(comment.getNotice().getId());
        comment.softDelete();
        notice.increaseCommentCount(-1);
    }

    @Transactional
    public NoticeLikeResponse likeNotice(String memberId, String noticeId) {
        Notice notice = findNoticeForUpdateOrThrow(noticeId);
        if (noticeLikeRepository.existsById_UserIdAndId_NoticeId(memberId, noticeId)) {
            return new NoticeLikeResponse(true, notice.getLikeCount());
        }
        noticeLikeRepository.save(NoticeLike.create(notice, memberId));
        notice.increaseLikeCount(1);
        return new NoticeLikeResponse(true, notice.getLikeCount());
    }

    @Transactional
    public NoticeLikeResponse unlikeNotice(String memberId, String noticeId) {
        Notice notice = findNoticeForUpdateOrThrow(noticeId);
        noticeLikeRepository.findById_UserIdAndId_NoticeId(memberId, noticeId)
                .ifPresent(like -> {
                    noticeLikeRepository.delete(like);
                    notice.increaseLikeCount(-1);
                });
        return new NoticeLikeResponse(false, notice.getLikeCount());
    }

    @Transactional
    public NoticeBookmarkResponse bookmarkNotice(String memberId, String noticeId) {
        Notice notice = findNoticeForUpdateOrThrow(noticeId);
        if (noticeBookmarkRepository.existsById_UserIdAndId_NoticeId(memberId, noticeId)) {
            return new NoticeBookmarkResponse(true, notice.getBookmarkCount());
        }
        noticeBookmarkRepository.save(NoticeBookmark.create(notice, memberId));
        notice.increaseBookmarkCount(1);
        return new NoticeBookmarkResponse(true, notice.getBookmarkCount());
    }

    @Transactional
    public NoticeBookmarkResponse unbookmarkNotice(String memberId, String noticeId) {
        Notice notice = findNoticeForUpdateOrThrow(noticeId);
        noticeBookmarkRepository.findById_UserIdAndId_NoticeId(memberId, noticeId)
                .ifPresent(bookmark -> {
                    noticeBookmarkRepository.delete(bookmark);
                    notice.increaseBookmarkCount(-1);
                });
        return new NoticeBookmarkResponse(false, notice.getBookmarkCount());
    }

    @Transactional(readOnly = true)
    public PageResponse<NoticeBookmarkSummaryResponse> getMyBookmarks(String memberId, Integer page, Integer size) {
        Pageable pageable = resolvePageable(page, size);
        Page<NoticeBookmarkSummaryResponse> bookmarkPage = noticeBookmarkRepository.findBookmarkedNotices(memberId, pageable)
                .map(this::toBookmarkSummaryResponse);
        return PageResponse.from(bookmarkPage);
    }

    @Transactional
    public void handleMemberWithdrawal(String memberId) {
        noticeCommentRepository.findByUserId(memberId)
                .forEach(NoticeComment::anonymizeAuthor);

        List<NoticeCommentLike> commentLikes = noticeCommentLikeRepository.findById_UserId(memberId);
        if (!commentLikes.isEmpty()) {
            Map<String, Integer> likeCountsByCommentId = new LinkedHashMap<>();
            commentLikes.forEach(commentLike -> likeCountsByCommentId.merge(commentLike.getId().getCommentId(), 1, Integer::sum));
            noticeCommentRepository.findAllById(likeCountsByCommentId.keySet()).forEach(comment ->
                    comment.increaseLikeCount(-likeCountsByCommentId.getOrDefault(comment.getId(), 0))
            );
            noticeCommentLikeRepository.deleteAllInBatch(commentLikes);
        }

        List<NoticeLike> likes = noticeLikeRepository.findById_UserId(memberId);
        if (!likes.isEmpty()) {
            Map<String, Integer> likeCounts = new LinkedHashMap<>();
            likes.forEach(like -> likeCounts.merge(like.getId().getNoticeId(), 1, Integer::sum));
            noticeRepository.findAllById(likeCounts.keySet()).forEach(notice ->
                    notice.increaseLikeCount(-likeCounts.getOrDefault(notice.getId(), 0))
            );
            noticeLikeRepository.deleteAllInBatch(likes);
        }

        List<NoticeBookmark> bookmarks = noticeBookmarkRepository.findById_UserId(memberId);
        if (!bookmarks.isEmpty()) {
            Map<String, Integer> bookmarkCounts = new LinkedHashMap<>();
            bookmarks.forEach(bookmark -> bookmarkCounts.merge(bookmark.getId().getNoticeId(), 1, Integer::sum));
            noticeRepository.findAllById(bookmarkCounts.keySet()).forEach(notice ->
                    notice.increaseBookmarkCount(-bookmarkCounts.getOrDefault(notice.getId(), 0))
            );
            noticeBookmarkRepository.deleteAllInBatch(bookmarks);
        }

        noticeReadStatusRepository.deleteById_UserId(memberId);
    }

    private NoticeSummaryResponse toSummaryResponse(
            NoticeSummaryProjection notice,
            boolean isRead,
            boolean isLiked,
            boolean isBookmarked,
            boolean isCommentedByMe
    ) {
        return new NoticeSummaryResponse(
                notice.id(),
                notice.title(),
                notice.rssPreview(),
                notice.category(),
                notice.department(),
                notice.author(),
                notice.postedAt(),
                notice.viewCount(),
                notice.likeCount(),
                notice.commentCount(),
                notice.bookmarkCount(),
                isRead,
                isLiked,
                isBookmarked,
                isCommentedByMe,
                notice.thumbnailUrl()
        );
    }

    private NoticeDetailResponse toDetailResponse(Notice notice, boolean isRead, boolean isLiked, boolean isBookmarked) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getRssPreview(),
                notice.getBodyHtml(),
                notice.getLink(),
                notice.getCategory(),
                notice.getDepartment(),
                notice.getAuthor(),
                notice.getSource(),
                notice.getPostedAt(),
                notice.getViewCount(),
                notice.getLikeCount(),
                notice.getCommentCount(),
                notice.getBookmarkCount(),
                List.copyOf(notice.getAttachments()),
                isRead,
                isLiked,
                isBookmarked
        );
    }

    private NoticeBookmarkSummaryResponse toBookmarkSummaryResponse(Notice notice) {
        return new NoticeBookmarkSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getRssPreview(),
                notice.getCategory(),
                notice.getPostedAt()
        );
    }

    private List<NoticeCommentResponse> flattenComments(
            List<NoticeComment> comments,
            String memberId,
            Set<String> likedCommentIds
    ) {
        Set<String> blockedAuthorIds = contentBlockQueryService.findBlockedMemberIds(
                memberId,
                comments.stream().map(NoticeComment::getUserId).toList()
        );
        Map<String, CurrentAuthorProfile> currentAuthorsById = resolveCurrentAuthors(
                comments.stream()
                        .map(NoticeComment::getUserId)
                        .filter(authorId -> !blockedAuthorIds.contains(authorId))
                        .toList()
        );
        Map<String, List<NoticeComment>> childrenByParent = new LinkedHashMap<>();
        List<NoticeComment> roots = new ArrayList<>();

        for (NoticeComment comment : comments) {
            if (comment.hasParent()) {
                childrenByParent.computeIfAbsent(comment.getParent().getId(), key -> new ArrayList<>()).add(comment);
            } else {
                roots.add(comment);
            }
        }

        List<NoticeCommentResponse> flattened = new ArrayList<>();
        for (NoticeComment root : roots) {
            appendCommentTree(
                    flattened,
                    root,
                    0,
                    memberId,
                    likedCommentIds,
                    childrenByParent,
                    currentAuthorsById,
                    blockedAuthorIds
            );
        }
        return flattened;
    }

    private void appendCommentTree(
            List<NoticeCommentResponse> flattened,
            NoticeComment comment,
            int depth,
            String memberId,
            Set<String> likedCommentIds,
            Map<String, List<NoticeComment>> childrenByParent,
            Map<String, CurrentAuthorProfile> currentAuthorsById,
            Set<String> blockedAuthorIds
    ) {
        flattened.add(toCommentResponse(
                comment,
                memberId,
                depth,
                likedCommentIds.contains(comment.getId()),
                currentAuthorsById,
                blockedAuthorIds.contains(comment.getUserId())
        ));
        for (NoticeComment child : childrenByParent.getOrDefault(comment.getId(), List.of())) {
            appendCommentTree(
                    flattened,
                    child,
                    depth + 1,
                    memberId,
                    likedCommentIds,
                    childrenByParent,
                    currentAuthorsById,
                    blockedAuthorIds
            );
        }
    }

    private int resolveDepth(NoticeComment comment) {
        int depth = 0;
        NoticeComment current = comment;
        while (current.hasParent()) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private NoticeCommentResponse toCommentResponse(NoticeComment comment, String memberId, int depth, boolean isLiked) {
        return toCommentResponse(
                comment,
                memberId,
                depth,
                isLiked,
                resolveCurrentAuthors(Collections.singletonList(comment.getUserId())),
                false
        );
    }

    private NoticeCommentResponse toCommentResponse(
            NoticeComment comment,
            String memberId,
            int depth,
            boolean isLiked,
            Map<String, CurrentAuthorProfile> currentAuthorsById,
            boolean blocked
    ) {
        boolean deleted = comment.isDeleted() || blocked;
        AuthorView authorView = resolveAuthorView(
                comment.isAnonymous(),
                deleted,
                comment.getUserId(),
                comment.getUserDisplayName(),
                currentAuthorsById.get(comment.getUserId()),
                comment.getAnonymousOrder()
        );
        return new NoticeCommentResponse(
                comment.getId(),
                comment.hasParent() ? comment.getParent().getId() : null,
                depth,
                blocked ? NoticeComment.BLOCKED_PLACEHOLDER : comment.getContent(),
                authorView.authorId,
                authorView.authorName,
                authorView.authorProfileImage,
                authorView.authorAdmin,
                !deleted && comment.isAnonymous(),
                deleted ? null : comment.getAnonymousOrder(),
                !deleted && comment.isAuthor(memberId),
                comment.getLikeCount(),
                !deleted && isLiked,
                deleted,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private Set<String> resolveLikedCommentIds(String memberId, List<NoticeComment> comments) {
        if (comments.isEmpty() || !StringUtils.hasText(memberId)) {
            return Set.of();
        }

        List<String> commentIds = comments.stream()
                .map(NoticeComment::getId)
                .toList();
        return Set.copyOf(noticeCommentLikeRepository.findLikedCommentIds(memberId, commentIds));
    }

    private boolean resolveCommentIsLiked(String memberId, String commentId) {
        if (!StringUtils.hasText(memberId)) {
            return false;
        }
        return noticeCommentLikeRepository.existsById_UserIdAndId_CommentId(memberId, commentId);
    }

    private Map<String, CurrentAuthorProfile> resolveCurrentAuthors(List<String> authorIds) {
        Set<String> activeAuthorIds = authorIds.stream()
                .filter(authorId -> authorId != null && !authorId.isBlank())
                .collect(Collectors.toSet());
        if (activeAuthorIds.isEmpty()) {
            return Map.of();
        }

        Map<String, CurrentAuthorProfile> currentAuthorsById = new HashMap<>();
        memberRepository.findAllActiveByIdIn(activeAuthorIds).forEach(member ->
                currentAuthorsById.put(
                        member.getId(),
                        new CurrentAuthorProfile(member.getPhotoUrl(), member.isAdmin())
                )
        );
        return currentAuthorsById;
    }

    private AuthorView resolveAuthorView(
            boolean anonymous,
            boolean deleted,
            String authorId,
            String authorName,
            CurrentAuthorProfile currentAuthor,
            Integer anonymousOrder
    ) {
        if (deleted) {
            return new AuthorView(null, null, null, false);
        }
        if (MemberWithdrawalSanitizer.isWithdrawnAuthorId(authorId)) {
            return new AuthorView(null, authorName, null, false);
        }
        if (!anonymous) {
            return new AuthorView(
                    authorId,
                    authorName,
                    currentAuthor == null ? null : currentAuthor.photoUrl(),
                    currentAuthor != null && currentAuthor.isAdmin()
            );
        }
        String displayName = anonymousOrder == null ? "익명" : "익명" + anonymousOrder;
        return new AuthorView(null, displayName, null, false);
    }

    private AnonymousMetadata resolveAnonymousMetadata(String noticeId, String userId, boolean anonymous) {
        if (!anonymous) {
            return new AnonymousMetadata(null, null);
        }

        String generatedAnonId = AnonymousCommentIdGenerator.generate(noticeId, userId);
        NoticeComment existingAnonymousComment = noticeCommentRepository
                .findFirstByNotice_IdAndUserIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(noticeId, userId)
                .orElse(null);
        if (existingAnonymousComment != null) {
            String anonId = StringUtils.hasText(existingAnonymousComment.getAnonId())
                    ? existingAnonymousComment.getAnonId()
                    : generatedAnonId;
            return new AnonymousMetadata(anonId, existingAnonymousComment.getAnonymousOrder());
        }

        int nextOrder = noticeCommentRepository.findMaxAnonymousOrderByNoticeId(noticeId) + 1;
        return new AnonymousMetadata(generatedAnonId, nextOrder);
    }

    private AnonymousMetadata resolveUpdatedAnonymousMetadata(
            NoticeComment comment,
            String memberId,
            Boolean requestedAnonymous
    ) {
        if (requestedAnonymous == null || requestedAnonymous == comment.isAnonymous()) {
            return new AnonymousMetadata(comment.getAnonId(), comment.getAnonymousOrder());
        }
        if (!requestedAnonymous) {
            return new AnonymousMetadata(null, null);
        }

        findNoticeForUpdateOrThrow(comment.getNotice().getId());
        return resolveAnonymousMetadata(comment.getNotice().getId(), memberId, true);
    }

    private void requireCommentAuthor(NoticeComment comment, String memberId) {
        if (!comment.isAuthor(memberId)) {
            throw new BusinessException(ErrorCode.NOT_NOTICE_COMMENT_AUTHOR);
        }
    }

    private NoticeComment findCommentForWriteOrThrow(String commentId) {
        NoticeComment comment = noticeCommentRepository.findByIdForUpdate(commentId)
                .orElseThrow(NoticeCommentNotFoundException::new);
        if (comment.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED);
        }
        return comment;
    }

    private Notice findNoticeOrThrow(String noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
    }

    private Notice findNoticeForUpdateOrThrow(String noticeId) {
        return noticeRepository.findByIdForUpdate(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
    }

    private Member findMemberOrThrow(String memberId) {
        return memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Pageable resolvePageable(Integer page, Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (resolvedPage < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 0 이상이어야 합니다.");
        }
        if (resolvedSize < 1 || resolvedSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1 이상 100 이하여야 합니다.");
        }
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "postedAt", "createdAt"));
    }

    private String resolveCategory(String category) {
        String normalized = trimToNull(category);
        if (normalized == null) {
            return null;
        }
        try {
            return NoticeCategory.fromLabel(normalized).label();
        } catch (IllegalArgumentException e) {
            String supported = java.util.Arrays.stream(NoticeCategory.values())
                    .map(NoticeCategory::label)
                    .collect(Collectors.joining(", "));
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "category는 다음 값만 허용됩니다: " + supported);
        }
    }

    private String resolveDisplayName(Member member) {
        if (StringUtils.hasText(member.getNickname())) {
            return member.getNickname().trim();
        }
        if (StringUtils.hasText(member.getRealname())) {
            return member.getRealname().trim();
        }
        return member.getId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record AnonymousMetadata(String anonId, Integer anonymousOrder) {
    }

    private record CurrentAuthorProfile(String photoUrl, boolean isAdmin) {
    }

    private record AuthorView(String authorId, String authorName, String authorProfileImage, boolean authorAdmin) {
    }
}
