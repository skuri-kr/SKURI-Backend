package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.academic.service.TimetableService;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.board.service.BoardService;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.friend.service.FriendWithdrawalCleanupService;
import com.skuri.skuri_backend.domain.member.dto.response.MemberWithdrawResponse;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.event.MemberLifecycleEvent;
import com.skuri.skuri_backend.domain.member.exception.MemberWithdrawalNotAllowedException;
import com.skuri.skuri_backend.domain.member.repository.LinkedAccountRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.service.FcmTokenService;
import com.skuri.skuri_backend.domain.notification.service.NotificationService;
import com.skuri.skuri_backend.domain.notice.service.NoticeService;
import com.skuri.skuri_backend.domain.support.service.InquiryService;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberLifecycleServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private LinkedAccountRepository linkedAccountRepository;

    @Mock
    private TaxiPartyService taxiPartyService;

    @Mock
    private ChatService chatService;

    @Mock
    private BoardService boardService;

    @Mock
    private AppNoticeService appNoticeService;

    @Mock
    private NoticeService noticeService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FcmTokenService fcmTokenService;

    @Mock
    private InquiryService inquiryService;

    @Mock
    private TimetableService timetableService;

    @Mock
    private FriendWithdrawalCleanupService friendWithdrawalCleanupService;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberLifecycleService memberLifecycleService;

    @Test
    void withdrawMyAccount_정상탈퇴면_회원스크럽과도메인정리를수행한다() {
        Member member = Member.create("member-1", "member@sungkyul.ac.kr", "홍길동", LocalDateTime.now().minusDays(5));
        when(memberRepository.findActiveByIdForUpdate("member-1")).thenReturn(Optional.of(member));

        MemberWithdrawResponse response = memberLifecycleService.withdrawMyAccount("member-1");

        assertEquals("회원 탈퇴가 완료되었습니다.", response.message());
        assertTrue(member.isWithdrawn());
        assertTrue(member.getEmail().contains("@deleted.skuri.local"));
        verify(taxiPartyService).validateWithdrawalAllowed("member-1");
        verify(friendWithdrawalCleanupService).cleanupWithdrawnMember(
                org.mockito.ArgumentMatchers.eq("member-1"),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
        verify(linkedAccountRepository).deleteByMemberId("member-1");
        verify(taxiPartyService).handleMemberWithdrawal("member-1");
        verify(chatService).removeMemberFromAllChatRooms("member-1");
        verify(boardService).handleMemberWithdrawal("member-1");
        verify(appNoticeService).handleMemberWithdrawal("member-1");
        verify(noticeService).handleMemberWithdrawal("member-1");
        verify(notificationService).deleteAllByUserId("member-1");
        verify(fcmTokenService).deleteAllByUserId("member-1");
        verify(inquiryService).anonymizeWithdrawnMemberInquiries("member-1");
        verify(timetableService).deleteAllByUserId("member-1");
        verify(eventPublisher).publish(new MemberLifecycleEvent.MemberWithdrawn("member-1"));
    }

    @Test
    void withdrawMyAccount_탈퇴불가상태면_후처리없이예외를던진다() {
        Member member = Member.create("member-1", "member@sungkyul.ac.kr", "홍길동", LocalDateTime.now().minusDays(5));
        when(memberRepository.findActiveByIdForUpdate("member-1")).thenReturn(Optional.of(member));
        MemberWithdrawalNotAllowedException exception = new MemberWithdrawalNotAllowedException("정산이 진행 중인 ARRIVED 파티에 참여 중인 멤버는 탈퇴할 수 없습니다.");
        org.mockito.Mockito.doThrow(exception).when(taxiPartyService).validateWithdrawalAllowed("member-1");

        MemberWithdrawalNotAllowedException actual = assertThrows(
                MemberWithdrawalNotAllowedException.class,
                () -> memberLifecycleService.withdrawMyAccount("member-1")
        );

        assertEquals("정산이 진행 중인 ARRIVED 파티에 참여 중인 멤버는 탈퇴할 수 없습니다.", actual.getMessage());
        verify(linkedAccountRepository, never()).deleteByMemberId("member-1");
        verify(taxiPartyService, never()).handleMemberWithdrawal("member-1");
        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }
}
