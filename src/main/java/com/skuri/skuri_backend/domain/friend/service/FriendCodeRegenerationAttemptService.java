package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.entity.FriendCodeRegistry;
import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.exception.FriendCodeRegenerationCooldownException;
import com.skuri.skuri_backend.domain.friend.repository.FriendCodeRegistryRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
class FriendCodeRegenerationAttemptService {

    private final MemberRepository memberRepository;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendCodeRegistryRepository friendCodeRegistryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FriendProfile regenerate(String memberId, String normalizedCode, LocalDateTime now) {
        memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);
        FriendProfile profile = friendProfileRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "친구 공개 프로필을 찾을 수 없습니다."));
        if (!profile.canRegenerateAt(now)) {
            throw new FriendCodeRegenerationCooldownException(now, profile.nextRegenerationAt());
        }

        FriendCodeRegistry currentCode = friendCodeRegistryRepository.findByIdForUpdate(profile.getActiveFriendCodeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "활성 친구 코드를 찾을 수 없습니다."));
        if (!currentCode.isActiveFor(memberId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "활성 친구 코드 상태가 올바르지 않습니다.");
        }

        currentCode.retire(now);
        friendCodeRegistryRepository.saveAndFlush(currentCode);
        FriendCodeRegistry nextCode = friendCodeRegistryRepository.saveAndFlush(
                FriendCodeRegistry.issue(normalizedCode, memberId, now)
        );
        profile.replaceActiveFriendCode(nextCode.getId(), now);
        return profile;
    }
}
