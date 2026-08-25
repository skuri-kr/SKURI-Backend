package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
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
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitation;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationNotificationStateResolverTest {

    @Mock
    private PartyInvitationRepository partyInvitationRepository;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private ChatRoomInvitationRepository chatRoomInvitationRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private FriendMemberPairLockService pairLockService;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private MemberBlockRepository memberBlockRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private InvitationNotificationStateResolver stateResolver;

    @Test
    void 파티초대알림은회원쌍_파티_초대순으로잠근최신PENDING상태만전달한다() {
        PartyInvitation invitation = partyInvitation();
        Party party = party();
        FriendMemberPair pair = FriendMemberPair.of("inviter-1", "invitee-1");
        when(partyInvitationRepository.findById("party-invitation-1")).thenReturn(Optional.of(invitation));
        when(pairLockService.lockActiveProfileCompletePairIfPresent("inviter-1", "invitee-1"))
                .thenReturn(Optional.of(pair));
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(partyInvitationRepository.findByIdForUpdate("party-invitation-1")).thenReturn(Optional.of(invitation));
        when(friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()))
                .thenReturn(Optional.of(Friendship.create(pair.lowMemberId(), pair.highMemberId())));
        when(memberRepository.findActiveById("inviter-1")).thenReturn(Optional.of(member("inviter-1", "초대자")));
        when(memberRepository.findActiveById("invitee-1")).thenReturn(Optional.of(member("invitee-1", "피초대자")));

        NotificationDispatchRequest dispatch = stateResolver.resolvePartyInvitation("party-invitation-1").orElseThrow();

        InOrder lockOrder = inOrder(pairLockService, partyRepository, partyInvitationRepository);
        lockOrder.verify(pairLockService).lockActiveProfileCompletePairIfPresent("inviter-1", "invitee-1");
        lockOrder.verify(partyRepository).findDetailByIdForUpdate("party-1");
        lockOrder.verify(partyInvitationRepository).findByIdForUpdate("party-invitation-1");
        assertThat(dispatch.type()).isEqualTo(NotificationType.PARTY_INVITATION);
        assertThat(dispatch.data().invitationType()).isEqualTo("PARTY");
    }

    @Test
    void 공개방초대알림은회원쌍_채팅방_초대순으로잠근최신PENDING상태만전달한다() {
        ChatRoomInvitation invitation = chatInvitation();
        ChatRoom room = ChatRoom.create(
                "room-1", "성결대학교 채팅방", ChatRoomType.UNIVERSITY,
                null, null, "inviter-1", true, 10
        );
        room.updateMemberCount(1);
        FriendMemberPair pair = FriendMemberPair.of("inviter-1", "invitee-1");
        when(chatRoomInvitationRepository.findById("chat-invitation-1")).thenReturn(Optional.of(invitation));
        when(pairLockService.lockActiveProfileCompletePairIfPresent("inviter-1", "invitee-1"))
                .thenReturn(Optional.of(pair));
        when(chatRoomRepository.findByIdForUpdate("room-1")).thenReturn(Optional.of(room));
        when(chatRoomInvitationRepository.findByIdForUpdate("chat-invitation-1")).thenReturn(Optional.of(invitation));
        when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("room-1", "invitee-1")).thenReturn(false);
        when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId("room-1", "inviter-1")).thenReturn(true);
        when(friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()))
                .thenReturn(Optional.of(Friendship.create(pair.lowMemberId(), pair.highMemberId())));
        when(memberRepository.findActiveById("inviter-1")).thenReturn(Optional.of(member("inviter-1", "초대자")));
        when(memberRepository.findActiveById("invitee-1")).thenReturn(Optional.of(member("invitee-1", "피초대자")));

        NotificationDispatchRequest dispatch = stateResolver.resolveChatRoomInvitation("chat-invitation-1").orElseThrow();

        InOrder lockOrder = inOrder(pairLockService, chatRoomRepository, chatRoomInvitationRepository);
        lockOrder.verify(pairLockService).lockActiveProfileCompletePairIfPresent("inviter-1", "invitee-1");
        lockOrder.verify(chatRoomRepository).findByIdForUpdate("room-1");
        lockOrder.verify(chatRoomInvitationRepository).findByIdForUpdate("chat-invitation-1");
        assertThat(dispatch.type()).isEqualTo(NotificationType.CHAT_ROOM_INVITATION);
        assertThat(dispatch.data().invitationType()).isEqualTo("CHAT_ROOM");
    }

    @Test
    void 관계가종료되어회원쌍잠금을얻지못하면초대알림을전달하지않는다() {
        PartyInvitation invitation = partyInvitation();
        when(partyInvitationRepository.findById("party-invitation-1")).thenReturn(Optional.of(invitation));
        when(pairLockService.lockActiveProfileCompletePairIfPresent("inviter-1", "invitee-1"))
                .thenReturn(Optional.empty());

        assertThat(stateResolver.resolvePartyInvitation("party-invitation-1")).isEmpty();
    }

    private PartyInvitation partyInvitation() {
        PartyInvitation invitation = PartyInvitation.create("party-1", "inviter-1", "invitee-1");
        ReflectionTestUtils.setField(invitation, "id", "party-invitation-1");
        return invitation;
    }

    private ChatRoomInvitation chatInvitation() {
        ChatRoomInvitation invitation = ChatRoomInvitation.create(
                "room-1", "inviter-1", "invitee-1", LocalDateTime.now()
        );
        ReflectionTestUtils.setField(invitation, "id", "chat-invitation-1");
        return invitation;
    }

    private Party party() {
        Party party = Party.create(
                "inviter-1",
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                4,
                List.of(),
                null
        );
        ReflectionTestUtils.setField(party, "id", "party-1");
        return party;
    }

    private Member member(String memberId, String nickname) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", nickname, LocalDateTime.now());
        member.updateProfile(nickname, null, "20260001", "컴퓨터공학과", null);
        return member;
    }
}
