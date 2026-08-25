package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.entity.FriendRequest;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendNotificationDispatchResolverTest {

    @Mock
    private FriendProfileRepository friendProfileRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private FriendNotificationDispatchResolver dispatchResolver;

    @Test
    void 수락알림은최신수락자공개식별자를사용한다() {
        FriendRequest request = request();
        request.accept(LocalDateTime.now());
        when(memberRepository.findActiveById("requester-1")).thenReturn(Optional.of(member("requester-1", "요청자")));
        when(memberRepository.findActiveById("recipient-1")).thenReturn(Optional.of(member("recipient-1", "수락자")));
        when(friendProfileRepository.findByMemberId("recipient-1"))
                .thenReturn(Optional.of(FriendProfile.create("recipient-1", "friend-public-1", "code-1")));

        NotificationDispatchRequest dispatch = dispatchResolver
                .resolve(FriendNotificationKind.REQUEST_ACCEPTED, request)
                .orElseThrow();

        assertThat(dispatch.type()).isEqualTo(NotificationType.FRIEND_ACCEPTED);
        assertThat(dispatch.data().friendPublicId()).isEqualTo("friend-public-1");
    }

    @Test
    void 친구알림을끄면친구요청dispatch를만들지않는다() {
        FriendRequest request = request();
        Member recipient = member("recipient-1", "수신자");
        ReflectionTestUtils.setField(recipient.getNotificationSetting(), "friendAndInvitationNotifications", false);
        when(memberRepository.findActiveById("requester-1")).thenReturn(Optional.of(member("requester-1", "요청자")));
        when(memberRepository.findActiveById("recipient-1")).thenReturn(Optional.of(recipient));

        assertThat(dispatchResolver.resolve(FriendNotificationKind.REQUEST_CREATED, request)).isEmpty();
    }

    private FriendRequest request() {
        FriendRequest request = FriendRequest.create(
                "requester-1", "recipient-1", "recipient-1:requester-1", LocalDateTime.now()
        );
        ReflectionTestUtils.setField(request, "id", "friend-request-1");
        return request;
    }

    private Member member(String memberId, String nickname) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", nickname, LocalDateTime.now());
        member.updateProfile(nickname, null, "20260001", "컴퓨터공학과", null);
        return member;
    }
}
