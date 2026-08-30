package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.policy.TermsConsentPolicy;
import com.skuri.skuri_backend.domain.member.repository.MemberTermsConsentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void recordIfRequested_현재약관동의면_SIGNUP출처로기록한다() {
        Member member = Member.create("member-1", "user@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        memberTermsConsentService.recordIfRequested(
                member,
                true,
                TermsConsentPolicy.CURRENT_VERSION,
                true
        );

        verify(memberTermsConsentRepository).insertSignupConsentIfAbsent(
                anyString(),
                eq(member.getId()),
                eq(TermsConsentPolicy.CURRENT_VERSION),
                any(LocalDateTime.class)
        );
    }

    @Test
    void recordIfRequested_프로필완료에동의가없으면_검증예외() {
        Member member = Member.create("member-1", "user@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberTermsConsentService.recordIfRequested(member, null, null, true)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals(
                "현재 이용약관에 동의해야 프로필 설정을 완료할 수 있습니다.",
                exception.getMessage()
        );
        verify(memberTermsConsentRepository, never()).insertSignupConsentIfAbsent(
                anyString(),
                anyString(),
                anyString(),
                any(LocalDateTime.class)
        );
    }

    @Test
    void recordIfRequested_기존프로필의동의필드생략은_변경하지않는다() {
        Member member = Member.create("member-1", "user@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        memberTermsConsentService.recordIfRequested(member, null, null, false);

        verify(memberTermsConsentRepository, never()).insertSignupConsentIfAbsent(
                anyString(),
                anyString(),
                anyString(),
                any(LocalDateTime.class)
        );
    }
}
