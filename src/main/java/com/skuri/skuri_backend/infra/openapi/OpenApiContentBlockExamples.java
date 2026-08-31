package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiContentBlockExamples {

    private OpenApiContentBlockExamples() {
    }

    public static final String REQUEST_CREATE = """
            {
              "targetType": "COMMENT",
              "targetId": "comment_uuid"
            }
            """;

    public static final String SUCCESS_CREATE = """
            {
              "success": true,
              "data": {
                "blockId": "81e33b43-2df2-49df-bc33-e7832e7801b5",
                "label": "차단한 사용자",
                "blockedAt": "2026-08-31T18:30:00"
              }
            }
            """;

    public static final String SUCCESS_LIST = """
            {
              "success": true,
              "data": [{
                "blockId": "81e33b43-2df2-49df-bc33-e7832e7801b5",
                "label": "차단한 사용자",
                "blockedAt": "2026-08-31T18:30:00"
              }]
            }
            """;

    public static final String ERROR_SELF_BLOCK_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"자기 자신의 콘텐츠는 차단할 수 없습니다.\",\"errorCode\":\"CONTENT_BLOCK_SELF_NOT_ALLOWED\",\"timestamp\":\"2026-08-31T18:30:00\"}";
}
