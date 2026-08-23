package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.domain.academic.dto.response.TimetableShareOverrideResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSharingSettingsResponse;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareOverride;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FriendProfile이 준비된 회원의 시간표 공유 설정 조회를 전담한다.
 *
 * <p>호출자는 이 읽기 트랜잭션을 열기 전에 FriendProfile lazy provisioning을 끝내야 한다.</p>
 */
@Service
@RequiredArgsConstructor
class TimetableSharingSettingsReadService {

    private final TimetableShareOverrideRepository timetableShareOverrideRepository;
    private final TimetableSharingScopeResolver timetableSharingScopeResolver;
    private final FriendRelationshipQueryService friendRelationshipQueryService;
    private final FriendProfileRepository friendProfileRepository;

    @Transactional(readOnly = true)
    public TimetableSharingSettingsResponse getForProvisionedMember(String ownerMemberId) {
        TimetableShareScope defaultScope = timetableSharingScopeResolver.defaultScope(ownerMemberId);
        Set<String> activeFriendPublicIds = friendRelationshipQueryService
                .getFriendsForProvisionedMember(ownerMemberId)
                .stream()
                .map(friend -> friend.friendPublicId())
                .collect(Collectors.toSet());
        List<TimetableShareOverride> storedOverrides = timetableShareOverrideRepository
                .findAllByOwnerMemberId(ownerMemberId);
        // Friend summaries intentionally hide internal member IDs, so map override targets through FriendProfile.
        Map<String, String> publicIdsByMemberId = friendProfileRepository.findAllByMemberIdIn(
                        storedOverrides.stream()
                                .map(TimetableShareOverride::getFriendMemberId)
                                .collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(profile -> profile.getMemberId(), profile -> profile.getPublicId()));
        List<TimetableShareOverrideResponse> overrides = storedOverrides.stream()
                .map(override -> new TimetableShareOverrideResponse(
                        publicIdsByMemberId.get(override.getFriendMemberId()),
                        override.getScope()
                ))
                .filter(override -> override.friendPublicId() != null
                        && activeFriendPublicIds.contains(override.friendPublicId()))
                .sorted(Comparator.comparing(TimetableShareOverrideResponse::friendPublicId))
                .toList();
        return new TimetableSharingSettingsResponse(defaultScope, overrides);
    }
}
