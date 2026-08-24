package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.ArrivePartyRequest;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.CreatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.PartyLocationRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.UpdatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestAcceptResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyCreateResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyParticipantSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementConfirmResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryRole;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryStatus;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistorySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyEndReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementAccountSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementTargetSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxiPartyServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PartyTagRepository partyTagRepository;

    @Mock
    private PartySseService partySseService;

    @Mock
    private JoinRequestSseService joinRequestSseService;

    @Mock
    private ChatService chatService;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @Mock
    private PartyInvitationLifecycleService partyInvitationLifecycleService;

    @InjectMocks
    private TaxiPartyService taxiPartyService;

    @Test
    void createParty_정상생성() {
        CreatePartyRequest request = createPartyRequest(4);
        when(memberRepository.findActiveByIdForUpdate("leader")).thenReturn(Optional.of(member("leader")));
        when(partyRepository.existsActivePartyByMemberId(eq("leader"), anySet(), isNull())).thenReturn(false);
        when(partyRepository.save(any(Party.class))).thenAnswer(invocation -> {
            Party saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "party-created");
            return saved;
        });

        PartyCreateResponse response = taxiPartyService.createParty("leader", request);

        assertEquals("party-created", response.id());
        assertEquals("party:party-created", response.chatRoomId());
        verify(chatService).createPartyChatRoom(any(Party.class));
        verify(partySseService).publishPartyCreated(any(Party.class), eq(null));
    }

    @Test
    void getParties_참가자요약과프로필사진을포함한다() {
        Party summaryParty = sampleParty("party-1", "leader", 4, "member-1", "member-2");
        Party detailedParty = sampleParty("party-1", "leader", 4, "member-1", "member-2");
        when(partyRepository.search(null, null, null, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(summaryParty), PageRequest.of(0, 20), 1));
        when(partyRepository.findDetailsByIds(List.of("party-1"))).thenReturn(List.of(detailedParty));
        when(partyTagRepository.findTagSummariesByPartyIds(List.of("party-1"))).thenReturn(List.of(
                tagSummary("party-1", "빠른출발")
        ));
        when(memberRepository.findAllById(List.of("leader", "member-1", "member-2"))).thenReturn(List.of(
                member("leader", "리더", "https://cdn.skuri.app/uploads/profiles/leader.jpg"),
                member("member-1", "김민수", null),
                member("member-2", "박서연", "https://cdn.skuri.app/uploads/profiles/member-2.jpg")
        ));

        PageResponse<PartySummaryResponse> response = taxiPartyService.getParties(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        PartySummaryResponse summary = response.getContent().getFirst();
        assertEquals("https://cdn.skuri.app/uploads/profiles/leader.jpg", summary.leaderPhotoUrl());
        assertEquals(3, summary.participantSummaries().size());
        assertEquals("leader", summary.participantSummaries().get(0).id());
        assertTrue(summary.participantSummaries().get(0).isLeader());
        assertEquals("member-1", summary.participantSummaries().get(1).id());
        assertEquals(null, summary.participantSummaries().get(1).photoUrl());
        assertEquals("member-2", summary.participantSummaries().get(2).id());
        assertEquals("https://cdn.skuri.app/uploads/profiles/member-2.jpg", summary.participantSummaries().get(2).photoUrl());
        assertEquals(List.of("빠른출발"), summary.tags());
    }

    @Test
    void createParty_활성파티가있으면_실패() {
        when(memberRepository.findActiveByIdForUpdate("leader")).thenReturn(Optional.of(member("leader")));
        when(partyRepository.existsActivePartyByMemberId(eq("leader"), anySet(), isNull())).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.createParty("leader", createPartyRequest(4))
        );

        assertEquals(ErrorCode.ALREADY_IN_PARTY, exception.getErrorCode());
        verify(partyRepository, never()).save(any(Party.class));
    }

    @Test
    void closeParty_리더가OPEN파티를마감하면CLOSED() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartyStatusResponse response = taxiPartyService.closeParty("leader", "party-1");

        assertEquals(PartyStatus.CLOSED, response.status());
        assertEquals(PartyStatus.CLOSED, party.getStatus());
        verify(chatService).createPartySystemMessage(party, "leader", "모집이 마감되었어요.");
        verify(partySseService).publishPartyStatusChanged(party);
    }

    @Test
    void reopenParty_리더가CLOSED파티를재개하면OPEN과시스템메시지를생성한다() {
        Party party = sampleParty("party-1", "leader", 4, true);
        party.close();
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartyStatusResponse response = taxiPartyService.reopenParty("leader", "party-1");

        assertEquals(PartyStatus.OPEN, response.status());
        assertEquals(PartyStatus.OPEN, party.getStatus());
        verify(chatService).createPartySystemMessage(party, "leader", "모집이 재개되었어요.");
        verify(partySseService).publishPartyStatusChanged(party);
    }

    @Test
    void closeParty_리더가아니면_NOT_PARTY_LEADER() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.closeParty("not-leader", "party-1")
        );

        assertEquals(ErrorCode.NOT_PARTY_LEADER, exception.getErrorCode());
        verify(partyRepository, never()).saveAndFlush(any(Party.class));
    }

    @Test
    void updateParty_OPEN에서_출발시간상세수정성공() {
        Party party = sampleParty("party-1", "leader", 4, true);
        LocalDateTime changed = LocalDateTime.now().plusHours(2);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findAllById(any())).thenReturn(List.of());

        PartyDetailResponse response = taxiPartyService.updateParty(
                "leader",
                "party-1",
                new UpdatePartyRequest(changed, "변경 상세")
        );

        assertEquals(changed, response.departureTime());
        assertEquals("변경 상세", response.detail());
        assertEquals(PartyStatus.OPEN, response.status());
        verify(partySseService).publishPartyUpdated(party, null);
    }

    @Test
    void updateParty_CLOSED에서_시간수정해도상태유지() {
        Party party = sampleParty("party-1", "leader", 4, true);
        party.close();
        LocalDateTime changed = LocalDateTime.now().plusHours(3);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findAllById(any())).thenReturn(List.of());

        PartyDetailResponse response = taxiPartyService.updateParty(
                "leader",
                "party-1",
                new UpdatePartyRequest(changed, null)
        );

        assertEquals(changed, response.departureTime());
        assertEquals(PartyStatus.CLOSED, response.status());
        assertEquals(PartyStatus.CLOSED, party.getStatus());
    }

    @Test
    void updateParty_detail만수정해도_성공() {
        Party party = sampleParty("party-1", "leader", 4, true);
        LocalDateTime originalTime = party.getDepartureTime();
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findAllById(any())).thenReturn(List.of());

        PartyDetailResponse response = taxiPartyService.updateParty(
                "leader",
                "party-1",
                new UpdatePartyRequest(null, "상세만 변경")
        );

        assertEquals(originalTime, response.departureTime());
        assertEquals("상세만 변경", response.detail());
        assertEquals("상세만 변경", party.getDetail());
    }

    @Test
    void updateParty_리더가아니면_NOT_PARTY_LEADER() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("not-leader", "party-1", new UpdatePartyRequest(LocalDateTime.now().plusHours(2), null))
        );

        assertEquals(ErrorCode.NOT_PARTY_LEADER, exception.getErrorCode());
    }

    @Test
    void updateParty_ARRIVED상태면_INVALID_PARTY_STATE_TRANSITION() {
        Party party = sampleParty("party-1", "leader", 4, true);
        arrive(party);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("leader", "party-1", new UpdatePartyRequest(LocalDateTime.now().plusHours(2), null))
        );

        assertEquals(ErrorCode.INVALID_PARTY_STATE_TRANSITION, exception.getErrorCode());
    }

    @Test
    void updateParty_ENDED상태면_INVALID_PARTY_STATE_TRANSITION() {
        Party party = sampleParty("party-1", "leader", 4, true);
        party.cancel();
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("leader", "party-1", new UpdatePartyRequest(LocalDateTime.now().plusHours(2), null))
        );

        assertEquals(ErrorCode.INVALID_PARTY_STATE_TRANSITION, exception.getErrorCode());
    }

    @Test
    void updateParty_수정필드없으면_VALIDATION_ERROR() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("leader", "party-1", new UpdatePartyRequest(null, null))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void updateParty_낙관적락충돌이면_PARTY_CONCURRENT_MODIFICATION() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Party.class, "party-1"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.updateParty("leader", "party-1", new UpdatePartyRequest(LocalDateTime.now().plusHours(2), "수정"))
        );

        assertEquals(ErrorCode.PARTY_CONCURRENT_MODIFICATION, exception.getErrorCode());
    }

    @Test
    void arriveParty_정산대상이없으면_NO_MEMBERS_TO_SETTLE() {
        Party party = sampleParty("party-1", "leader", 4, false);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.arriveParty("leader", "party-1", arriveRequest(12000, List.of()))
        );

        assertEquals(ErrorCode.NO_MEMBERS_TO_SETTLE, exception.getErrorCode());
    }

    @Test
    void arriveParty_정상처리시_정산요약과채팅메시지를생성한다() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findAllById(any())).thenReturn(List.of(member("leader"), member("member-1", "홍길동")));

        PartyDetailResponse response = taxiPartyService.arriveParty(
                "leader",
                "party-1",
                arriveRequest(14000, List.of("member-1"))
        );

        assertEquals(PartyStatus.ARRIVED, response.status());
        assertEquals(14000, response.settlement().taxiFare());
        assertEquals(2, response.settlement().splitMemberCount());
        assertEquals(7000, response.settlement().perPersonAmount());
        assertEquals(List.of("member-1"), response.settlement().settlementTargetMemberIds());
        assertEquals("홍길동", response.settlement().memberSettlements().get(0).displayName());
        assertFalse(response.settlement().memberSettlements().get(0).leftParty());
        assertEquals("홍*동", response.settlement().account().accountHolder());
        verify(chatService).createPartyArrivalMessage(party, "leader");
        verify(partySseService).publishPartyStatusChanged(party);
    }

    @Test
    void arriveParty_공백이포함된정산대상ID도_정규화해저장한다() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findAllById(any())).thenReturn(List.of(member("leader"), member("member-1", "홍길동")));

        PartyDetailResponse response = taxiPartyService.arriveParty(
                "leader",
                "party-1",
                arriveRequest(14000, List.of(" member-1 "))
        );

        assertEquals(List.of("member-1"), response.settlement().settlementTargetMemberIds());
        assertEquals("member-1", response.settlement().memberSettlements().getFirst().memberId());

        SettlementConfirmResponse confirmResponse = taxiPartyService.confirmSettlement("leader", "party-1", "member-1");

        assertTrue(confirmResponse.settled());
        assertEquals("member-1", party.getSettlementItems().iterator().next().getMemberId());
    }

    @Test
    void createJoinRequest_정상생성() {
        Party party = sampleParty("party-1", "leader", 4, false);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(member("requester-1")));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), isNull())).thenReturn(false);
        when(joinRequestRepository.existsByParty_IdAndRequesterIdAndStatus("party-1", "requester-1", JoinRequestStatus.PENDING))
                .thenReturn(false);
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> {
            JoinRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "request-1");
            return saved;
        });

        JoinRequestResponse response = taxiPartyService.createJoinRequest("requester-1", "party-1");

        assertEquals("request-1", response.id());
        assertEquals(JoinRequestStatus.PENDING, response.status());
        verify(joinRequestSseService).publishJoinRequestCreated(any(JoinRequest.class));
    }

    @Test
    void createJoinRequest_중복요청이면_ALREADY_REQUESTED() {
        Party party = sampleParty("party-1", "leader", 4, false);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(member("requester-1")));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), isNull())).thenReturn(false);
        when(joinRequestRepository.existsByParty_IdAndRequesterIdAndStatus("party-1", "requester-1", JoinRequestStatus.PENDING))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.createJoinRequest("requester-1", "party-1")
        );

        assertEquals(ErrorCode.ALREADY_REQUESTED, exception.getErrorCode());
    }

    @Test
    void createJoinRequest_이전취소이력이있어도_재요청가능() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest canceled = JoinRequest.create(party, "requester-1");
        canceled.cancel();

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(member("requester-1")));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), isNull())).thenReturn(false);
        when(joinRequestRepository.existsByParty_IdAndRequesterIdAndStatus("party-1", "requester-1", JoinRequestStatus.PENDING))
                .thenReturn(false);
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> {
            JoinRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "request-2");
            return saved;
        });

        JoinRequestResponse response = taxiPartyService.createJoinRequest("requester-1", "party-1");

        assertEquals("request-2", response.id());
        assertEquals(JoinRequestStatus.PENDING, response.status());
        assertEquals(JoinRequestStatus.CANCELED, canceled.getStatus());
    }

    @Test
    void acceptJoinRequest_정원도달시_자동CLOSED() {
        Party party = sampleParty("party-1", "leader", 2, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");
        Member requester = member("requester-1");

        stubTransitionRequest("request-1", joinRequest);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(requester));
        when(memberRepository.findById("requester-1")).thenReturn(Optional.of(requester));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), eq("party-1"))).thenReturn(false);
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestAcceptResponse response = taxiPartyService.acceptJoinRequest("leader", "request-1");

        assertEquals(JoinRequestStatus.ACCEPTED, response.status());
        assertEquals("party-1", response.partyId());
        assertEquals(2, party.getCurrentMembers());
        assertEquals(PartyStatus.CLOSED, party.getStatus());
        assertTrue(party.isMember("requester-1"));
        InOrder chatInOrder = inOrder(chatService);
        chatInOrder.verify(chatService).syncPartyChatRoomMembers(party);
        chatInOrder.verify(chatService).createPartyMemberJoinSystemMessage(party, "leader", "스쿠리 유저님이 입장했어요.");
        chatInOrder.verify(chatService).createPartySystemMessage(party, "leader", "모집이 마감되었어요.");
        verify(partySseService).publishPartyMemberJoined(party, "requester-1", "스쿠리 유저", party.getMemberIds());
        verify(joinRequestSseService).publishJoinRequestUpdated(joinRequest, JoinRequestStatus.PENDING);
        verify(partySseService).publishPartyStatusChanged(party);
    }

    @Test
    void acceptJoinRequest_정원미도달이면_합류SYSTEM메시지만생성한다() {
        Party party = sampleParty("party-1", "leader", 3, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");
        Member requester = member("requester-1");

        stubTransitionRequest("request-1", joinRequest);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(requester));
        when(memberRepository.findById("requester-1")).thenReturn(Optional.of(requester));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), eq("party-1"))).thenReturn(false);
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestAcceptResponse response = taxiPartyService.acceptJoinRequest("leader", "request-1");

        assertEquals(JoinRequestStatus.ACCEPTED, response.status());
        assertEquals(2, party.getCurrentMembers());
        assertEquals(PartyStatus.OPEN, party.getStatus());
        InOrder aggregateLockOrder = inOrder(memberRepository, partyRepository);
        aggregateLockOrder.verify(memberRepository).findActiveByIdForUpdate("requester-1");
        aggregateLockOrder.verify(partyRepository).findDetailByIdForUpdate("party-1");
        verify(chatService).createPartyMemberJoinSystemMessage(party, "leader", "스쿠리 유저님이 입장했어요.");
        verify(chatService, never()).createPartySystemMessage(party, "leader", "모집이 마감되었어요.");
        verify(partySseService, never()).publishPartyStatusChanged(party);
        verify(partyInvitationLifecycleService).expirePendingForInviteeInParty(
                "party-1",
                "requester-1",
                PartyInvitationExpiryReason.ALREADY_JOINED
        );
    }

    @Test
    void 초대수락으로참가하면_같은파티의대기중참가요청을취소한다() {
        Party party = sampleParty("party-1", "leader", 3, false);
        JoinRequest pendingRequest = JoinRequest.create(party, "invitee-1");
        ReflectionTestUtils.setField(pendingRequest, "id", "request-1");
        Member invitee = member("invitee-1");

        when(partyRepository.existsActivePartyByMemberId(eq("invitee-1"), anySet(), eq("party-1"))).thenReturn(false);
        when(joinRequestRepository.findPendingByPartyIdAndRequesterIdForUpdate("party-1", "invitee-1"))
                .thenReturn(List.of(pendingRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findById("invitee-1")).thenReturn(Optional.of(invitee));

        taxiPartyService.acceptInvitedMemberWithLockedParty(party, "invitee-1", "leader");

        assertEquals(JoinRequestStatus.CANCELED, pendingRequest.getStatus());
        assertTrue(party.isMember("invitee-1"));
        verify(joinRequestSseService).publishJoinRequestUpdated(pendingRequest, JoinRequestStatus.PENDING);
    }

    @Test
    void acceptJoinRequest_닉네임이비어있으면_합류fallback시스템메시지를생성한다() {
        Party party = sampleParty("party-1", "leader", 3, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");
        Member requester = member("requester-1", "   ");

        stubTransitionRequest("request-1", joinRequest);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(requester));
        when(memberRepository.findById("requester-1")).thenReturn(Optional.of(requester));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), eq("party-1"))).thenReturn(false);
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taxiPartyService.acceptJoinRequest("leader", "request-1");

        verify(chatService).createPartyMemberJoinSystemMessage(party, "leader", "새 멤버가 입장했어요.");
    }

    @Test
    void declineJoinRequest_정상처리시_SSE업데이트발행() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");
        stubTransitionRequest("request-1", joinRequest);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = taxiPartyService.declineJoinRequest("leader", "request-1");

        assertEquals("request-1", response.id());
        assertEquals(JoinRequestStatus.DECLINED, response.status());
        verify(joinRequestSseService).publishJoinRequestUpdated(joinRequest, JoinRequestStatus.PENDING);
    }

    @Test
    void declineJoinRequest_파티잠금후최신요청상태가처리완료면덮어쓰지않는다() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest canceledRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(canceledRequest, "id", "request-1");
        canceledRequest.cancel();

        when(joinRequestRepository.findTransitionSnapshotById("request-1"))
                .thenReturn(Optional.of(transitionSnapshot(
                        "request-1",
                        "party-1",
                        "leader",
                        "requester-1",
                        JoinRequestStatus.PENDING
                )));
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(joinRequestRepository.findByIdForUpdate("request-1")).thenReturn(Optional.of(canceledRequest));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.declineJoinRequest("leader", "request-1")
        );

        assertEquals(ErrorCode.REQUEST_ALREADY_PROCESSED, exception.getErrorCode());
        assertEquals(JoinRequestStatus.CANCELED, canceledRequest.getStatus());
        verify(joinRequestRepository, never()).save(canceledRequest);
        InOrder lockOrder = inOrder(joinRequestRepository, partyRepository);
        lockOrder.verify(joinRequestRepository).findTransitionSnapshotById("request-1");
        lockOrder.verify(partyRepository).findDetailByIdForUpdate("party-1");
        lockOrder.verify(joinRequestRepository).findByIdForUpdate("request-1");
    }

    @Test
    void cancelJoinRequest_정상처리시_SSE업데이트발행() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");
        stubTransitionRequest("request-1", joinRequest);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = taxiPartyService.cancelJoinRequest("requester-1", "request-1");

        assertEquals("request-1", response.id());
        assertEquals(JoinRequestStatus.CANCELED, response.status());
        verify(joinRequestSseService).publishJoinRequestUpdated(joinRequest, JoinRequestStatus.PENDING);
    }

    @Test
    void declineJoinRequest_이전거절이력이있어도_새요청을다시거절할수있다() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest previousRequest = JoinRequest.create(party, "requester-1");
        previousRequest.decline();
        JoinRequest currentRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(currentRequest, "id", "request-2");

        stubTransitionRequest("request-2", currentRequest);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = taxiPartyService.declineJoinRequest("leader", "request-2");

        assertEquals("request-2", response.id());
        assertEquals(JoinRequestStatus.DECLINED, response.status());
        assertEquals(JoinRequestStatus.DECLINED, previousRequest.getStatus());
    }

    @Test
    void cancelJoinRequest_이전취소이력이있어도_새요청을다시취소할수있다() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest previousRequest = JoinRequest.create(party, "requester-1");
        previousRequest.cancel();
        JoinRequest currentRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(currentRequest, "id", "request-2");

        stubTransitionRequest("request-2", currentRequest);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinRequestResponse response = taxiPartyService.cancelJoinRequest("requester-1", "request-2");

        assertEquals("request-2", response.id());
        assertEquals(JoinRequestStatus.CANCELED, response.status());
        assertEquals(JoinRequestStatus.CANCELED, previousRequest.getStatus());
    }

    @Test
    void acceptJoinRequest_낙관적락충돌이면_PARTY_CONCURRENT_MODIFICATION() {
        Party party = sampleParty("party-1", "leader", 4, false);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");

        stubTransitionRequest("request-1", joinRequest);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("requester-1")).thenReturn(Optional.of(member("requester-1")));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.existsActivePartyByMemberId(eq("requester-1"), anySet(), eq("party-1"))).thenReturn(false);
        when(partyRepository.saveAndFlush(any(Party.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Party.class, "party-1"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.acceptJoinRequest("leader", "request-1")
        );

        assertEquals(ErrorCode.PARTY_CONCURRENT_MODIFICATION, exception.getErrorCode());
    }

    @Test
    void leaveParty_OPEN상태일반멤버는_탈퇴성공() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member("member-1", "홍길동")));

        taxiPartyService.leaveParty("member-1", "party-1");

        assertFalse(party.isMember("member-1"));
        verify(chatService).syncPartyChatRoomMembers(party);
        verify(chatService).createPartyMemberLeaveSystemMessage(party, "member-1", "홍길동님이 나갔어요.");
        verify(partySseService).publishPartyMemberLeft(party, "member-1", "LEFT", party.getMemberIds());
    }

    @Test
    void leaveParty_ARRIVED상태일반멤버는_정산스냅샷유지하며_탈퇴성공() {
        Party party = sampleParty("party-1", "leader", 4, true);
        arrive(party);

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member("member-1", "홍길동")));

        taxiPartyService.leaveParty("member-1", "party-1");

        assertFalse(party.isMember("member-1"));
        assertEquals(1, party.getCurrentMembers());
        assertEquals(1, party.getSettlementItems().size());
        assertEquals(List.of("member-1"), party.getSettlementTargetMemberIds());
        assertEquals(2, party.getSplitMemberCount());
        assertEquals(14000, party.getTaxiFare());
        assertEquals(7000, party.getPerPersonAmount());

        var settlement = party.getSettlementItems().stream()
                .filter(item -> item.getMemberId().equals("member-1"))
                .findFirst()
                .orElseThrow();
        assertEquals("홍길동", settlement.getDisplayName());
        assertTrue(settlement.isLeftParty());
        assertNotNull(settlement.getLeftAt());

        verify(chatService).syncPartyChatRoomMembers(party);
        verify(chatService).syncPartyArrivalMessageSnapshot(party);
        verify(chatService).createPartyMemberLeaveSystemMessage(party, "member-1", "홍길동님이 나갔어요.");
        verify(partySseService).publishPartyMemberLeft(party, "member-1", "LEFT", party.getMemberIds());
    }

    @Test
    void leaveParty_닉네임이비어있으면_fallback시스템메시지를생성한다() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member("member-1", "")));

        taxiPartyService.leaveParty("member-1", "party-1");

        verify(chatService).createPartyMemberLeaveSystemMessage(party, "member-1", "멤버가 나갔어요.");
    }

    @Test
    void kickMember_강퇴당사자도_KICKED이벤트수신대상에포함() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member("member-1", "홍길동")));

        taxiPartyService.kickMember("leader", "party-1", "member-1");

        verify(chatService).syncPartyChatRoomMembers(party);
        verify(chatService).createPartyMemberLeaveSystemMessage(party, "leader", "홍길동님이 나갔어요.");
        verify(partySseService).publishPartyMemberLeft(
                eq(party),
                eq("member-1"),
                eq("KICKED"),
                argThat(recipients -> recipients.contains("member-1"))
        );
    }

    @Test
    void kickMember_닉네임이비어있으면_퇴장fallback시스템메시지를생성한다() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member("member-1", " ")));

        taxiPartyService.kickMember("leader", "party-1", "member-1");

        verify(chatService).createPartyMemberLeaveSystemMessage(party, "leader", "멤버가 나갔어요.");
    }

    @Test
    void leaveParty_리더는_탈퇴불가() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.leaveParty("leader", "party-1")
        );

        assertEquals(ErrorCode.LEADER_CANNOT_LEAVE, exception.getErrorCode());
    }

    @Test
    void leaveParty_ENDED상태는_실패() {
        Party party = sampleParty("party-1", "leader", 4, true);
        party.cancel();
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.leaveParty("member-1", "party-1")
        );

        assertEquals(ErrorCode.PARTY_ENDED, exception.getErrorCode());
    }

    @Test
    void confirmSettlement_마지막멤버확인시_정산만완료되고파티는ARRIVED유지() {
        Party party = sampleParty("party-1", "leader", 4, true);
        arrive(party);

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementConfirmResponse response = taxiPartyService.confirmSettlement("leader", "party-1", "member-1");

        assertTrue(response.allSettled());
        assertEquals(PartyStatus.ARRIVED, party.getStatus());
        assertEquals(SettlementStatus.COMPLETED, party.getSettlementStatus());
        verify(chatService).syncPartyArrivalMessageSnapshot(party);
        verify(partySseService, never()).publishPartyStatusChanged(any(Party.class));
    }

    @Test
    void confirmSettlement_부분정산이어도_ARRIVED메시지스냅샷을동기화한다() {
        Party party = sampleParty("party-1", "leader", 4, "member-1", "member-2");
        arrive(party, 21000, List.of("member-1", "member-2"));

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementConfirmResponse response = taxiPartyService.confirmSettlement("leader", "party-1", "member-1");

        assertFalse(response.allSettled());
        assertEquals(SettlementStatus.PENDING, party.getSettlementStatus());
        verify(chatService).syncPartyArrivalMessageSnapshot(party);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void confirmSettlement_ARRIVED에서나간멤버도_정산확인가능() {
        Party party = sampleParty("party-1", "leader", 4, true);
        arrive(party);
        party.leaveArrivedMember("member-1");

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementConfirmResponse response = taxiPartyService.confirmSettlement("leader", "party-1", "member-1");

        assertEquals("member-1", response.memberId());
        assertTrue(response.settled());
        assertTrue(response.allSettled());
        assertTrue(party.getSettlementItems().stream().findFirst().orElseThrow().isLeftParty());
    }

    @Test
    void confirmSettlement_ARRIVED가아닌상태면_INVALID_PARTY_STATE_TRANSITION() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.confirmSettlement("leader", "party-1", "member-1")
        );

        assertEquals(ErrorCode.INVALID_PARTY_STATE_TRANSITION, exception.getErrorCode());
    }

    @Test
    void cancelParty_리더가OPEN파티를취소하면_ENDED_CANCELLED() {
        Party party = sampleParty("party-1", "leader", 4, true);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartyStatusResponse response = taxiPartyService.cancelParty("leader", "party-1");

        assertEquals(PartyStatus.ENDED, response.status());
        assertEquals(PartyEndReason.CANCELLED, response.endReason());
        assertEquals(PartyStatus.ENDED, party.getStatus());
        assertEquals(PartyEndReason.CANCELLED, party.getEndReason());
        verify(chatService).createPartyEndMessage(party, "leader");
        verify(partySseService).publishPartyDeleted("party-1");
    }

    @Test
    void endParty_강제종료시_END채팅메시지를생성한다() {
        Party party = sampleParty("party-1", "leader", 4, true);
        arrive(party);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartyStatusResponse response = taxiPartyService.endParty("leader", "party-1");

        assertEquals(PartyStatus.ENDED, response.status());
        assertEquals(PartyEndReason.FORCE_ENDED, response.endReason());
        verify(chatService).createPartyEndMessage(party, "leader");
        verify(partySseService).publishPartyStatusChanged(party);
    }

    @Test
    void cancelParty_ARRIVED상태취소는_PARTY_NOT_CANCELABLE() {
        Party party = sampleParty("party-1", "leader", 4, true);
        arrive(party);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.cancelParty("leader", "party-1")
        );

        assertEquals(ErrorCode.PARTY_NOT_CANCELABLE, exception.getErrorCode());
    }

    @Test
    void leaveParty_ARRIVED탈퇴후_다른파티생성과동승요청이가능하다() {
        Party arrivedParty = sampleParty("arrived-party", "leader", 4, true);
        arrive(arrivedParty);

        Party openParty = sampleParty("open-party", "other-leader", 4, false);

        when(partyRepository.findDetailById("arrived-party")).thenReturn(Optional.of(arrivedParty));
        when(partyRepository.findDetailById("open-party")).thenReturn(Optional.of(openParty));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member("member-1", "홍길동")));
        when(memberRepository.findActiveByIdForUpdate("member-1")).thenReturn(Optional.of(member("member-1", "홍길동")));
        when(partyRepository.existsActivePartyByMemberId(eq("member-1"), anySet(), isNull())).thenReturn(false);
        when(joinRequestRepository.existsByParty_IdAndRequesterIdAndStatus("open-party", "member-1", JoinRequestStatus.PENDING))
                .thenReturn(false);
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> {
            JoinRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "request-after-leave");
            return saved;
        });
        when(partyRepository.save(any(Party.class))).thenAnswer(invocation -> {
            Party saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "new-party");
            return saved;
        });

        taxiPartyService.leaveParty("member-1", "arrived-party");
        JoinRequestResponse joinRequestResponse = taxiPartyService.createJoinRequest("member-1", "open-party");
        PartyCreateResponse createPartyResponse = taxiPartyService.createParty("member-1", createPartyRequest(4));

        assertEquals("request-after-leave", joinRequestResponse.id());
        assertEquals("new-party", createPartyResponse.id());
    }

    @Test
    void acceptJoinRequest_ARRIVED탈퇴한멤버는_ALREADY_IN_PARTY로막히지않는다() {
        Party arrivedParty = sampleParty("arrived-party", "leader", 4, true);
        arrive(arrivedParty);
        Party targetParty = sampleParty("target-party", "target-leader", 4, false);
        JoinRequest joinRequest = JoinRequest.create(targetParty, "member-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-after-leave");

        when(partyRepository.findDetailById("arrived-party")).thenReturn(Optional.of(arrivedParty));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member("member-1", "홍길동")));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubTransitionRequest("request-after-leave", joinRequest);
        when(partyRepository.findDetailByIdForUpdate("target-party")).thenReturn(Optional.of(targetParty));
        when(memberRepository.findActiveByIdForUpdate("member-1")).thenReturn(Optional.of(member("member-1", "홍길동")));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partyRepository.existsActivePartyByMemberId(eq("member-1"), anySet(), eq("target-party"))).thenReturn(false);

        taxiPartyService.leaveParty("member-1", "arrived-party");
        JoinRequestAcceptResponse response = taxiPartyService.acceptJoinRequest("target-leader", "request-after-leave");

        assertEquals(JoinRequestStatus.ACCEPTED, response.status());
        assertTrue(targetParty.isMember("member-1"));
    }

    @Test
    void validateWithdrawalAllowed_ARRIVED일반멤버면_탈퇴불가예외() {
        Party arrivedParty = sampleParty("party-1", "leader", 4, true);
        arrive(arrivedParty);
        when(partyRepository.findActiveDetailsByMemberId("member-1", java.util.EnumSet.of(PartyStatus.OPEN, PartyStatus.CLOSED, PartyStatus.ARRIVED)))
                .thenReturn(List.of(arrivedParty));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyService.validateWithdrawalAllowed("member-1")
        );

        assertEquals(ErrorCode.MEMBER_WITHDRAWAL_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void handleMemberWithdrawal_리더탈퇴면_WITHDRAWED종료와대기요청거절을수행한다() {
        Party party = sampleParty("party-1", "leader", 4, true);
        JoinRequest joinRequest = JoinRequest.create(party, "requester-1");
        ReflectionTestUtils.setField(joinRequest, "id", "request-1");

        when(partyRepository.findActiveIdsByMemberId("leader", java.util.EnumSet.of(PartyStatus.OPEN, PartyStatus.CLOSED, PartyStatus.ARRIVED)))
                .thenReturn(List.of("party-1"));
        when(partyInvitationLifecycleService.findPendingPartyIdsByInviter("leader"))
                .thenReturn(List.of("invitation-only-party"));
        when(partyInvitationLifecycleService.findPendingPartyIdsByInvitee("leader"))
                .thenReturn(List.of());
        when(partyRepository.findDetailByIdForUpdate("invitation-only-party")).thenReturn(Optional.empty());
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(joinRequestRepository.findPendingByPartyIdForUpdate("party-1"))
                .thenReturn(List.of(joinRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taxiPartyService.handleMemberWithdrawal("leader");

        assertEquals(PartyStatus.ENDED, party.getStatus());
        assertEquals(PartyEndReason.WITHDRAWED, party.getEndReason());
        assertEquals(JoinRequestStatus.DECLINED, joinRequest.getStatus());
        verify(chatService).createPartyEndMessage(party, "leader");
        verify(partySseService).publishPartyStatusChanged(party);
        verify(joinRequestSseService).publishJoinRequestUpdated(joinRequest, JoinRequestStatus.PENDING);
        verify(partyInvitationLifecycleService).expirePendingByInviterInParty(
                "invitation-only-party",
                "leader",
                PartyInvitationExpiryReason.MEMBER_WITHDRAWN
        );
        verify(partyInvitationLifecycleService).expirePendingForInviteeInParty(
                "invitation-only-party",
                "leader",
                PartyInvitationExpiryReason.MEMBER_WITHDRAWN
        );
        InOrder lockOrder = inOrder(partyRepository, partyInvitationLifecycleService);
        lockOrder.verify(partyRepository).findDetailByIdForUpdate("party-1");
        lockOrder.verify(partyInvitationLifecycleService).expirePendingByInviterInParty(
                "party-1",
                "leader",
                PartyInvitationExpiryReason.MEMBER_WITHDRAWN
        );
    }

    @Test
    void handleMemberWithdrawal_수신대기초대만있어도만료처리한다() {
        when(partyRepository.findActiveIdsByMemberId("invitee-1", java.util.EnumSet.of(PartyStatus.OPEN, PartyStatus.CLOSED, PartyStatus.ARRIVED)))
                .thenReturn(List.of());
        when(partyInvitationLifecycleService.findPendingPartyIdsByInviter("invitee-1"))
                .thenReturn(List.of());
        when(partyInvitationLifecycleService.findPendingPartyIdsByInvitee("invitee-1"))
                .thenReturn(List.of("party-invited"));
        when(partyRepository.findDetailByIdForUpdate("party-invited")).thenReturn(Optional.empty());

        taxiPartyService.handleMemberWithdrawal("invitee-1");

        verify(partyInvitationLifecycleService).expirePendingForInviteeInParty(
                "party-invited",
                "invitee-1",
                PartyInvitationExpiryReason.MEMBER_WITHDRAWN
        );
    }

    @Test
    void getMyTaxiHistory_역할과결제금액을화면계약에맞게매핑한다() {
        Party leaderParty = sampleParty("party-leader", "member-1", 4, "member-2");
        leaderParty.updateDepartureTime(LocalDateTime.now().plusHours(1));
        arrive(leaderParty, 14000, List.of("member-2"));

        Party memberParty = sampleParty("party-member", "leader", 4, true);
        memberParty.updateDepartureTime(LocalDateTime.now().plusHours(2));
        arrive(memberParty);
        memberParty.forceEnd();

        when(partyRepository.findMyParties("member-1")).thenReturn(List.of(memberParty, leaderParty));

        List<TaxiHistoryItemResponse> response = taxiPartyService.getMyTaxiHistory("member-1");

        assertEquals(2, response.size());
        assertEquals("party-member", response.get(0).id());
        assertEquals(TaxiHistoryRole.MEMBER, response.get(0).role());
        assertEquals(TaxiHistoryStatus.COMPLETED, response.get(0).status());
        assertEquals(7000, response.get(0).paymentAmount());
        assertEquals(2, response.get(0).passengerCount());
        assertEquals(TaxiHistoryRole.LEADER, response.get(1).role());
        assertEquals("성결대학교", response.get(1).departureLabel());
        assertEquals("안양역", response.get(1).arrivalLabel());
    }

    @Test
    void getMyTaxiHistory_상태매핑은완료와취소를명시적으로구분한다() {
        Party arrivedParty = sampleParty("party-arrived", "leader", 4, true);
        arrive(arrivedParty);

        Party cancelledParty = sampleParty("party-cancelled", "leader", 4, true);
        cancelledParty.cancel();

        Party timeoutCompletedParty = sampleParty("party-timeout-completed", "leader", 4, true);
        arrive(timeoutCompletedParty);
        timeoutCompletedParty.timeoutEnd();

        Party timeoutCancelledParty = sampleParty("party-timeout-cancelled", "leader", 4, true);
        timeoutCancelledParty.timeoutEnd();

        when(partyRepository.findMyParties("member-1"))
                .thenReturn(List.of(arrivedParty, cancelledParty, timeoutCompletedParty, timeoutCancelledParty));

        List<TaxiHistoryItemResponse> response = taxiPartyService.getMyTaxiHistory("member-1");

        assertEquals(TaxiHistoryStatus.COMPLETED, findTaxiHistory(response, "party-arrived").status());
        assertEquals(TaxiHistoryStatus.CANCELLED, findTaxiHistory(response, "party-cancelled").status());
        assertEquals(TaxiHistoryStatus.COMPLETED, findTaxiHistory(response, "party-timeout-completed").status());
        assertEquals(TaxiHistoryStatus.CANCELLED, findTaxiHistory(response, "party-timeout-cancelled").status());
        assertNull(findTaxiHistory(response, "party-cancelled").paymentAmount());
    }

    @Test
    void getMyTaxiHistorySummary_완료건수와절약금액은동일기준으로집계한다() {
        Party completedLeaderParty = sampleParty("party-completed-1", "member-1", 4, "member-2");
        arrive(completedLeaderParty, 14000, List.of("member-2"));

        Party completedMemberParty = sampleParty("party-completed-2", "leader", 4, true);
        arrive(completedMemberParty);
        completedMemberParty.forceEnd();

        Party cancelledParty = sampleParty("party-cancelled", "leader", 4, true);
        cancelledParty.cancel();

        when(partyRepository.findMyParties("member-1"))
                .thenReturn(List.of(completedLeaderParty, completedMemberParty, cancelledParty));

        TaxiHistorySummaryResponse response = taxiPartyService.getMyTaxiHistorySummary("member-1");

        assertEquals(3, response.totalRideCount());
        assertEquals(2, response.completedRideCount());
        assertEquals(14000, response.savedFareAmount());
    }

    private CreatePartyRequest createPartyRequest(int maxMembers) {
        return new CreatePartyRequest(
                new PartyLocationRequest("성결대학교", 37.38, 126.93),
                new PartyLocationRequest("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                maxMembers,
                List.of("빠른출발"),
                "택시비 나눠요"
        );
    }

    private Party sampleParty(String partyId, String leaderId, int maxMembers, boolean includeMember) {
        if (!includeMember) {
            return sampleParty(partyId, leaderId, maxMembers);
        }
        return sampleParty(partyId, leaderId, maxMembers, "member-1");
    }

    private Party sampleParty(String partyId, String leaderId, int maxMembers, String... memberIds) {
        Party party = sampleParty(partyId, leaderId, maxMembers);

        for (String memberId : memberIds) {
            party.addMember(memberId);
        }
        return party;
    }

    private Party sampleParty(String partyId, String leaderId, int maxMembers) {
        Party party = Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                maxMembers,
                List.of("빠른출발"),
                "택시비 나눠요"
        );
        ReflectionTestUtils.setField(party, "id", partyId);
        return party;
    }

    private void stubTransitionRequest(String requestId, JoinRequest joinRequest) {
        when(joinRequestRepository.findTransitionSnapshotById(requestId))
                .thenReturn(Optional.of(transitionSnapshot(
                        requestId,
                        joinRequest.getParty().getId(),
                        joinRequest.getLeaderId(),
                        joinRequest.getRequesterId(),
                        joinRequest.getStatus()
                )));
        when(joinRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(joinRequest));
    }

    private JoinRequestRepository.TransitionSnapshot transitionSnapshot(
            String id,
            String partyId,
            String leaderId,
            String requesterId,
            JoinRequestStatus status
    ) {
        return new JoinRequestRepository.TransitionSnapshot() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getPartyId() {
                return partyId;
            }

            @Override
            public String getLeaderId() {
                return leaderId;
            }

            @Override
            public String getRequesterId() {
                return requesterId;
            }

            @Override
            public JoinRequestStatus getStatus() {
                return status;
            }
        };
    }

    private Member member(String memberId) {
        return Member.create(memberId, memberId + "@sungkyul.ac.kr", memberId, LocalDateTime.now());
    }

    private Member member(String memberId, String nickname) {
        Member member = member(memberId);
        member.updateProfile(nickname, null, null, null);
        return member;
    }

    private Member member(String memberId, String nickname, String photoUrl) {
        Member member = member(memberId, nickname);
        if (photoUrl != null) {
            member.updateProfile(null, null, null, photoUrl);
        }
        return member;
    }

    private PartyTagRepository.PartyTagSummary tagSummary(String partyId, String tag) {
        return new PartyTagRepository.PartyTagSummary() {
            @Override
            public String getPartyId() {
                return partyId;
            }

            @Override
            public String getTag() {
                return tag;
            }
        };
    }

    private ArrivePartyRequest arriveRequest(int taxiFare, List<String> settlementTargetMemberIds) {
        return new ArrivePartyRequest(
                taxiFare,
                settlementTargetMemberIds,
                new ArrivePartyRequest.SettlementAccountRequest(
                        "카카오뱅크",
                        "3333-01-1234567",
                        "홍길동",
                        true
                )
        );
    }

    private void arrive(Party party) {
        arrive(party, 14000, List.of("member-1"));
    }

    private void arrive(Party party, int taxiFare, List<String> settlementTargetMemberIds) {
        party.arriveWithSnapshots(
                taxiFare,
                settlementTargetMemberIds.stream()
                        .map(memberId -> new SettlementTargetSnapshot(memberId, displayNameFor(memberId)))
                        .toList(),
                SettlementAccountSnapshot.of("카카오뱅크", "3333-01-1234567", "홍길동", true)
        );
    }

    private String displayNameFor(String memberId) {
        return switch (memberId) {
            case "member-1" -> "홍길동";
            case "member-2" -> "김철수";
            case "requester-1" -> "스쿠리 유저";
            default -> memberId;
        };
    }

    private TaxiHistoryItemResponse findTaxiHistory(List<TaxiHistoryItemResponse> responses, String partyId) {
        return responses.stream()
                .filter(item -> item.id().equals(partyId))
                .findFirst()
                .orElseThrow();
    }
}
