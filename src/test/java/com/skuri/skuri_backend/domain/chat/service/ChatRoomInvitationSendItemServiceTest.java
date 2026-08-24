package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationOutcome;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationExpiryReason;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomInvitationSendItemServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ChatRoomInvitationRepository invitationRepository;
    @Mock private FriendProfileRepository friendProfileRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private MemberBlockRepository memberBlockRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private FriendMemberPairLockService pairLockService;
    @Mock private AfterCommitApplicationEventPublisher eventPublisher;

    private ChatRoomInvitationSendItemService service;

    @BeforeEach
    void setUp() {
        service = new ChatRoomInvitationSendItemService(
                chatRoomRepository,
                chatRoomMemberRepository,
                invitationRepository,
                friendProfileRepository,
                friendshipRepository,
                memberBlockRepository,
                memberRepository,
                pairLockService,
                eventPublisher
        );
    }

    @Test
    void 만료시간이지난_PENDING은_만료한뒤_새초대를발송한다() {
        String friendPublicId = "friend-public-1";
        FriendMemberPair pair = FriendMemberPair.of("inviter-1", "invitee-1");
        ChatRoom room = room();
        ChatRoomInvitation expiredPending = ChatRoomInvitation.create(
                "room-1",
                "other-inviter",
                "invitee-1",
                LocalDateTime.now().minusDays(8)
        );

        when(friendProfileRepository.findMemberIdByPublicId(friendPublicId))
                .thenReturn(Optional.of("invitee-1"));
        when(pairLockService.lockActivePair("inviter-1", "invitee-1")).thenReturn(pair);
        when(chatRoomRepository.findByIdForUpdate("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("room-1", "inviter-1"))
                .thenReturn(true);
        when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("room-1", "invitee-1"))
                .thenReturn(false);
        when(memberRepository.findActiveById("invitee-1")).thenReturn(Optional.of(completeMember("invitee-1")));
        when(friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()))
                .thenReturn(Optional.of(Friendship.create(pair.lowMemberId(), pair.highMemberId())));
        when(invitationRepository.findByActiveTargetKeyForUpdate("room-1:invitee-1"))
                .thenReturn(Optional.of(expiredPending));
        when(invitationRepository.saveAndFlush(any(ChatRoomInvitation.class))).thenAnswer(invocation -> {
            ChatRoomInvitation created = invocation.getArgument(0);
            ReflectionTestUtils.setField(created, "id", "invite-new");
            return created;
        });

        ChatRoomInvitationSendResultResponse result = service.send("inviter-1", "room-1", friendPublicId);

        assertThat(result.outcome()).isEqualTo(ChatRoomInvitationOutcome.SENT);
        assertThat(result.invitationId()).isEqualTo("invite-new");
        assertThat(expiredPending.getStatus()).isEqualTo(ChatRoomInvitationStatus.EXPIRED);
        assertThat(expiredPending.getExpiryReason()).isEqualTo(ChatRoomInvitationExpiryReason.INVITATION_TIMEOUT);
        verify(invitationRepository).flush();
        var lockOrder = inOrder(pairLockService, chatRoomRepository, invitationRepository);
        lockOrder.verify(pairLockService).lockActivePair("inviter-1", "invitee-1");
        lockOrder.verify(chatRoomRepository).findByIdForUpdate("room-1");
        lockOrder.verify(invitationRepository).findByActiveTargetKeyForUpdate("room-1:invitee-1");
        verify(eventPublisher).publish(new NotificationDomainEvent.ChatRoomInvitationCreated("invite-new"));
    }

    @Test
    void 존재하지않는친구공개ID는_회원잠금전_초대불가로처리한다() {
        String friendPublicId = "missing-friend-public-id";
        when(friendProfileRepository.findMemberIdByPublicId(friendPublicId)).thenReturn(Optional.empty());

        ChatRoomInvitationSendResultResponse result = service.send("inviter-1", "room-1", friendPublicId);

        assertThat(result.outcome()).isEqualTo(ChatRoomInvitationOutcome.NOT_ELIGIBLE);
        verifyNoInteractions(pairLockService);
    }

    @Test
    void 친구관계가없으면이미채팅방참가중이어도초대불가로마스킹한다() {
        String friendPublicId = "former-friend-public-id";
        FriendMemberPair pair = FriendMemberPair.of("inviter-1", "invitee-1");
        ChatRoom room = room();

        when(friendProfileRepository.findMemberIdByPublicId(friendPublicId))
                .thenReturn(Optional.of("invitee-1"));
        when(pairLockService.lockActivePair("inviter-1", "invitee-1")).thenReturn(pair);
        when(chatRoomRepository.findByIdForUpdate("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("room-1", "inviter-1"))
                .thenReturn(true);
        when(memberRepository.findActiveById("invitee-1")).thenReturn(Optional.of(completeMember("invitee-1")));
        when(friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()))
                .thenReturn(Optional.empty());

        ChatRoomInvitationSendResultResponse result = service.send("inviter-1", "room-1", friendPublicId);

        assertThat(result.outcome()).isEqualTo(ChatRoomInvitationOutcome.NOT_ELIGIBLE);
    }

    private ChatRoom room() {
        ChatRoom room = ChatRoom.create(
                "room-1",
                "성결대학교 채팅방",
                ChatRoomType.UNIVERSITY,
                null,
                null,
                "inviter-1",
                true,
                10
        );
        room.updateMemberCount(1);
        return room;
    }

    private Member completeMember(String memberId) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", memberId, LocalDateTime.now());
        member.updateProfile(memberId, memberId, "20201234", "컴퓨터공학과", null);
        return member;
    }
}
