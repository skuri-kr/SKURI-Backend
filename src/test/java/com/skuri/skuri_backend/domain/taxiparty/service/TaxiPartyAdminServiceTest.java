package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.constant.AdminPartyStatusAction;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyJoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyEndReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementAccountSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementTargetSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxiPartyAdminServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private PartyInvitationLifecycleService partyInvitationLifecycleService;

    @Mock
    private PartySseService partySseService;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TaxiPartyAdminService taxiPartyAdminService;

    @Test
    void updatePartyStatus_END_정상처리시_리더기준_END메시지를재사용한다() {
        Party party = sampleParty("party-1", "leader");
        arrive(party);
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartyStatusResponse response = taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.END);

        assertEquals(PartyStatus.ENDED, response.status());
        assertEquals(PartyEndReason.FORCE_ENDED, response.endReason());
        verify(chatService).createPartyEndMessage(party, "leader");
        verify(partyInvitationLifecycleService).expirePendingForParty(
                "party-1",
                PartyInvitationExpiryReason.TARGET_UNAVAILABLE
        );
        verify(partySseService).publishPartyStatusChanged(party);
        verify(eventPublisher).publish(any());
    }

    @Test
    void updatePartyStatus_REOPEN_만료된초대를복원하거나추가만료하지않는다() {
        Party party = sampleParty("party-1", "leader");
        party.close();
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartyStatusResponse response = taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.REOPEN);

        assertEquals(PartyStatus.OPEN, response.status());
        verify(partyInvitationLifecycleService, never()).expirePendingForParty(any(), any());
    }

    @Test
    void updatePartyStatus_END_허용되지않는전이면_실패한다() {
        Party party = sampleParty("party-1", "leader");
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyAdminService.updatePartyStatus("party-1", AdminPartyStatusAction.END)
        );

        assertEquals(ErrorCode.INVALID_PARTY_STATE_TRANSITION, exception.getErrorCode());
        verify(partyRepository, never()).saveAndFlush(any(Party.class));
        verify(chatService, never()).createPartyEndMessage(any(), any());
        verify(partyInvitationLifecycleService, never()).expirePendingForParty(any(), any());
    }

    @Test
    void removePartyMember_일반멤버면_성공한다() {
        Party party = sampleParty("party-1", "leader");
        Member member = memberWithProfile("member-1", "김철수", "김철수", null, "컴퓨터공학과", "20230001");
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taxiPartyAdminService.removePartyMember("admin-1", "party-1", "member-1");

        assertEquals(1, party.getCurrentMembers());
        verify(chatService).syncPartyChatRoomMembers(party);
        verify(partyInvitationLifecycleService).expirePendingByInviterInParty(
                "party-1",
                "member-1",
                PartyInvitationExpiryReason.INVITER_LEFT
        );
        verify(chatService).createPartyMemberLeaveSystemMessage(party, "admin-1", "김철수님이 나갔어요.");
        verify(partySseService).publishPartyMemberLeft(party, "member-1", "KICKED", List.of("leader", "member-1"));
        verify(eventPublisher).publish(any());
    }

    @Test
    void removePartyMember_리더면_실패한다() {
        Party party = sampleParty("party-1", "leader");
        Member leader = sampleMember("leader", "리더");
        when(partyRepository.findDetailByIdForUpdate("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findById("leader")).thenReturn(Optional.of(leader));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyAdminService.removePartyMember("admin-1", "party-1", "leader")
        );

        assertEquals(ErrorCode.PARTY_LEADER_REMOVAL_NOT_ALLOWED, exception.getErrorCode());
        verify(partyRepository, never()).saveAndFlush(any(Party.class));
    }

    @Test
    void createPartySystemMessage_채팅방있으면_성공한다() {
        Party party = sampleParty("party-1", "leader");
        ChatMessageResponse expected = new ChatMessageResponse(
                "message-1",
                "party:party-1",
                "admin-1",
                "관리자",
                null,
                ChatMessageType.SYSTEM,
                "관리자 안내 메시지",
                null,
                null,
                null,
                LocalDateTime.of(2026, 3, 29, 12, 10)
        );
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(chatService.createPartyAdminSystemMessage(party, "admin-1", "관리자 안내 메시지")).thenReturn(expected);

        ChatMessageResponse response = taxiPartyAdminService.createPartySystemMessage("admin-1", "party-1", "관리자 안내 메시지");

        assertEquals("message-1", response.id());
        assertEquals("관리자", response.senderName());
        verify(chatService).createPartyAdminSystemMessage(party, "admin-1", "관리자 안내 메시지");
    }

    @Test
    void createPartySystemMessage_채팅방없으면_실패한다() {
        Party party = sampleParty("party-1", "leader");
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(chatService.createPartyAdminSystemMessage(party, "admin-1", "관리자 안내 메시지"))
                .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> taxiPartyAdminService.createPartySystemMessage("admin-1", "party-1", "관리자 안내 메시지")
        );

        assertEquals(ErrorCode.CHAT_ROOM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getAdminPartyMessages_파티가있으면_partyChat조회로위임한다() {
        Party party = sampleParty("party-1", "leader");
        ChatMessagePageResponse expected = new ChatMessagePageResponse(List.of(), false, null);
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(chatService.getAdminPartyChatMessages("party:party-1", null, null, 50)).thenReturn(expected);

        ChatMessagePageResponse response = taxiPartyAdminService.getAdminPartyMessages("party-1", null, null, 50);

        assertEquals(expected, response);
        verify(chatService).getAdminPartyChatMessages("party:party-1", null, null, 50);
    }

    @Test
    void getPartyJoinRequests_PENDING만_최신순으로반환한다() {
        Party party = sampleParty("party-1", "leader");
        JoinRequest latest = JoinRequest.create(party, "member-2");
        JoinRequest earlier = JoinRequest.create(party, "member-1");
        ReflectionTestUtils.setField(latest, "id", "request-2");
        ReflectionTestUtils.setField(earlier, "id", "request-1");
        ReflectionTestUtils.setField(latest, "createdAt", LocalDateTime.of(2026, 3, 29, 12, 5));
        ReflectionTestUtils.setField(earlier, "createdAt", LocalDateTime.of(2026, 3, 29, 12, 0));

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(joinRequestRepository.findByParty_IdAndStatusOrderByCreatedAtDesc("party-1", JoinRequestStatus.PENDING))
                .thenReturn(List.of(latest, earlier));
        when(memberRepository.findAllById(List.of("member-2", "member-1"))).thenReturn(List.of(
                memberWithProfile("member-2", "김철수", "김철수", "https://cdn.skuri.app/profiles/member-2.png", "컴퓨터공학과", "20230001"),
                memberWithProfile("member-1", "이영희", "이영희", null, "미디어소프트웨어학과", "20230002")
        ));

        List<AdminPartyJoinRequestResponse> responses = taxiPartyAdminService.getPartyJoinRequests("party-1");

        assertEquals(2, responses.size());
        assertEquals("request-2", responses.get(0).requestId());
        assertEquals("member-2", responses.get(0).memberId());
        assertEquals("request-1", responses.get(1).requestId());
        assertNotNull(responses.get(0).requestedAt());
    }

    private Party sampleParty(String partyId, String leaderId) {
        Party party = Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                4,
                List.of("빠른출발"),
                "택시비 나눠요"
        );
        ReflectionTestUtils.setField(party, "id", partyId);
        party.addMember("member-1");
        return party;
    }

    private Member sampleMember(String memberId, String realname) {
        return Member.create(memberId, memberId + "@sungkyul.ac.kr", realname, LocalDateTime.now().minusDays(1));
    }

    private Member memberWithProfile(
            String memberId,
            String nickname,
            String realname,
            String photoUrl,
            String department,
            String studentId
    ) {
        Member member = sampleMember(memberId, realname);
        member.updateProfile(nickname, studentId, department, photoUrl);
        return member;
    }

    private void arrive(Party party) {
        party.arriveWithSnapshots(
                14000,
                List.of(new SettlementTargetSnapshot("member-1", "홍길동")),
                SettlementAccountSnapshot.of("카카오뱅크", "3333-01-1234567", "홍길동", true)
        );
    }
}
