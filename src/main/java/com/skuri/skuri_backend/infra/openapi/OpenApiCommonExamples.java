package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiCommonExamples {

    private OpenApiCommonExamples() {
    }

    public static final String SUCCESS_OBJECT = """
            {
              "success": true,
              "data": {}
            }
            """;

    public static final String SUCCESS_ARRAY = """
            {
              "success": true,
              "data": []
            }
            """;

    public static final String SUCCESS_NULL = """
            {
              "success": true,
              "data": null
            }
            """;

    public static final String SUCCESS_PAGE = """
            {
              "success": true,
              "data": {
                "content": [],
                "page": 0,
                "size": 20,
                "totalElements": 0,
                "hasNext": false
              }
            }
            """;

    public static final String ERROR_INVALID_REQUEST =
            "{\"success\":false,\"message\":\"잘못된 요청입니다.\",\"errorCode\":\"INVALID_REQUEST\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_UNAUTHORIZED =
            "{\"success\":false,\"message\":\"인증이 필요합니다.\",\"errorCode\":\"UNAUTHORIZED\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_FORBIDDEN =
            "{\"success\":false,\"message\":\"접근 권한이 없습니다.\",\"errorCode\":\"FORBIDDEN\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_ADMIN_REQUIRED =
            "{\"success\":false,\"message\":\"관리자 권한이 필요합니다.\",\"errorCode\":\"ADMIN_REQUIRED\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_EMAIL_DOMAIN_RESTRICTED =
            "{\"success\":false,\"message\":\"성결대학교 이메일(@sungkyul.ac.kr)만 사용 가능합니다.\",\"errorCode\":\"EMAIL_DOMAIN_RESTRICTED\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_NOT_FOUND =
            "{\"success\":false,\"message\":\"리소스를 찾을 수 없습니다.\",\"errorCode\":\"NOT_FOUND\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_MEMBER_NOT_FOUND =
            "{\"success\":false,\"message\":\"회원을 찾을 수 없습니다.\",\"errorCode\":\"MEMBER_NOT_FOUND\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_MEMBER_PROFILE_INCOMPLETE =
            "{\"success\":false,\"message\":\"회원가입을 완료한 후 친구 기능을 이용할 수 있습니다.\",\"errorCode\":\"MEMBER_PROFILE_INCOMPLETE\",\"timestamp\":\"2026-08-21T12:00:00\"}";

    public static final String ERROR_CONFLICT =
            "{\"success\":false,\"message\":\"리소스 충돌이 발생했습니다.\",\"errorCode\":\"CONFLICT\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_VALIDATION =
            "{\"success\":false,\"message\":\"입력값 검증에 실패했습니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_INTERNAL =
            "{\"success\":false,\"message\":\"서버 내부 오류가 발생했습니다.\",\"errorCode\":\"INTERNAL_ERROR\",\"timestamp\":\"2026-03-04T12:00:00\"}";
}
