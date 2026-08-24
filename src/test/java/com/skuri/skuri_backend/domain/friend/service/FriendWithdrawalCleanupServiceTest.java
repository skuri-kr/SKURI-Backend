package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.academic.repository.TimetableSharingSettingRepository;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendWithdrawalCleanupServiceTest {

    @Mock
    private FriendProfileProvisioningService friendProfileProvisioningService;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendPreferenceRepository friendPreferenceRepository;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @Mock
    private TimetableSharingSettingRepository timetableSharingSettingRepository;

    @Mock
    private TimetableShareOverrideRepository timetableShareOverrideRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FriendWithdrawalCleanupService friendWithdrawalCleanupService;

    @Test
    void cleanupWithdrawnMember_친구관계와시간표공유파생데이터를정리한다() {
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 8, 24, 12, 0);
        when(friendProfileProvisioningService.retireForWithdrawnMember("member-1", withdrawnAt))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterIdOrRecipientId("member-1", "member-1"))
                .thenReturn(List.of());

        friendWithdrawalCleanupService.cleanupWithdrawnMember("member-1", withdrawnAt);

        verify(friendProfileProvisioningService).retireForWithdrawnMember("member-1", withdrawnAt);
        verify(friendRequestRepository).findByRequesterIdOrRecipientId("member-1", "member-1");
        verify(notificationService).deleteFriendRelatedNotifications(Map.of(), null);
        verify(friendRequestRepository).deleteByRequesterIdOrRecipientId("member-1", "member-1");
        verify(friendshipRepository).deleteByMemberLowIdOrMemberHighId("member-1", "member-1");
        verify(friendPreferenceRepository).deleteByOwnerMemberIdOrFriendMemberId("member-1", "member-1");
        verify(memberBlockRepository).deleteByBlockerIdOrBlockedId("member-1", "member-1");
        verify(timetableShareOverrideRepository).deleteByOwnerMemberIdOrFriendMemberId("member-1", "member-1");
        verify(timetableSharingSettingRepository).deleteByOwnerMemberId("member-1");
    }

    @Test
    void cleanupWithdrawnMember_상대방의친구요청알림을요청삭제전에정리한다() {
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 8, 24, 12, 0);
        when(friendProfileProvisioningService.retireForWithdrawnMember("member-1", withdrawnAt))
                .thenReturn(Optional.of("friend-public-1"));
        FriendRequest sentRequest = friendRequest("request-1", "member-1", "member-2");
        FriendRequest receivedRequest = friendRequest("request-2", "member-3", "member-1");
        when(friendRequestRepository.findByRequesterIdOrRecipientId("member-1", "member-1"))
                .thenReturn(List.of(sentRequest, receivedRequest));

        friendWithdrawalCleanupService.cleanupWithdrawnMember("member-1", withdrawnAt);

        org.mockito.ArgumentCaptor<Map<String, Set<String>>> requestIdsCaptor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(notificationService).deleteFriendRelatedNotifications(requestIdsCaptor.capture(), eq("friend-public-1"));
        assertEquals(
                Map.of("member-2", Set.of("request-1"), "member-3", Set.of("request-2")),
                requestIdsCaptor.getValue()
        );

        org.mockito.InOrder inOrder = inOrder(friendRequestRepository, notificationService);
        inOrder.verify(friendRequestRepository).findByRequesterIdOrRecipientId("member-1", "member-1");
        inOrder.verify(notificationService).deleteFriendRelatedNotifications(anyMap(), org.mockito.ArgumentMatchers.any());
        inOrder.verify(friendRequestRepository).deleteByRequesterIdOrRecipientId("member-1", "member-1");
    }

    private FriendRequest friendRequest(String id, String requesterId, String recipientId) {
        FriendRequest request = FriendRequest.create(
                requesterId,
                recipientId,
                requesterId + ":" + recipientId,
                LocalDateTime.of(2026, 8, 24, 12, 0)
        );
        org.springframework.test.util.ReflectionTestUtils.setField(request, "id", id);
        return request;
    }
}
