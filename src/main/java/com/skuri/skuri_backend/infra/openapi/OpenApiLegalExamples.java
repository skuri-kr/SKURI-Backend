package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiLegalExamples {

    private OpenApiLegalExamples() {
    }

    public static final String SUCCESS_LEGAL_DOCUMENT_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "termsOfUse",
                "title": "이용약관",
                "banner": {
                  "iconKey": "document",
                  "lines": [
                    {
                      "text": "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일",
                      "tone": "primary"
                    }
                  ],
                  "title": "SKURI 이용약관",
                  "tone": "green"
                },
                "sections": [
                  {
                    "id": "article-01",
                    "paragraphs": [
                      "이 약관은 스쿠리 (이하 '회사' 라고 합니다)가 제공하는 제반 서비스의 이용과 관련하여 회사와 회원과의 권리, 의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다."
                    ],
                    "title": "제1조(목적)"
                  }
                ],
                "footerLines": [
                  "본 약관에 대한 문의는",
                  "앱 내 문의하기를 이용해 주세요."
                ]
              }
            }
            """;

    public static final String SUCCESS_ADMIN_LEGAL_DOCUMENT_SUMMARIES = """
            {
              "success": true,
              "data": [
                {
                  "id": "privacyPolicy",
                  "title": "개인정보 처리방침",
                  "isActive": true,
                  "updatedAt": "2026-03-28T10:00:00"
                },
                {
                  "id": "termsOfUse",
                  "title": "이용약관",
                  "isActive": true,
                  "updatedAt": "2026-03-28T10:00:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_ADMIN_LEGAL_DOCUMENT_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "privacyPolicy",
                "title": "개인정보 처리방침",
                "banner": {
                  "iconKey": "shield",
                  "lines": [
                    {
                      "text": "SKURI는 이용자의 개인정보를 소중히 보호합니다.",
                      "tone": "primary"
                    },
                    {
                      "text": "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일",
                      "tone": "secondary"
                    }
                  ],
                  "title": "SKURI 개인정보 처리방침",
                  "tone": "blue"
                },
                "sections": [
                  {
                    "id": "article-01",
                    "paragraphs": [
                      "스쿠리(이하 '회사'라고 함)는 회사가 제공하고자 하는 서비스(이하 '회사 서비스')를 이용하는 개인(이하 '이용자' 또는 '개인')의 정보(이하 '개인정보')를 보호하기 위해..."
                    ],
                    "title": "제1조(목적)"
                  }
                ],
                "footerLines": [
                  "개인정보 관련 문의는",
                  "앱 내 문의하기를 이용해 주세요."
                ],
                "isActive": true,
                "createdAt": "2026-03-28T10:00:00",
                "updatedAt": "2026-03-28T10:00:00"
              }
            }
            """;

    public static final String REQUEST_ADMIN_LEGAL_DOCUMENT_UPSERT = """
            {
              "title": "이용약관",
              "banner": {
                "iconKey": "document",
                "lines": [
                  {
                    "text": "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일",
                    "tone": "primary"
                  }
                ],
                "title": "SKURI 이용약관",
                "tone": "green"
              },
              "sections": [
                {
                  "id": "article-01",
                  "paragraphs": [
                    "이 약관은 회사와 회원 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다."
                  ],
                  "title": "제1조(목적)"
                }
              ],
              "footerLines": [
                "본 약관에 대한 문의는",
                "앱 내 문의하기를 이용해 주세요."
              ],
              "isActive": true
            }
            """;

    public static final String SUCCESS_ADMIN_LEGAL_DOCUMENT_UPSERT = SUCCESS_ADMIN_LEGAL_DOCUMENT_DETAIL;

    public static final String SUCCESS_ADMIN_LEGAL_DOCUMENT_DELETE = """
            {
              "success": true,
              "data": {
                "id": "termsOfUse"
              }
            }
            """;

    public static final String ERROR_LEGAL_DOCUMENT_NOT_FOUND =
            "{\"success\":false,\"message\":\"법적 문서를 찾을 수 없습니다.\",\"errorCode\":\"LEGAL_DOCUMENT_NOT_FOUND\",\"timestamp\":\"2026-03-28T12:00:00\"}";
}
