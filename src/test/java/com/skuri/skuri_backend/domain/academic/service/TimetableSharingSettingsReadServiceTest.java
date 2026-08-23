package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableSharingSettingsReadServiceTest {

    @Mock
    private TimetableShareOverrideRepository timetableShareOverrideRepository;

    @Mock
    private TimetableSharingScopeResolver timetableSharingScopeResolver;

    @Mock
    private FriendRelationshipQueryService friendRelationshipQueryService;

    @Mock
    private FriendProfileRepository friendProfileRepository;

    @InjectMocks
    private TimetableSharingSettingsReadService readService;

    @Test
    void 이미준비된프로필의친구목록은_lazyProvisioning없이조회한다() {
        when(timetableSharingScopeResolver.defaultScope("owner"))
                .thenReturn(TimetableShareScope.PRIVATE);
        when(friendRelationshipQueryService.getFriendsForProvisionedMember("owner"))
                .thenReturn(List.of());
        when(timetableShareOverrideRepository.findAllByOwnerMemberId("owner"))
                .thenReturn(List.of());

        var response = readService.getForProvisionedMember("owner");

        assertThat(response.defaultScope()).isEqualTo(TimetableShareScope.PRIVATE);
        assertThat(response.overrides()).isEmpty();
        verify(friendRelationshipQueryService).getFriendsForProvisionedMember("owner");
        verify(friendRelationshipQueryService, never()).getFriends("owner");
    }
}
