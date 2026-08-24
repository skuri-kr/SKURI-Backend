package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.academic.repository.TimetableSharingSettingRepository;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 회원 탈퇴 뒤 Friend 도메인이 소유한 관계·공유 파생 데이터를 제거한다.
 * 택시파티와 공개 채팅방의 PENDING 초대 전이는 각 aggregate 서비스가 담당한다.
 */
@Service
@RequiredArgsConstructor
public class FriendWithdrawalCleanupService {

    private final FriendProfileProvisioningService friendProfileProvisioningService;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendPreferenceRepository friendPreferenceRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final TimetableSharingSettingRepository timetableSharingSettingRepository;
    private final TimetableShareOverrideRepository timetableShareOverrideRepository;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.MANDATORY)
    public void cleanupWithdrawnMember(String memberId, LocalDateTime withdrawnAt) {
        String withdrawnFriendPublicId = friendProfileProvisioningService
                .retireForWithdrawnMember(memberId, withdrawnAt)
                .orElse(null);
        List<FriendRequest> friendRequests = friendRequestRepository.findByRequesterIdOrRecipientId(memberId, memberId);
        notificationService.deleteFriendRelatedNotifications(
                resolveCounterpartRequestIds(memberId, friendRequests),
                withdrawnFriendPublicId
        );
        friendRequestRepository.deleteByRequesterIdOrRecipientId(memberId, memberId);
        friendshipRepository.deleteByMemberLowIdOrMemberHighId(memberId, memberId);
        friendPreferenceRepository.deleteByOwnerMemberIdOrFriendMemberId(memberId, memberId);
        memberBlockRepository.deleteByBlockerIdOrBlockedId(memberId, memberId);
        timetableShareOverrideRepository.deleteByOwnerMemberIdOrFriendMemberId(memberId, memberId);
        timetableSharingSettingRepository.deleteByOwnerMemberId(memberId);
    }

    private Map<String, Set<String>> resolveCounterpartRequestIds(String memberId, List<FriendRequest> friendRequests) {
        Map<String, Set<String>> requestIdsByCounterpartMember = new LinkedHashMap<>();
        for (FriendRequest friendRequest : friendRequests) {
            String counterpartMemberId = resolveCounterpartMemberId(memberId, friendRequest);
            if (counterpartMemberId == null || counterpartMemberId.isBlank()
                    || counterpartMemberId.equals(memberId)
                    || friendRequest.getId() == null || friendRequest.getId().isBlank()) {
                continue;
            }
            requestIdsByCounterpartMember
                    .computeIfAbsent(counterpartMemberId, ignored -> new java.util.LinkedHashSet<>())
                    .add(friendRequest.getId());
        }
        return requestIdsByCounterpartMember;
    }

    private String resolveCounterpartMemberId(String memberId, FriendRequest friendRequest) {
        if (memberId.equals(friendRequest.getRequesterId())) {
            return friendRequest.getRecipientId();
        }
        if (memberId.equals(friendRequest.getRecipientId())) {
            return friendRequest.getRequesterId();
        }
        return null;
    }
}
