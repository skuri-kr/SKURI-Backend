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

    public static final String REQUEST_CREATE_FRIEND_REQUEST = """
            {
              "friendPublicId": "2fdbf426-a778-4b6a-8261-9c0549a8b2b4"
            }
            """;

    public static final String REQUEST_UPDATE_FAVORITE = """
            {
              "favorite": true
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
                "relationshipState": "REQUESTABLE"
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

    public static final String SUCCESS_FRIEND_LIST = """
            {
              "success": true,
              "data": [{
                "friendPublicId": "2fdbf426-a778-4b6a-8261-9c0549a8b2b4",
                "nickname": "스쿠리",
                "department": "컴퓨터공학과",
                "photoUrl": null,
                "favorite": true,
                "primaryMinecraftGameName": "skuriPlayer",
                "minecraftAccountCount": 3
              }]
            }
            """;

    public static final String SUCCESS_FRIEND_DETAIL = """
            {
              "success": true,
              "data": {
                "friendPublicId": "2fdbf426-a778-4b6a-8261-9c0549a8b2b4",
                "nickname": "스쿠리",
                "department": "컴퓨터공학과",
                "photoUrl": null,
                "favorite": true,
                "primaryMinecraftGameName": "skuriPlayer",
                "minecraftAccountCount": 3
              }
            }
            """;

    public static final String SUCCESS_FRIEND_MINECRAFT_ACCOUNTS = """
            {
              "success": true,
              "data": {
                "selfAccounts": [{
                  "gameName": "skuriPlayer",
                  "edition": "JAVA",
                  "avatarUuid": "8667ba71b85a4004af54457a9734eed7",
                  "friendAccounts": [{
                    "gameName": "skuriBedrock",
                    "edition": "BEDROCK",
                    "avatarUuid": "8667ba71b85a4004af54457a9734eed7"
                  }]
                }]
              }
            }
            """;

    public static final String SUCCESS_FRIEND_REQUEST_PENDING = """
            {
              "success": true,
              "data": {
                "status": "PENDING",
                "requestId": "6b8dd965-5f04-45dd-bbab-8a043e64222e"
              }
            }
            """;

    public static final String SUCCESS_FRIEND_REQUEST_ACCEPTED = """
            {
              "success": true,
              "data": {
                "status": "ACCEPTED",
                "requestId": "6b8dd965-5f04-45dd-bbab-8a043e64222e",
                "friend": {
                  "friendPublicId": "2fdbf426-a778-4b6a-8261-9c0549a8b2b4",
                  "nickname": "스쿠리",
                  "department": "컴퓨터공학과",
                  "photoUrl": null,
                  "favorite": false
                }
              }
            }
            """;

    public static final String SUCCESS_FRIEND_SEARCH = """
            {
              "success": true,
              "data": {
                "items": [{
                  "friendPublicId": "2fdbf426-a778-4b6a-8261-9c0549a8b2b4",
                  "nickname": "스쿠리",
                  "department": "컴퓨터공학과",
                  "photoUrl": null,
                  "relationshipState": "REQUESTABLE"
                }],
                "hasNext": false,
                "nextCursor": null
              }
            }
            """;

    public static final String SUCCESS_FRIEND_REQUEST_PAGE = """
            {
              "success": true,
              "data": {
                "items": [{
                  "requestId": "6b8dd965-5f04-45dd-bbab-8a043e64222e",
                  "friendPublicId": "2fdbf426-a778-4b6a-8261-9c0549a8b2b4",
                  "nickname": "스쿠리",
                  "department": "컴퓨터공학과",
                  "photoUrl": null,
                  "createdAt": "2026-08-18T12:00:00",
                  "expiresAt": "2026-09-17T12:00:00"
                }],
                "hasNext": false,
                "nextCursor": null
              }
            }
            """;

    public static final String SUCCESS_FRIEND_INBOX_COUNTS = """
            {
              "success": true,
              "data": {
                "incomingRequestCount": 1,
                "partyInvitationCount": 0,
                "chatRoomInvitationCount": 0,
                "totalActionCount": 1
              }
            }
            """;

    public static final String SUCCESS_FRIEND_BLOCK_LIST = """
            {
              "success": true,
              "data": [{
                "friendPublicId": "2fdbf426-a778-4b6a-8261-9c0549a8b2b4",
                "nickname": "스쿠리",
                "department": "컴퓨터공학과",
                "photoUrl": null,
                "blockedAt": "2026-08-18T12:00:00"
              }]
            }
            """;

    public static final String ERROR_FRIEND_CODE_NOT_FOUND =
            "{\"success\":false,\"message\":\"친구 코드를 찾을 수 없습니다.\",\"errorCode\":\"FRIEND_CODE_NOT_FOUND\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIEND_CODE_REGENERATION_COOLDOWN =
            "{\"success\":false,\"message\":\"친구 코드는 24시간에 한 번만 재발급할 수 있습니다.\",\"errorCode\":\"FRIEND_CODE_REGENERATION_COOLDOWN\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIEND_SELF_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"자기 자신의 친구 코드는 확인할 수 없습니다.\",\"errorCode\":\"FRIEND_SELF_NOT_ALLOWED\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIEND_TARGET_NOT_FOUND =
            "{\"success\":false,\"message\":\"친구 대상을 찾을 수 없습니다.\",\"errorCode\":\"FRIEND_TARGET_NOT_FOUND\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIENDSHIP_NOT_FOUND =
            "{\"success\":false,\"message\":\"친구 관계를 찾을 수 없습니다.\",\"errorCode\":\"FRIENDSHIP_NOT_FOUND\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIEND_REQUEST_NOT_FOUND =
            "{\"success\":false,\"message\":\"친구 요청을 찾을 수 없습니다.\",\"errorCode\":\"FRIEND_REQUEST_NOT_FOUND\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIEND_REQUEST_ALREADY_PENDING =
            "{\"success\":false,\"message\":\"이미 친구 요청을 보냈습니다.\",\"errorCode\":\"FRIEND_REQUEST_ALREADY_PENDING\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIEND_REQUEST_STATE_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"현재 상태에서는 친구 요청을 처리할 수 없습니다.\",\"errorCode\":\"FRIEND_REQUEST_STATE_NOT_ALLOWED\",\"timestamp\":\"2026-08-18T12:00:00\"}";
    public static final String ERROR_FRIEND_SELF_BLOCK_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"자기 자신은 차단할 수 없습니다.\",\"errorCode\":\"FRIEND_SELF_BLOCK_NOT_ALLOWED\",\"timestamp\":\"2026-08-18T12:00:00\"}";
}
