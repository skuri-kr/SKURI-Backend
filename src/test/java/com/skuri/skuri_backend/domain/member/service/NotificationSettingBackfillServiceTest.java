package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSettingBackfillServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private NotificationSettingBackfillService notificationSettingBackfillService;

    @Test
    void backfillNotificationSettingDefaults_null인행만일괄갱신한다() {
        when(memberRepository.backfillNotificationSettingDefaults()).thenReturn(3);

        notificationSettingBackfillService.backfillNotificationSettingDefaults();

        verify(memberRepository).backfillNotificationSettingDefaults();
    }
}
