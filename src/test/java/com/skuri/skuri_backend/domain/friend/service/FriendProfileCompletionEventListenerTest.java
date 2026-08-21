package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.member.event.MemberLifecycleEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendProfileCompletionEventListenerTest {

    @Mock
    private FriendProfileProvisioningService provisioningService;

    @InjectMocks
    private FriendProfileCompletionEventListener listener;

    @Test
    void 프로필완료이벤트를받으면_친구프로필발급을시도한다() {
        listener.onMemberProfileCompleted(new MemberLifecycleEvent.MemberProfileCompleted("member-1"));

        verify(provisioningService).ensureForActiveMember("member-1");
    }

    @Test
    void 발급실패는_프로필완료처리까지실패시키지않고_기동보정에맡긴다() {
        doThrow(new IllegalStateException("temporary failure"))
                .when(provisioningService).ensureForActiveMember("member-1");

        assertDoesNotThrow(() -> listener.onMemberProfileCompleted(
                new MemberLifecycleEvent.MemberProfileCompleted("member-1")
        ));
    }
}
