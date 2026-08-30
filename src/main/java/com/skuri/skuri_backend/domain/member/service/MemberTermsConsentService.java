package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.policy.TermsConsentPolicy;
import com.skuri.skuri_backend.domain.member.repository.MemberTermsConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberTermsConsentService {

    private static final String CONSENT_REQUIRED_MESSAGE =
            "현재 이용약관에 동의해야 프로필 설정을 완료할 수 있습니다.";

    private final MemberTermsConsentRepository memberTermsConsentRepository;

    public void recordIfRequested(
            Member member,
            Boolean termsAccepted,
            String termsVersion,
            boolean requiredForProfileCompletion
    ) {
        if (termsAccepted == null && !requiredForProfileCompletion) {
            return;
        }
        if (!Boolean.TRUE.equals(termsAccepted)
                || !TermsConsentPolicy.CURRENT_VERSION.equals(termsVersion)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, CONSENT_REQUIRED_MESSAGE);
        }
        memberTermsConsentRepository.insertSignupConsentIfAbsent(
                UUID.randomUUID().toString(),
                member.getId(),
                TermsConsentPolicy.CURRENT_VERSION,
                LocalDateTime.now()
        );
    }

    public boolean hasCurrentConsent(String memberId) {
        return memberTermsConsentRepository.existsByMember_IdAndTermsVersion(
                memberId,
                TermsConsentPolicy.CURRENT_VERSION
        );
    }
}
