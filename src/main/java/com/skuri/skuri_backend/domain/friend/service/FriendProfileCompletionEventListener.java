package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.member.event.MemberLifecycleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendProfileCompletionEventListener {

    private final FriendProfileProvisioningService provisioningService;

    @EventListener
    public void onMemberProfileCompleted(MemberLifecycleEvent.MemberProfileCompleted event) {
        try {
            provisioningService.ensureForActiveMember(event.memberId());
        } catch (RuntimeException exception) {
            log.error(
                    "프로필 완료 회원의 친구 공개 프로필 발급 실패, 기동 backfill에서 재시도합니다: memberId={}",
                    event.memberId(),
                    exception
            );
        }
    }
}
