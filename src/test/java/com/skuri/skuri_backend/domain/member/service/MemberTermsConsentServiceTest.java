package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberTermsConsent;
import com.skuri.skuri_backend.domain.member.policy.TermsConsentPolicy;
import com.skuri.skuri_backend.domain.member.repository.MemberTermsConsentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberTermsConsentServiceTest {

    @Mock
    private MemberTermsConsentRepository memberTermsConsentRepository;

    @InjectMocks
    private MemberTermsConsentService memberTermsConsentService;

    @Test
    void recordIfRequested_현재약관동의면_SIGNUP출처로기록한다() {
        Member member = Member.create("member-1", "user@sungkyul.ac.kr", "사용자", LocalDateTime.now());
        when(memberTermsConsentRepository.existsByMember_IdAndTermsVersion(
                member.getId(),
                TermsConsentPolicy.CURRENT_VERSION
        )).thenReturn(false, true);

        memberTermsConsentService.recordIfRequested(
                member,
                true,
                TermsConsentPolicy.CURRENT_VERSION,
                true
        );

        ArgumentCaptor<MemberTermsConsent> captor = ArgumentCaptor.forClass(MemberTermsConsent.class);
        verify(memberTermsConsentRepository).saveAndFlush(captor.capture());
        assertEquals(member, captor.getValue().getMember());
        assertEquals(TermsConsentPolicy.CURRENT_VERSION, captor.getValue().getTermsVersion());
        assertEquals("SIGNUP", captor.getValue().getSource().name());
        assertTrue(captor.getValue().getAcceptedAt() != null);
    }

    @Test
    void recordIfRequested_프로필완료에동의가없으면_검증예외() {
        Member member = Member.create("member-1", "user@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberTermsConsentService.recordIfRequested(member, null, null, true)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(memberTermsConsentRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordIfRequested_기존프로필의동의필드생략은_변경하지않는다() {
        Member member = Member.create("member-1", "user@sungkyul.ac.kr", "사용자", LocalDateTime.now());

        memberTermsConsentService.recordIfRequested(member, null, null, false);

        verify(memberTermsConsentRepository, never()).saveAndFlush(any());
    }
}
