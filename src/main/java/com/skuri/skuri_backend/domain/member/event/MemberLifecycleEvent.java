package com.skuri.skuri_backend.domain.member.event;

public sealed interface MemberLifecycleEvent permits MemberLifecycleEvent.MemberProfileCompleted, MemberLifecycleEvent.MemberWithdrawn {

    record MemberProfileCompleted(String memberId) implements MemberLifecycleEvent {
    }

    record MemberWithdrawn(String memberId) implements MemberLifecycleEvent {
    }
}
