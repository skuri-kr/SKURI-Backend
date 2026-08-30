package com.skuri.skuri_backend.infra.openapi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiLegalExamplesTest {

    private static final String CURRENT_EFFECTIVE_DATE =
            "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일";
    private static final String PREVIOUS_EFFECTIVE_DATE =
            "시행일: 2025년 3월 1일 · 최종 수정: 2025년 3월 1일";

    @Test
    void 법적문서예시는_현재시행일을사용한다() {
        for (String example : List.of(
                OpenApiLegalExamples.SUCCESS_LEGAL_DOCUMENT_DETAIL,
                OpenApiLegalExamples.SUCCESS_ADMIN_LEGAL_DOCUMENT_DETAIL,
                OpenApiLegalExamples.REQUEST_ADMIN_LEGAL_DOCUMENT_UPSERT
        )) {
            assertThat(example)
                    .contains(CURRENT_EFFECTIVE_DATE)
                    .doesNotContain(PREVIOUS_EFFECTIVE_DATE);
        }
    }
}
