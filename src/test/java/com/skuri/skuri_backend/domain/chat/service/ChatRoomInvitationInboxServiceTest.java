package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomInvitationInboxServiceTest {

    @Mock
    private ChatRoomInvitationRepository invitationRepository;

    @Mock
    private ChatRoomInvitationExpirationService expirationService;

    @InjectMocks
    private ChatRoomInvitationInboxService inboxService;

    @Test
    void countActionablePending_만료정리는제한하고_유효한행은DB에서센다() {
        when(invitationRepository.findTimedOutPendingReceivedIds(
                eq("invitee-1"),
                any(LocalDateTime.class),
                eq(PageRequest.of(0, 100))
        )).thenReturn(List.of("timed-out-1", "timed-out-2"));
        when(invitationRepository.countByInviteeIdAndStatusAndExpiresAtAfter(
                eq("invitee-1"),
                eq(ChatRoomInvitationStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(3L);

        int count = inboxService.countActionablePending("invitee-1");

        assertEquals(3, count);
        verify(expirationService).expireIfTimedOut("timed-out-1");
        verify(expirationService).expireIfTimedOut("timed-out-2");
    }
}
