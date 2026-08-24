package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPair;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationOutcome;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitation;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyInvitationSendItemServiceTest {

    @Mock private PartyRepository partyRepository;
    @Mock private PartyInvitationRepository partyInvitationRepository;
    @Mock private FriendProfileRepository friendProfileRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private MemberBlockRepository memberBlockRepository;
    @Mock private FriendMemberPairLockService pairLockService;

    private PartyInvitationSendItemService service;

    @BeforeEach
    void setUp() {
        service = new PartyInvitationSendItemService(
                partyRepository,
                partyInvitationRepository,
                friendProfileRepository,
                friendshipRepository,
                memberBlockRepository,
                pairLockService
        );
    }

    @Test
    void 발송은_회원쌍을잠근뒤_파티와초대를잠근다() {
        String friendPublicId = "friend-public-1";
        FriendMemberPair pair = FriendMemberPair.of("inviter-1", "invitee-1");
        Party party = party();

        when(friendProfileRepository.findMemberIdByPublicId(friendPublicId))
                .thenReturn(Optional.of("invitee-1"));
        when(pairLockService.lockActivePair("inviter-1", "invitee-1")).thenReturn(pair);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()))
                .thenReturn(Optional.of(Friendship.create(pair.lowMemberId(), pair.highMemberId())));
        when(partyRepository.existsActivePartyByMemberId(
                eq("invitee-1"),
                eq(EnumSet.of(PartyStatus.OPEN, PartyStatus.CLOSED, PartyStatus.ARRIVED)),
                isNull()
        )).thenReturn(false);
        when(partyInvitationRepository.findByActiveTargetKeyForUpdate("party-1:invitee-1"))
                .thenReturn(Optional.empty());
        when(partyInvitationRepository.saveAndFlush(any(PartyInvitation.class))).thenAnswer(invocation -> {
            PartyInvitation created = invocation.getArgument(0);
            ReflectionTestUtils.setField(created, "id", "invite-new");
            return created;
        });

        PartyInvitationSendResultResponse result = service.send("inviter-1", "party-1", friendPublicId);

        assertThat(result.outcome()).isEqualTo(PartyInvitationOutcome.SENT);
        var lockOrder = inOrder(pairLockService, partyRepository, partyInvitationRepository);
        lockOrder.verify(pairLockService).lockActivePair("inviter-1", "invitee-1");
        lockOrder.verify(partyRepository).findDetailByIdForUpdate("party-1");
        lockOrder.verify(partyInvitationRepository).findByActiveTargetKeyForUpdate("party-1:invitee-1");
    }

    @Test
    void 존재하지않는친구공개ID는_회원잠금전_초대불가로처리한다() {
        String friendPublicId = "missing-friend-public-id";
        when(friendProfileRepository.findMemberIdByPublicId(friendPublicId)).thenReturn(Optional.empty());

        PartyInvitationSendResultResponse result = service.send("inviter-1", "party-1", friendPublicId);

        assertThat(result.outcome()).isEqualTo(PartyInvitationOutcome.NOT_ELIGIBLE);
        verifyNoInteractions(pairLockService);
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
}
