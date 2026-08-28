package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiBoardExamples {

    private OpenApiBoardExamples() {
    }

    public static final String SUCCESS_POST_CREATE = """
            {
              "success": true,
              "data": {
                "id": "post_uuid",
                "title": "게시글 제목",
                "content": "게시글 전체 내용",
                "authorId": "user_uuid",
                "authorName": "홍길동",
                "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                "isAuthorAdmin": true,
                "isAnonymous": false,
                "category": "GENERAL",
                "viewCount": 0,
                "likeCount": 0,
                "commentCount": 0,
                "bookmarkCount": 0,
                "images": [
                  {
                    "url": "https://cdn.skuri.app/posts/post-1/image-1.jpg",
                    "thumbUrl": "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg",
                    "width": 800,
                    "height": 600,
                    "size": 245123,
                    "mime": "image/jpeg"
                  }
                ],
                "isLiked": false,
                "isBookmarked": false,
                "isAuthor": true,
                "createdAt": "2026-03-05T20:30:00",
                "updatedAt": "2026-03-05T20:30:00"
              }
            }
            """;

    public static final String SUCCESS_POST_LIST_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "post_uuid",
                    "title": "게시글 제목",
                    "content": "내용 미리보기...",
                    "authorId": "user_uuid",
                    "authorName": "홍길동",
                    "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                    "isAuthorAdmin": true,
                    "isAnonymous": false,
                    "category": "GENERAL",
                    "viewCount": 100,
                    "likeCount": 10,
                    "commentCount": 5,
                    "bookmarkCount": 3,
                    "isLiked": true,
                    "isBookmarked": false,
                    "isCommentedByMe": true,
                    "hasImage": true,
                    "thumbnailUrl": "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg",
                    "isPinned": false,
                    "createdAt": "2026-02-03T12:00:00"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 100,
                "totalPages": 5,
                "hasNext": true,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_POST_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "post_uuid",
                "title": "게시글 제목",
                "content": "게시글 전체 내용",
                "authorId": "user_uuid",
                "authorName": "홍길동",
                "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                "isAuthorAdmin": true,
                "isAnonymous": false,
                "category": "GENERAL",
                "viewCount": 101,
                "likeCount": 10,
                "commentCount": 5,
                "bookmarkCount": 3,
                "images": [],
                "isLiked": true,
                "isBookmarked": false,
                "isAuthor": true,
                "createdAt": "2026-02-03T12:00:00",
                "updatedAt": "2026-02-03T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_POST_LIST_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "post_uuid",
                    "category": "GENERAL",
                    "title": "관리 대상 게시글",
                    "authorId": "member-1",
                    "authorNickname": "스쿠리유저",
                    "authorRealname": "홍길동",
                    "isAnonymous": false,
                    "commentCount": 5,
                    "likeCount": 10,
                    "createdAt": "2026-03-29T12:00:00",
                    "updatedAt": "2026-03-29T12:30:00",
                    "moderationStatus": "VISIBLE",
                    "thumbnailUrl": "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg"
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

    public static final String SUCCESS_ADMIN_POST_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "post_uuid",
                "category": "GENERAL",
                "title": "관리 대상 게시글",
                "content": "관리자 상세에서 확인하는 본문 전체 내용",
                "authorId": "member-1",
                "authorNickname": "스쿠리유저",
                "authorRealname": "홍길동",
                "isAnonymous": false,
                "viewCount": 42,
                "likeCount": 10,
                "commentCount": 5,
                "bookmarkCount": 3,
                "createdAt": "2026-03-29T12:00:00",
                "updatedAt": "2026-03-29T12:30:00",
                "moderationStatus": "HIDDEN",
                "thumbnailUrl": "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg",
                "images": [
                  {
                    "url": "https://cdn.skuri.app/posts/post-1/image-1.jpg",
                    "thumbUrl": "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg",
                    "width": 800,
                    "height": 600,
                    "size": 245123,
                    "mime": "image/jpeg"
                  }
                ]
              }
            }
            """;

    public static final String SUCCESS_ADMIN_POST_MODERATION_UPDATE = """
            {
              "success": true,
              "data": {
                "id": "post_uuid",
                "moderationStatus": "HIDDEN"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_COMMENT_LIST_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "comment_uuid",
                    "postId": "post_uuid",
                    "postTitle": "관리 대상 게시글",
                    "authorId": "member-2",
                    "authorNickname": "댓글유저",
                    "authorRealname": "김철수",
                    "contentPreview": "문제되는 댓글 내용 일부...",
                    "parentCommentId": null,
                    "createdAt": "2026-03-29T13:00:00",
                    "moderationStatus": "VISIBLE"
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

    public static final String SUCCESS_ADMIN_COMMENT_MODERATION_UPDATE = """
            {
              "success": true,
              "data": {
                "id": "comment_uuid",
                "moderationStatus": "DELETED"
              }
            }
            """;

    public static final String SUCCESS_LIKE = """
            {
              "success": true,
              "data": {
                "isLiked": true,
                "likeCount": 11
              }
            }
            """;

    public static final String SUCCESS_BOOKMARK = """
            {
              "success": true,
              "data": {
                "isBookmarked": true,
                "bookmarkCount": 4
              }
            }
            """;

    public static final String SUCCESS_COMMENTS_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "comment_uuid",
                  "parentId": null,
                  "depth": 0,
                  "content": "댓글 내용",
                  "authorId": "user_uuid",
                  "authorName": "홍길동",
                  "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                  "isAuthorAdmin": true,
                  "isAnonymous": false,
                  "anonymousOrder": null,
                  "isAuthor": false,
                  "isPostAuthor": true,
                  "likeCount": 3,
                  "isLiked": true,
                  "isDeleted": false,
                  "createdAt": "2026-02-03T12:00:00",
                  "updatedAt": "2026-02-03T12:00:00"
                },
                {
                  "id": "reply_uuid",
                  "parentId": "comment_uuid",
                  "depth": 1,
                  "content": "대댓글 내용",
                  "authorId": null,
                  "authorName": "익명2",
                  "authorProfileImage": null,
                  "isAuthorAdmin": false,
                  "isAnonymous": true,
                  "anonymousOrder": 2,
                  "isAuthor": false,
                  "isPostAuthor": false,
                  "likeCount": 0,
                  "isLiked": false,
                  "isDeleted": false,
                  "createdAt": "2026-02-03T12:30:00",
                  "updatedAt": "2026-02-03T12:30:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_COMMENT_CREATE = """
            {
              "success": true,
              "data": {
                "id": "comment_uuid",
                "parentId": "root_comment_uuid",
                "depth": 2,
                "content": "댓글 내용",
                "authorId": null,
                "authorName": "익명1",
                "authorProfileImage": null,
                "isAuthorAdmin": false,
                "isAnonymous": true,
                "anonymousOrder": 1,
                "isAuthor": true,
                "isPostAuthor": false,
                "likeCount": 0,
                "isLiked": false,
                "isDeleted": false,
                "createdAt": "2026-02-03T12:00:00",
                "updatedAt": "2026-02-03T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_COMMENT_UPDATE = """
            {
              "success": true,
              "data": {
                "id": "comment_uuid",
                "parentId": "root_comment_uuid",
                "depth": 2,
                "content": "수정된 댓글 내용",
                "authorId": null,
                "authorName": "익명1",
                "authorProfileImage": null,
                "isAuthorAdmin": false,
                "isAnonymous": true,
                "anonymousOrder": 1,
                "isAuthor": true,
                "isPostAuthor": false,
                "likeCount": 3,
                "isLiked": true,
                "isDeleted": false,
                "createdAt": "2026-02-03T12:00:00",
                "updatedAt": "2026-02-03T12:30:00"
              }
            }
            """;

    public static final String SUCCESS_COMMENT_LIKE = """
            {
              "success": true,
              "data": {
                "commentId": "comment_uuid",
                "isLiked": true,
                "likeCount": 3
              }
            }
            """;

    public static final String SUCCESS_COMMENT_UNLIKE = """
            {
              "success": true,
              "data": {
                "commentId": "comment_uuid",
                "isLiked": false,
                "likeCount": 2
              }
            }
            """;

    public static final String SUCCESS_COMMENT_DELETED = """
            {
              "success": true,
              "data": null
            }
            """;

    public static final String SUCCESS_MEMBER_POSTS_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "post_uuid",
                    "title": "내가 작성한 글",
                    "content": "내용 미리보기...",
                    "authorId": "user_uuid",
                    "authorName": "홍길동",
                    "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                    "isAuthorAdmin": true,
                    "isAnonymous": false,
                    "category": "GENERAL",
                    "viewCount": 100,
                    "likeCount": 10,
                    "commentCount": 5,
                    "bookmarkCount": 4,
                    "isLiked": true,
                    "isBookmarked": true,
                    "isCommentedByMe": false,
                    "hasImage": false,
                    "thumbnailUrl": null,
                    "isPinned": false,
                    "createdAt": "2026-02-03T12:00:00"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 12,
                "totalPages": 1,
                "hasNext": false,
                "hasPrevious": false
              }
            }
            """;

    public static final String ERROR_POST_NOT_FOUND = """
            {
              "success": false,
              "errorCode": "POST_NOT_FOUND",
              "message": "게시글을 찾을 수 없습니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;

    public static final String ERROR_COMMENT_NOT_FOUND = """
            {
              "success": false,
              "errorCode": "COMMENT_NOT_FOUND",
              "message": "댓글을 찾을 수 없습니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;

    public static final String ERROR_NOT_POST_AUTHOR = """
            {
              "success": false,
              "errorCode": "NOT_POST_AUTHOR",
              "message": "게시글 작성자만 수정/삭제할 수 있습니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;

    public static final String ERROR_NOT_COMMENT_AUTHOR = """
            {
              "success": false,
              "errorCode": "NOT_COMMENT_AUTHOR",
              "message": "댓글 작성자만 수정/삭제할 수 있습니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;

    public static final String ERROR_COMMENT_ALREADY_DELETED = """
            {
              "success": false,
              "errorCode": "COMMENT_ALREADY_DELETED",
              "message": "이미 삭제된 댓글입니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;

    public static final String ERROR_ADMIN_POST_CATEGORY_INVALID = """
            {
              "success": false,
              "errorCode": "VALIDATION_ERROR",
              "message": "category는 GENERAL, QUESTION, REVIEW, ANNOUNCEMENT 중 하나여야 합니다.",
              "timestamp": "2026-03-29T14:00:00"
            }
            """;

    public static final String ERROR_ADMIN_MODERATION_STATUS_INVALID = """
            {
              "success": false,
              "errorCode": "VALIDATION_ERROR",
              "message": "moderationStatus는 VISIBLE, HIDDEN, DELETED 중 하나여야 합니다.",
              "timestamp": "2026-03-29T14:00:00"
            }
            """;

    public static final String ERROR_ADMIN_REQUEST_STATUS_INVALID = """
            {
              "success": false,
              "errorCode": "VALIDATION_ERROR",
              "message": "status는 VISIBLE, HIDDEN, DELETED 중 하나여야 합니다.",
              "timestamp": "2026-03-29T14:00:00"
            }
            """;

    public static final String ERROR_INVALID_POST_MODERATION_STATUS_TRANSITION = """
            {
              "success": false,
              "errorCode": "INVALID_POST_MODERATION_STATUS_TRANSITION",
              "message": "허용되지 않는 게시글 moderation 상태 전이입니다.",
              "timestamp": "2026-03-29T14:00:00"
            }
            """;

    public static final String ERROR_INVALID_COMMENT_MODERATION_STATUS_TRANSITION = """
            {
              "success": false,
              "errorCode": "INVALID_COMMENT_MODERATION_STATUS_TRANSITION",
              "message": "허용되지 않는 댓글 moderation 상태 전이입니다.",
              "timestamp": "2026-03-29T14:00:00"
            }
            """;
}
