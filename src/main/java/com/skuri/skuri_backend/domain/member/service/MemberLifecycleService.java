package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.academic.service.TimetableService;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.board.service.BoardService;
import com.skuri.skuri_backend.domain.member.dto.response.MemberWithdrawResponse;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.event.MemberLifecycleEvent;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.LinkedAccountRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.service.FcmTokenService;
import com.skuri.skuri_backend.domain.notification.service.NotificationService;
import com.skuri.skuri_backend.domain.notice.service.NoticeService;
import com.skuri.skuri_backend.domain.support.service.InquiryService;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyService;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.friend.service.FriendWithdrawalCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberLifecycleService {

    private static final String WITHDRAW_SUCCESS_MESSAGE = "회원 탈퇴가 완료되었습니다.";

    private final MemberRepository memberRepository;
    private final LinkedAccountRepository linkedAccountRepository;
    private final TaxiPartyService taxiPartyService;
    private final ChatService chatService;
    private final BoardService boardService;
    private final AppNoticeService appNoticeService;
    private final NoticeService noticeService;
    private final NotificationService notificationService;
    private final FcmTokenService fcmTokenService;
    private final InquiryService inquiryService;
    private final TimetableService timetableService;
    private final FriendWithdrawalCleanupService friendWithdrawalCleanupService;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional
    public MemberWithdrawResponse withdrawMyAccount(String memberId) {
        Member member = memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);
        taxiPartyService.validateWithdrawalAllowed(memberId);

        LocalDateTime withdrawnAt = LocalDateTime.now();
        member.withdraw(withdrawnAt);
        friendWithdrawalCleanupService.cleanupWithdrawnMember(memberId, withdrawnAt);
        linkedAccountRepository.deleteByMemberId(memberId);
        taxiPartyService.handleMemberWithdrawal(memberId);
        chatService.removeMemberFromAllChatRooms(memberId);
        boardService.handleMemberWithdrawal(memberId);
        appNoticeService.deleteAllReadStatusesByUserId(memberId);
        noticeService.handleMemberWithdrawal(memberId);
        notificationService.deleteAllByUserId(memberId);
        fcmTokenService.deleteAllByUserId(memberId);
        inquiryService.anonymizeWithdrawnMemberInquiries(memberId);
        timetableService.deleteAllByUserId(memberId);
        eventPublisher.publish(new MemberLifecycleEvent.MemberWithdrawn(memberId));

        return new MemberWithdrawResponse(WITHDRAW_SUCCESS_MESSAGE);
    }
}
