package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyTimeoutCommandServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private PartySseService partySseService;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @Mock
    private PartyInvitationLifecycleService partyInvitationLifecycleService;

    @InjectMocks
    private PartyTimeoutCommandService partyTimeoutCommandService;

    @Test
    void endExpiredParty_대상이없으면_false() {
        when(partyRepository.findById("party-1")).thenReturn(Optional.empty());

        boolean result = partyTimeoutCommandService.endExpiredParty("party-1");

        assertFalse(result);
        verify(partyRepository, never()).saveAndFlush(any(Party.class));
        verify(partySseService, never()).publishPartyStatusChanged(any(Party.class));
    }

    @Test
    void endExpiredParty_이미종료된파티면_false() {
        Party party = sampleParty("leader");
        party.timeoutEnd();
        when(partyRepository.findById("party-1")).thenReturn(Optional.of(party));

        boolean result = partyTimeoutCommandService.endExpiredParty("party-1");

        assertFalse(result);
        verify(partyRepository, never()).saveAndFlush(any(Party.class));
        verify(partySseService, never()).publishPartyStatusChanged(any(Party.class));
    }

    @Test
    void endExpiredParty_정상종료_true() {
        Party party = sampleParty("leader");
        when(partyRepository.findById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = partyTimeoutCommandService.endExpiredParty("party-1");

        assertTrue(result);
        assertEquals(PartyStatus.ENDED, party.getStatus());
        verify(partySseService).publishPartyStatusChanged(party);
    }

    @Test
    void endExpiredParty_낙관적락충돌이면_PARTY_CONCURRENT_MODIFICATION() {
        Party party = sampleParty("leader");
        when(partyRepository.findById("party-1")).thenReturn(Optional.of(party));
        when(partyRepository.saveAndFlush(any(Party.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Party.class, "party-1"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> partyTimeoutCommandService.endExpiredParty("party-1")
        );

        assertEquals(ErrorCode.PARTY_CONCURRENT_MODIFICATION, exception.getErrorCode());
        verify(partySseService, never()).publishPartyStatusChanged(any(Party.class));
    }

    private Party sampleParty(String leaderId) {
        return Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                4,
                List.of("빠른출발"),
                "택시비 나눠요"
        );
    }
}
