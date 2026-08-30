package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.policy.TermsConsentPolicy;
import com.skuri.skuri_backend.domain.member.repository.MemberTermsConsentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberTermsConsentServiceTest {

    @Mock
    private MemberTermsConsentRepository memberTermsConsentRepository;

    @InjectMocks
    private MemberTermsConsentService memberTermsConsentService;

    @Test
    void recordForProfileCompletion_프로필이처음완료되면_SIGNUP동의를현재시각으로기록한다() {
        Member member = Member.create("member-1", "user@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        memberTermsConsentService.recordForProfileCompletion(member, true);

        verify(memberTermsConsentRepository).upsertSignupConsent(
                anyString(),
                eq(member.getId()),
                eq(TermsConsentPolicy.CURRENT_VERSION),
                any(LocalDateTime.class)
        );
    }

    @Test
    void recordForProfileCompletion_프로필완료전환이아니면_동의행을변경하지않는다() {
        Member member = Member.create("member-1", "user@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        memberTermsConsentService.recordForProfileCompletion(member, false);

        verify(memberTermsConsentRepository, never()).upsertSignupConsent(
                anyString(),
                anyString(),
                anyString(),
                any(LocalDateTime.class)
        );
    }
}
