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
import com.skuri.skuri_backend.domain.friend.entity.FriendRequestStatus;
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

    private final FriendProfileProvisioningService provisioningService;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendPreferenceRepository friendPreferenceRepository;
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;

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
        if (friendshipRepository.findByMemberPair(lowMemberId(ownerMemberId, friend.memberId()), highMemberId(ownerMemberId, friend.memberId())).isEmpty()) {
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
        List<FriendSearchProjection> fetched = friendProfileRepository.findNicknameSearchResults(
                requesterMemberId,
                query,
                searchCursor == null ? null : searchCursor.nickname(),
                searchCursor == null ? null : searchCursor.friendPublicId(),
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = fetched.size() > pageSize;
        List<FriendSearchProjection> page = hasNext ? fetched.subList(0, pageSize) : fetched;
        List<FriendSearchResultResponse> items = page.stream()
                .map(result -> new FriendSearchResultResponse(
                        result.getFriendPublicId(),
                        result.getNickname(),
                        result.getDepartment(),
                        result.getPhotoUrl(),
                        canSendFriendRequest(requesterMemberId, result.getMemberId())
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
        List<FriendRequest> requests = direction == FriendRequestDirection.RECEIVED
                ? friendRequestRepository.findAllByRecipientIdAndStatusOrderByCreatedAtDescIdDesc(memberId, FriendRequestStatus.PENDING)
                : friendRequestRepository.findAllByRequesterIdAndStatusOrderByCreatedAtDescIdDesc(memberId, FriendRequestStatus.PENDING);
        List<FriendRequest> pending = requests.stream()
                .filter(request -> reconcileExpiration(request))
                .filter(request -> isAfterRequestCursor(request, requestCursor))
                .limit(pageSize + 1L)
                .toList();
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
        List<FriendRequest> requests = friendRequestRepository
                .findAllByRecipientIdAndStatusOrderByCreatedAtDescIdDesc(memberId, FriendRequestStatus.PENDING);
        int incomingRequestCount = (int) requests.stream().filter(this::reconcileExpiration).count();
        return new FriendInboxCountsResponse(incomingRequestCount, 0, 0, incomingRequestCount);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedPair(String firstMemberId, String secondMemberId) {
        return memberBlockRepository.existsByBlockerIdAndBlockedId(firstMemberId, secondMemberId)
                || memberBlockRepository.existsByBlockerIdAndBlockedId(secondMemberId, firstMemberId);
    }

    @Transactional(readOnly = true)
    public boolean canSendFriendRequest(String requesterMemberId, String targetMemberId) {
        if (requesterMemberId.equals(targetMemberId) || isBlockedPair(requesterMemberId, targetMemberId)) {
            return false;
        }
        String lowMemberId = lowMemberId(requesterMemberId, targetMemberId);
        String highMemberId = highMemberId(requesterMemberId, targetMemberId);
        if (friendshipRepository.findByMemberPair(lowMemberId, highMemberId).isPresent()) {
            return false;
        }
        return friendRequestRepository.findByActivePairKey(activePairKey(lowMemberId, highMemberId))
                .map(request -> request.isExpiredAt(LocalDateTime.now()))
                .orElse(true);
    }

    private boolean reconcileExpiration(FriendRequest snapshot) {
        LocalDateTime now = LocalDateTime.now();
        if (!snapshot.isExpiredAt(now)) {
            return true;
        }
        List<Member> members = memberRepository.findAllActiveByIdInForUpdateOrdered(
                Set.of(snapshot.getRequesterId(), snapshot.getRecipientId())
        );
        if (members.size() != 2) {
            return false;
        }
        FriendRequest request = friendRequestRepository.findByIdForUpdate(snapshot.getId()).orElse(null);
        if (request == null || !request.isPending()) {
            return false;
        }
        if (request.isExpiredAt(now)) {
            request.expire(now);
            return false;
        }
        return true;
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

    private boolean isAfterRequestCursor(FriendRequest request, RequestCursor cursor) {
        if (cursor == null) {
            return true;
        }
        int createdAtComparison = request.getCreatedAt().compareTo(cursor.createdAt());
        return createdAtComparison < 0 || (createdAtComparison == 0 && request.getId().compareTo(cursor.requestId()) < 0);
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

    private String lowMemberId(String firstMemberId, String secondMemberId) {
        return firstMemberId.compareTo(secondMemberId) <= 0 ? firstMemberId : secondMemberId;
    }

    private String highMemberId(String firstMemberId, String secondMemberId) {
        return firstMemberId.compareTo(secondMemberId) <= 0 ? secondMemberId : firstMemberId;
    }

    private String activePairKey(String lowMemberId, String highMemberId) {
        return lowMemberId + ":" + highMemberId;
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
