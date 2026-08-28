package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiNoticeExamples {

    private OpenApiNoticeExamples() {
    }

    public static final String SUCCESS_NOTICE_LIST_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "bm90aWNlLTE",
                    "title": "2026학년도 1학기 수강신청 안내",
                    "rssPreview": "수강신청 일정, 대상 학년, 유의사항을 안내합니다.",
                    "category": "학사",
                    "department": "성결대학교",
                    "author": "교무처",
                    "postedAt": "2026-02-01T09:00:00",
                    "viewCount": 500,
                    "likeCount": 10,
                    "commentCount": 10,
                    "bookmarkCount": 3,
                    "isRead": true,
                    "isLiked": false,
                    "isBookmarked": true,
                    "isCommentedByMe": true,
                    "thumbnailUrl": "https://www.sungkyul.ac.kr/upload/notice-thumb.jpg"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1,
                "totalPages": 1,
                "hasNext": false,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_NOTICE_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "bm90aWNlLTE",
                "title": "2026학년도 1학기 수강신청 안내",
                "rssPreview": "수강신청 안내 요약 내용",
                "bodyHtml": "<p>상세 본문</p>",
                "link": "https://www.sungkyul.ac.kr/bbs/skukr/96/12345/artclView.do",
                "category": "학사",
                "department": "성결대학교",
                "author": "교무처",
                "source": "RSS",
                "postedAt": "2026-02-01T09:00:00",
                "viewCount": 501,
                "likeCount": 11,
                "commentCount": 10,
                "bookmarkCount": 4,
                "attachments": [
                  {
                    "name": "수강신청 안내.pdf",
                    "downloadUrl": "https://www.sungkyul.ac.kr/download.do?file=1",
                    "previewUrl": "https://www.sungkyul.ac.kr/synapView.do?file=1"
                  }
                ],
                "isRead": true,
                "isLiked": true,
                "isBookmarked": true
              }
            }
            """;

    public static final String SUCCESS_NOTICE_READ = """
            {
              "success": true,
              "data": {
                "noticeId": "bm90aWNlLTE",
                "isRead": true,
                "readAt": "2026-02-01T12:34:56"
              }
            }
            """;

    public static final String SUCCESS_NOTICE_LIKE = """
            {
              "success": true,
              "data": {
                "isLiked": true,
                "likeCount": 11
              }
            }
            """;

    public static final String SUCCESS_NOTICE_BOOKMARK = """
            {
              "success": true,
              "data": {
                "isBookmarked": true,
                "bookmarkCount": 4
              }
            }
            """;

    public static final String SUCCESS_NOTICE_BOOKMARK_REMOVED = """
            {
              "success": true,
              "data": {
                "isBookmarked": false,
                "bookmarkCount": 3
              }
            }
            """;

    public static final String SUCCESS_NOTICE_BOOKMARK_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "bm90aWNlLTE",
                    "title": "2026학년도 1학기 수강신청 안내",
                    "rssPreview": "수강신청 일정, 대상 학년, 유의사항을 안내합니다.",
                    "category": "학사",
                    "postedAt": "2026-02-01T09:00:00"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1,
                "totalPages": 1,
                "hasNext": false,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_NOTICE_COMMENTS_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "notice-comment-1",
                  "parentId": null,
                  "depth": 0,
                  "content": "댓글 내용",
                  "authorId": "member-1",
                  "authorName": "홍길동",
                  "authorProfileImage": "https://cdn.skuri.app/profiles/member-1-current.png",
                  "isAnonymous": false,
                  "anonymousOrder": null,
                  "isAuthor": true,
                  "likeCount": 5,
                  "isLiked": true,
                  "isDeleted": false,
                  "createdAt": "2026-02-03T12:00:00",
                  "updatedAt": "2026-02-03T12:00:00"
                },
                {
                  "id": "notice-comment-2",
                  "parentId": "notice-comment-1",
                  "depth": 1,
                  "content": "대댓글 내용",
                  "authorId": null,
                  "authorName": "익명2",
                  "authorProfileImage": null,
                  "isAnonymous": true,
                  "anonymousOrder": 2,
                  "isAuthor": false,
                  "likeCount": 0,
                  "isLiked": false,
                  "isDeleted": false,
                  "createdAt": "2026-02-03T12:10:00",
                  "updatedAt": "2026-02-03T12:10:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_NOTICE_COMMENT_CREATE = """
            {
              "success": true,
              "data": {
                "id": "notice-comment-1",
                "parentId": "notice-comment-parent",
                "depth": 1,
                "content": "댓글 내용",
                "authorId": "member-1",
                "authorName": "홍길동",
                "authorProfileImage": "https://cdn.skuri.app/profiles/member-1-current.png",
                "isAnonymous": false,
                "anonymousOrder": null,
                "isAuthor": true,
                "likeCount": 5,
                "isLiked": true,
                "isDeleted": false,
                "createdAt": "2026-02-03T12:00:00",
                "updatedAt": "2026-02-03T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_NOTICE_COMMENT_UPDATE = """
            {
              "success": true,
              "data": {
                "id": "notice-comment-1",
                "parentId": "notice-comment-parent",
                "depth": 1,
                "content": "수정된 댓글 내용",
                "authorId": null,
                "authorName": "익명1",
                "authorProfileImage": null,
                "isAnonymous": true,
                "anonymousOrder": 1,
                "isAuthor": true,
                "likeCount": 5,
                "isLiked": true,
                "isDeleted": false,
                "createdAt": "2026-02-03T12:00:00",
                "updatedAt": "2026-02-03T12:30:00"
              }
            }
            """;

    public static final String SUCCESS_NOTICE_COMMENT_LIKE = """
            {
              "success": true,
              "data": {
                "commentId": "notice_comment_uuid",
                "isLiked": true,
                "likeCount": 5
              }
            }
            """;

    public static final String SUCCESS_NOTICE_COMMENT_UNLIKE = """
            {
              "success": true,
              "data": {
                "commentId": "notice_comment_uuid",
                "isLiked": false,
                "likeCount": 4
              }
            }
            """;

    public static final String SUCCESS_NOTICE_SYNC = """
            {
              "success": true,
              "data": {
                "created": 15,
                "updated": 3,
                "skipped": 42,
                "failed": 2,
                "syncedAt": "2026-02-19T12:00:00"
              }
            }
            """;

    public static final String ERROR_NOTICE_NOT_FOUND =
            "{\"success\":false,\"message\":\"공지사항을 찾을 수 없습니다.\",\"errorCode\":\"NOTICE_NOT_FOUND\",\"timestamp\":\"2026-03-06T12:00:00\"}";

    public static final String ERROR_NOTICE_COMMENT_NOT_FOUND =
            "{\"success\":false,\"message\":\"공지 댓글을 찾을 수 없습니다.\",\"errorCode\":\"NOTICE_COMMENT_NOT_FOUND\",\"timestamp\":\"2026-03-06T12:00:00\"}";

    public static final String ERROR_NOT_NOTICE_COMMENT_AUTHOR =
            "{\"success\":false,\"message\":\"공지 댓글 작성자만 수정/삭제할 수 있습니다.\",\"errorCode\":\"NOT_NOTICE_COMMENT_AUTHOR\",\"timestamp\":\"2026-03-06T12:00:00\"}";

    public static final String ERROR_NOTICE_COMMENT_ALREADY_DELETED =
            "{\"success\":false,\"message\":\"이미 삭제된 댓글입니다.\",\"errorCode\":\"COMMENT_ALREADY_DELETED\",\"timestamp\":\"2026-03-06T12:00:00\"}";

    public static final String ERROR_NOTICE_CATEGORY_INVALID =
            "{\"success\":false,\"message\":\"category는 다음 값만 허용됩니다: 새소식, 학사, 학생, 장학/등록/학자금, 입학, 취업/진로개발/창업, 공모/행사, 교육/글로벌, 일반, 입찰구매정보, 사회봉사센터, 장애학생지원센터, 생활관, 비교과\",\"errorCode\":\"INVALID_REQUEST\",\"timestamp\":\"2026-03-06T12:00:00\"}";

    public static final String ERROR_NOTICE_SYNC_IN_PROGRESS =
            "{\"success\":false,\"message\":\"공지 동기화가 이미 진행 중입니다.\",\"errorCode\":\"RESOURCE_CONCURRENT_MODIFICATION\",\"timestamp\":\"2026-03-06T12:00:00\"}";
}
