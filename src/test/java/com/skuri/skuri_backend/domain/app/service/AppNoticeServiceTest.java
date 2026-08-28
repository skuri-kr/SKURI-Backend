package com.skuri.skuri_backend.domain.app.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.domain.app.dto.request.CreateAppNoticeRequest;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeReadResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeUnreadCountResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeReadStatus;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeReadStatusRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeLikeRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentLikeRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.notice.dto.request.CreateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppNoticeServiceTest {

    @Mock
    private AppNoticeRepository appNoticeRepository;

    @Mock
    private AppNoticeReadStatusRepository appNoticeReadStatusRepository;

    @Mock
    private AppNoticeLikeRepository appNoticeLikeRepository;

    @Mock
    private AppNoticeCommentRepository appNoticeCommentRepository;

    @Mock
    private AppNoticeCommentLikeRepository appNoticeCommentLikeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppNoticeService appNoticeService;

    @Test
    void getUnreadCount_게시된공지기준개수를반환한다() {
        when(appNoticeRepository.countPublishedUnread(eq("member-1"), any(LocalDateTime.class))).thenReturn(3L);

        AppNoticeUnreadCountResponse response = appNoticeService.getUnreadCount("member-1");

        assertEquals(3, response.count());
    }

    @Test
    void getPublishedNotice_상세조회마다조회수를증가시키고좋아요상태를합성한다() {
        AppNotice appNotice = appNotice("app-notice-1");
        appNotice.incrementViewCount();
        when(appNoticeRepository.incrementPublishedViewCount(eq("app-notice-1"), any(LocalDateTime.class))).thenReturn(1);
        when(appNoticeRepository.findPublishedById(eq("app-notice-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(appNotice));
        when(appNoticeLikeRepository.existsById_UserIdAndId_AppNoticeId("member-1", "app-notice-1"))
                .thenReturn(true);

        AppNoticeResponse response = appNoticeService.getPublishedNotice("member-1", "app-notice-1");

        assertEquals(1, response.viewCount());
        assertTrue(response.isLiked());
    }

    @Test
    void createAppNotice_HTTP액션URL은거부한다() {
        CreateAppNoticeRequest request = new CreateAppNoticeRequest(
                "제목", "본문", AppNoticeCategory.GENERAL, AppNoticePriority.NORMAL,
                List.of(), "http://example.com", "자세히 보기", LocalDateTime.now()
        );

        assertThrows(BusinessException.class, () -> appNoticeService.createAppNotice(request));
        verify(appNoticeRepository, never()).save(any(AppNotice.class));
    }

    @Test
    void createComment_댓글수를증가시키고알림이벤트를발행한다() {
        AppNotice appNotice = appNotice("app-notice-1");
        Member member = Member.create("member-1", "member-1@sungkyul.ac.kr", "회원", LocalDateTime.now());
        when(appNoticeRepository.findByIdForUpdate("app-notice-1")).thenReturn(Optional.of(appNotice));
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(appNoticeCommentRepository.save(any())).thenAnswer(invocation -> {
            Object comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", "app-comment-1");
            return comment;
        });

        appNoticeService.createComment(
                "member-1", "app-notice-1", new CreateNoticeCommentRequest("댓글", false, null)
        );

        assertEquals(1, appNotice.getCommentCount());
        verify(eventPublisher).publish(new NotificationDomainEvent.AppNoticeCommentCreated("app-comment-1"));
    }

    @Test
    void markRead_기존읽음상태가있으면_기존readAt을그대로반환한다() {
        AppNotice appNotice = appNotice("app-notice-1");
        AppNoticeReadStatus existing = AppNoticeReadStatus.create(
                appNotice,
                "member-1",
                LocalDateTime.of(2026, 3, 20, 9, 30)
        );

        when(appNoticeRepository.findPublishedById(eq("app-notice-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(appNotice));
        when(appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId("member-1", "app-notice-1"))
                .thenReturn(Optional.of(existing));

        AppNoticeReadResponse response = appNoticeService.markRead("member-1", "app-notice-1");

        assertEquals("app-notice-1", response.appNoticeId());
        assertEquals(LocalDateTime.of(2026, 3, 20, 9, 30), response.readAt());
        verify(appNoticeReadStatusRepository, never()).saveAndFlush(any(AppNoticeReadStatus.class));
    }

    @Test
    void markRead_기존상태없으면_새로생성한다() {
        AppNotice appNotice = appNotice("app-notice-1");

        when(appNoticeRepository.findPublishedById(eq("app-notice-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(appNotice));
        when(appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId("member-1", "app-notice-1"))
                .thenReturn(Optional.empty());
        when(appNoticeReadStatusRepository.saveAndFlush(any(AppNoticeReadStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppNoticeReadResponse response = appNoticeService.markRead("member-1", "app-notice-1");

        assertEquals("app-notice-1", response.appNoticeId());
        assertNotNull(response.readAt());
    }

    @Test
    void markRead_동시중복읽음이면_기존상태로복구해성공한다() {
        AppNotice appNotice = appNotice("app-notice-1");
        AppNoticeReadStatus existing = AppNoticeReadStatus.create(
                appNotice,
                "member-1",
                LocalDateTime.of(2026, 3, 21, 8, 0)
        );

        when(appNoticeRepository.findPublishedById(eq("app-notice-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(appNotice));
        when(appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId("member-1", "app-notice-1"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(appNoticeReadStatusRepository.saveAndFlush(any(AppNoticeReadStatus.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        AppNoticeReadResponse response = appNoticeService.markRead("member-1", "app-notice-1");

        assertEquals(LocalDateTime.of(2026, 3, 21, 8, 0), response.readAt());
    }

    @Test
    void deleteAppNotice_읽음상태를먼저정리한다() {
        AppNotice appNotice = appNotice("app-notice-1");
        when(appNoticeRepository.findById("app-notice-1")).thenReturn(Optional.of(appNotice));

        appNoticeService.deleteAppNotice("app-notice-1");

        verify(appNoticeReadStatusRepository).deleteById_AppNoticeId("app-notice-1");
        verify(appNoticeRepository).delete(appNotice);
    }

    @Test
    void deleteAllReadStatusesByUserId_리포지토리에위임한다() {
        appNoticeService.deleteAllReadStatusesByUserId("member-1");

        verify(appNoticeReadStatusRepository).deleteById_UserId("member-1");
    }

    private AppNotice appNotice(String id) {
        AppNotice appNotice = AppNotice.create(
                "앱 공지",
                "내용",
                AppNoticeCategory.MAINTENANCE,
                AppNoticePriority.NORMAL,
                List.of(),
                null,
                LocalDateTime.of(2026, 3, 20, 0, 0)
        );
        ReflectionTestUtils.setField(appNotice, "id", id);
        return appNotice;
    }
}
