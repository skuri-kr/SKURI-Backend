package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitation;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyInvitationTransitionServiceTest {

    @Mock private PartyInvitationRepository invitationRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private MemberBlockRepository memberBlockRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private FriendMemberPairLockService pairLockService;
    @Mock private TaxiPartyService taxiPartyService;

    private PartyInvitationTransitionService service;

    @BeforeEach
    void setUp() {
        service = new PartyInvitationTransitionService(
                invitationRepository,
                partyRepository,
                friendshipRepository,
                memberBlockRepository,
                memberRepository,
                pairLockService,
                taxiPartyService
        );
    }

    @Test
    void 수락시점에_다시검증하고_파티참가를_처리한다() {
        Party party = party("party-1", 3);
        PartyInvitation invitation = invitation("invite-1");
        stubAcceptBoundary(party, invitation, invitation);
        when(partyRepository.existsActivePartyByMemberId("invitee-1", PartyInvitationTransitionServiceTestHelper.ACTIVE_STATUSES, "party-1"))
                .thenReturn(false);

        PartyInvitationTransitionService.AcceptAttempt result = service.accept("invitee-1", "invite-1");

        assertThat(result.outcome()).isEqualTo(PartyInvitationTransitionService.AcceptOutcome.ACCEPTED);
        assertThat(invitation.getStatus()).isEqualTo(PartyInvitationStatus.ACCEPTED);
        assertThat(invitation.getActiveTargetKey()).isNull();
        verify(taxiPartyService).acceptInvitedMemberWithLockedParty(party, "invitee-1", "inviter-1");
        var lockOrder = inOrder(pairLockService, partyRepository, invitationRepository);
        lockOrder.verify(pairLockService).lockActivePair("inviter-1", "invitee-1");
        lockOrder.verify(partyRepository).findDetailByIdForUpdate("party-1");
        lockOrder.verify(invitationRepository).findByIdForUpdate("invite-1");
    }

    @Test
    void 발송뒤_정원이차면_좌석을예약하지않고_만료한다() {
        Party party = party("party-1", 1);
        PartyInvitation invitation = invitation("invite-1");
        stubAcceptBoundary(party, invitation, invitation);

        PartyInvitationTransitionService.AcceptAttempt result = service.accept("invitee-1", "invite-1");

        assertThat(result.outcome()).isEqualTo(PartyInvitationTransitionService.AcceptOutcome.EXPIRED);
        assertThat(invitation.getStatus()).isEqualTo(PartyInvitationStatus.EXPIRED);
        assertThat(invitation.getExpiryReason()).isEqualTo(PartyInvitationExpiryReason.CAPACITY_FULL);
        verify(taxiPartyService, never()).acceptInvitedMemberWithLockedParty(party, "invitee-1", "inviter-1");
    }

    @Test
    void 잠금전에_종료된_초대는_파티에_참가시키지않는다() {
        Party party = party("party-1", 3);
        PartyInvitation initialSnapshot = invitation("invite-1");
        PartyInvitation lockedInvitation = invitation("invite-1");
        lockedInvitation.cancel(LocalDateTime.now());
        stubAcceptBoundary(party, initialSnapshot, lockedInvitation);

        PartyInvitationTransitionService.AcceptAttempt result = service.accept("invitee-1", "invite-1");

        assertThat(result.outcome()).isEqualTo(PartyInvitationTransitionService.AcceptOutcome.STATE_NOT_ALLOWED);
        verify(taxiPartyService, never()).acceptInvitedMemberWithLockedParty(party, "invitee-1", "inviter-1");
        verify(invitationRepository, never()).findById("invite-1");
    }

    private void stubAcceptBoundary(
            Party party,
            PartyInvitation snapshot,
            PartyInvitation lockedInvitation
    ) {
        Member inviter = completeMember("inviter-1");
        Member invitee = completeMember("invitee-1");
        FriendMemberPair pair = FriendMemberPair.of("inviter-1", "invitee-1");
        when(invitationRepository.findAcceptanceSnapshotById("invite-1"))
                .thenReturn(Optional.of(acceptanceSnapshot(snapshot)));
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveById("inviter-1")).thenReturn(Optional.of(inviter));
        when(memberRepository.findActiveById("invitee-1")).thenReturn(Optional.of(invitee));
        when(pairLockService.lockActivePair("inviter-1", "invitee-1")).thenReturn(pair);
        when(invitationRepository.findByIdForUpdate("invite-1")).thenReturn(Optional.of(lockedInvitation));
        lenient().when(friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()))
                .thenReturn(Optional.of(Friendship.create(pair.lowMemberId(), pair.highMemberId())));
    }

    private PartyInvitationRepository.AcceptanceSnapshot acceptanceSnapshot(PartyInvitation invitation) {
        return new PartyInvitationRepository.AcceptanceSnapshot() {
            @Override
            public String getPartyId() {
                return invitation.getPartyId();
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
            public PartyInvitationStatus getStatus() {
                return invitation.getStatus();
            }
        };
    }

    private Party party(String partyId, int maxMembers) {
        Party party = Party.create(
                "inviter-1",
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                maxMembers,
                List.of(),
                null
        );
        ReflectionTestUtils.setField(party, "id", partyId);
        return party;
    }

    private PartyInvitation invitation(String invitationId) {
        PartyInvitation invitation = PartyInvitation.create("party-1", "inviter-1", "invitee-1");
        ReflectionTestUtils.setField(invitation, "id", invitationId);
        return invitation;
    }

    private Member completeMember(String memberId) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", memberId, LocalDateTime.now());
        member.updateProfile(memberId, memberId, "20201234", "컴퓨터공학과", null);
        return member;
    }

    private static final class PartyInvitationTransitionServiceTestHelper {
        private static final java.util.Set<com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus> ACTIVE_STATUSES =
                java.util.EnumSet.of(
                        com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus.OPEN,
                        com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus.CLOSED,
                        com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus.ARRIVED
                );
    }
}
