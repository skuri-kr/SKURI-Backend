package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.entity.FriendCodeRegistry;
import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.repository.FriendCodeRegistryRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendProfileProvisioningService {

    private static final int MAX_ISSUE_ATTEMPTS = 5;

    private final FriendProfileProvisioningAttemptService attemptService;
    private final FriendCodeGenerator friendCodeGenerator;
    private final FriendProfileRepository friendProfileRepository;
    private final FriendCodeRegistryRepository friendCodeRegistryRepository;

    public FriendProfile ensureForActiveMember(String memberId) {
        for (int attempt = 0; attempt < MAX_ISSUE_ATTEMPTS; attempt++) {
            try {
                return attemptService.ensureForActiveMember(
                        memberId,
                        UUID.randomUUID().toString(),
                        friendCodeGenerator.generateNormalizedCode()
                );
            } catch (DataIntegrityViolationException ignored) {
                // public_id 또는 normalized_code unique 충돌은 새 난수로 제한 횟수만 재시도한다.
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "친구 코드 발급 처리 중 충돌이 반복되었습니다.");
    }

    @Transactional
    public void retireForWithdrawnMember(String memberId, LocalDateTime withdrawnAt) {
        friendProfileRepository.findByMemberIdForUpdate(memberId).ifPresent(profile -> {
            FriendCodeRegistry activeCode = friendCodeRegistryRepository.findByIdForUpdate(profile.getActiveFriendCodeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "활성 친구 코드를 찾을 수 없습니다."));
            activeCode.retire(withdrawnAt);
            friendProfileRepository.delete(profile);
        });
    }
}
