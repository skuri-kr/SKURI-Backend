package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiShareExamples {

    private OpenApiShareExamples() {
    }

    public static final String REQUEST_CREATE = """
            {"resourceType":"NOTICE","resourceId":"notice-internal-id"}
            """;
    public static final String SUCCESS_CREATE = """
            {"success":true,"data":{"resourceType":"NOTICE","code":"7Kp3mQxA","url":"https://link.skuri.kr/notice/7Kp3mQxA"}}
            """;
    public static final String SUCCESS_RESOLVE = """
            {"success":true,"data":{"resourceType":"NOTICE","code":"7Kp3mQxA","resourceId":"notice-internal-id"}}
            """;
    public static final String SUCCESS_NOTICE_PREVIEW = """
            {"success":true,"data":{"code":"7Kp3mQxA","title":"2026학년도 수강 안내","category":"학사","department":"교무처","author":"성결대학교","postedAt":"2026-08-28T09:00:00","blocks":[{"type":"TEXT","text":"수강 신청 일정을 안내합니다.","truncated":false},{"type":"TABLE","rows":[{"cells":[{"text":"구분","header":true,"rowSpan":1,"colSpan":1},{"text":"일정","header":true,"rowSpan":1,"colSpan":1}]}],"truncated":true}],"truncated":true}}
            """;
    public static final String SUCCESS_BOARD_PREVIEW = """
            {"success":true,"data":{"code":"5Rm2Qn8B","title":"교내 행사 같이 가실 분","category":"GENERAL","author":"익명","createdAt":"2026-08-28T10:30:00","content":"행사에 같이 가실 분을 구합니다.","truncated":false}}
            """;
    public static final String SUCCESS_CAFETERIA_PREVIEW = """
            {"success":true,"data":{"weekId":"2026-W35","weekStart":"2026-08-24","weekEnd":"2026-08-30","categories":[{"code":"LUNCH","label":"중식"}],"days":{"2026-08-28":{"LUNCH":[{"title":"제육덮밥","badges":[{"code":"SPICY","label":"매콤"}]}]}}}}
            """;
    public static final String ERROR_NOT_FOUND =
            "{\"success\":false,\"message\":\"공유 링크를 찾을 수 없습니다.\",\"errorCode\":\"SHARE_LINK_NOT_FOUND\",\"timestamp\":\"2026-08-28T12:00:00\"}";
}
