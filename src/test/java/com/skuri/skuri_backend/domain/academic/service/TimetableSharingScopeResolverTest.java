package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.domain.academic.entity.TimetableShareOverride;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import com.skuri.skuri_backend.domain.academic.entity.TimetableSharingSetting;
import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.academic.repository.TimetableSharingSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableSharingScopeResolverTest {

    @Mock
    private TimetableSharingSettingRepository timetableSharingSettingRepository;

    @Mock
    private TimetableShareOverrideRepository timetableShareOverrideRepository;

    @InjectMocks
    private TimetableSharingScopeResolver timetableSharingScopeResolver;

    @Test
    void 친구목록범위는_친구가_조회자에게공개한설정과예외를따른다() {
        List<String> friendMemberIds = List.of("friend-a", "friend-b");
        when(timetableSharingSettingRepository.findAllById(friendMemberIds)).thenReturn(List.of(
                TimetableSharingSetting.create("friend-a", TimetableShareScope.BUSY_ONLY),
                TimetableSharingSetting.create("friend-b", TimetableShareScope.DETAILS)
        ));
        when(timetableShareOverrideRepository.findAllByFriendMemberIdAndOwnerMemberIdIn(
                "viewer", friendMemberIds
        )).thenReturn(List.of(
                TimetableShareOverride.create("friend-a", "viewer", TimetableShareScope.PRIVATE)
        ));

        Map<String, TimetableShareScope> result = timetableSharingScopeResolver.resolveScopesForViewer(
                friendMemberIds,
                "viewer"
        );

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "friend-a", TimetableShareScope.PRIVATE,
                "friend-b", TimetableShareScope.DETAILS
        ));
        verify(timetableShareOverrideRepository).findAllByFriendMemberIdAndOwnerMemberIdIn(
                eq("viewer"),
                eq(friendMemberIds)
        );
    }
}
