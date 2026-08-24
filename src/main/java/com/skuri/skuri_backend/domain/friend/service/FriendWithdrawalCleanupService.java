package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.academic.repository.TimetableSharingSettingRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @Transactional(propagation = Propagation.MANDATORY)
    public void cleanupWithdrawnMember(String memberId, LocalDateTime withdrawnAt) {
        friendProfileProvisioningService.retireForWithdrawnMember(memberId, withdrawnAt);
        friendRequestRepository.deleteByRequesterIdOrRecipientId(memberId, memberId);
        friendshipRepository.deleteByMemberLowIdOrMemberHighId(memberId, memberId);
        friendPreferenceRepository.deleteByOwnerMemberIdOrFriendMemberId(memberId, memberId);
        memberBlockRepository.deleteByBlockerIdOrBlockedId(memberId, memberId);
        timetableShareOverrideRepository.deleteByOwnerMemberIdOrFriendMemberId(memberId, memberId);
        timetableSharingSettingRepository.deleteByOwnerMemberId(memberId);
    }
}
