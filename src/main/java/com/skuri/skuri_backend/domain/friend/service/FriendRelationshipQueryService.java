package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendBlockResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInboxCountsResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendRequestItemResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendRequestPageResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSearchPageResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSearchResultResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSummaryResponse;
import com.skuri.skuri_backend.domain.friend.entity.FriendPreference;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.entity.Friendship;
import com.skuri.skuri_backend.domain.friend.entity.MemberBlock;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendSearchProjection;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendRelationshipQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 20;
    private static final int REQUEST_SCAN_BATCH_SIZE = 50;
    private static final int INBOX_EXPIRY_RECONCILIATION_BATCH_SIZE = 100;

    private final FriendProfileProvisioningService provisioningService;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendPreferenceRepository friendPreferenceRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;
    private final FriendRequestExpiryService friendRequestExpiryService;

    @Transactional
    public List<FriendSummaryResponse> getFriends(String ownerMemberId) {
        provisioningService.ensureForActiveMember(ownerMemberId);
        List<Friendship> friendships = friendshipRepository.findAllByMemberId(ownerMemberId);
        Set<String> friendMemberIds = friendships.stream()
                .map(friendship -> friendship.otherMemberId(ownerMemberId))
                .collect(Collectors.toSet());
        Map<String, PublicMember> members = getPublicMembers(friendMemberIds);
        Set<String> favorites = friendPreferenceRepository
                .findAllByOwnerMemberIdAndFriendMemberIdIn(ownerMemberId, friendMemberIds)
                .stream()
                .filter(FriendPreference::isFavorite)
                .map(FriendPreference::getFriendMemberId)
                .collect(Collectors.toSet());

        Collator koreanCollator = Collator.getInstance(java.util.Locale.KOREAN);
        return friendMemberIds.stream()
                .map(members::get)
                .filter(Objects::nonNull)
                .filter(member -> !isBlockedPair(ownerMemberId, member.memberId()))
                .sorted(Comparator.<PublicMember, Boolean>comparing(member -> favorites.contains(member.memberId())).reversed()
                        .thenComparing(PublicMember::nickname, koreanCollator)
                        .thenComparing(PublicMember::memberId))
                .map(member -> member.toSummary(favorites.contains(member.memberId())))
                .toList();
    }

    @Transactional
    public FriendSummaryResponse getFriend(String ownerMemberId, String friendPublicId) {
        provisioningService.ensureForActiveMember(ownerMemberId);
        PublicMember friend = resolvePublicMember(friendPublicId);
        return getFriendSummary(ownerMemberId, friend);
    }

    @Transactional
    public FriendSummaryResponse getFriendByMemberId(String ownerMemberId, String friendMemberId) {
        provisioningService.ensureForActiveMember(ownerMemberId);
        PublicMember friend = getPublicMembers(Set.of(friendMemberId)).get(friendMemberId);
        if (friend == null) {
            throw new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        }
        return getFriendSummary(ownerMemberId, friend);
    }

    private FriendSummaryResponse getFriendSummary(String ownerMemberId, PublicMember friend) {
        if (isBlockedPair(ownerMemberId, friend.memberId())) {
            throw new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        }
        FriendMemberPair pair = FriendMemberPair.of(ownerMemberId, friend.memberId());
        if (friendshipRepository.findByMemberPair(pair.lowMemberId(), pair.highMemberId()).isEmpty()) {
            throw new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND);
        }
        boolean favorite = friendPreferenceRepository
                .findByOwnerMemberIdAndFriendMemberId(ownerMemberId, friend.memberId())
                .map(FriendPreference::isFavorite)
                .orElse(false);
        return friend.toSummary(favorite);
    }

    @Transactional
    public FriendSearchPageResponse search(String requesterMemberId, String query, String cursor, Integer size) {
        provisioningService.ensureForActiveMember(requesterMemberId);
        int pageSize = resolveSize(size);
        SearchCursor searchCursor = decodeSearchCursor(cursor, query);
        String escapedQuery = escapeLike(query);
        List<FriendSearchProjection> fetched = friendProfileRepository.findNicknameSearchResults(
                requesterMemberId,
                escapedQuery,
                searchCursor == null ? null : searchCursor.nickname(),
                searchCursor == null ? null : searchCursor.friendPublicId(),
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = fetched.size() > pageSize;
        List<FriendSearchProjection> page = hasNext ? fetched.subList(0, pageSize) : fetched;
        Set<String> candidateMemberIds = page.stream()
                .map(FriendSearchProjection::getMemberId)
                .collect(Collectors.toSet());
        Set<String> existingFriendMemberIds = candidateMemberIds.isEmpty()
                ? Set.of()
                : Set.copyOf(friendshipRepository
                .findFriendMemberIdsByOwnerMemberIdAndCandidateMemberIds(requesterMemberId, candidateMemberIds));
        Set<String> activePairKeys = candidateMemberIds.stream()
                .map(candidateMemberId -> FriendMemberPair.of(requesterMemberId, candidateMemberId).activePairKey())
                .collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        Set<String> pendingPairKeys = activePairKeys.isEmpty()
                ? Set.of()
                : friendRequestRepository.findAllByActivePairKeyIn(activePairKeys).stream()
                .filter(request -> reconcileExpiration(request))
                .map(FriendRequest::getActivePairKey)
                .collect(Collectors.toSet());
        List<FriendSearchResultResponse> items = page.stream()
                .map(result -> new FriendSearchResultResponse(
                        result.getFriendPublicId(),
                        result.getNickname(),
                        result.getDepartment(),
                        result.getPhotoUrl(),
                        !existingFriendMemberIds.contains(result.getMemberId())
                                && !pendingPairKeys.contains(FriendMemberPair.of(requesterMemberId, result.getMemberId()).activePairKey())
                ))
                .toList();
        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            FriendSearchProjection last = page.getLast();
            nextCursor = encodeSearchCursor(query, last.getNickname(), last.getFriendPublicId());
        }
        return new FriendSearchPageResponse(items, hasNext, nextCursor);
    }

    @Transactional
    public FriendRequestPageResponse getRequests(
            String memberId,
            FriendRequestDirection direction,
            String cursor,
            Integer size
    ) {
        provisioningService.ensureForActiveMember(memberId);
        int pageSize = resolveSize(size);
        RequestCursor requestCursor = decodeRequestCursor(cursor, direction);
        List<FriendRequest> pending = findPendingRequests(memberId, direction, requestCursor, pageSize);
        boolean hasNext = pending.size() > pageSize;
        List<FriendRequest> page = hasNext ? pending.subList(0, pageSize) : pending;
        Set<String> counterpartIds = page.stream()
                .map(request -> otherPartyId(request, memberId))
                .collect(Collectors.toSet());
        Map<String, PublicMember> counterparts = getPublicMembers(counterpartIds);
        List<FriendRequestItemResponse> items = page.stream()
                .map(request -> toRequestItem(request, counterparts.get(otherPartyId(request, memberId))))
                .filter(Objects::nonNull)
                .toList();
        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            FriendRequest last = page.getLast();
            nextCursor = encodeRequestCursor(direction, last.getCreatedAt(), last.getId());
        }
        return new FriendRequestPageResponse(items, hasNext, nextCursor);
    }

    @Transactional
    public List<FriendBlockResponse> getBlocks(String blockerMemberId) {
        provisioningService.ensureForActiveMember(blockerMemberId);
        List<MemberBlock> blocks = memberBlockRepository.findAllByBlockerIdOrderByCreatedAtDesc(blockerMemberId);
        Map<String, PublicMember> blockedMembers = getPublicMembers(
                blocks.stream().map(MemberBlock::getBlockedId).collect(Collectors.toSet())
        );
        return blocks.stream()
                .map(block -> {
                    PublicMember member = blockedMembers.get(block.getBlockedId());
                    return member == null ? null : new FriendBlockResponse(
                            member.publicId(), member.nickname(), member.department(), member.photoUrl(), block.getCreatedAt()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public FriendInboxCountsResponse getInboxCounts(String memberId) {
        provisioningService.ensureForActiveMember(memberId);
        LocalDateTime now = LocalDateTime.now();
        friendRequestRepository.findExpiredPendingReceivedIds(
                        memberId,
                        now,
                        PageRequest.of(0, INBOX_EXPIRY_RECONCILIATION_BATCH_SIZE)
                )
                .forEach(friendRequestExpiryService::expireRequestIfNeeded);
        int incomingRequestCount = Math.toIntExact(friendRequestRepository
                .countActionablePendingReceivedByRecipientId(memberId, now));
        return new FriendInboxCountsResponse(incomingRequestCount, 0, 0, incomingRequestCount);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedPair(String firstMemberId, String secondMemberId) {
        return memberBlockRepository.existsByBlockerIdAndBlockedId(firstMemberId, secondMemberId)
                || memberBlockRepository.existsByBlockerIdAndBlockedId(secondMemberId, firstMemberId);
    }

    @Transactional
    public boolean canSendFriendRequest(String requesterMemberId, String targetMemberId) {
        if (requesterMemberId.equals(targetMemberId) || isBlockedPair(requesterMemberId, targetMemberId)) {
            return false;
        }
        FriendMemberPair pair = FriendMemberPair.of(requesterMemberId, targetMemberId);
        if (friendshipRepository.findByMemberPair(pair.lowMemberId(), pair.highMemberId()).isPresent()) {
            return false;
        }
        FriendRequest request = friendRequestRepository.findByActivePairKey(pair.activePairKey()).orElse(null);
        if (request == null) {
            return true;
        }
        return !reconcileExpiration(request);
    }

    private boolean reconcileExpiration(FriendRequest snapshot) {
        if (!snapshot.isExpiredAt(LocalDateTime.now())) {
            return true;
        }
        friendRequestExpiryService.expireRequestIfNeeded(snapshot.getId());
        return false;
    }

    private FriendRequestItemResponse toRequestItem(FriendRequest request, PublicMember friend) {
        if (friend == null) {
            return null;
        }
        return new FriendRequestItemResponse(
                request.getId(), friend.publicId(), friend.nickname(), friend.department(), friend.photoUrl(),
                request.getCreatedAt(), request.getExpiresAt()
        );
    }

    private Map<String, PublicMember> getPublicMembers(Collection<String> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Member> members = memberRepository.findAllActiveByIdIn(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
        return friendProfileRepository.findAllByMemberIdIn(memberIds).stream()
                .filter(profile -> members.containsKey(profile.getMemberId()))
                .collect(Collectors.toMap(
                        profile -> profile.getMemberId(),
                        profile -> {
                            Member member = members.get(profile.getMemberId());
                            return new PublicMember(
                                    member.getId(), profile.getPublicId(), member.getNickname(),
                                    member.getDepartment(), member.getPhotoUrl()
                            );
                        }
                ));
    }

    private PublicMember resolvePublicMember(String friendPublicId) {
        String memberId = friendProfileRepository.findByPublicId(friendPublicId)
                .map(profile -> profile.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND));
        Member member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND));
        return new PublicMember(member.getId(), friendPublicId, member.getNickname(), member.getDepartment(), member.getPhotoUrl());
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }

    private String otherPartyId(FriendRequest request, String memberId) {
        return request.getRequesterId().equals(memberId) ? request.getRecipientId() : request.getRequesterId();
    }

    private String encodeSearchCursor(String query, String nickname, String friendPublicId) {
        return encode(String.join("\u001F", "SEARCH", query, nickname, friendPublicId));
    }

    private SearchCursor decodeSearchCursor(String cursor, String query) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String[] parts = decode(cursor, 4);
        if (!"SEARCH".equals(parts[0]) || !query.equals(parts[1])) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return new SearchCursor(parts[2], parts[3]);
    }

    private String encodeRequestCursor(FriendRequestDirection direction, LocalDateTime createdAt, String requestId) {
        return encode(String.join("\u001F", "REQUEST", direction.name(), createdAt.toString(), requestId));
    }

    private RequestCursor decodeRequestCursor(String cursor, FriendRequestDirection direction) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String[] parts = decode(cursor, 4);
        if (!"REQUEST".equals(parts[0]) || !direction.name().equals(parts[1])) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        try {
            return new RequestCursor(LocalDateTime.parse(parts[2]), parts[3]);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String[] decode(String cursor, int expectedSize) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\u001F", -1);
            if (parts.length != expectedSize || List.of(parts).contains("")) {
                throw new IllegalArgumentException();
            }
            return parts;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private List<FriendRequest> findPendingRequests(
            String memberId,
            FriendRequestDirection direction,
            RequestCursor requestCursor,
            int pageSize
    ) {
        List<FriendRequest> pending = new ArrayList<>(pageSize + 1);
        RequestCursor scanCursor = requestCursor;

        while (pending.size() <= pageSize) {
            List<FriendRequest> batch = direction == FriendRequestDirection.RECEIVED
                    ? friendRequestRepository.findPendingReceivedAfterCursor(
                    memberId,
                    scanCursor == null ? null : scanCursor.createdAt(),
                    scanCursor == null ? null : scanCursor.requestId(),
                    PageRequest.of(0, REQUEST_SCAN_BATCH_SIZE)
            )
                    : friendRequestRepository.findPendingSentAfterCursor(
                    memberId,
                    scanCursor == null ? null : scanCursor.createdAt(),
                    scanCursor == null ? null : scanCursor.requestId(),
                    PageRequest.of(0, REQUEST_SCAN_BATCH_SIZE)
            );
            if (batch.isEmpty()) {
                break;
            }

            Set<String> counterpartIds = batch.stream()
                    .map(request -> otherPartyId(request, memberId))
                    .collect(Collectors.toSet());
            Set<String> visibleCounterpartIds = getPublicMembers(counterpartIds).keySet();

            for (FriendRequest request : batch) {
                if (reconcileExpiration(request) && visibleCounterpartIds.contains(otherPartyId(request, memberId))) {
                    pending.add(request);
                    if (pending.size() > pageSize) {
                        break;
                    }
                }
            }
            if (pending.size() > pageSize || batch.size() < REQUEST_SCAN_BATCH_SIZE) {
                break;
            }
            FriendRequest last = batch.getLast();
            scanCursor = new RequestCursor(last.getCreatedAt(), last.getId());
        }
        return pending;
    }

    private String escapeLike(String query) {
        return query.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    public enum FriendRequestDirection {
        RECEIVED,
        SENT
    }

    private record PublicMember(String memberId, String publicId, String nickname, String department, String photoUrl) {
        private FriendSummaryResponse toSummary(boolean favorite) {
            return new FriendSummaryResponse(publicId, nickname, department, photoUrl, favorite);
        }
    }

    private record SearchCursor(String nickname, String friendPublicId) {
    }

    private record RequestCursor(LocalDateTime createdAt, String requestId) {
    }
}
