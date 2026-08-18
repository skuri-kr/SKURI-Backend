package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodePreviewResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodeResponse;
import com.skuri.skuri_backend.domain.friend.entity.FriendCodeRegistry;
import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.exception.FriendCodeNotFoundException;
import com.skuri.skuri_backend.domain.friend.repository.FriendCodeRegistryRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendCodeService {

    private static final int MAX_REGENERATION_ATTEMPTS = 5;

    private final FriendProfileProvisioningService provisioningService;
    private final FriendCodeRegenerationAttemptService regenerationAttemptService;
    private final FriendCodeGenerator friendCodeGenerator;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendCodeRegistryRepository friendCodeRegistryRepository;
    private final MemberRepository memberRepository;
    private final FriendRelationshipQueryService friendRelationshipQueryService;

    @Transactional(readOnly = true)
    public FriendCodeResponse getMyCode(String memberId) {
        provisioningService.ensureForActiveMember(memberId);
        FriendProfile profile = friendProfileRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "친구 공개 프로필을 찾을 수 없습니다."));
        FriendCodeRegistry code = friendCodeRegistryRepository.findById(profile.getActiveFriendCodeId())
                .filter(registry -> registry.isActiveFor(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "활성 친구 코드를 찾을 수 없습니다."));
        LocalDateTime now = LocalDateTime.now();
        boolean canRegenerate = profile.canRegenerateAt(now);
        return new FriendCodeResponse(
                friendCodeGenerator.formatForDisplay(code.getNormalizedCode()),
                canRegenerate,
                canRegenerate ? null : profile.nextRegenerationAt()
        );
    }

    public FriendCodeResponse regenerateMyCode(String memberId) {
        provisioningService.ensureForActiveMember(memberId);
        for (int attempt = 0; attempt < MAX_REGENERATION_ATTEMPTS; attempt++) {
            LocalDateTime now = LocalDateTime.now();
            try {
                FriendProfile profile = regenerationAttemptService.regenerate(
                        memberId,
                        friendCodeGenerator.generateNormalizedCode(),
                        now
                );
                FriendCodeRegistry code = friendCodeRegistryRepository.findById(profile.getActiveFriendCodeId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "새 친구 코드를 찾을 수 없습니다."));
                return new FriendCodeResponse(
                        friendCodeGenerator.formatForDisplay(code.getNormalizedCode()),
                        false,
                        profile.nextRegenerationAt()
                );
            } catch (DataIntegrityViolationException ignored) {
                // 새 코드 충돌은 이전 코드 폐기와 함께 rollback되므로 GET으로 현재 코드를 조정할 수 있다.
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "친구 코드 재발급 처리 중 충돌이 반복되었습니다.");
    }

    @Transactional(readOnly = true)
    public FriendCodePreviewResponse preview(String requesterMemberId, String rawFriendCode) {
        provisioningService.ensureForActiveMember(requesterMemberId);
        String normalizedCode = friendCodeGenerator.normalizeForLookup(rawFriendCode);
        if (normalizedCode == null) {
            throw new FriendCodeNotFoundException();
        }

        FriendCodeRegistry code = friendCodeRegistryRepository.findByNormalizedCode(normalizedCode)
                .filter(registry -> registry.isActiveFor(registry.getOwnerMemberId()))
                .orElseThrow(FriendCodeNotFoundException::new);
        if (requesterMemberId.equals(code.getOwnerMemberId())) {
            throw new BusinessException(ErrorCode.FRIEND_SELF_NOT_ALLOWED);
        }

        FriendProfile profile = friendProfileRepository.findById(code.getOwnerMemberId())
                .filter(value -> value.getActiveFriendCodeId().equals(code.getId()))
                .orElseThrow(FriendCodeNotFoundException::new);
        Member member = memberRepository.findActiveById(code.getOwnerMemberId())
                .orElseThrow(FriendCodeNotFoundException::new);
        if (friendRelationshipQueryService.isBlockedPair(requesterMemberId, member.getId())) {
            throw new FriendCodeNotFoundException();
        }
        return new FriendCodePreviewResponse(
                profile.getPublicId(),
                member.getNickname(),
                member.getPhotoUrl(),
                member.getDepartment(),
                friendRelationshipQueryService.canSendFriendRequest(requesterMemberId, member.getId())
        );
    }
}
