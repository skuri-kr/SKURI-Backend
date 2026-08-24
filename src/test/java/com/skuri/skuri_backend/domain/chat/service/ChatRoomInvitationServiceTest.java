package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationReceivedResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService.InvitationCandidate;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomInvitationServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatRoomInvitationRepository invitationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FriendRelationshipQueryService friendRelationshipQueryService;

    @Mock
    private FriendMemberPairLockService pairLockService;

    @Mock
    private ChatRoomInvitationSendItemService sendItemService;

    @Mock
    private ChatRoomInvitationTransitionService transitionService;

    @Mock
    private ChatRoomInvitationExpirationService expirationService;

    @InjectMocks
    private ChatRoomInvitationService invitationService;

    @Test
    void 학과방초대목록은_다른학과친구를숨기고_참여초대중친구를분리한다() {
        ChatRoom room = ChatRoom.create(
                "public:department:computer",
                "컴퓨터공학과 채팅방",
                ChatRoomType.DEPARTMENT,
                "컴퓨터공학과",
                null,
                null,
                true,
                null
        );
        List<InvitationCandidate> candidates = List.of(
                candidate("member-friend", "public-member", "참여친구", "컴퓨터공학과"),
                candidate("pending-friend", "public-pending", "초대중친구", "컴퓨터공학과"),
                candidate("eligible-friend", "public-eligible", "초대가능친구", "컴퓨터공학과"),
                candidate("other-department", "public-other", "다른학과친구", "법학과")
        );

        when(chatRoomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId(room.getId(), "inviter-1"))
                .thenReturn(true);
        when(friendRelationshipQueryService.getInvitationCandidates("inviter-1")).thenReturn(candidates);
        when(chatRoomMemberRepository.findMemberIdsByChatRoomIdAndCandidateMemberIds(eq(room.getId()), any()))
                .thenReturn(List.of("member-friend"));
        when(invitationRepository.findPendingInviteeIds(eq(room.getId()), any(), any()))
                .thenReturn(List.of("pending-friend"));

        ChatRoomInvitationEligibleFriendsResponse response = invitationService.getEligibleFriends(
                "inviter-1",
                room.getId()
        );

        assertThat(response.sameDepartmentOnly()).isTrue();
        assertThat(response.friends()).extracting(FriendInvitationCandidateResponse::nickname)
                .containsExactly("초대가능친구");
        assertThat(response.alreadyMemberFriends()).extracting(FriendInvitationCandidateResponse::nickname)
                .containsExactly("참여친구");
        assertThat(response.alreadyPendingFriends()).extracting(FriendInvitationCandidateResponse::nickname)
                .containsExactly("초대중친구");
        assertThat(response.notEligibleCount()).isEqualTo(1);
    }

    @Test
    void getReceived_현재학과가다른미참여학과방은_대상을숨긴다() {
        List<ChatRoomInvitationReceivedResponse> response = getReceived("컴퓨터공학과", List.of());

        assertNull(response.getFirst().target());
    }

    @Test
    void getReceived_현재학과와같은학과방은_대상을보여준다() {
        List<ChatRoomInvitationReceivedResponse> response = getReceived("법학과", List.of());

        assertNotNull(response.getFirst().target());
    }

    @Test
    void getReceived_이미참여한학과방은_현재학과가달라도대상을보여준다() {
        List<ChatRoomInvitationReceivedResponse> response = getReceived(
                "컴퓨터공학과",
                List.of("public:department:law")
        );

        assertNotNull(response.getFirst().target());
    }

    private List<ChatRoomInvitationReceivedResponse> getReceived(
            String inviteeDepartment,
            List<String> joinedChatRoomIds
    ) {
        Member invitee = Member.create(
                "invitee-1",
                "invitee@sungkyul.ac.kr",
                "초대받은사람",
                LocalDateTime.now()
        );
        invitee.updateProfile("초대받은사람", "초대받은사람", "20260001", inviteeDepartment, null);
        ChatRoom room = ChatRoom.create(
                "public:department:law",
                "법학과 채팅방",
                ChatRoomType.DEPARTMENT,
                "법학과",
                null,
                null,
                true,
                null
        );
        ChatRoomInvitation invitation = ChatRoomInvitation.create(
                room.getId(),
                "inviter-1",
                invitee.getId(),
                LocalDateTime.now()
        );

        when(memberRepository.findActiveById(invitee.getId())).thenReturn(Optional.of(invitee));
        when(invitationRepository.findTimedOutPendingReceivedIds(eq(invitee.getId()), any(), any()))
                .thenReturn(List.of());
        when(invitationRepository.findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
                invitee.getId(),
                List.of(ChatRoomInvitationStatus.PENDING)
        )).thenReturn(List.of());
        when(invitationRepository.findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
                invitee.getId(),
                List.of(ChatRoomInvitationStatus.PENDING, ChatRoomInvitationStatus.EXPIRED)
        )).thenReturn(List.of(invitation));
        when(chatRoomRepository.findAllById(any())).thenReturn(List.of(room));
        when(friendRelationshipQueryService.findInvitationCandidatesByMemberIds(eq(invitee.getId()), any()))
                .thenReturn(Map.of());
        when(chatRoomMemberRepository.findChatRoomIdsByMemberId(invitee.getId()))
                .thenReturn(joinedChatRoomIds);

        return invitationService.getReceived(invitee.getId());
    }

    private InvitationCandidate candidate(
            String memberId,
            String publicId,
            String nickname,
            String department
    ) {
        return new InvitationCandidate(
                memberId,
                new FriendInvitationCandidateResponse(publicId, nickname, department, null, false)
        );
    }
}
