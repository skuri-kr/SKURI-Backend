package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendProfileBackfillServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FriendProfileRepository friendProfileRepository;

    @Mock
    private FriendProfileProvisioningService provisioningService;

    @InjectMocks
    private FriendProfileBackfillService backfillService;

    @Test
    void 프로필이없는활성회원만_첫페이지부터_batch로_backfill한다() {
        when(memberRepository.findActiveMemberIdsWithoutFriendProfile(any(Pageable.class)))
                .thenReturn(List.of("member-1", "member-2"), List.of());
        when(memberRepository.countByStatus(MemberStatus.ACTIVE)).thenReturn(4L);
        when(friendProfileRepository.countForActiveMembers()).thenReturn(4L);

        backfillService.backfillActiveMemberProfiles();

        verify(provisioningService).ensureForActiveMember("member-1");
        verify(provisioningService).ensureForActiveMember("member-2");
        verify(memberRepository, never()).findAllMemberIds();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(memberRepository, times(2)).findActiveMemberIdsWithoutFriendProfile(pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues())
                .allSatisfy(pageable -> {
                    assertThat(pageable.getPageNumber()).isZero();
                    assertThat(pageable.getPageSize()).isEqualTo(100);
                });
    }

    @Test
    void 누락프로필이없으면_기존활성회원을잠그거나_provision하지않는다() {
        when(memberRepository.findActiveMemberIdsWithoutFriendProfile(any(Pageable.class)))
                .thenReturn(List.of());
        when(memberRepository.countByStatus(MemberStatus.ACTIVE)).thenReturn(3L);
        when(friendProfileRepository.countForActiveMembers()).thenReturn(3L);

        backfillService.backfillActiveMemberProfiles();

        verifyNoInteractions(provisioningService);
        verify(memberRepository, never()).findAllMemberIds();
    }
}
