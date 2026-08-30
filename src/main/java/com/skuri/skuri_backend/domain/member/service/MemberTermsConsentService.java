package com.skuri.skuri_backend.domain.member.service;

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

    private final MemberTermsConsentRepository memberTermsConsentRepository;

    public void recordForProfileCompletion(Member member, boolean profileCompletedNow) {
        if (!profileCompletedNow) {
            return;
        }
        memberTermsConsentRepository.upsertSignupConsent(
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
