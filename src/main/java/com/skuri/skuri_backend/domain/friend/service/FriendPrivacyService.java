package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendPrivacyResponse;
import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendPrivacyService {

    private final FriendProfileProvisioningService provisioningService;
    private final MemberRepository memberRepository;
    private final FriendProfileRepository friendProfileRepository;

    @Transactional(readOnly = true)
    public FriendPrivacyResponse getMyPrivacy(String memberId) {
        provisioningService.ensureForActiveMember(memberId);
        return friendProfileRepository.findById(memberId)
                .map(profile -> new FriendPrivacyResponse(profile.isNicknameSearchable()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "친구 공개 프로필을 찾을 수 없습니다."));
    }

    @Transactional
    public FriendPrivacyResponse updateMyPrivacy(String memberId, boolean nicknameSearchable) {
        provisioningService.ensureForActiveMember(memberId);
        memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);
        FriendProfile profile = friendProfileRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "친구 공개 프로필을 찾을 수 없습니다."));
        profile.updateNicknameSearchable(nicknameSearchable);
        return new FriendPrivacyResponse(profile.isNicknameSearchable());
    }
}
