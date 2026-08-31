package com.skuri.skuri_backend.domain.contentblock.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeComment;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentRepository;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.contentblock.dto.request.CreateContentBlockRequest;
import com.skuri.skuri_backend.domain.contentblock.entity.ContentBlock;
import com.skuri.skuri_backend.domain.contentblock.entity.ContentBlockTargetType;
import com.skuri.skuri_backend.domain.contentblock.repository.ContentBlockRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notice.entity.NoticeComment;
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentBlockServiceTest {

    @Mock
    private ContentBlockRepository contentBlockRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private NoticeCommentRepository noticeCommentRepository;
    @Mock
    private AppNoticeCommentRepository appNoticeCommentRepository;

    private ContentBlockService contentBlockService;

    @BeforeEach
    void setUp() {
        contentBlockService = new ContentBlockService(
                contentBlockRepository,
                memberRepository,
                postRepository,
                commentRepository,
                noticeCommentRepository,
                appNoticeCommentRepository
        );
    }

    @Test
    void create_네가지콘텐츠유형을_작성자회원ID노출없이차단한다() {
        Member blocker = member("blocker");
        Member target = member("target");
        when(memberRepository.findAllActiveByIdInForUpdateOrdered(List.of("blocker", "target")))
                .thenReturn(List.of(blocker, target));

        Post post = org.mockito.Mockito.mock(Post.class);
        Comment comment = org.mockito.Mockito.mock(Comment.class);
        NoticeComment noticeComment = org.mockito.Mockito.mock(NoticeComment.class);
        AppNoticeComment appNoticeComment = org.mockito.Mockito.mock(AppNoticeComment.class);
        when(post.getAuthorId()).thenReturn("target");
        when(comment.getAuthorId()).thenReturn("target");
        when(noticeComment.getUserId()).thenReturn("target");
        when(appNoticeComment.getUserId()).thenReturn("target");
        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));
        when(commentRepository.findVisibleById("comment-1")).thenReturn(Optional.of(comment));
        when(noticeCommentRepository.findByIdAndDeletedFalse("notice-comment-1")).thenReturn(Optional.of(noticeComment));
        when(appNoticeCommentRepository.findVisibleById(org.mockito.ArgumentMatchers.eq("app-comment-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(appNoticeComment));
        when(contentBlockRepository.findByBlockerIdAndBlockedId("blocker", "target")).thenReturn(Optional.empty());
        when(contentBlockRepository.saveAndFlush(any(ContentBlock.class))).thenAnswer(invocation -> savedBlock(invocation.getArgument(0)));

        var postResponse = contentBlockService.create(
                "blocker",
                new CreateContentBlockRequest(ContentBlockTargetType.POST, "post-1")
        );
        var commentResponse = contentBlockService.create(
                "blocker",
                new CreateContentBlockRequest(ContentBlockTargetType.COMMENT, "comment-1")
        );
        var noticeResponse = contentBlockService.create(
                "blocker",
                new CreateContentBlockRequest(ContentBlockTargetType.NOTICE_COMMENT, "notice-comment-1")
        );
        var appNoticeResponse = contentBlockService.create(
                "blocker",
                new CreateContentBlockRequest(ContentBlockTargetType.APP_NOTICE_COMMENT, "app-comment-1")
        );

        assertEquals("차단한 사용자", postResponse.label());
        assertEquals("차단한 사용자", commentResponse.label());
        assertEquals("차단한 사용자", noticeResponse.label());
        assertEquals("차단한 사용자", appNoticeResponse.label());
        assertEquals("block-id", postResponse.blockId());
    }

    @Test
    void create_같은대상을다시차단하면_기존opaque식별자를반환한다() {
        when(memberRepository.findAllActiveByIdInForUpdateOrdered(List.of("blocker", "target")))
                .thenReturn(List.of(member("blocker"), member("target")));
        Post post = org.mockito.Mockito.mock(Post.class);
        when(post.getAuthorId()).thenReturn("target");
        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));
        ContentBlock existing = savedBlock(ContentBlock.create("blocker", "target"));
        when(contentBlockRepository.findByBlockerIdAndBlockedId("blocker", "target"))
                .thenReturn(Optional.of(existing));

        var response = contentBlockService.create(
                "blocker",
                new CreateContentBlockRequest(ContentBlockTargetType.POST, "post-1")
        );

        assertEquals("block-id", response.blockId());
        verify(contentBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_숨김또는삭제된부모게시글의댓글은차단할수없다() {
        when(commentRepository.findVisibleById("comment-1")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> contentBlockService.create(
                        "blocker",
                        new CreateContentBlockRequest(ContentBlockTargetType.COMMENT, "comment-1")
                )
        );

        assertEquals(ErrorCode.COMMENT_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(memberRepository, contentBlockRepository);
    }

    @Test
    void create_예약상태앱공지의댓글은차단할수없다() {
        when(appNoticeCommentRepository.findVisibleById(
                org.mockito.ArgumentMatchers.eq("app-comment-1"),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> contentBlockService.create(
                        "blocker",
                        new CreateContentBlockRequest(ContentBlockTargetType.APP_NOTICE_COMMENT, "app-comment-1")
                )
        );

        assertEquals(ErrorCode.APP_NOTICE_COMMENT_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(memberRepository, contentBlockRepository);
    }

    @Test
    void create_자기콘텐츠면_CONTENT_BLOCK_SELF_NOT_ALLOWED() {
        when(memberRepository.findAllActiveByIdInForUpdateOrdered(List.of("blocker", "blocker")))
                .thenReturn(List.of(member("blocker")));
        Post post = org.mockito.Mockito.mock(Post.class);
        when(post.getAuthorId()).thenReturn("blocker");
        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> contentBlockService.create(
                        "blocker",
                        new CreateContentBlockRequest(ContentBlockTargetType.POST, "post-1")
                )
        );

        assertEquals(ErrorCode.CONTENT_BLOCK_SELF_NOT_ALLOWED, exception.getErrorCode());
        verify(contentBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_잠금후대상이비활성이면_기존대상notFound로거절한다() {
        Member blocker = member("blocker");
        Post post = org.mockito.Mockito.mock(Post.class);
        when(post.getAuthorId()).thenReturn("target");
        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));
        when(memberRepository.findAllActiveByIdInForUpdateOrdered(List.of("blocker", "target")))
                .thenReturn(List.of(blocker));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> contentBlockService.create(
                        "blocker",
                        new CreateContentBlockRequest(ContentBlockTargetType.POST, "post-1")
                )
        );

        assertEquals(ErrorCode.POST_NOT_FOUND, exception.getErrorCode());
        verify(memberRepository).findAllActiveByIdInForUpdateOrdered(List.of("blocker", "target"));
        verifyNoInteractions(contentBlockRepository);
    }

    @Test
    void create_상호차단경합방지를위해_두회원ID를정렬해잠근다() {
        Member blocker = member("z-blocker");
        Member target = member("a-target");
        Post post = org.mockito.Mockito.mock(Post.class);
        when(post.getAuthorId()).thenReturn("a-target");
        when(postRepository.findByIdAndDeletedFalseAndHiddenFalse("post-1")).thenReturn(Optional.of(post));
        when(memberRepository.findAllActiveByIdInForUpdateOrdered(List.of("a-target", "z-blocker")))
                .thenReturn(List.of(target, blocker));
        ContentBlock existing = savedBlock(ContentBlock.create("z-blocker", "a-target"));
        when(contentBlockRepository.findByBlockerIdAndBlockedId("z-blocker", "a-target"))
                .thenReturn(Optional.of(existing));

        contentBlockService.create(
                "z-blocker",
                new CreateContentBlockRequest(ContentBlockTargetType.POST, "post-1")
        );

        verify(memberRepository).findAllActiveByIdInForUpdateOrdered(List.of("a-target", "z-blocker"));
    }

    @Test
    void getMyBlocks_실제회원정보없이최신순리포지토리결과를반환한다() {
        when(memberRepository.findActiveById("blocker")).thenReturn(Optional.of(member("blocker")));
        ContentBlock contentBlock = savedBlock(ContentBlock.create("blocker", "target"));
        when(contentBlockRepository.findAllByBlockerIdOrderByCreatedAtDesc("blocker"))
                .thenReturn(List.of(contentBlock));

        var response = contentBlockService.getMyBlocks("blocker");

        assertEquals(1, response.size());
        assertEquals("block-id", response.get(0).blockId());
        assertEquals("차단한 사용자", response.get(0).label());
    }

    @Test
    void unblock_소유자와opaque식별자로만삭제하고_없는차단도멱등처리한다() {
        when(memberRepository.findActiveById("blocker")).thenReturn(Optional.of(member("blocker")));

        contentBlockService.unblock("blocker", "block-id");

        verify(contentBlockRepository).deleteByIdAndBlockerId("block-id", "blocker");
    }

    private ContentBlock savedBlock(ContentBlock contentBlock) {
        ReflectionTestUtils.setField(contentBlock, "id", "block-id");
        ReflectionTestUtils.setField(contentBlock, "createdAt", LocalDateTime.of(2026, 8, 31, 18, 30));
        return contentBlock;
    }

    private Member member(String id) {
        return Member.create(id, id + "@sungkyul.ac.kr", "회원", LocalDateTime.now());
    }
}
