package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiFriendExamples {

    private OpenApiFriendExamples() {
    }

    public static final String REQUEST_FRIEND_CODE_PREVIEW = """
            {
              "friendCode": "SKR-7K4M-9Q2D"
            }
            """;

    public static final String REQUEST_UPDATE_PRIVACY = """
            {
              "nicknameSearchable": true
            }
            """;

    public static final String SUCCESS_MY_FRIEND_CODE = """
            {
              "success": true,
              "data": {
                "friendCode": "SKR-7K4M-9Q2D",
                "canRegenerate": true,
                "nextRegenerationAt": null
              }
            }
            """;

    public static final String SUCCESS_REGENERATED_FRIEND_CODE = """
            {
              "success": true,
              "data": {
                "friendCode": "SKR-8P7N-4X5C",
                "canRegenerate": false,
                "nextRegenerationAt": "2026-08-19T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_FRIEND_CODE_PREVIEW = """
            {
              "success": true,
              "data": {
                "friendPublicId": "2fdbf426-a778-4b6a-8261-9c0549a8b2b4",
                "nickname": "스쿠리",
                "photoUrl": null,
                "department": "컴퓨터공학과",
                "canSendFriendRequest": true
              }
            }
            """;

    public static final String SUCCESS_FRIEND_PRIVACY = """
            {
              "success": true,
              "data": {
                "nicknameSearchable": true
              }
            }
            """;

    public static final String ERROR_FRIEND_CODE_NOT_FOUND =
            "{\"success\":false,\"message\":\"친구 코드를 찾을 수 없습니다.\",\"errorCode\":\"FRIEND_CODE_NOT_FOUND\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIEND_CODE_REGENERATION_COOLDOWN =
            "{\"success\":false,\"message\":\"친구 코드는 24시간에 한 번만 재발급할 수 있습니다.\",\"errorCode\":\"FRIEND_CODE_REGENERATION_COOLDOWN\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIEND_SELF_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"자기 자신의 친구 코드는 확인할 수 없습니다.\",\"errorCode\":\"FRIEND_SELF_NOT_ALLOWED\",\"timestamp\":\"2026-08-18T12:00:00\"}";
}
