package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationUnavailableReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyInvitationServiceTest {

    @Mock private PartyRepository partyRepository;
    @Mock private PartyInvitationRepository partyInvitationRepository;
    @Mock private FriendRelationshipQueryService friendRelationshipQueryService;
    @Mock private FriendMemberPairLockService pairLockService;
    @Mock private PartyInvitationSendItemService sendItemService;
    @Mock private PartyInvitationTransitionService transitionService;

    @InjectMocks private PartyInvitationService service;

    @Test
    void 정원이가득차도_친구목록과참여초대상태를반환한다() {
        Party party = Party.create(
                "leader-1",
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                2,
                List.of(),
                null
        );
        ReflectionTestUtils.setField(party, "id", "party-1");
        party.addMember("member-friend");
        List<FriendRelationshipQueryService.InvitationCandidate> candidates = List.of(
                candidate("member-friend", "public-member", "참여친구"),
                candidate("pending-friend", "public-pending", "초대중친구"),
                candidate("other-party-friend", "public-other", "다른파티친구"),
                candidate("eligible-friend", "public-eligible", "초대가능친구")
        );

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(friendRelationshipQueryService.getInvitationCandidates("leader-1")).thenReturn(candidates);
        when(partyRepository.findActivePartyMemberIds(anySet(), eq(Set.of(
                PartyStatus.OPEN,
                PartyStatus.CLOSED,
                PartyStatus.ARRIVED
        )))).thenReturn(List.of("other-party-friend"));
        when(partyInvitationRepository.findPendingInviteeIds(eq("party-1"), anySet()))
                .thenReturn(List.of("pending-friend"));

        PartyInvitationEligibleFriendsResponse response = service.getEligibleFriends("leader-1", "party-1");

        assertThat(response.canInvite()).isFalse();
        assertThat(response.unavailableReason()).isEqualTo(PartyInvitationUnavailableReason.PARTY_FULL);
        assertThat(response.remainingCapacity()).isZero();
        assertThat(response.friends()).extracting(FriendInvitationCandidateResponse::nickname)
                .containsExactly("초대가능친구");
        assertThat(response.alreadyMemberFriends()).extracting(FriendInvitationCandidateResponse::nickname)
                .containsExactly("참여친구");
        assertThat(response.alreadyPendingFriends()).extracting(FriendInvitationCandidateResponse::nickname)
                .containsExactly("초대중친구");
        assertThat(response.notEligibleCount()).isEqualTo(1);
    }

    @Test
    void 수동모집마감중이고정원여유가있으면_참가자에게초대가능상태를반환한다() {
        Party party = Party.create(
                "leader-1",
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                4,
                List.of(),
                null
        );
        ReflectionTestUtils.setField(party, "id", "party-1");
        party.close();

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(friendRelationshipQueryService.getInvitationCandidates("leader-1"))
                .thenReturn(List.of(candidate("friend-1", "public-friend", "초대친구")));
        when(partyRepository.findActivePartyMemberIds(anySet(), eq(Set.of(
                PartyStatus.OPEN,
                PartyStatus.CLOSED,
                PartyStatus.ARRIVED
        )))).thenReturn(List.of());
        when(partyInvitationRepository.findPendingInviteeIds(eq("party-1"), anySet()))
                .thenReturn(List.of());

        PartyInvitationEligibleFriendsResponse response = service.getEligibleFriends("leader-1", "party-1");

        assertThat(response.canInvite()).isTrue();
        assertThat(response.remainingCapacity()).isEqualTo(3);
        assertThat(response.unavailableReason()).isNull();
    }

    private FriendRelationshipQueryService.InvitationCandidate candidate(
            String memberId,
            String publicId,
            String nickname
    ) {
        return new FriendRelationshipQueryService.InvitationCandidate(
                memberId,
                new FriendInvitationCandidateResponse(
                        publicId,
                        nickname,
                        "컴퓨터공학과",
                        null,
                        false
                )
        );
    }
}
