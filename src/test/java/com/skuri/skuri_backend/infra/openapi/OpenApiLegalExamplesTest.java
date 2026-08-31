package com.skuri.skuri_backend.infra.openapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiLegalExamplesTest {

    private static final String CURRENT_TERMS_DATE =
            "공고일: 2026년 8월 31일 · 시행일: 2026년 8월 31일";
    private static final String CURRENT_PRIVACY_DATE =
            "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일";
    private static final String PREVIOUS_EFFECTIVE_DATE =
            "시행일: 2025년 3월 1일 · 최종 수정: 2025년 3월 1일";

    @Test
    void 법적문서예시는_문서별현재공고일과시행일을사용한다() {
        assertThat(OpenApiLegalExamples.SUCCESS_LEGAL_DOCUMENT_DETAIL)
                .contains(CURRENT_TERMS_DATE)
                .doesNotContain(PREVIOUS_EFFECTIVE_DATE);
        assertThat(OpenApiLegalExamples.REQUEST_ADMIN_LEGAL_DOCUMENT_UPSERT)
                .contains(CURRENT_TERMS_DATE)
                .doesNotContain(PREVIOUS_EFFECTIVE_DATE);
        assertThat(OpenApiLegalExamples.SUCCESS_ADMIN_LEGAL_DOCUMENT_DETAIL)
                .contains(CURRENT_PRIVACY_DATE)
                .doesNotContain(PREVIOUS_EFFECTIVE_DATE);
    }
}
