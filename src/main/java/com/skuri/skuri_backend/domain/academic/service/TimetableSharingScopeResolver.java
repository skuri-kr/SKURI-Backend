package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.domain.academic.entity.TimetableShareOverride;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.academic.repository.TimetableSharingSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableSharingScopeResolver {

    private final TimetableSharingSettingRepository timetableSharingSettingRepository;
    private final TimetableShareOverrideRepository timetableShareOverrideRepository;

    @Transactional(readOnly = true)
    public TimetableShareScope resolveScope(String ownerMemberId, String friendMemberId) {
        TimetableShareScope defaultScope = defaultScope(ownerMemberId);
        return timetableShareOverrideRepository
                .findById(new TimetableShareOverride.Key(ownerMemberId, friendMemberId))
                .map(TimetableShareOverride::getScope)
                .orElse(defaultScope);
    }

    @Transactional(readOnly = true)
    public Map<String, TimetableShareScope> resolveScopes(
            String ownerMemberId,
            Collection<String> friendMemberIds
    ) {
        if (friendMemberIds.isEmpty()) {
            return Map.of();
        }
        TimetableShareScope defaultScope = defaultScope(ownerMemberId);
        Map<String, TimetableShareScope> overrideScopes = timetableShareOverrideRepository
                .findAllByOwnerMemberIdAndFriendMemberIdIn(ownerMemberId, friendMemberIds)
                .stream()
                .collect(Collectors.toMap(TimetableShareOverride::getFriendMemberId, TimetableShareOverride::getScope));
        return friendMemberIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        friendMemberId -> overrideScopes.getOrDefault(friendMemberId, defaultScope)
                ));
    }

    @Transactional(readOnly = true)
    public TimetableShareScope defaultScope(String ownerMemberId) {
        return timetableSharingSettingRepository.findById(ownerMemberId)
                .map(setting -> setting.getDefaultScope())
                .orElse(TimetableShareScope.PRIVATE);
    }
}
