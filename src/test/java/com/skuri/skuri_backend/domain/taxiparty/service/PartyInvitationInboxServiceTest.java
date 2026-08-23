package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyInvitationInboxServiceTest {

    @Mock
    private PartyInvitationRepository invitationRepository;

    @InjectMocks
    private PartyInvitationInboxService inboxService;

    @Test
    void countActionablePending_PENDING행을DB에서바로센다() {
        when(invitationRepository.countByInviteeIdAndStatus(
                "invitee-1",
                PartyInvitationStatus.PENDING
        )).thenReturn(4L);

        int count = inboxService.countActionablePending("invitee-1");

        assertEquals(4, count);
        verify(invitationRepository).countByInviteeIdAndStatus(
                "invitee-1",
                PartyInvitationStatus.PENDING
        );
    }
}
