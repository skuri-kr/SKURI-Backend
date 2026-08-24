package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationExpiryReason;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomInvitationTransitionServiceTest {

    @Mock private ChatRoomInvitationRepository invitationRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private MemberBlockRepository memberBlockRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private FriendMemberPairLockService pairLockService;
    @Mock private ChatService chatService;

    private ChatRoomInvitationTransitionService service;

    @BeforeEach
    void setUp() {
        service = new ChatRoomInvitationTransitionService(
                invitationRepository,
                chatRoomRepository,
                chatRoomMemberRepository,
                friendshipRepository,
                memberBlockRepository,
                memberRepository,
                pairLockService,
                chatService
        );
    }

    @Test
    void 수락시점에_다시검증하고_공개방에_참가시킨다() {
        ChatRoom room = room(3, 1);
        ChatRoomInvitation invitation = invitation(LocalDateTime.now());
        stubAcceptBoundary(room, invitation, invitation);

        ChatRoomInvitationTransitionService.AcceptAttempt result = service.accept("invitee-1", "invite-1");

        assertThat(result.outcome()).isEqualTo(ChatRoomInvitationTransitionService.AcceptOutcome.ACCEPTED);
        assertThat(invitation.getStatus()).isEqualTo(ChatRoomInvitationStatus.ACCEPTED);
        verify(chatService).joinInvitedMemberWithLockedRoom(
                eq(room),
                argThat(member -> member.getId().equals("invitee-1"))
        );
        var lockOrder = inOrder(pairLockService, chatRoomRepository, invitationRepository);
        lockOrder.verify(pairLockService).lockActivePair("inviter-1", "invitee-1");
        lockOrder.verify(chatRoomRepository).findByIdForUpdate("room-1");
        lockOrder.verify(invitationRepository).findByIdForUpdate("invite-1");
        verify(chatRoomRepository, never()).findById("room-1");
    }

    @Test
    void 발송뒤_정원이차면_좌석을예약하지않고_만료한다() {
        ChatRoom room = room(1, 1);
        ChatRoomInvitation invitation = invitation(LocalDateTime.now());
        stubAcceptBoundary(room, invitation, invitation);

        ChatRoomInvitationTransitionService.AcceptAttempt result = service.accept("invitee-1", "invite-1");

        assertThat(result.outcome()).isEqualTo(ChatRoomInvitationTransitionService.AcceptOutcome.EXPIRED);
        assertThat(invitation.getStatus()).isEqualTo(ChatRoomInvitationStatus.EXPIRED);
        assertThat(invitation.getExpiryReason()).isEqualTo(ChatRoomInvitationExpiryReason.CAPACITY_FULL);
        verify(chatService, never()).joinInvitedMemberWithLockedRoom(
                eq(room),
                argThat(member -> member.getId().equals("invitee-1"))
        );
    }

    @Test
    void 잠금전에_종료된_초대는_공개방에_참가시키지않는다() {
        ChatRoom room = room(3, 1);
        ChatRoomInvitation initialSnapshot = invitation(LocalDateTime.now());
        ChatRoomInvitation lockedInvitation = invitation(LocalDateTime.now());
        lockedInvitation.decline(LocalDateTime.now());
        stubAcceptBoundary(room, initialSnapshot, lockedInvitation);

        ChatRoomInvitationTransitionService.AcceptAttempt result = service.accept("invitee-1", "invite-1");

        assertThat(result.outcome()).isEqualTo(ChatRoomInvitationTransitionService.AcceptOutcome.STATE_NOT_ALLOWED);
        verify(chatService, never()).joinInvitedMemberWithLockedRoom(
                eq(room),
                argThat(member -> member.getId().equals("invitee-1"))
        );
        verify(invitationRepository, never()).findById("invite-1");
    }

    @Test
    void 재조정은_잠금전_프로젝션만_읽고_종료된초대를_덮어쓰지않는다() {
        ChatRoom room = room(3, 1);
        ChatRoomInvitation initialSnapshot = invitation(LocalDateTime.now());
        ChatRoomInvitation lockedInvitation = invitation(LocalDateTime.now());
        lockedInvitation.decline(LocalDateTime.now());
        Member inviter = completeMember("inviter-1");
        Member invitee = completeMember("invitee-1");
        FriendMemberPair pair = FriendMemberPair.of("inviter-1", "invitee-1");
        when(invitationRepository.findAcceptanceSnapshotById("invite-1"))
                .thenReturn(Optional.of(acceptanceSnapshot(initialSnapshot)));
        when(chatRoomRepository.existsByIdForInvitationAcceptance("room-1")).thenReturn(true);
        when(pairLockService.lockActivePair("inviter-1", "invitee-1")).thenReturn(pair);
        when(chatRoomRepository.findByIdForUpdate("room-1")).thenReturn(Optional.of(room));
        when(memberRepository.findActiveById("inviter-1")).thenReturn(Optional.of(inviter));
        when(memberRepository.findActiveById("invitee-1")).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByIdForUpdate("invite-1")).thenReturn(Optional.of(lockedInvitation));

        service.reconcile("invite-1");

        assertThat(lockedInvitation.getStatus()).isEqualTo(ChatRoomInvitationStatus.DECLINED);
        verify(invitationRepository, never()).findById("invite-1");
    }

    @Test
    void 칠일이지난_초대는_수락전에_만료한다() {
        ChatRoomInvitation invitation = invitation(LocalDateTime.now().minusDays(8));
        when(invitationRepository.findAcceptanceSnapshotById("invite-1"))
                .thenReturn(Optional.of(acceptanceSnapshot(invitation)));
        when(invitationRepository.findByIdForUpdate("invite-1")).thenReturn(Optional.of(invitation));

        ChatRoomInvitationTransitionService.AcceptAttempt result = service.accept("invitee-1", "invite-1");

        assertThat(result.outcome()).isEqualTo(ChatRoomInvitationTransitionService.AcceptOutcome.EXPIRED);
        assertThat(invitation.getExpiryReason()).isEqualTo(ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT);
        verify(chatRoomRepository, never()).findByIdForUpdate("room-1");
    }

    @Test
    void 칠일이지난_초대를_취소하면_TIMEOUT으로_만료한다() {
        ChatRoomInvitation invitation = invitation(LocalDateTime.now().minusDays(8));
        when(invitationRepository.findByIdForUpdate("invite-1")).thenReturn(Optional.of(invitation));

        boolean canceled = service.cancel("inviter-1", "invite-1");

        assertThat(canceled).isFalse();
        assertThat(invitation.getStatus()).isEqualTo(ChatRoomInvitationStatus.EXPIRED);
        assertThat(invitation.getExpiryReason()).isEqualTo(ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT);
    }

    @Test
    void 수신자는_만료된초대를목록에서지울수있다() {
        ChatRoomInvitation invitation = invitation(LocalDateTime.now());
        invitation.expire(ChatRoomInvitationExpiryReason.TARGET_UNAVAILABLE, LocalDateTime.now());
        when(invitationRepository.findByIdForUpdate("invite-1")).thenReturn(Optional.of(invitation));

        boolean removed = service.cancel("invitee-1", "invite-1");

        assertThat(removed).isTrue();
        assertThat(invitation.getStatus()).isEqualTo(ChatRoomInvitationStatus.DISMISSED);
    }

    @Test
    void 수신자는_재조정되지않은시간만료초대도_목록에서지울수있다() {
        ChatRoomInvitation invitation = invitation(LocalDateTime.now().minusDays(8));
        when(invitationRepository.findByIdForUpdate("invite-1")).thenReturn(Optional.of(invitation));

        boolean removed = service.cancel("invitee-1", "invite-1");

        assertThat(removed).isTrue();
        assertThat(invitation.getStatus()).isEqualTo(ChatRoomInvitationStatus.DISMISSED);
        assertThat(invitation.getExpiryReason()).isEqualTo(ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT);
    }

    private void stubAcceptBoundary(
            ChatRoom room,
            ChatRoomInvitation snapshot,
            ChatRoomInvitation lockedInvitation
    ) {
        Member inviter = completeMember("inviter-1");
        Member invitee = completeMember("invitee-1");
        FriendMemberPair pair = FriendMemberPair.of("inviter-1", "invitee-1");
        when(invitationRepository.findAcceptanceSnapshotById("invite-1"))
                .thenReturn(Optional.of(acceptanceSnapshot(snapshot)));
        when(chatRoomRepository.existsByIdForInvitationAcceptance("room-1")).thenReturn(true);
        when(chatRoomRepository.findByIdForUpdate("room-1")).thenReturn(Optional.of(room));
        when(memberRepository.findActiveById("inviter-1")).thenReturn(Optional.of(inviter));
        when(memberRepository.findActiveById("invitee-1")).thenReturn(Optional.of(invitee));
        when(pairLockService.lockActivePair("inviter-1", "invitee-1")).thenReturn(pair);
        when(invitationRepository.findByIdForUpdate("invite-1")).thenReturn(Optional.of(lockedInvitation));
        lenient().when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("room-1", "inviter-1"))
                .thenReturn(true);
        lenient().when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("room-1", "invitee-1"))
                .thenReturn(false);
        lenient().when(friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()))
                .thenReturn(Optional.of(Friendship.create(pair.lowMemberId(), pair.highMemberId())));
    }

    private ChatRoomInvitationRepository.AcceptanceSnapshot acceptanceSnapshot(
            ChatRoomInvitation invitation
    ) {
        return new ChatRoomInvitationRepository.AcceptanceSnapshot() {
            @Override
            public String getChatRoomId() {
                return invitation.getChatRoomId();
            }

            @Override
            public String getInviterId() {
                return invitation.getInviterId();
            }

            @Override
            public String getInviteeId() {
                return invitation.getInviteeId();
            }

            @Override
            public ChatRoomInvitationStatus getStatus() {
                return invitation.getStatus();
            }

            @Override
            public LocalDateTime getExpiresAt() {
                return invitation.getExpiresAt();
            }
        };
    }

    private ChatRoom room(int maxMembers, int memberCount) {
        ChatRoom room = ChatRoom.create(
                "room-1",
                "성결대학교 채팅방",
                ChatRoomType.UNIVERSITY,
                null,
                null,
                "inviter-1",
                true,
                maxMembers
        );
        room.updateMemberCount(memberCount);
        return room;
    }

    private ChatRoomInvitation invitation(LocalDateTime now) {
        ChatRoomInvitation invitation = ChatRoomInvitation.create("room-1", "inviter-1", "invitee-1", now);
        ReflectionTestUtils.setField(invitation, "id", "invite-1");
        return invitation;
    }

    private Member completeMember(String memberId) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", memberId, LocalDateTime.now());
        member.updateProfile(memberId, memberId, "20201234", "컴퓨터공학과", null);
        return member;
    }
}
