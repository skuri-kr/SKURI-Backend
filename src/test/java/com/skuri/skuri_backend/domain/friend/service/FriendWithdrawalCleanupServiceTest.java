package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.academic.repository.TimetableSharingSettingRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

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

    @InjectMocks
    private FriendWithdrawalCleanupService friendWithdrawalCleanupService;

    @Test
    void cleanupWithdrawnMember_친구관계와시간표공유파생데이터를정리한다() {
        LocalDateTime withdrawnAt = LocalDateTime.of(2026, 8, 24, 12, 0);

        friendWithdrawalCleanupService.cleanupWithdrawnMember("member-1", withdrawnAt);

        verify(friendProfileProvisioningService).retireForWithdrawnMember("member-1", withdrawnAt);
        verify(friendRequestRepository).deleteByRequesterIdOrRecipientId("member-1", "member-1");
        verify(friendshipRepository).deleteByMemberLowIdOrMemberHighId("member-1", "member-1");
        verify(friendPreferenceRepository).deleteByOwnerMemberIdOrFriendMemberId("member-1", "member-1");
        verify(memberBlockRepository).deleteByBlockerIdOrBlockedId("member-1", "member-1");
        verify(timetableShareOverrideRepository).deleteByOwnerMemberIdOrFriendMemberId("member-1", "member-1");
        verify(timetableSharingSettingRepository).deleteByOwnerMemberId("member-1");
    }
}
