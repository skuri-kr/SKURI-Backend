package com.skuri.skuri_backend.domain.board.service;

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
import com.skuri.skuri_backend.domain.board.dto.response.PostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostBookmarkResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostLikeResponse;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.CommentLike;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.entity.PostInteraction;
import com.skuri.skuri_backend.domain.board.repository.CommentLikeRepository;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostImageRepository;
import com.skuri.skuri_backend.domain.board.repository.PostInteractionRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.board.repository.PostSummaryProjection;
import com.skuri.skuri_backend.domain.board.repository.PostThumbnailProjection;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private PostInteractionRepository postInteractionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BoardService boardService;

    @Test
    void createPost_정상생성() {
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "post-1");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        PostDetailResponse response = boardService.createPost(
                "member-1",
                new CreatePostRequest(
                        "게시글 제목",
                        "게시글 내용",
                        PostCategory.GENERAL,
                        true,
                        List.of(new CreatePostImageRequest("https://example.com/image.jpg", null, 800, 600, 12345, "image/jpeg"))
                )
        );

        assertEquals("post-1", response.id());
        assertTrue(response.isAnonymous());
        assertEquals("익명", response.authorName());
    }

    @Test
    void createPost_thumbUrl이_빈문자열이면_null로_정규화한다() {
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "post-1");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        PostDetailResponse response = boardService.createPost(
                "member-1",
                new CreatePostRequest(
                        "게시글 제목",
                        "게시글 내용",
                        PostCategory.GENERAL,
                        false,
                        List.of(new CreatePostImageRequest("https://example.com/image.jpg", "   ", 800, 600, 12345, "image/jpeg"))
                )
        );

        assertEquals(1, response.images().size());
        assertNull(response.images().get(0).thumbUrl());
    }

    @Test
    void createPost_images항목에_null이있으면_VALIDATION_ERROR() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardService.createPost(
                        "member-1",
                        new CreatePostRequest(
                                "게시글 제목",
                                "게시글 내용",
                                PostCategory.GENERAL,
                                false,
                                Arrays.asList((CreatePostImageRequest) null)
                        )
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(memberRepository, never()).findActiveById("member-1");
    }

    @Test
    void createComment_대댓글은_무제한으로허용된다() {
        Post post = post("post-1", "author-1");
        Comment parent = comment("comment-1", post, null, "author-1", false, null);
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(commentRepository.findByIdAndPostId("comment-1", "post-1")).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "comment-2");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        CommentResponse response = boardService.createComment(
                "member-1",
                "post-1",
                new CreateCommentRequest("대댓글", false, "comment-1")
        );

        assertEquals("comment-2", response.id());
        assertEquals("comment-1", response.parentId());
        assertEquals(1, response.depth());
        assertEquals(1, post.getCommentCount());
    }

    @Test
    void createComment_대대댓글도허용된다() {
        Post post = post("post-1", "author-1");
        Comment root = comment("root-1", post, null, "author-1", false, null);
        Comment child = comment("child-1", post, root, "member-2", false, null);
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(commentRepository.findByIdAndPostId("child-1", "post-1")).thenReturn(Optional.of(child));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "comment-3");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        CommentResponse response = boardService.createComment(
                "member-1",
                "post-1",
                new CreateCommentRequest("대대댓글", false, "child-1")
        );

        assertEquals("comment-3", response.id());
        assertEquals("child-1", response.parentId());
        assertEquals(2, response.depth());
    }

    @Test
    void getComments_flatList로반환되고_부모삭제시_placeholder유지_자손은유지된다() {
        Post post = post("post-1", "author-1");
        Comment parent = comment("comment-1", post, null, "author-1", false, null);
        Comment child = comment("comment-2", post, parent, "member-2", true, 2);
        Comment grandChild = comment("comment-3", post, child, "member-3", false, null);
        parent.softDelete();

        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdOrderByCreatedAtAsc("post-1")).thenReturn(List.of(parent, child, grandChild));

        List<CommentResponse> responses = boardService.getComments("member-3", "post-1");

        assertEquals(3, responses.size());
        assertTrue(responses.get(0).isDeleted());
        assertEquals(Comment.DELETED_PLACEHOLDER, responses.get(0).content());
        assertEquals(0, responses.get(0).depth());
        assertEquals("comment-2", responses.get(1).id());
        assertEquals("comment-1", responses.get(1).parentId());
        assertEquals(1, responses.get(1).depth());
        assertEquals("comment-3", responses.get(2).id());
        assertEquals("comment-2", responses.get(2).parentId());
        assertEquals(2, responses.get(2).depth());
    }

    @Test
    void getComments_likeCount와_isLiked를_합성한다() {
        Post post = post("post-1", "author-1");
        Comment likedComment = comment("comment-1", post, null, "member-2", false, null);
        Comment unlikedComment = comment("comment-2", post, null, "member-3", false, null);
        ReflectionTestUtils.setField(likedComment, "likeCount", 3);
        ReflectionTestUtils.setField(unlikedComment, "likeCount", 1);

        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdOrderByCreatedAtAsc("post-1")).thenReturn(List.of(likedComment, unlikedComment));
        when(commentLikeRepository.findLikedCommentIds("member-1", List.of("comment-1", "comment-2")))
                .thenReturn(List.of("comment-1"));

        List<CommentResponse> responses = boardService.getComments("member-1", "post-1");

        assertEquals(3, responses.get(0).likeCount());
        assertTrue(responses.get(0).isLiked());
        assertEquals(1, responses.get(1).likeCount());
        assertFalse(responses.get(1).isLiked());
    }

    @Test
    void getComments_현재프로필이미지를_한번에조회하고_익명에는노출하지않는다() {
        Post post = post("post-1", "author-1");
        Comment namedComment = comment("comment-1", post, null, "member-2", false, null);
        Comment anonymousComment = comment("comment-2", post, null, "member-3", true, 1);
        Member namedAuthor = memberWithProfileImage("member-2", "https://example.com/current-member-2.jpg");
        Member anonymousAuthor = memberWithProfileImage("member-3", "https://example.com/current-member-3.jpg");
        ReflectionTestUtils.setField(namedAuthor, "isAdmin", true);
        ReflectionTestUtils.setField(anonymousAuthor, "isAdmin", true);

        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdOrderByCreatedAtAsc("post-1"))
                .thenReturn(List.of(namedComment, anonymousComment));
        when(memberRepository.findAllActiveByIdIn(any())).thenReturn(List.of(namedAuthor, anonymousAuthor));

        List<CommentResponse> responses = boardService.getComments("member-1", "post-1");

        assertEquals("https://example.com/current-member-2.jpg", responses.get(0).authorProfileImage());
        assertTrue(responses.get(0).isAuthorAdmin());
        assertNull(responses.get(1).authorProfileImage());
        assertFalse(responses.get(1).isAuthorAdmin());
        verify(memberRepository).findAllActiveByIdIn(any());
    }

    @Test
    void createComment_익명순번은_기존순번을재사용한다() {
        Post post = post("post-1", "author-1");
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        Comment existingAnonymous = comment("old-comment", post, null, "member-1", true, 2);

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(commentRepository.findFirstByPost_IdAndAuthorIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc("post-1", "member-1"))
                .thenReturn(Optional.of(existingAnonymous));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "comment-new");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        boardService.createComment(
                "member-1",
                "post-1",
                new CreateCommentRequest("익명 댓글", true, null)
        );

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertEquals("post-1:member-1", captor.getValue().getAnonId());
        assertEquals(2, captor.getValue().getAnonymousOrder());
    }

    @Test
    void createComment_긴scopeId여도_짧은anonId를_생성한다() {
        String postId = "p".repeat(140);
        Post post = post(postId, "author-1");
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        when(postRepository.findActiveByIdForUpdate(postId)).thenReturn(Optional.of(post));
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(commentRepository.findFirstByPost_IdAndAuthorIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(postId, "member-1"))
                .thenReturn(Optional.empty());
        when(commentRepository.findMaxAnonymousOrderByPostId(postId)).thenReturn(0);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boardService.createComment("member-1", postId, new CreateCommentRequest("익명 댓글", true, null));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertEquals(AnonymousCommentIdGenerator.generate(postId, "member-1"), captor.getValue().getAnonId());
        assertTrue(captor.getValue().getAnonId().length() <= 35);
    }

    @Test
    void likeUnlike_카운트가동기화된다() {
        Post post = post("post-1", "author-1");
        PostInteraction interaction = PostInteraction.create(post, "member-1");

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(postInteractionRepository.findById_UserIdAndId_PostId("member-1", "post-1"))
                .thenReturn(Optional.of(interaction));

        PostLikeResponse liked = boardService.likePost("member-1", "post-1");
        PostLikeResponse unliked = boardService.unlikePost("member-1", "post-1");

        assertTrue(liked.isLiked());
        assertEquals(1, liked.likeCount());
        assertFalse(unliked.isLiked());
        assertEquals(0, unliked.likeCount());
    }

    @Test
    void commentLikeUnlike_카운트가동기화된다() {
        Post post = post("post-1", "author-1");
        Comment comment = comment("comment-1", post, null, "author-2", false, null);
        CommentLike commentLike = CommentLike.create(comment, "member-1");

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1"))
                .thenReturn(false)
                .thenReturn(false);
        when(commentLikeRepository.findById_UserIdAndId_CommentId("member-1", "comment-1"))
                .thenReturn(Optional.of(commentLike));

        CommentLikeResponse liked = boardService.likeComment("member-1", "comment-1");
        CommentLikeResponse unliked = boardService.unlikeComment("member-1", "comment-1");

        assertEquals("comment-1", liked.commentId());
        assertTrue(liked.isLiked());
        assertEquals(1, liked.likeCount());
        assertFalse(unliked.isLiked());
        assertEquals(0, unliked.likeCount());
    }

    @Test
    void updateComment_행잠금을사용해_본문을수정한다() {
        Post post = post("post-1", "author-1");
        Comment comment = comment("comment-1", post, null, "member-1", false, null);
        ReflectionTestUtils.setField(comment, "likeCount", 2);

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1")).thenReturn(true);

        CommentResponse response = boardService.updateComment(
                "member-1",
                "comment-1",
                new UpdateCommentRequest("수정된 댓글")
        );

        assertEquals("수정된 댓글", response.content());
        assertEquals("수정된 댓글", comment.getContent());
        assertEquals(2, response.likeCount());
        assertTrue(response.isLiked());
        verify(commentRepository).findByIdForUpdate("comment-1");
        verify(commentRepository, never()).findActiveById("comment-1");
    }

    @Test
    void updateComment_숨김게시글에서도_실명에서익명으로전환할수있다() {
        Post post = post("post-1", "author-1");
        post.hide();
        Comment comment = comment("comment-1", post, null, "member-1", false, null);

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(postRepository.findByIdAndDeletedFalseForUpdate("post-1")).thenReturn(Optional.of(post));
        when(commentRepository.findFirstByPost_IdAndAuthorIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc("post-1", "member-1"))
                .thenReturn(Optional.empty());
        when(commentRepository.findMaxAnonymousOrderByPostId("post-1")).thenReturn(0);
        when(commentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1")).thenReturn(false);

        CommentResponse response = boardService.updateComment(
                "member-1",
                "comment-1",
                new UpdateCommentRequest("수정된 댓글", true)
        );

        assertTrue(comment.isAnonymous());
        assertEquals(1, comment.getAnonymousOrder());
        assertEquals(AnonymousCommentIdGenerator.generate("post-1", "member-1"), comment.getAnonId());
        assertTrue(response.isAnonymous());
        assertEquals(1, response.anonymousOrder());
        verify(postRepository).findByIdAndDeletedFalseForUpdate("post-1");
        verify(postRepository, never()).findActiveByIdForUpdate("post-1");
    }

    @Test
    void updateComment_실명에서익명으로전환하면_기존순번을재사용한다() {
        Post post = post("post-1", "author-1");
        Comment comment = comment("comment-1", post, null, "member-1", false, null);
        Comment existingAnonymous = comment("comment-2", post, null, "member-1", true, 2);

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(postRepository.findByIdAndDeletedFalseForUpdate("post-1")).thenReturn(Optional.of(post));
        when(commentRepository.findFirstByPost_IdAndAuthorIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc("post-1", "member-1"))
                .thenReturn(Optional.of(existingAnonymous));
        when(commentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1")).thenReturn(false);

        CommentResponse response = boardService.updateComment(
                "member-1",
                "comment-1",
                new UpdateCommentRequest("수정된 댓글", true)
        );

        assertTrue(comment.isAnonymous());
        assertEquals(existingAnonymous.getAnonId(), comment.getAnonId());
        assertEquals(2, comment.getAnonymousOrder());
        assertTrue(response.isAnonymous());
        assertEquals(2, response.anonymousOrder());
    }

    @Test
    void updateComment_실명에서익명으로전환하면_기존순번이없을때_새순번을부여한다() {
        Post post = post("post-1", "author-1");
        Comment comment = comment("comment-1", post, null, "member-1", false, null);

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(postRepository.findByIdAndDeletedFalseForUpdate("post-1")).thenReturn(Optional.of(post));
        when(commentRepository.findFirstByPost_IdAndAuthorIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc("post-1", "member-1"))
                .thenReturn(Optional.empty());
        when(commentRepository.findMaxAnonymousOrderByPostId("post-1")).thenReturn(3);
        when(commentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1")).thenReturn(false);

        CommentResponse response = boardService.updateComment(
                "member-1",
                "comment-1",
                new UpdateCommentRequest("수정된 댓글", true)
        );

        assertTrue(comment.isAnonymous());
        assertEquals(4, comment.getAnonymousOrder());
        assertEquals(AnonymousCommentIdGenerator.generate("post-1", "member-1"), comment.getAnonId());
        assertEquals(4, response.anonymousOrder());
    }

    @Test
    void updateComment_익명에서실명으로전환하면_anon메타데이터를정리한다() {
        Post post = post("post-1", "author-1");
        Comment comment = comment("comment-1", post, null, "member-1", true, 2);

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1")).thenReturn(true);

        CommentResponse response = boardService.updateComment(
                "member-1",
                "comment-1",
                new UpdateCommentRequest("수정된 댓글", false)
        );

        assertFalse(comment.isAnonymous());
        assertNull(comment.getAnonId());
        assertNull(comment.getAnonymousOrder());
        assertFalse(response.isAnonymous());
        assertNull(response.anonymousOrder());
    }

    @Test
    void bookmarkUnbookmark_카운트가동기화된다() {
        Post post = post("post-1", "author-1");
        PostInteraction interaction = PostInteraction.create(post, "member-1");

        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));
        when(postInteractionRepository.findById_UserIdAndId_PostId("member-1", "post-1"))
                .thenReturn(Optional.of(interaction));

        PostBookmarkResponse bookmarked = boardService.bookmarkPost("member-1", "post-1");
        PostBookmarkResponse unbookmarked = boardService.unbookmarkPost("member-1", "post-1");

        assertTrue(bookmarked.isBookmarked());
        assertEquals(1, bookmarked.bookmarkCount());
        assertFalse(unbookmarked.isBookmarked());
        assertEquals(0, unbookmarked.bookmarkCount());
    }

    @Test
    void getPosts_목록summary에_bookmarkCount를포함한다() {
        PostSummaryProjection projection = mock(PostSummaryProjection.class);
        when(projection.getId()).thenReturn("post-1");
        when(projection.getTitle()).thenReturn("제목");
        when(projection.getContent()).thenReturn("본문");
        when(projection.getAuthorId()).thenReturn("author-1");
        when(projection.getAuthorName()).thenReturn("작성자");
        when(projection.isAnonymous()).thenReturn(false);
        when(projection.getCategory()).thenReturn(PostCategory.GENERAL);
        when(projection.getViewCount()).thenReturn(10);
        when(projection.getLikeCount()).thenReturn(4);
        when(projection.getCommentCount()).thenReturn(2);
        when(projection.getBookmarkCount()).thenReturn(3);
        when(projection.isHasImage()).thenReturn(true);
        when(projection.isPinned()).thenReturn(false);
        when(projection.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(postRepository.searchSummaries(any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(projection)));
        when(postImageRepository.findFirstThumbnailByPostIds(List.of("post-1")))
                .thenReturn(List.of(thumbnailProjection("post-1", "https://example.com/post-1-thumb.jpg")));

        var response = boardService.getPosts("member-1", PostCategory.GENERAL, null, null, "latest", 0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals(3, response.getContent().get(0).bookmarkCount());
        assertEquals("https://example.com/post-1-thumb.jpg", response.getContent().get(0).thumbnailUrl());
    }

    @Test
    void getPosts_작성시스냅샷대신_현재프로필이미지를반환한다() {
        PostSummaryProjection projection = summaryProjection("post-1", 0);
        Member author = memberWithProfileImage("author-post-1", "https://example.com/current-profile.jpg");
        ReflectionTestUtils.setField(author, "isAdmin", true);

        when(postRepository.searchSummaries(any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(projection)));
        when(postImageRepository.findFirstThumbnailByPostIds(List.of("post-1"))).thenReturn(List.of());
        when(postInteractionRepository.findById_UserIdAndId_PostIdIn("member-1", List.of("post-1")))
                .thenReturn(List.of());
        when(commentRepository.findCommentedPostIds("member-1", List.of("post-1"))).thenReturn(List.of());
        when(memberRepository.findAllActiveByIdIn(any())).thenReturn(List.of(author));

        var response = boardService.getPosts("member-1", PostCategory.GENERAL, null, null, "latest", 0, 20);

        assertEquals("https://example.com/current-profile.jpg", response.getContent().get(0).authorProfileImage());
        assertTrue(response.getContent().get(0).isAuthorAdmin());
        verify(memberRepository).findAllActiveByIdIn(any());
    }

    @Test
    void getPostDetail_작성시스냅샷대신_현재프로필이미지를반환한다() {
        Post post = post("post-1", "author-1");
        Member author = memberWithProfileImage("author-1", "https://example.com/current-profile.jpg");
        ReflectionTestUtils.setField(author, "isAdmin", true);

        when(postRepository.incrementViewCount("post-1")).thenReturn(1);
        when(postRepository.findActiveDetailById("post-1")).thenReturn(Optional.of(post));
        when(postInteractionRepository.existsById_UserIdAndId_PostIdAndLikedTrue("member-1", "post-1"))
                .thenReturn(false);
        when(postInteractionRepository.existsById_UserIdAndId_PostIdAndBookmarkedTrue("member-1", "post-1"))
                .thenReturn(false);
        when(memberRepository.findAllActiveByIdIn(any())).thenReturn(List.of(author));

        PostDetailResponse response = boardService.getPostDetail("member-1", "post-1");

        assertEquals("https://example.com/current-profile.jpg", response.authorProfileImage());
        assertTrue(response.isAuthorAdmin());
        verify(memberRepository).findAllActiveByIdIn(any());
    }

    @Test
    void getPosts_개인화상태를합성한다() {
        PostSummaryProjection personalized = summaryProjection("post-1", 2);
        PostSummaryProjection othersOnly = summaryProjection("post-2", 5);
        PostInteraction interaction = mock(PostInteraction.class);
        when(interaction.getPostId()).thenReturn("post-1");
        when(interaction.isLiked()).thenReturn(true);
        when(interaction.isBookmarked()).thenReturn(true);

        when(postRepository.searchSummaries(any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(personalized, othersOnly)));
        when(postImageRepository.findFirstThumbnailByPostIds(List.of("post-1", "post-2")))
                .thenReturn(List.of(
                        thumbnailProjection("post-1", "https://example.com/post-1-thumb.jpg"),
                        thumbnailProjection("post-2", "https://example.com/post-2-thumb.jpg")
                ));
        when(postInteractionRepository.findById_UserIdAndId_PostIdIn("member-1", List.of("post-1", "post-2")))
                .thenReturn(List.of(interaction));
        when(commentRepository.findCommentedPostIds("member-1", List.of("post-1", "post-2")))
                .thenReturn(List.of("post-1"));

        var response = boardService.getPosts("member-1", PostCategory.GENERAL, null, null, "latest", 0, 20);

        assertTrue(response.getContent().get(0).isLiked());
        assertTrue(response.getContent().get(0).isBookmarked());
        assertTrue(response.getContent().get(0).isCommentedByMe());
        assertEquals("https://example.com/post-1-thumb.jpg", response.getContent().get(0).thumbnailUrl());
        assertFalse(response.getContent().get(1).isLiked());
        assertFalse(response.getContent().get(1).isBookmarked());
        assertFalse(response.getContent().get(1).isCommentedByMe());
        assertEquals("https://example.com/post-2-thumb.jpg", response.getContent().get(1).thumbnailUrl());
    }

    @Test
    void getPosts_삭제된내댓글만남으면_isCommentedByMe_false() {
        PostSummaryProjection projection = summaryProjection("post-1", 3);

        when(postRepository.searchSummaries(any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(projection)));
        when(postImageRepository.findFirstThumbnailByPostIds(List.of("post-1")))
                .thenReturn(List.of(thumbnailProjection("post-1", "https://example.com/post-1-thumb.jpg")));
        when(postInteractionRepository.findById_UserIdAndId_PostIdIn("member-1", List.of("post-1")))
                .thenReturn(List.of());
        when(commentRepository.findCommentedPostIds("member-1", List.of("post-1")))
                .thenReturn(List.of());

        var response = boardService.getPosts("member-1", PostCategory.GENERAL, null, null, "latest", 0, 20);

        assertFalse(response.getContent().get(0).isCommentedByMe());
    }

    @Test
    void getPosts_이미지가없으면_thumbnailUrl은_null이다() {
        PostSummaryProjection projection = summaryProjection("post-1", 0, false);

        when(postRepository.searchSummaries(any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(projection)));
        when(postImageRepository.findFirstThumbnailByPostIds(List.of("post-1")))
                .thenReturn(List.of());
        when(postInteractionRepository.findById_UserIdAndId_PostIdIn("member-1", List.of("post-1")))
                .thenReturn(List.of());
        when(commentRepository.findCommentedPostIds("member-1", List.of("post-1")))
                .thenReturn(List.of());

        var response = boardService.getPosts("member-1", PostCategory.GENERAL, null, null, "latest", 0, 20);

        assertFalse(response.getContent().get(0).hasImage());
        assertNull(response.getContent().get(0).thumbnailUrl());
    }

    @Test
    void handleMemberWithdrawal_댓글좋아요와인터랙션카운트정리를수행한다() {
        Post authoredPost = post("post-authored", "member-1");
        Post likedPost = post("post-liked", "author-2");
        ReflectionTestUtils.setField(likedPost, "likeCount", 3);
        ReflectionTestUtils.setField(likedPost, "bookmarkCount", 2);
        Comment comment = comment("comment-1", likedPost, null, "member-1", false, null);
        ReflectionTestUtils.setField(comment, "likeCount", 2);
        CommentLike commentLike = CommentLike.create(comment, "member-1");
        PostInteraction interaction = PostInteraction.create(likedPost, "member-1");
        interaction.like();
        interaction.bookmark();

        when(postRepository.findByAuthorId("member-1")).thenReturn(List.of(authoredPost));
        when(commentRepository.findByAuthorId("member-1")).thenReturn(List.of(comment));
        when(commentLikeRepository.findById_UserId("member-1")).thenReturn(List.of(commentLike));
        when(postInteractionRepository.findById_UserId("member-1")).thenReturn(List.of(interaction));
        when(commentRepository.findAllById(any())).thenReturn(List.of(comment));
        when(postRepository.findAllById(any())).thenReturn(List.of(likedPost));

        boardService.handleMemberWithdrawal("member-1");

        assertEquals("withdrawn-member", authoredPost.getAuthorId());
        assertEquals("탈퇴한 사용자", authoredPost.getAuthorName());
        assertEquals("withdrawn-member", comment.getAuthorId());
        assertEquals("탈퇴한 사용자", comment.getAuthorName());
        assertEquals(1, comment.getLikeCount());
        assertEquals(2, likedPost.getLikeCount());
        assertEquals(1, likedPost.getBookmarkCount());
        verify(commentLikeRepository).deleteAllInBatch(List.of(commentLike));
        verify(postInteractionRepository).deleteAllInBatch(List.of(interaction));
    }

    @Test
    void updatePost_작성자위반이면_예외() {
        Post post = post("post-1", "author-1");
        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardService.updatePost("member-2", "post-1", new UpdatePostRequest("수정", null, null, null, null))
        );

        assertEquals(ErrorCode.NOT_POST_AUTHOR, exception.getErrorCode());
    }

    @Test
    void updatePost_공백만전달되면_VALIDATION_ERROR() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardService.updatePost("member-1", "post-1", new UpdatePostRequest("   ", null, null, null, null))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(postRepository, never()).findByIdAndDeletedFalseAndHiddenFalse("post-1");
    }

    @Test
    void updatePost_익명여부와이미지를전체교체한다() {
        Post post = post("post-1", "author-1");
        post.appendImage("https://example.com/old.jpg", null, 400, 300, 1000, "image/jpeg", 0);
        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));
        when(postInteractionRepository.existsById_UserIdAndId_PostIdAndLikedTrue("author-1", "post-1")).thenReturn(false);
        when(postInteractionRepository.existsById_UserIdAndId_PostIdAndBookmarkedTrue("author-1", "post-1")).thenReturn(false);

        PostDetailResponse response = boardService.updatePost(
                "author-1",
                "post-1",
                new UpdatePostRequest(
                        null,
                        null,
                        null,
                        true,
                        List.of(
                                new CreatePostImageRequest("https://example.com/new-1.jpg", null, 800, 600, 2000, "image/jpeg"),
                                new CreatePostImageRequest("https://example.com/new-2.jpg", null, 1024, 768, 3000, "image/jpeg")
                        )
                )
        );

        assertTrue(response.isAnonymous());
        assertEquals("익명", response.authorName());
        assertEquals(2, response.images().size());
        assertEquals("https://example.com/new-1.jpg", response.images().get(0).url());
        assertEquals(2, post.getImages().size());
        assertEquals("https://example.com/new-2.jpg", post.getImages().get(1).getUrl());
        assertEquals("post-1:author-1", post.getAnonId());
    }

    @Test
    void updatePost_images항목에_null이있으면_VALIDATION_ERROR() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardService.updatePost(
                        "author-1",
                        "post-1",
                        new UpdatePostRequest(
                                null,
                                null,
                                null,
                                null,
                                Arrays.asList((CreatePostImageRequest) null)
                        )
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(postRepository, never()).findByIdAndDeletedFalseAndHiddenFalse("post-1");
    }

    @Test
    void deletePost_작성자이면_softDelete() {
        Post post = post("post-1", "author-1");
        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));

        boardService.deletePost("author-1", "post-1");

        assertTrue(post.isDeleted());
    }

    @Test
    void deleteComment_정상삭제시_placeholder와카운트동기화() {
        Post post = post("post-1", "author-1");
        ReflectionTestUtils.setField(post, "commentCount", 2);
        Comment comment = comment("comment-1", post, null, "member-1", false, null);

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));
        when(postRepository.findActiveByIdForUpdate("post-1")).thenReturn(Optional.of(post));

        boardService.deleteComment("member-1", "comment-1");

        assertTrue(comment.isDeleted());
        assertEquals(Comment.DELETED_PLACEHOLDER, comment.getContent());
        assertEquals(1, post.getCommentCount());
    }

    @Test
    void deleteComment_이미삭제된댓글이면_COMMENT_ALREADY_DELETED() {
        Post post = post("post-1", "author-1");
        ReflectionTestUtils.setField(post, "commentCount", 2);
        Comment comment = comment("comment-1", post, null, "member-1", false, null);
        comment.softDelete();

        when(commentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(comment));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> boardService.deleteComment("member-1", "comment-1")
        );

        assertEquals(ErrorCode.COMMENT_ALREADY_DELETED, exception.getErrorCode());
        verify(postRepository, never()).findActiveByIdForUpdate("post-1");
    }

    private Post post(String id, String authorId) {
        Post post = Post.create("제목", "본문", authorId, "작성자", "https://example.com/profile.jpg", false, PostCategory.GENERAL);
        ReflectionTestUtils.setField(post, "id", id);
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());
        return post;
    }

    private Member memberWithProfileImage(String id, String photoUrl) {
        Member member = Member.create(id, id + "@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        member.updateProfile(null, null, null, null, photoUrl);
        return member;
    }

    private PostSummaryProjection summaryProjection(String id, int commentCount) {
        return summaryProjection(id, commentCount, true);
    }

    private PostSummaryProjection summaryProjection(String id, int commentCount, boolean hasImage) {
        PostSummaryProjection projection = mock(PostSummaryProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getTitle()).thenReturn("제목 " + id);
        when(projection.getContent()).thenReturn("본문 " + id);
        when(projection.getAuthorId()).thenReturn("author-" + id);
        when(projection.getAuthorName()).thenReturn("작성자");
        when(projection.isAnonymous()).thenReturn(false);
        when(projection.getCategory()).thenReturn(PostCategory.GENERAL);
        when(projection.getViewCount()).thenReturn(10);
        when(projection.getLikeCount()).thenReturn(4);
        when(projection.getCommentCount()).thenReturn(commentCount);
        when(projection.getBookmarkCount()).thenReturn(3);
        when(projection.isHasImage()).thenReturn(hasImage);
        when(projection.isPinned()).thenReturn(false);
        when(projection.getCreatedAt()).thenReturn(LocalDateTime.now());
        return projection;
    }

    private PostThumbnailProjection thumbnailProjection(String postId, String thumbnailUrl) {
        return new PostThumbnailProjection() {
            @Override
            public String getPostId() {
                return postId;
            }

            @Override
            public String getThumbnailUrl() {
                return thumbnailUrl;
            }
        };
    }

    private Comment comment(
            String id,
            Post post,
            Comment parent,
            String authorId,
            boolean anonymous,
            Integer anonymousOrder
    ) {
        Comment comment = Comment.create(
                post,
                "댓글",
                authorId,
                "작성자",
                "https://example.com/profile.jpg",
                anonymous,
                anonymous ? post.getId() + ":" + authorId : null,
                anonymousOrder,
                parent
        );
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.now());
        return comment;
    }
}
