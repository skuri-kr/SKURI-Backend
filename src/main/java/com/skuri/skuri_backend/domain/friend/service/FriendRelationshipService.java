package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.entity.FriendPreference;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.entity.MemberBlock;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FriendRelationshipService {

    private final FriendProfileProvisioningService provisioningService;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendPreferenceRepository friendPreferenceRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public FriendRequestCreationResult createRequest(String requesterMemberId, String targetFriendPublicId) {
        provisioningService.ensureForActiveMember(requesterMemberId);
        String targetMemberId = resolveTargetMemberId(targetFriendPublicId);
        if (requesterMemberId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_SELF_NOT_ALLOWED);
        }
        LockedPair pair = lockActivePair(requesterMemberId, targetMemberId);
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
        return FriendRequestCreationResult.accepted(activeRequest.getId(), targetMemberId);
    }

    @Transactional
    public String acceptRequest(String recipientMemberId, String requestId) {
        FriendRequest snapshot = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        LockedPair pair = lockActivePair(snapshot.getRequesterId(), snapshot.getRecipientId());
        FriendRequest request = friendRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        if (!request.getRecipientId().equals(recipientMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_RECIPIENT_REQUIRED);
        }
        if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
            if (friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId()).isPresent()) {
                return request.getRequesterId();
            }
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);
        }
        LocalDateTime now = LocalDateTime.now();
        expireIfNeeded(request, now);
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);
        }
        rejectBlockedPair(pair);
        request.accept(now);
        friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId())
                .orElseGet(() -> friendshipRepository.save(Friendship.create(pair.lowMemberId(), pair.highMemberId())));
        return request.getRequesterId();
    }

    @Transactional
    public void declineRequest(String recipientMemberId, String requestId) {
        FriendRequest snapshot = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        lockActivePair(snapshot.getRequesterId(), snapshot.getRecipientId());
        FriendRequest request = friendRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        if (!request.getRecipientId().equals(recipientMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_RECIPIENT_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now();
        expireIfNeeded(request, now);
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);
        }
        request.decline(now);
    }

    @Transactional
    public void cancelRequest(String requesterMemberId, String requestId) {
        FriendRequest snapshot = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        lockActivePair(snapshot.getRequesterId(), snapshot.getRecipientId());
        FriendRequest request = friendRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        if (!request.getRequesterId().equals(requesterMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_REQUESTER_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now();
        expireIfNeeded(request, now);
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED);
        }
        request.cancel(now);
    }

    @Transactional
    public void setFavorite(String ownerMemberId, String friendPublicId, boolean favorite) {
        String friendMemberId = resolveTargetMemberId(friendPublicId);
        LockedPair pair = lockActivePair(ownerMemberId, friendMemberId);
        requireFriendship(pair);
        friendPreferenceRepository.findByOwnerMemberIdAndFriendMemberId(ownerMemberId, friendMemberId)
                .ifPresentOrElse(
                        preference -> preference.updateFavorite(favorite),
                        () -> friendPreferenceRepository.save(FriendPreference.create(ownerMemberId, friendMemberId, favorite))
                );
    }

    @Transactional
    public void removeFriendship(String ownerMemberId, String friendPublicId) {
        String friendMemberId = resolveTargetMemberId(friendPublicId);
        LockedPair pair = lockActivePair(ownerMemberId, friendMemberId);
        Friendship friendship = requireFriendship(pair);
        friendshipRepository.delete(friendship);
        friendPreferenceRepository.deleteByOwnerMemberIdAndFriendMemberId(ownerMemberId, friendMemberId);
        friendPreferenceRepository.deleteByOwnerMemberIdAndFriendMemberId(friendMemberId, ownerMemberId);
    }

    @Transactional
    public void blockMember(String blockerMemberId, String targetFriendPublicId) {
        String blockedMemberId = resolveTargetMemberId(targetFriendPublicId);
        if (blockerMemberId.equals(blockedMemberId)) {
            throw new BusinessException(ErrorCode.FRIEND_SELF_BLOCK_NOT_ALLOWED);
        }
        LockedPair pair = lockActivePair(blockerMemberId, blockedMemberId);
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
        memberBlockRepository.save(MemberBlock.create(blockerMemberId, blockedMemberId));
    }

    @Transactional
    public void unblockMember(String blockerMemberId, String targetFriendPublicId) {
        String blockedMemberId = resolveTargetMemberId(targetFriendPublicId);
        lockActivePair(blockerMemberId, blockedMemberId);
        memberBlockRepository.deleteByBlockerIdAndBlockedId(blockerMemberId, blockedMemberId);
    }

    private String resolveTargetMemberId(String friendPublicId) {
        return friendProfileRepository.findByPublicId(friendPublicId)
                .map(profile -> profile.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND));
    }

    private LockedPair lockActivePair(String firstMemberId, String secondMemberId) {
        if (firstMemberId.equals(secondMemberId)) {
            memberRepository.findActiveByIdForUpdate(firstMemberId).orElseThrow(MemberNotFoundException::new);
            return new LockedPair(firstMemberId, secondMemberId);
        }
        List<Member> members = memberRepository.findAllActiveByIdInForUpdateOrdered(Set.of(firstMemberId, secondMemberId));
        if (members.isEmpty() || members.stream().noneMatch(member -> member.getId().equals(firstMemberId))) {
            throw new MemberNotFoundException();
        }
        if (members.size() != 2) {
            throw new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        }
        return new LockedPair(members.get(0).getId(), members.get(1).getId());
    }

    private void rejectBlockedPair(LockedPair pair) {
        if (memberBlockRepository.existsByBlockerIdAndBlockedId(pair.lowMemberId(), pair.highMemberId())
                || memberBlockRepository.existsByBlockerIdAndBlockedId(pair.highMemberId(), pair.lowMemberId())) {
            throw new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        }
    }

    private Friendship requireFriendship(LockedPair pair) {
        return friendshipRepository.findByMemberPairForUpdate(pair.lowMemberId(), pair.highMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND));
    }

    private void expireIfNeeded(FriendRequest request, LocalDateTime now) {
        if (request.isExpiredAt(now)) {
            request.expire(now);
        }
    }

    public record FriendRequestCreationResult(String requestId, String friendMemberId, boolean accepted) {
        private static FriendRequestCreationResult pending(String requestId) {
            return new FriendRequestCreationResult(requestId, null, false);
        }

        private static FriendRequestCreationResult accepted(String requestId, String friendMemberId) {
            return new FriendRequestCreationResult(requestId, friendMemberId, true);
        }
    }

    private record LockedPair(String lowMemberId, String highMemberId) {
        private String activePairKey() {
            return lowMemberId + ":" + highMemberId;
        }
    }
}
