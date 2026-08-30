package com.skuri.skuri_backend.domain.member.policy;

import java.time.LocalDateTime;

public final class TermsConsentPolicy {

    public static final String CURRENT_VERSION = "2026-08-30";
    public static final LocalDateTime EMAIL_CONSENT_MEMBER_JOINED_AT_CUTOFF =
            LocalDateTime.of(2026, 8, 30, 10, 13, 9);

    private TermsConsentPolicy() {
    }
}
