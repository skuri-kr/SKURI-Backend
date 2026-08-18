package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.friend.entity.FriendCodeRegistry;
import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
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
class FriendProfileProvisioningAttemptService {

    private final MemberRepository memberRepository;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendCodeRegistryRepository friendCodeRegistryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FriendProfile ensureForActiveMember(String memberId, String publicId, String normalizedCode) {
        memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);

        return friendProfileRepository.findByMemberIdForUpdate(memberId)
                .orElseGet(() -> createProfile(memberId, publicId, normalizedCode));
    }

    private FriendProfile createProfile(String memberId, String publicId, String normalizedCode) {
        LocalDateTime now = LocalDateTime.now();
        FriendCodeRegistry code = friendCodeRegistryRepository.saveAndFlush(
                FriendCodeRegistry.issue(normalizedCode, memberId, now)
        );
        return friendProfileRepository.saveAndFlush(FriendProfile.create(memberId, publicId, code.getId()));
    }
}
