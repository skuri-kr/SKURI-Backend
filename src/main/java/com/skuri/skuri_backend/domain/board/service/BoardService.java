package com.skuri.skuri_backend.domain.board.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.common.util.AnonymousCommentIdGenerator;
import com.skuri.skuri_backend.domain.board.dto.request.CreateCommentRequest;
import com.skuri.skuri_backend.domain.board.dto.request.CreatePostImageRequest;
import com.skuri.skuri_backend.domain.board.dto.request.CreatePostRequest;
import com.skuri.skuri_backend.domain.board.dto.request.UpdateCommentRequest;
import com.skuri.skuri_backend.domain.board.dto.request.UpdatePostRequest;
import com.skuri.skuri_backend.domain.board.dto.response.CommentLikeResponse;
import com.skuri.skuri_backend.domain.board.dto.response.CommentResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostBookmarkResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostImageResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostLikeResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostSummaryResponse;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.CommentLike;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.entity.PostImage;
import com.skuri.skuri_backend.domain.board.repository.CommentLikeRepository;
import com.skuri.skuri_backend.domain.board.repository.PostImageRepository;
import com.skuri.skuri_backend.domain.board.entity.PostInteraction;
import com.skuri.skuri_backend.domain.board.exception.CommentNotFoundException;
import com.skuri.skuri_backend.domain.board.exception.PostNotFoundException;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostInteractionRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.board.repository.PostSummaryProjection;
import com.skuri.skuri_backend.domain.board.repository.PostThumbnailProjection;
import com.skuri.skuri_backend.domain.contentblock.service.ContentBlockQueryService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberWithdrawalSanitizer;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostInteractionRepository postInteractionRepository;
    private final MemberRepository memberRepository;
    private final ContentBlockQueryService contentBlockQueryService;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional
    public PostDetailResponse createPost(String memberId, CreatePostRequest request) {
        validatePostImages(request.images());

        Member author = findMemberOrThrow(memberId);
        Post post = Post.create(
                request.title().trim(),
                request.content().trim(),
                author.getId(),
                author.getNickname(),
                author.getPhotoUrl(),
                request.isAnonymous(),
                request.category()
        );

        replacePostImages(post, request.images());

        Post saved = postRepository.save(post);
        saved.assignAnonId();

        return toPostDetailResponse(saved, memberId, false, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getPosts(
            String memberId,
            PostCategory category,
            String search,
            String authorId,
            String sort,
            Integer page,
            Integer size
    ) {
        Pageable pageable = resolvePageable(page, size, sort);
        Page<PostSummaryProjection> postPage = postRepository.searchSummaries(
                category,
                trimToNull(search),
                trimToNull(authorId),
                memberId,
                pageable
        );
        return toPostSummaryPage(postPage, memberId);
    }

    @Transactional
    public PostDetailResponse getPostDetail(String memberId, String postId) {
        int updatedRows = postRepository.incrementViewCountForViewer(postId, memberId);
        if (updatedRows == 0) {
            throw new PostNotFoundException();
        }

        Post post = postRepository.findActiveDetailById(postId)
                .orElseThrow(PostNotFoundException::new);

        boolean isLiked = postInteractionRepository.existsById_UserIdAndId_PostIdAndLikedTrue(memberId, postId);
        boolean isBookmarked = postInteractionRepository.existsById_UserIdAndId_PostIdAndBookmarkedTrue(memberId, postId);

        return toPostDetailResponse(post, memberId, isLiked, isBookmarked);
    }

    @Transactional
    public PostDetailResponse updatePost(String memberId, String postId, UpdatePostRequest request) {
        String title = trimToNull(request.title());
        String content = trimToNull(request.content());
        boolean updatesImages = request.images() != null;
        boolean updatesAnonymous = request.isAnonymous() != null;
        if (title == null && content == null && request.category() == null && !updatesAnonymous && !updatesImages) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 최소 1개 이상 입력해야 합니다.");
        }
        validatePostImages(request.images());

        Post post = findActivePostOrThrow(postId);
        requirePostAuthor(post, memberId);

        post.update(
                title,
                content,
                request.category(),
                request.isAnonymous()
        );
        if (updatesImages) {
            replacePostImages(post, request.images());
        }

        boolean isLiked = postInteractionRepository.existsById_UserIdAndId_PostIdAndLikedTrue(memberId, postId);
        boolean isBookmarked = postInteractionRepository.existsById_UserIdAndId_PostIdAndBookmarkedTrue(memberId, postId);

        return toPostDetailResponse(post, memberId, isLiked, isBookmarked);
    }

    @Transactional
    public void deletePost(String memberId, String postId) {
        Post post = findActivePostOrThrow(postId);
        requirePostAuthor(post, memberId);
        post.markDeleted();
    }

    @Transactional
    public PostLikeResponse likePost(String memberId, String postId) {
        Post post = findActivePostForUpdateOrThrow(postId);
        PostInteraction interaction = getOrCreateInteraction(post, memberId);

        if (interaction.like()) {
            post.increaseLikeCount(1);
            eventPublisher.publish(new NotificationDomainEvent.BoardPostLiked(post.getId(), memberId));
        }

        return new PostLikeResponse(interaction.isLiked(), post.getLikeCount());
    }

    @Transactional
    public PostLikeResponse unlikePost(String memberId, String postId) {
        Post post = findActivePostForUpdateOrThrow(postId);
        PostInteraction interaction = getOrCreateInteraction(post, memberId);

        if (interaction.unlike()) {
            post.increaseLikeCount(-1);
        }

        return new PostLikeResponse(interaction.isLiked(), post.getLikeCount());
    }

    @Transactional
    public PostBookmarkResponse bookmarkPost(String memberId, String postId) {
        Post post = findActivePostForUpdateOrThrow(postId);
        PostInteraction interaction = getOrCreateInteraction(post, memberId);

        if (interaction.bookmark()) {
            post.increaseBookmarkCount(1);
        }

        return new PostBookmarkResponse(interaction.isBookmarked(), post.getBookmarkCount());
    }

    @Transactional
    public PostBookmarkResponse unbookmarkPost(String memberId, String postId) {
        Post post = findActivePostForUpdateOrThrow(postId);
        PostInteraction interaction = getOrCreateInteraction(post, memberId);

        if (interaction.unbookmark()) {
            post.increaseBookmarkCount(-1);
        }

        return new PostBookmarkResponse(interaction.isBookmarked(), post.getBookmarkCount());
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getBookmarkedPosts(String memberId, Integer page, Integer size) {
        Pageable pageable = resolvePageable(page, size, "latest");
        Page<PostSummaryProjection> postPage = postRepository.findBookmarkedSummaries(memberId, pageable);
        return toPostSummaryPage(postPage, memberId);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String memberId, String postId) {
        Post post = findActivePostOrThrow(postId);
        if (contentBlockQueryService.isBlockedBy(memberId, post.getAuthorId())) {
            throw new PostNotFoundException();
        }
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        Set<String> likedCommentIds = resolveLikedCommentIds(memberId, comments);
        return flattenComments(comments, memberId, post.getAuthorId(), likedCommentIds);
    }

    @Transactional
    public CommentResponse createComment(String memberId, String postId, CreateCommentRequest request) {
        Post post = findActivePostForUpdateOrThrow(postId);
        Member author = findMemberOrThrow(memberId);

        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findByIdAndPostId(request.parentId(), postId)
                    .orElseThrow(CommentNotFoundException::new);
        }

        AnonymousMetadata anonymousMetadata = resolveAnonymousMetadata(postId, memberId, request.isAnonymous());
        Comment comment = Comment.create(
                post,
                request.content().trim(),
                author.getId(),
                author.getNickname(),
                author.getPhotoUrl(),
                request.isAnonymous(),
                anonymousMetadata.anonId,
                anonymousMetadata.anonymousOrder,
                parent
        );
        Comment saved = commentRepository.save(comment);

        post.increaseCommentCount(1);
        post.updateLastCommentAt(LocalDateTime.now());
        eventPublisher.publish(new NotificationDomainEvent.BoardCommentCreated(saved.getId()));

        return toCommentResponse(saved, memberId, post.getAuthorId(), resolveDepth(saved), false);
    }

    @Transactional
    public CommentResponse updateComment(String memberId, String commentId, UpdateCommentRequest request) {
        Comment comment = findCommentForWriteOrThrow(commentId);
        requireCommentAuthor(comment, memberId);
        boolean targetAnonymous = request.isAnonymous() != null ? request.isAnonymous() : comment.isAnonymous();
        AnonymousMetadata anonymousMetadata = resolveUpdatedAnonymousMetadata(comment, memberId, request.isAnonymous());
        comment.update(
                request.content().trim(),
                targetAnonymous,
                anonymousMetadata.anonId,
                anonymousMetadata.anonymousOrder
        );
        return toCommentResponse(
                comment,
                memberId,
                comment.getPost().getAuthorId(),
                resolveDepth(comment),
                resolveCommentIsLiked(memberId, comment.getId())
        );
    }

    @Transactional
    public CommentLikeResponse likeComment(String memberId, String commentId) {
        Comment comment = findCommentForWriteOrThrow(commentId);
        if (commentLikeRepository.existsById_UserIdAndId_CommentId(memberId, commentId)) {
            return new CommentLikeResponse(commentId, true, comment.getLikeCount());
        }

        commentLikeRepository.save(CommentLike.create(comment, memberId));
        comment.increaseLikeCount(1);
        return new CommentLikeResponse(commentId, true, comment.getLikeCount());
    }

    @Transactional
    public CommentLikeResponse unlikeComment(String memberId, String commentId) {
        Comment comment = findCommentForWriteOrThrow(commentId);
        commentLikeRepository.findById_UserIdAndId_CommentId(memberId, commentId)
                .ifPresent(commentLike -> {
                    commentLikeRepository.delete(commentLike);
                    comment.increaseLikeCount(-1);
                });
        return new CommentLikeResponse(commentId, false, comment.getLikeCount());
    }

    @Transactional
    public void deleteComment(String memberId, String commentId) {
        Comment comment = findCommentForWriteOrThrow(commentId);
        requireCommentAuthor(comment, memberId);
        Post post = findActivePostForUpdateOrThrow(comment.getPost().getId());
        comment.softDelete();
        post.increaseCommentCount(-1);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getMyPosts(String memberId, Integer page, Integer size) {
        Pageable pageable = resolvePageable(page, size, "latest");
        Page<PostSummaryProjection> postPage = postRepository.findActiveSummariesByAuthorId(memberId, pageable);
        return toPostSummaryPage(postPage, memberId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getMyBookmarks(String memberId, Integer page, Integer size) {
        return getBookmarkedPosts(memberId, page, size);
    }

    @Transactional
    public void handleMemberWithdrawal(String memberId) {
        postRepository.findByAuthorId(memberId).forEach(Post::anonymizeAuthor);
        commentRepository.findByAuthorId(memberId).forEach(Comment::anonymizeAuthor);

        List<CommentLike> commentLikes = commentLikeRepository.findById_UserId(memberId);
        if (!commentLikes.isEmpty()) {
            Map<String, Integer> likeCountsByCommentId = new LinkedHashMap<>();
            commentLikes.forEach(commentLike -> likeCountsByCommentId.merge(commentLike.getId().getCommentId(), 1, Integer::sum));
            commentRepository.findAllById(likeCountsByCommentId.keySet()).forEach(comment ->
                    comment.increaseLikeCount(-likeCountsByCommentId.getOrDefault(comment.getId(), 0))
            );
            commentLikeRepository.deleteAllInBatch(commentLikes);
        }

        List<PostInteraction> interactions = postInteractionRepository.findById_UserId(memberId);
        if (interactions.isEmpty()) {
            return;
        }

        Map<String, int[]> deltasByPostId = new LinkedHashMap<>();
        for (PostInteraction interaction : interactions) {
            String postId = interaction.getPost() != null ? interaction.getPost().getId() : interaction.getPostId();
            int[] delta = deltasByPostId.computeIfAbsent(postId, key -> new int[2]);
            if (interaction.isLiked()) {
                delta[0]++;
            }
            if (interaction.isBookmarked()) {
                delta[1]++;
            }
        }

        Set<String> postIds = new HashSet<>(deltasByPostId.keySet());
        postRepository.findAllById(postIds).forEach(post -> {
            int[] delta = deltasByPostId.get(post.getId());
            if (delta == null) {
                return;
            }
            if (delta[0] > 0) {
                post.increaseLikeCount(-delta[0]);
            }
            if (delta[1] > 0) {
                post.increaseBookmarkCount(-delta[1]);
            }
        });
        postInteractionRepository.deleteAllInBatch(interactions);
    }

    private Pageable resolvePageable(Integer page, Integer size, String sort) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_PAGE_SIZE : size;

        if (resolvedPage < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "page는 0 이상이어야 합니다.");
        }
        if (resolvedSize < 1 || resolvedSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1 이상 100 이하여야 합니다.");
        }

        return PageRequest.of(resolvedPage, resolvedSize, resolveSort(sort));
    }

    private Sort resolveSort(String sort) {
        String normalized = trimToNull(sort);
        if (normalized == null || "latest".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"));
        }
        if ("popular".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt"));
        }
        if ("mostCommented".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Order.desc("commentCount"), Sort.Order.desc("createdAt"));
        }
        if ("mostViewed".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("createdAt"));
        }

        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 sort 값입니다: " + normalized);
    }

    private PageResponse<PostSummaryResponse> toPostSummaryPage(Page<PostSummaryProjection> postPage, String memberId) {
        List<String> postIds = postPage.getContent().stream()
                .map(PostSummaryProjection::getId)
                .toList();
        Map<String, String> thumbnailUrlsByPostId = resolvePostSummaryThumbnailUrls(postIds);
        PostSummaryPersonalization personalization = resolvePostSummaryPersonalization(memberId, postIds);
        Map<String, CurrentAuthorProfile> currentAuthorsById = resolveCurrentAuthors(
                postPage.getContent().stream().map(PostSummaryProjection::getAuthorId).toList()
        );

        return PageResponse.from(postPage.map(post -> toPostSummaryResponse(
                post,
                thumbnailUrlsByPostId.get(post.getId()),
                personalization.likedPostIds.contains(post.getId()),
                personalization.bookmarkedPostIds.contains(post.getId()),
                personalization.commentedPostIds.contains(post.getId()),
                currentAuthorsById
        )));
    }

    private Map<String, String> resolvePostSummaryThumbnailUrls(List<String> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        return postImageRepository.findFirstThumbnailByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostThumbnailProjection::getPostId,
                        PostThumbnailProjection::getThumbnailUrl,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private PostSummaryPersonalization resolvePostSummaryPersonalization(String memberId, List<String> postIds) {
        if (postIds.isEmpty() || memberId == null || memberId.isBlank()) {
            return PostSummaryPersonalization.empty();
        }

        List<PostInteraction> interactions = postInteractionRepository.findById_UserIdAndId_PostIdIn(memberId, postIds);
        Set<String> likedPostIds = interactions.stream()
                .filter(PostInteraction::isLiked)
                .map(PostInteraction::getPostId)
                .collect(Collectors.toSet());
        Set<String> bookmarkedPostIds = interactions.stream()
                .filter(PostInteraction::isBookmarked)
                .map(PostInteraction::getPostId)
                .collect(Collectors.toSet());
        Set<String> commentedPostIds = Set.copyOf(commentRepository.findCommentedPostIds(memberId, postIds));

        return new PostSummaryPersonalization(likedPostIds, bookmarkedPostIds, commentedPostIds);
    }

    private PostSummaryResponse toPostSummaryResponse(
            PostSummaryProjection post,
            String thumbnailUrl,
            boolean isLiked,
            boolean isBookmarked,
            boolean isCommentedByMe,
            Map<String, CurrentAuthorProfile> currentAuthorsById
    ) {
        AuthorView authorView = resolveAuthorView(
                post.isAnonymous(),
                false,
                post.getAuthorId(),
                post.getAuthorName(),
                currentAuthorsById.get(post.getAuthorId()),
                null
        );

        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                toPreview(post.getContent()),
                authorView.authorId,
                authorView.authorName,
                authorView.authorProfileImage,
                authorView.authorAdmin,
                post.isAnonymous(),
                post.getCategory(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getBookmarkCount(),
                isLiked,
                isBookmarked,
                isCommentedByMe,
                post.isHasImage(),
                thumbnailUrl,
                post.isPinned(),
                post.getCreatedAt()
        );
    }

    private PostDetailResponse toPostDetailResponse(Post post, String memberId, boolean isLiked, boolean isBookmarked) {
        AuthorView authorView = resolveAuthorView(
                post.isAnonymous(),
                post.isDeleted(),
                post.getAuthorId(),
                post.getAuthorName(),
                resolveCurrentAuthor(post.getAuthorId()),
                null
        );

        List<PostImageResponse> images = post.getImages().stream()
                .map(this::toPostImageResponse)
                .toList();

        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                authorView.authorId,
                authorView.authorName,
                authorView.authorProfileImage,
                authorView.authorAdmin,
                post.isAnonymous(),
                post.getCategory(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getBookmarkCount(),
                images,
                isLiked,
                isBookmarked,
                post.isAuthor(memberId),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private PostImageResponse toPostImageResponse(PostImage image) {
        return new PostImageResponse(
                image.getUrl(),
                image.getThumbUrl(),
                image.getWidth(),
                image.getHeight(),
                image.getSize(),
                image.getMime()
        );
    }

    private void replacePostImages(Post post, List<CreatePostImageRequest> images) {
        post.clearImages();
        if (images == null) {
            return;
        }

        for (int index = 0; index < images.size(); index++) {
            CreatePostImageRequest image = images.get(index);
            post.appendImage(
                    image.url(),
                    trimToNull(image.thumbUrl()),
                    image.width(),
                    image.height(),
                    image.size(),
                    image.mime(),
                    index
            );
        }
    }

    private void validatePostImages(List<CreatePostImageRequest> images) {
        if (images == null) {
            return;
        }

        for (CreatePostImageRequest image : images) {
            if (image == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "images 항목은 null일 수 없습니다.");
            }
        }
    }

    private List<CommentResponse> flattenComments(
            List<Comment> comments,
            String memberId,
            String postAuthorId,
            Set<String> likedCommentIds
    ) {
        Set<String> blockedAuthorIds = contentBlockQueryService.findBlockedMemberIds(
                memberId,
                comments.stream().map(Comment::getAuthorId).toList()
        );
        Map<String, CurrentAuthorProfile> currentAuthorsById = resolveCurrentAuthors(
                comments.stream()
                        .map(Comment::getAuthorId)
                        .filter(authorId -> !blockedAuthorIds.contains(authorId))
                        .toList()
        );
        Map<String, List<Comment>> childrenByParent = new LinkedHashMap<>();
        List<Comment> roots = new ArrayList<>();

        for (Comment comment : comments) {
            if (comment.hasParent()) {
                childrenByParent.computeIfAbsent(comment.getParent().getId(), key -> new ArrayList<>()).add(comment);
            } else {
                roots.add(comment);
            }
        }

        List<CommentResponse> flattened = new ArrayList<>();
        for (Comment root : roots) {
            appendCommentTree(
                    flattened,
                    root,
                    0,
                    memberId,
                    postAuthorId,
                    likedCommentIds,
                    childrenByParent,
                    currentAuthorsById,
                    blockedAuthorIds
            );
        }
        return flattened;
    }

    private void appendCommentTree(
            List<CommentResponse> flattened,
            Comment comment,
            int depth,
            String memberId,
            String postAuthorId,
            Set<String> likedCommentIds,
            Map<String, List<Comment>> childrenByParent,
            Map<String, CurrentAuthorProfile> currentAuthorsById,
            Set<String> blockedAuthorIds
    ) {
        flattened.add(toCommentResponse(
                comment,
                memberId,
                postAuthorId,
                depth,
                likedCommentIds.contains(comment.getId()),
                currentAuthorsById,
                blockedAuthorIds.contains(comment.getAuthorId())
        ));
        for (Comment child : childrenByParent.getOrDefault(comment.getId(), List.of())) {
            appendCommentTree(
                    flattened,
                    child,
                    depth + 1,
                    memberId,
                    postAuthorId,
                    likedCommentIds,
                    childrenByParent,
                    currentAuthorsById,
                    blockedAuthorIds
            );
        }
    }

    private int resolveDepth(Comment comment) {
        int depth = 0;
        Comment current = comment;
        while (current.hasParent()) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private CommentResponse toCommentResponse(
            Comment comment,
            String memberId,
            String postAuthorId,
            int depth,
            boolean isLiked
    ) {
        return toCommentResponse(
                comment,
                memberId,
                postAuthorId,
                depth,
                isLiked,
                resolveCurrentAuthors(Collections.singletonList(comment.getAuthorId())),
                false
        );
    }

    private CommentResponse toCommentResponse(
            Comment comment,
            String memberId,
            String postAuthorId,
            int depth,
            boolean isLiked,
            Map<String, CurrentAuthorProfile> currentAuthorsById,
            boolean blocked
    ) {
        boolean masked = comment.isDeleted() || comment.isHidden() || blocked;
        AuthorView authorView = resolveAuthorView(
                comment.isAnonymous(),
                masked,
                comment.getAuthorId(),
                comment.getAuthorName(),
                currentAuthorsById.get(comment.getAuthorId()),
                comment.getAnonymousOrder()
        );

        return new CommentResponse(
                comment.getId(),
                comment.hasParent() ? comment.getParent().getId() : null,
                depth,
                blocked
                        ? Comment.BLOCKED_PLACEHOLDER
                        : masked && comment.isHidden() ? Comment.HIDDEN_PLACEHOLDER : comment.getContent(),
                authorView.authorId,
                authorView.authorName,
                authorView.authorProfileImage,
                authorView.authorAdmin,
                !masked && comment.isAnonymous(),
                masked ? null : comment.getAnonymousOrder(),
                !masked && comment.isAuthor(memberId),
                !masked && comment.isAuthor(postAuthorId),
                comment.getLikeCount(),
                !masked && isLiked,
                masked,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private Set<String> resolveLikedCommentIds(String memberId, List<Comment> comments) {
        if (comments.isEmpty() || memberId == null || memberId.isBlank()) {
            return Set.of();
        }

        List<String> commentIds = comments.stream()
                .map(Comment::getId)
                .toList();
        return Set.copyOf(commentLikeRepository.findLikedCommentIds(memberId, commentIds));
    }

    private boolean resolveCommentIsLiked(String memberId, String commentId) {
        if (memberId == null || memberId.isBlank()) {
            return false;
        }
        return commentLikeRepository.existsById_UserIdAndId_CommentId(memberId, commentId);
    }

    private CurrentAuthorProfile resolveCurrentAuthor(String authorId) {
        if (authorId == null || authorId.isBlank()) {
            return null;
        }
        return resolveCurrentAuthors(List.of(authorId)).get(authorId);
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

    private String toPreview(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= 120) {
            return content;
        }
        return content.substring(0, 117) + "...";
    }

    private Post findActivePostOrThrow(String postId) {
        return postRepository.findByIdAndDeletedFalseAndHiddenFalse(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Post findActivePostForUpdateOrThrow(String postId) {
        return postRepository.findActiveByIdForUpdate(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Post findNotDeletedPostForUpdateOrThrow(String postId) {
        return postRepository.findByIdAndDeletedFalseForUpdate(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    private Comment findCommentForWriteOrThrow(String commentId) {
        Comment comment = commentRepository.findByIdForUpdate(commentId)
                .orElseThrow(CommentNotFoundException::new);
        if (comment.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED);
        }
        return comment;
    }

    private Member findMemberOrThrow(String memberId) {
        return memberRepository.findActiveById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private void requirePostAuthor(Post post, String actorId) {
        if (!post.isAuthor(actorId)) {
            throw new BusinessException(ErrorCode.NOT_POST_AUTHOR);
        }
    }

    private void requireCommentAuthor(Comment comment, String actorId) {
        if (!comment.isAuthor(actorId)) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
    }

    private PostInteraction getOrCreateInteraction(Post post, String userId) {
        return postInteractionRepository.findById_UserIdAndId_PostId(userId, post.getId())
                .orElseGet(() -> postInteractionRepository.save(PostInteraction.create(post, userId)));
    }

    private AnonymousMetadata resolveAnonymousMetadata(String postId, String userId, boolean anonymous) {
        if (!anonymous) {
            return new AnonymousMetadata(null, null);
        }

        String generatedAnonId = AnonymousCommentIdGenerator.generate(postId, userId);
        Comment existingAnonymousComment = commentRepository
                .findFirstByPost_IdAndAuthorIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(postId, userId)
                .orElse(null);

        if (existingAnonymousComment != null) {
            String anonId = existingAnonymousComment.getAnonId();
            if (anonId == null || anonId.isBlank()) {
                anonId = generatedAnonId;
            }
            return new AnonymousMetadata(anonId, existingAnonymousComment.getAnonymousOrder());
        }

        int nextOrder = commentRepository.findMaxAnonymousOrderByPostId(postId) + 1;
        return new AnonymousMetadata(generatedAnonId, nextOrder);
    }

    private AnonymousMetadata resolveUpdatedAnonymousMetadata(Comment comment, String memberId, Boolean requestedAnonymous) {
        if (requestedAnonymous == null || requestedAnonymous == comment.isAnonymous()) {
            return new AnonymousMetadata(comment.getAnonId(), comment.getAnonymousOrder());
        }
        if (!requestedAnonymous) {
            return new AnonymousMetadata(null, null);
        }

        findNotDeletedPostForUpdateOrThrow(comment.getPost().getId());
        return resolveAnonymousMetadata(comment.getPost().getId(), memberId, true);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class AnonymousMetadata {
        private final String anonId;
        private final Integer anonymousOrder;

        private AnonymousMetadata(String anonId, Integer anonymousOrder) {
            this.anonId = anonId;
            this.anonymousOrder = anonymousOrder;
        }
    }

    private static final class PostSummaryPersonalization {
        private final Set<String> likedPostIds;
        private final Set<String> bookmarkedPostIds;
        private final Set<String> commentedPostIds;

        private PostSummaryPersonalization(
                Set<String> likedPostIds,
                Set<String> bookmarkedPostIds,
                Set<String> commentedPostIds
        ) {
            this.likedPostIds = likedPostIds;
            this.bookmarkedPostIds = bookmarkedPostIds;
            this.commentedPostIds = commentedPostIds;
        }

        private static PostSummaryPersonalization empty() {
            return new PostSummaryPersonalization(Set.of(), Set.of(), Set.of());
        }
    }

    private record CurrentAuthorProfile(String photoUrl, boolean isAdmin) {
    }

    private static final class AuthorView {
        private final String authorId;
        private final String authorName;
        private final String authorProfileImage;
        private final boolean authorAdmin;

        private AuthorView(String authorId, String authorName, String authorProfileImage, boolean authorAdmin) {
            this.authorId = authorId;
            this.authorName = authorName;
            this.authorProfileImage = authorProfileImage;
            this.authorAdmin = authorAdmin;
        }
    }
}
