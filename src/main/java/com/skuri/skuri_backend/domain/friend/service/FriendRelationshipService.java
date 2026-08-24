package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.academic.service.TimetableSharingRelationshipCleanupService;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationExpiryReason;
import com.skuri.skuri_backend.domain.chat.service.ChatRoomInvitationLifecycleService;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSummaryResponse;
import com.skuri.skuri_backend.domain.friend.entity.FriendPreference;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.entity.MemberBlock;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.service.PartyInvitationLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendRelationshipService {

    private final FriendProfileProvisioningService provisioningService;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendPreferenceRepository friendPreferenceRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final FriendMemberPairLockService pairLockService;
    private final FriendRequestTransitionService friendRequestTransitionService;
    private final FriendRequestExpiryService friendRequestExpiryService;
    private final FriendSummarySnapshotFactory friendSummarySnapshotFactory;
    private final MemberRepository memberRepository;
    private final TimetableSharingRelationshipCleanupService timetableSharingRelationshipCleanupService;
    private final PartyInvitationLifecycleService partyInvitationLifecycleService;
    private final ChatRoomInvitationLifecycleService chatRoomInvitationLifecycleService;

    @Transactional
    public FriendRequestCreationResult createRequest(String requesterMemberId, String targetFriendPublicId) {
        provisioningService.ensureForActiveMember(requesterMemberId);
        String targetMemberId = resolveTargetMemberId(targetFriendPublicId);
        if (requesterMemberId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_SELF_REQUEST_NOT_ALLOWED);
        }
        FriendMemberPair pair = pairLockService.lockActivePair(requesterMemberId, targetMemberId);
        rejectBlockedPair(pair);
        if (friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()).isPresent()) {
            throw new BusinessException(ErrorCode.FRIEND_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        FriendRequest activeRequest = friendRequestRepository.findByActivePairKeyForUpdate(pair.activePairKey())
                .orElse(null);
        if (activeRequest != null && activeRequest.isExpiredAt(now)) {
            activeRequest.expire(now);
            friendRequestRepository.saveAndFlush(activeRequest);
            activeRequest = null;
        }
        if (activeRequest == null) {
            FriendRequest created = friendRequestRepository.save(
                    FriendRequest.create(requesterMemberId, targetMemberId, pair.activePairKey(), now)
            );
            return FriendRequestCreationResult.pending(created.getId());
        }
        if (activeRequest.getRequesterId().equals(requesterMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_PENDING);
        }

        activeRequest.accept(now);
        friendshipRepository.save(Friendship.create(pair.lowMemberId(), pair.highMemberId()));
        return FriendRequestCreationResult.accepted(
                activeRequest.getId(),
                friendSummarySnapshotFactory.create(requesterMemberId, targetMemberId)
        );
    }

    public FriendRequestAcceptResult acceptRequest(String recipientMemberId, String requestId) {
        FriendRequestTransitionService.FriendRequestAcceptAttempt attempt = friendRequestTransitionService
                .acceptRequest(recipientMemberId, requestId);
        if (!attempt.accepted()) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);
        }
        return new FriendRequestAcceptResult(attempt.friend());
    }

    public void declineRequest(String recipientMemberId, String requestId) {
        if (!friendRequestTransitionService.declineRequest(recipientMemberId, requestId).completed()) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);
        }
    }

    public void cancelRequest(String requesterMemberId, String requestId) {
        if (!friendRequestTransitionService.cancelRequest(requesterMemberId, requestId).completed()) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);
        }
    }

    public boolean expireRequestIfNeeded(String requestId) {
        return friendRequestExpiryService.expireRequestIfNeeded(requestId);
    }

    @Transactional
    public void setFavorite(String ownerMemberId, String friendPublicId, boolean favorite) {
        pairLockService.requireActiveProfileCompleteMember(ownerMemberId);
        String friendMemberId = resolveTargetMemberId(friendPublicId);
        FriendMemberPair pair = pairLockService.lockActivePair(ownerMemberId, friendMemberId);
        requireFriendship(pair);
        friendPreferenceRepository.findByOwnerMemberIdAndFriendMemberId(ownerMemberId, friendMemberId)
                .ifPresentOrElse(
                        preference -> preference.updateFavorite(favorite),
                        () -> friendPreferenceRepository.save(FriendPreference.create(ownerMemberId, friendMemberId, favorite))
                );
    }

    @Transactional
    public void removeFriendship(String ownerMemberId, String friendPublicId) {
        pairLockService.requireActiveProfileCompleteMember(ownerMemberId);
        String friendMemberId = resolveTargetMemberId(friendPublicId);
        FriendMemberPair pair = pairLockService.lockActivePair(ownerMemberId, friendMemberId);
        Friendship friendship = requireFriendship(pair);
        friendshipRepository.delete(friendship);
        friendPreferenceRepository.deleteByOwnerMemberIdAndFriendMemberId(ownerMemberId, friendMemberId);
        friendPreferenceRepository.deleteByOwnerMemberIdAndFriendMemberId(friendMemberId, ownerMemberId);
        timetableSharingRelationshipCleanupService.deleteOverridesForMemberPair(ownerMemberId, friendMemberId);
        expirePendingInvitationsForRelationship(ownerMemberId, friendMemberId);
    }

    @Transactional
    public void blockMember(String blockerMemberId, String targetFriendPublicId) {
        pairLockService.requireActiveProfileCompleteMember(blockerMemberId);
        String blockedMemberId = resolveTargetMemberId(targetFriendPublicId);
        if (blockerMemberId.equals(blockedMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_SELF_BLOCK_NOT_ALLOWED);
        }
        FriendMemberPair pair = pairLockService.lockActivePair(blockerMemberId, blockedMemberId);
        if (memberBlockRepository.existsByBlockerIdAndBlockedId(blockerMemberId, blockedMemberId)) {
            return;
        }
        friendRequestRepository.findByActivePairKeyForUpdate(pair.activePairKey()).ifPresent(request -> {
            LocalDateTime now = LocalDateTime.now();
            if (request.isExpiredAt(now)) {
                request.expire(now);
                return;
            }
            request.cancel(now);
        });
        friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()).ifPresent(friendshipRepository::delete);
        friendPreferenceRepository.deleteByOwnerMemberIdAndFriendMemberId(blockerMemberId, blockedMemberId);
        friendPreferenceRepository.deleteByOwnerMemberIdAndFriendMemberId(blockedMemberId, blockerMemberId);
        timetableSharingRelationshipCleanupService.deleteOverridesForMemberPair(blockerMemberId, blockedMemberId);
        expirePendingInvitationsForRelationship(blockerMemberId, blockedMemberId);
        memberBlockRepository.save(MemberBlock.create(blockerMemberId, blockedMemberId));
    }

    @Transactional
    public void unblockMember(String blockerMemberId, String targetFriendPublicId) {
        pairLockService.requireActiveProfileCompleteMember(blockerMemberId);
        String blockedMemberId = resolveTargetMemberId(targetFriendPublicId);
        pairLockService.lockActivePair(blockerMemberId, blockedMemberId);
        memberBlockRepository.deleteByBlockerIdAndBlockedId(blockerMemberId, blockedMemberId);
    }

    private String resolveTargetMemberId(String friendPublicId) {
        String memberId = friendProfileRepository.findByPublicId(friendPublicId)
                .map(profile -> profile.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND));
        return memberRepository.findActiveById(memberId)
                .filter(Member::isProfileComplete)
                .map(Member::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND));
    }

    private void rejectBlockedPair(FriendMemberPair pair) {
        if (memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                || memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId())) {
            throw new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        }
    }

    private Friendship requireFriendship(FriendMemberPair pair) {
        return friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND));
    }

    private void expirePendingInvitationsForRelationship(String firstMemberId, String secondMemberId) {
        partyInvitationLifecycleService.expirePendingForMemberPair(
                firstMemberId,
                secondMemberId,
                PartyInvitationExpiryReason.RELATIONSHIP_UNAVAILABLE
        );
        chatRoomInvitationLifecycleService.expirePendingForMemberPair(
                firstMemberId,
                secondMemberId,
                ChatRoomInvitationExpiryReason.RELATIONSHIP_UNAVAILABLE
        );
    }

    public record FriendRequestCreationResult(String requestId, FriendSummaryResponse friend, boolean accepted) {
        private static FriendRequestCreationResult pending(String requestId) {
            return new FriendRequestCreationResult(requestId, null, false);
        }

        private static FriendRequestCreationResult accepted(
                String requestId,
                FriendSummaryResponse friend
        ) {
            return new FriendRequestCreationResult(requestId, friend, true);
        }
    }

    public record FriendRequestAcceptResult(FriendSummaryResponse friend) {
    }
}
