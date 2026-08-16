package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.dto.request.SendChatMessageRequest;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementAccountSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementTargetSnapshot;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyMessageServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PartyMessageService partyMessageService;

    @Test
    void buildClientPayload_ACCOUNT_remember면회원계좌도함께저장한다() {
        Party party = sampleParty("leader-1", true);
        Member member = Member.create("leader-1", "leader-1@sungkyul.ac.kr", "리더", LocalDateTime.now());
        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findActiveByIdForUpdate("leader-1")).thenReturn(Optional.of(member));

        PartySpecialMessagePayload payload = partyMessageService.buildClientPayload(
                "party:party-1",
                "leader-1",
                new SendChatMessageRequest(
                        com.skuri.skuri_backend.domain.chat.entity.ChatMessageType.ACCOUNT,
                        null,
                        null,
                        new SendChatMessageRequest.AccountPayload(
                                "카카오뱅크",
                                "3333-01-1234567",
                                "홍길동",
                                true,
                                true
                        )
                )
        );

        assertEquals("계좌 정보를 공유했어요. (카카오뱅크 3333-01-1234567)", payload.text());
        assertNotNull(payload.accountData());
        assertEquals("카카오뱅크", payload.accountData().getBankName());
        assertEquals("홍*동", payload.accountData().getAccountHolder());
        assertTrue(payload.accountData().getHideName());
        assertNotNull(member.getBankAccount());
        assertEquals("홍길동", member.getBankAccount().getAccountHolder());
        assertTrue(member.getBankAccount().getHideName());
        verify(memberRepository).findActiveByIdForUpdate("leader-1");
    }

    @Test
    void buildArrivalPayload_정산스냅샷을포함한다() {
        Party party = sampleParty("leader-1", true);
        party.arriveWithSnapshots(
                14000,
                List.of(new SettlementTargetSnapshot("member-2", "김철수")),
                SettlementAccountSnapshot.of("카카오뱅크", "3333-01-1234567", "홍길동", true)
        );

        PartySpecialMessagePayload payload = partyMessageService.buildArrivalPayload(party, "leader-1");

        assertEquals("택시가 목적지에 도착했어요. 총 14000원, 2명 정산, 1인당 7000원입니다.", payload.text());
        assertNotNull(payload.arrivalData());
        assertEquals(14000, payload.arrivalData().getTaxiFare());
        assertEquals(7000, payload.arrivalData().getPerPersonAmount());
        assertEquals(2, payload.arrivalData().getSplitMemberCount());
        assertEquals(List.of("member-2"), payload.arrivalData().getSettlementTargetMemberIds());
        assertEquals(1, payload.arrivalData().getMemberSettlements().size());
        assertEquals("김철수", payload.arrivalData().getMemberSettlements().get(0).getDisplayName());
        assertFalse(payload.arrivalData().getMemberSettlements().get(0).isLeftParty());
        assertEquals("홍*동", payload.arrivalData().getAccountData().getAccountHolder());
        assertTrue(payload.arrivalData().getAccountData().getHideName());
    }

    @Test
    void buildArrivalPayload_ARRIVED이후나간정산대상도표시한다() {
        Party party = sampleParty("leader-1", true);
        party.arriveWithSnapshots(
                14000,
                List.of(new SettlementTargetSnapshot("member-2", "김철수")),
                SettlementAccountSnapshot.of("카카오뱅크", "3333-01-1234567", "홍길동", true)
        );
        party.leaveArrivedMember("member-2");

        PartySpecialMessagePayload payload = partyMessageService.buildArrivalPayload(party, "leader-1");

        assertEquals(List.of("member-2"), payload.arrivalData().getSettlementTargetMemberIds());
        assertTrue(payload.arrivalData().getMemberSettlements().get(0).isLeftParty());
        assertNotNull(payload.arrivalData().getMemberSettlements().get(0).getLeftAt());
    }

    @Test
    void buildEndPayload_CANCELLED면취소문구를사용한다() {
        Party party = sampleParty("leader-1", true);
        party.cancel();

        PartySpecialMessagePayload payload = partyMessageService.buildEndPayload(party, "leader-1");

        assertEquals("리더가 파티를 취소했어요.", payload.text());
        assertEquals(null, payload.accountData());
        assertEquals(null, payload.arrivalData());
    }

    private Party sampleParty(String leaderId, boolean includeMember) {
        Party party = Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(2),
                4,
                List.of("빠른출발"),
                "테스트"
        );
        if (includeMember) {
            party.addMember("member-2");
        }
        ReflectionTestUtils.setField(party, "id", "party-1");
        return party;
    }
}
