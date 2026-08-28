package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiAppExamples {

    private OpenApiAppExamples() {
    }

    public static final String SUCCESS_APP_NOTICES = """
            {
              "success": true,
              "data": [
                {
                  "id": "app_notice_uuid",
                  "title": "앱 업데이트 안내",
                  "content": "새로운 기능이 추가되었습니다.",
                  "category": "UPDATE",
                  "priority": "NORMAL",
                  "imageUrls": ["https://cdn.skuri.app/notices/update.png"],
                  "actionUrl": "https://skuri.app/update",
                  "actionLabel": "업데이트 안내 보기",
                  "viewCount": 42,
                  "likeCount": 7,
                  "commentCount": 3,
                  "isLiked": false,
                  "publishedAt": "2026-02-01T00:00:00",
                  "createdAt": "2026-01-31T18:00:00",
                  "updatedAt": "2026-01-31T18:00:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_APP_NOTICE_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "app_notice_uuid",
                "title": "서버 점검 안내",
                "content": "2월 20일 새벽 2시~4시 서버 점검이 있습니다.",
                "category": "MAINTENANCE",
                "priority": "HIGH",
                "imageUrls": [],
                "actionUrl": null,
                "actionLabel": null,
                "viewCount": 101,
                "likeCount": 12,
                "commentCount": 4,
                "isLiked": true,
                "publishedAt": "2026-02-20T00:00:00",
                "createdAt": "2026-02-19T12:00:00",
                "updatedAt": "2026-02-19T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_APP_NOTICE_CREATE = """
            {
              "success": true,
              "data": {
                "id": "app_notice_uuid",
                "title": "서버 점검 안내",
                "createdAt": "2026-02-19T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_APP_NOTICE_UPDATE = SUCCESS_APP_NOTICE_DETAIL;

    public static final String SUCCESS_APP_NOTICE_UNREAD_COUNT = """
            {
              "success": true,
              "data": {
                "count": 2
              }
            }
            """;

    public static final String SUCCESS_APP_NOTICE_MARK_READ = """
            {
              "success": true,
              "data": {
                "appNoticeId": "app_notice_uuid",
                "isRead": true,
                "readAt": "2026-03-26T14:30:00"
              }
            }
            """;

    public static final String SUCCESS_APP_NOTICE_LIKE = """
            {"success":true,"data":{"isLiked":true,"likeCount":8}}
            """;

    public static final String SUCCESS_APP_NOTICE_UNLIKE = """
            {"success":true,"data":{"isLiked":false,"likeCount":7}}
            """;

    public static final String SUCCESS_APP_NOTICE_COMMENTS_LIST = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENTS_LIST;

    public static final String SUCCESS_APP_NOTICE_COMMENT_CREATE = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENT_CREATE;

    public static final String SUCCESS_APP_NOTICE_COMMENT_UPDATE = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENT_UPDATE;

    public static final String SUCCESS_APP_NOTICE_COMMENT_LIKE = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENT_LIKE;

    public static final String SUCCESS_APP_NOTICE_COMMENT_UNLIKE = OpenApiNoticeExamples.SUCCESS_NOTICE_COMMENT_UNLIKE;

    public static final String ERROR_APP_NOTICE_NOT_FOUND =
            "{\"success\":false,\"message\":\"앱 공지를 찾을 수 없습니다.\",\"errorCode\":\"APP_NOTICE_NOT_FOUND\",\"timestamp\":\"2026-03-06T12:00:00\"}";

    public static final String ERROR_APP_NOTICE_COMMENT_NOT_FOUND =
            "{\"success\":false,\"message\":\"앱 공지 댓글을 찾을 수 없습니다.\",\"errorCode\":\"APP_NOTICE_COMMENT_NOT_FOUND\",\"timestamp\":\"2026-03-06T12:00:00\"}";

    public static final String ERROR_NOT_APP_NOTICE_COMMENT_AUTHOR =
            "{\"success\":false,\"message\":\"앱 공지 댓글 작성자만 수정/삭제할 수 있습니다.\",\"errorCode\":\"NOT_APP_NOTICE_COMMENT_AUTHOR\",\"timestamp\":\"2026-03-06T12:00:00\"}";

    public static final String ERROR_APP_NOTICE_COMMENT_ALREADY_DELETED =
            "{\"success\":false,\"message\":\"이미 삭제된 댓글입니다.\",\"errorCode\":\"COMMENT_ALREADY_DELETED\",\"timestamp\":\"2026-03-06T12:00:00\"}";
}
