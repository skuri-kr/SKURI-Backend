package com.skuri.skuri_backend.domain.notice.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.common.util.AnonymousCommentIdGenerator;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.contentblock.service.ContentBlockQueryService;
import com.skuri.skuri_backend.domain.notice.dto.request.CreateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.request.UpdateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeBookmarkResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeReadResponse;
import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.entity.NoticeBookmark;
import com.skuri.skuri_backend.domain.notice.entity.NoticeComment;
import com.skuri.skuri_backend.domain.notice.entity.NoticeCommentLike;
import com.skuri.skuri_backend.domain.notice.entity.NoticeLike;
import com.skuri.skuri_backend.domain.notice.entity.NoticeReadStatus;
import com.skuri.skuri_backend.domain.notice.repository.NoticeBookmarkRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentLikeRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeLikeRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeReadStatusRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeSummaryProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeCommentRepository noticeCommentRepository;

    @Mock
    private NoticeCommentLikeRepository noticeCommentLikeRepository;

    @Mock
    private NoticeReadStatusRepository noticeReadStatusRepository;

    @Mock
    private NoticeLikeRepository noticeLikeRepository;

    @Mock
    private NoticeBookmarkRepository noticeBookmarkRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ContentBlockQueryService contentBlockQueryService;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NoticeService noticeService;

    @Test
    void createComment_익명순번은_기존순번을재사용한다() {
        Notice notice = notice("notice-1");
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        CommentFixture existingAnonymous = comment("old-comment", notice, null, "member-1", true, 2);

        when(noticeRepository.findByIdForUpdate("notice-1")).thenReturn(Optional.of(notice));
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(noticeCommentRepository.findFirstByNotice_IdAndUserIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc("notice-1", "member-1"))
                .thenReturn(Optional.of(existingAnonymous.comment));
        when(noticeCommentRepository.save(any(NoticeComment.class))).thenAnswer(invocation -> {
            NoticeComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "notice-comment-new");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        NoticeCommentResponse response = noticeService.createComment(
                "member-1",
                "notice-1",
                new CreateNoticeCommentRequest("익명 댓글", true, null)
        );

        ArgumentCaptor<NoticeComment> captor = ArgumentCaptor.forClass(NoticeComment.class);
        verify(noticeCommentRepository).save(captor.capture());
        assertEquals("notice-1:member-1", captor.getValue().getAnonId());
        assertEquals(2, captor.getValue().getAnonymousOrder());
        assertEquals("notice-comment-new", response.id());
    }

    @Test
    void createComment_긴noticeId여도_짧은anonId를_생성한다() {
        String noticeId = "n".repeat(120);
        Notice notice = notice(noticeId);
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        when(noticeRepository.findByIdForUpdate(noticeId)).thenReturn(Optional.of(notice));
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(noticeCommentRepository.findFirstByNotice_IdAndUserIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc(noticeId, "member-1"))
                .thenReturn(Optional.empty());
        when(noticeCommentRepository.findMaxAnonymousOrderByNoticeId(noticeId)).thenReturn(0);
        when(noticeCommentRepository.save(any(NoticeComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        noticeService.createComment("member-1", noticeId, new CreateNoticeCommentRequest("익명 댓글", true, null));

        ArgumentCaptor<NoticeComment> captor = ArgumentCaptor.forClass(NoticeComment.class);
        verify(noticeCommentRepository).save(captor.capture());
        assertEquals(AnonymousCommentIdGenerator.generate(noticeId, "member-1"), captor.getValue().getAnonId());
        assertTrue(captor.getValue().getAnonId().length() <= 35);
    }

    @Test
    void createComment_대대댓글도허용된다() {
        Notice notice = notice("notice-1");
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        CommentFixture root = comment("comment-1", notice, null, "member-2", false, null);
        CommentFixture child = comment("comment-2", notice, root.comment, "member-3", false, null);

        when(noticeRepository.findByIdForUpdate("notice-1")).thenReturn(Optional.of(notice));
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(noticeCommentRepository.findByIdAndNoticeId("comment-2", "notice-1")).thenReturn(Optional.of(child.comment));
        when(noticeCommentRepository.save(any(NoticeComment.class))).thenAnswer(invocation -> {
            NoticeComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "notice-comment-3");
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });

        NoticeCommentResponse response = noticeService.createComment(
                "member-1",
                "notice-1",
                new CreateNoticeCommentRequest("대대댓글", false, "comment-2")
        );

        assertEquals("notice-comment-3", response.id());
        assertEquals("comment-2", response.parentId());
        assertEquals(2, response.depth());
    }

    @Test
    void markRead_기존상태없으면_생성된다() {
        Notice notice = notice("notice-1");
        when(noticeRepository.findById("notice-1")).thenReturn(Optional.of(notice));
        when(noticeReadStatusRepository.findById_UserIdAndId_NoticeId("member-1", "notice-1")).thenReturn(Optional.empty());
        when(noticeReadStatusRepository.save(any(NoticeReadStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoticeReadResponse response = noticeService.markRead("member-1", "notice-1");

        assertEquals("notice-1", response.noticeId());
        assertTrue(response.isRead());
        assertNotNull(response.readAt());
    }

    @Test
    void likeUnlike_카운트가동기화된다() {
        Notice notice = notice("notice-1");
        NoticeLike like = NoticeLike.create(notice, "member-1");

        when(noticeRepository.findByIdForUpdate("notice-1")).thenReturn(Optional.of(notice));
        when(noticeLikeRepository.existsById_UserIdAndId_NoticeId("member-1", "notice-1"))
                .thenReturn(false)
                .thenReturn(false);
        when(noticeLikeRepository.findById_UserIdAndId_NoticeId("member-1", "notice-1"))
                .thenReturn(Optional.of(like));

        NoticeLikeResponse liked = noticeService.likeNotice("member-1", "notice-1");
        NoticeLikeResponse unliked = noticeService.unlikeNotice("member-1", "notice-1");

        assertTrue(liked.isLiked());
        assertEquals(1, liked.likeCount());
        assertFalse(unliked.isLiked());
        assertEquals(0, unliked.likeCount());
    }

    @Test
    void commentLikeUnlike_카운트가동기화된다() {
        Notice notice = notice("notice-1");
        CommentFixture fixture = comment("comment-1", notice, null, "author-1", false, null);
        NoticeCommentLike like = NoticeCommentLike.create(fixture.comment, "member-1");

        when(noticeCommentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(fixture.comment));
        when(noticeCommentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1"))
                .thenReturn(false)
                .thenReturn(false);
        when(noticeCommentLikeRepository.findById_UserIdAndId_CommentId("member-1", "comment-1"))
                .thenReturn(Optional.of(like));

        NoticeCommentLikeResponse liked = noticeService.likeComment("member-1", "comment-1");
        NoticeCommentLikeResponse unliked = noticeService.unlikeComment("member-1", "comment-1");

        assertEquals("comment-1", liked.commentId());
        assertTrue(liked.isLiked());
        assertEquals(1, liked.likeCount());
        assertFalse(unliked.isLiked());
        assertEquals(0, unliked.likeCount());
    }

    @Test
    void bookmarkNotice_처음등록하면_저장된다() {
        Notice notice = notice("notice-1");
        when(noticeRepository.findByIdForUpdate("notice-1")).thenReturn(Optional.of(notice));
        when(noticeBookmarkRepository.existsById_UserIdAndId_NoticeId("member-1", "notice-1")).thenReturn(false);

        NoticeBookmarkResponse response = noticeService.bookmarkNotice("member-1", "notice-1");

        assertTrue(response.isBookmarked());
        assertEquals(1, response.bookmarkCount());
        assertEquals(1, notice.getBookmarkCount());
        verify(noticeBookmarkRepository).save(any(NoticeBookmark.class));
    }

    @Test
    void bookmarkNotice_중복등록이면_멱등하게성공한다() {
        Notice notice = notice("notice-1");
        when(noticeRepository.findByIdForUpdate("notice-1")).thenReturn(Optional.of(notice));
        when(noticeBookmarkRepository.existsById_UserIdAndId_NoticeId("member-1", "notice-1")).thenReturn(true);

        NoticeBookmarkResponse response = noticeService.bookmarkNotice("member-1", "notice-1");

        assertTrue(response.isBookmarked());
        assertEquals(0, response.bookmarkCount());
        verify(noticeBookmarkRepository, never()).save(any(NoticeBookmark.class));
    }

    @Test
    void unbookmarkNotice_기존북마크가있으면_삭제된다() {
        Notice notice = notice("notice-1");
        NoticeBookmark bookmark = NoticeBookmark.create(notice, "member-1");
        when(noticeRepository.findByIdForUpdate("notice-1")).thenReturn(Optional.of(notice));
        when(noticeBookmarkRepository.findById_UserIdAndId_NoticeId("member-1", "notice-1"))
                .thenReturn(Optional.of(bookmark));

        NoticeBookmarkResponse response = noticeService.unbookmarkNotice("member-1", "notice-1");

        assertFalse(response.isBookmarked());
        assertEquals(0, response.bookmarkCount());
        verify(noticeBookmarkRepository).delete(bookmark);
    }

    @Test
    void getNotices_저장된thumbnailUrl과_개인화상태를합성한다() {
        NoticeSummaryProjection personalized = noticeSummary("notice-1", "https://www.sungkyul.ac.kr/upload/notice-1-thumb.jpg");
        NoticeSummaryProjection othersOnly = noticeSummary("notice-2", null);

        when(noticeRepository.searchSummaries(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(personalized, othersOnly)));
        when(noticeReadStatusRepository.findReadNoticeIds("member-1", List.of("notice-1", "notice-2")))
                .thenReturn(List.of("notice-1"));
        when(noticeLikeRepository.findLikedNoticeIds("member-1", List.of("notice-1", "notice-2")))
                .thenReturn(List.of("notice-1"));
        when(noticeBookmarkRepository.findBookmarkedNoticeIds("member-1", List.of("notice-1", "notice-2")))
                .thenReturn(List.of("notice-1"));
        when(noticeCommentRepository.findCommentedNoticeIds("member-1", List.of("notice-1", "notice-2")))
                .thenReturn(List.of("notice-1"));

        var response = noticeService.getNotices("member-1", null, null, 0, 20);

        assertTrue(response.getContent().get(0).isRead());
        assertTrue(response.getContent().get(0).isLiked());
        assertTrue(response.getContent().get(0).isBookmarked());
        assertTrue(response.getContent().get(0).isCommentedByMe());
        assertEquals("https://www.sungkyul.ac.kr/upload/notice-1-thumb.jpg", response.getContent().get(0).thumbnailUrl());
        assertFalse(response.getContent().get(1).isRead());
        assertFalse(response.getContent().get(1).isLiked());
        assertFalse(response.getContent().get(1).isBookmarked());
        assertFalse(response.getContent().get(1).isCommentedByMe());
        assertNull(response.getContent().get(1).thumbnailUrl());
    }

    @Test
    void getNotices_삭제된내댓글만남으면_isCommentedByMe_false() {
        NoticeSummaryProjection notice = noticeSummary("notice-1", null);

        when(noticeRepository.searchSummaries(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(notice)));
        when(noticeReadStatusRepository.findReadNoticeIds("member-1", List.of("notice-1")))
                .thenReturn(List.of());
        when(noticeLikeRepository.findLikedNoticeIds("member-1", List.of("notice-1")))
                .thenReturn(List.of());
        when(noticeBookmarkRepository.findBookmarkedNoticeIds("member-1", List.of("notice-1")))
                .thenReturn(List.of());
        when(noticeCommentRepository.findCommentedNoticeIds("member-1", List.of("notice-1")))
                .thenReturn(List.of());

        var response = noticeService.getNotices("member-1", null, null, 0, 20);

        assertFalse(response.getContent().get(0).isCommentedByMe());
        assertNull(response.getContent().get(0).thumbnailUrl());
    }

    @Test
    void deleteComment_작성자위반이면_예외() {
        Notice notice = notice("notice-1");
        CommentFixture fixture = comment("comment-1", notice, null, "author-1", false, null);
        when(noticeCommentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(fixture.comment));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> noticeService.deleteComment("member-2", "comment-1")
        );

        assertEquals(ErrorCode.NOT_NOTICE_COMMENT_AUTHOR, exception.getErrorCode());
        verify(noticeCommentRepository).findByIdForUpdate("comment-1");
        verify(noticeCommentRepository, never()).findById("comment-1");
    }

    @Test
    void updateComment_작성자이면_본문을수정한다() {
        Notice notice = notice("notice-1");
        CommentFixture fixture = comment("comment-1", notice, null, "member-1", false, null);
        ReflectionTestUtils.setField(fixture.comment, "likeCount", 3);
        when(noticeCommentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(fixture.comment));
        when(noticeCommentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1")).thenReturn(true);

        NoticeCommentResponse response = noticeService.updateComment(
                "member-1",
                "comment-1",
                new UpdateNoticeCommentRequest("수정된 댓글")
        );

        assertEquals("수정된 댓글", response.content());
        assertEquals("수정된 댓글", fixture.comment.getContent());
        assertEquals(3, response.likeCount());
        assertTrue(response.isLiked());
        verify(noticeCommentRepository).findByIdForUpdate("comment-1");
        verify(noticeCommentRepository, never()).findById("comment-1");
    }

    @Test
    void updateComment_실명에서익명으로전환하면_기존순번을재사용한다() {
        Notice notice = notice("notice-1");
        CommentFixture fixture = comment("comment-1", notice, null, "member-1", false, null);
        CommentFixture existingAnonymous = comment("comment-2", notice, null, "member-1", true, 2);

        when(noticeCommentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(fixture.comment));
        when(noticeRepository.findByIdForUpdate("notice-1")).thenReturn(Optional.of(notice));
        when(noticeCommentRepository.findFirstByNotice_IdAndUserIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc("notice-1", "member-1"))
                .thenReturn(Optional.of(existingAnonymous.comment));
        when(noticeCommentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1")).thenReturn(true);

        NoticeCommentResponse response = noticeService.updateComment(
                "member-1",
                "comment-1",
                new UpdateNoticeCommentRequest("수정된 댓글", true)
        );

        assertTrue(fixture.comment.isAnonymous());
        assertEquals(existingAnonymous.comment.getAnonId(), fixture.comment.getAnonId());
        assertEquals(2, fixture.comment.getAnonymousOrder());
        assertTrue(response.isAnonymous());
        assertEquals(2, response.anonymousOrder());
    }

    @Test
    void updateComment_실명에서익명으로전환하면_기존순번이없을때_새순번을부여한다() {
        Notice notice = notice("notice-1");
        CommentFixture fixture = comment("comment-1", notice, null, "member-1", false, null);

        when(noticeCommentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(fixture.comment));
        when(noticeRepository.findByIdForUpdate("notice-1")).thenReturn(Optional.of(notice));
        when(noticeCommentRepository.findFirstByNotice_IdAndUserIdAndAnonymousTrueAndAnonymousOrderIsNotNullOrderByCreatedAtAsc("notice-1", "member-1"))
                .thenReturn(Optional.empty());
        when(noticeCommentRepository.findMaxAnonymousOrderByNoticeId("notice-1")).thenReturn(3);
        when(noticeCommentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1")).thenReturn(false);

        NoticeCommentResponse response = noticeService.updateComment(
                "member-1",
                "comment-1",
                new UpdateNoticeCommentRequest("수정된 댓글", true)
        );

        assertTrue(fixture.comment.isAnonymous());
        assertEquals(4, fixture.comment.getAnonymousOrder());
        assertEquals(AnonymousCommentIdGenerator.generate("notice-1", "member-1"), fixture.comment.getAnonId());
        assertEquals(4, response.anonymousOrder());
    }

    @Test
    void updateComment_익명에서실명으로전환하면_anon메타데이터를정리한다() {
        Notice notice = notice("notice-1");
        CommentFixture fixture = comment("comment-1", notice, null, "member-1", true, 2);
        when(noticeCommentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(fixture.comment));
        when(noticeCommentLikeRepository.existsById_UserIdAndId_CommentId("member-1", "comment-1")).thenReturn(true);

        NoticeCommentResponse response = noticeService.updateComment(
                "member-1",
                "comment-1",
                new UpdateNoticeCommentRequest("수정된 댓글", false)
        );

        assertFalse(fixture.comment.isAnonymous());
        assertNull(fixture.comment.getAnonId());
        assertNull(fixture.comment.getAnonymousOrder());
        assertFalse(response.isAnonymous());
        assertNull(response.anonymousOrder());
    }

    @Test
    void updateComment_이미삭제된댓글이면_COMMENT_ALREADY_DELETED() {
        Notice notice = notice("notice-1");
        CommentFixture fixture = comment("comment-1", notice, null, "member-1", false, null);
        fixture.comment.softDelete();
        when(noticeCommentRepository.findByIdForUpdate("comment-1")).thenReturn(Optional.of(fixture.comment));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> noticeService.updateComment("member-1", "comment-1", new UpdateNoticeCommentRequest("수정"))
        );

        assertEquals(ErrorCode.COMMENT_ALREADY_DELETED, exception.getErrorCode());
        verify(noticeCommentRepository).findByIdForUpdate("comment-1");
        verify(noticeCommentRepository, never()).findById("comment-1");
    }

    @Test
    void getComments_flatList로반환되고_자손순서를유지한다() {
        Notice notice = notice("notice-1");
        CommentFixture root = comment("comment-1", notice, null, "member-1", false, null);
        CommentFixture child = comment("comment-2", notice, root.comment, "member-2", true, 2);
        CommentFixture grandChild = comment("comment-3", notice, child.comment, "member-3", false, null);

        when(noticeRepository.findById("notice-1")).thenReturn(Optional.of(notice));
        when(noticeCommentRepository.findByNoticeIdOrderByCreatedAtAsc("notice-1"))
                .thenReturn(List.of(root.comment, child.comment, grandChild.comment));

        List<NoticeCommentResponse> responses = noticeService.getComments("member-9", "notice-1");

        assertEquals(3, responses.size());
        assertEquals("comment-1", responses.get(0).id());
        assertEquals(0, responses.get(0).depth());
        assertEquals("comment-2", responses.get(1).id());
        assertEquals("comment-1", responses.get(1).parentId());
        assertEquals(1, responses.get(1).depth());
        assertEquals("comment-3", responses.get(2).id());
        assertEquals("comment-2", responses.get(2).parentId());
        assertEquals(2, responses.get(2).depth());
    }

    @Test
    void getComments_차단댓글은삭제형응답값으로마스킹하고_자손구조는유지한다() {
        Notice notice = notice("notice-1");
        CommentFixture blockedParent = comment("comment-1", notice, null, "blocked-author", true, 1);
        CommentFixture visibleChild = comment("comment-2", notice, blockedParent.comment, "visible-author", false, null);

        when(noticeRepository.findById("notice-1")).thenReturn(Optional.of(notice));
        when(noticeCommentRepository.findByNoticeIdOrderByCreatedAtAsc("notice-1"))
                .thenReturn(List.of(blockedParent.comment, visibleChild.comment));
        when(contentBlockQueryService.findBlockedMemberIds(
                "member-1",
                List.of("blocked-author", "visible-author")
        )).thenReturn(Set.of("blocked-author"));

        List<NoticeCommentResponse> responses = noticeService.getComments("member-1", "notice-1");

        assertEquals(NoticeComment.BLOCKED_PLACEHOLDER, responses.get(0).content());
        assertTrue(responses.get(0).isDeleted());
        assertNull(responses.get(0).authorId());
        assertNull(responses.get(0).authorName());
        assertFalse(responses.get(0).isAnonymous());
        assertEquals("comment-1", responses.get(1).parentId());
        assertEquals(1, responses.get(1).depth());
        assertFalse(responses.get(1).isDeleted());
    }

    @Test
    void getComments_likeCount와_isLiked를_합성한다() {
        Notice notice = notice("notice-1");
        CommentFixture likedComment = comment("comment-1", notice, null, "member-2", false, null);
        CommentFixture unlikedComment = comment("comment-2", notice, null, "member-3", false, null);
        ReflectionTestUtils.setField(likedComment.comment, "likeCount", 5);
        ReflectionTestUtils.setField(unlikedComment.comment, "likeCount", 1);

        when(noticeRepository.findById("notice-1")).thenReturn(Optional.of(notice));
        when(noticeCommentRepository.findByNoticeIdOrderByCreatedAtAsc("notice-1"))
                .thenReturn(List.of(likedComment.comment, unlikedComment.comment));
        when(noticeCommentLikeRepository.findLikedCommentIds("member-1", List.of("comment-1", "comment-2")))
                .thenReturn(List.of("comment-1"));

        List<NoticeCommentResponse> responses = noticeService.getComments("member-1", "notice-1");

        assertEquals(5, responses.get(0).likeCount());
        assertTrue(responses.get(0).isLiked());
        assertEquals(1, responses.get(1).likeCount());
        assertFalse(responses.get(1).isLiked());
    }

    @Test
    void getComments_현재프로필이미지를_한번에조회하고_익명에는노출하지않는다() {
        Notice notice = notice("notice-1");
        CommentFixture namedComment = comment("comment-1", notice, null, "member-2", false, null);
        CommentFixture anonymousComment = comment("comment-2", notice, null, "member-3", true, 1);
        Member namedAuthor = memberWithProfileImage("member-2", "https://example.com/current-member-2.jpg");
        Member anonymousAuthor = memberWithProfileImage("member-3", "https://example.com/current-member-3.jpg");
        ReflectionTestUtils.setField(namedAuthor, "isAdmin", true);
        ReflectionTestUtils.setField(anonymousAuthor, "isAdmin", true);

        when(noticeRepository.findById("notice-1")).thenReturn(Optional.of(notice));
        when(noticeCommentRepository.findByNoticeIdOrderByCreatedAtAsc("notice-1"))
                .thenReturn(List.of(namedComment.comment, anonymousComment.comment));
        when(memberRepository.findAllActiveByIdIn(any())).thenReturn(List.of(namedAuthor, anonymousAuthor));

        List<NoticeCommentResponse> responses = noticeService.getComments("member-1", "notice-1");

        assertEquals("https://example.com/current-member-2.jpg", responses.get(0).authorProfileImage());
        assertTrue(responses.get(0).isAuthorAdmin());
        assertNull(responses.get(1).authorProfileImage());
        assertFalse(responses.get(1).isAuthorAdmin());
        verify(memberRepository).findAllActiveByIdIn(any());
    }

    @Test
    void handleMemberWithdrawal_댓글좋아요익명화와좋아요읽음기록삭제를수행한다() {
        Notice notice = notice("notice-1");
        ReflectionTestUtils.setField(notice, "likeCount", 2);
        ReflectionTestUtils.setField(notice, "bookmarkCount", 3);
        CommentFixture commentFixture = comment("comment-1", notice, null, "member-1", false, null);
        ReflectionTestUtils.setField(commentFixture.comment, "likeCount", 2);
        NoticeCommentLike commentLike = NoticeCommentLike.create(commentFixture.comment, "member-1");
        NoticeLike like = NoticeLike.create(notice, "member-1");
        NoticeBookmark bookmark = NoticeBookmark.create(notice, "member-1");

        when(noticeCommentRepository.findByUserId("member-1")).thenReturn(List.of(commentFixture.comment));
        when(noticeCommentLikeRepository.findById_UserId("member-1")).thenReturn(List.of(commentLike));
        when(noticeLikeRepository.findById_UserId("member-1")).thenReturn(List.of(like));
        when(noticeBookmarkRepository.findById_UserId("member-1")).thenReturn(List.of(bookmark));
        when(noticeCommentRepository.findAllById(any())).thenReturn(List.of(commentFixture.comment));
        when(noticeRepository.findAllById(any())).thenReturn(List.of(notice));

        noticeService.handleMemberWithdrawal("member-1");

        assertEquals("withdrawn-member", commentFixture.comment.getUserId());
        assertEquals("탈퇴한 사용자", commentFixture.comment.getUserDisplayName());
        assertEquals(1, commentFixture.comment.getLikeCount());
        assertEquals(1, notice.getLikeCount());
        assertEquals(2, notice.getBookmarkCount());
        verify(noticeCommentLikeRepository).deleteAllInBatch(List.of(commentLike));
        verify(noticeLikeRepository).deleteAllInBatch(List.of(like));
        verify(noticeBookmarkRepository).deleteAllInBatch(List.of(bookmark));
        verify(noticeReadStatusRepository).deleteById_UserId("member-1");
    }

    private Notice notice(String id) {
        return notice(id, "<p>상세</p>");
    }

    private Notice notice(String id, String bodyHtml) {
        Notice notice = Notice.create(
                id,
                "공지 제목",
                "공지 RSS 미리보기",
                "https://www.sungkyul.ac.kr/notice/1",
                LocalDateTime.now(),
                "학사",
                "성결대학교",
                "교무처",
                "RSS",
                "rss-hash",
                "detail-hash",
                "content-hash",
                LocalDateTime.now(),
                "상세 본문 텍스트",
                bodyHtml,
                NoticeThumbnailExtractor.extract(bodyHtml),
                List.of()
        );
        ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(notice, "updatedAt", LocalDateTime.now());
        return notice;
    }

    private Member memberWithProfileImage(String id, String photoUrl) {
        Member member = Member.create(id, id + "@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        member.updateProfile(null, null, null, null, photoUrl);
        return member;
    }

    private NoticeSummaryProjection noticeSummary(String id, String thumbnailUrl) {
        return new NoticeSummaryProjection(
                id,
                "공지 제목",
                "공지 RSS 미리보기",
                "학사",
                "성결대학교",
                "교무처",
                LocalDateTime.now(),
                10,
                3,
                2,
                1,
                thumbnailUrl
        );
    }

    private CommentFixture comment(
            String id,
            Notice notice,
            NoticeComment parent,
            String authorId,
            boolean anonymous,
            Integer anonymousOrder
    ) {
        NoticeComment comment = NoticeComment.create(
                notice,
                authorId,
                "작성자",
                "댓글",
                anonymous,
                anonymous ? notice.getId() + ":" + authorId : null,
                anonymousOrder,
                parent
        );
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.now());
        return new CommentFixture(comment);
    }

    private record CommentFixture(NoticeComment comment) {
    }
}
