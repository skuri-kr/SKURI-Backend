package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiSupportExamples {

    private OpenApiSupportExamples() {
    }

    public static final String SUCCESS_INQUIRY_CREATE = """
            {
              "success": true,
              "data": {
                "id": "inquiry_uuid",
                "status": "PENDING",
                "createdAt": "2026-02-03T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_MY_INQUIRIES = """
            {
              "success": true,
              "data": [
                {
                  "id": "inquiry_uuid",
                  "type": "BUG",
                  "subject": "앱 오류 문의",
                  "content": "채팅 화면에서 오류가 발생합니다.",
                  "status": "PENDING",
                  "answer": "재현 후 수정 배포 완료",
                  "attachments": [
                    {
                      "url": "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0.jpg",
                      "thumbUrl": "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0_thumb.jpg",
                      "width": 800,
                      "height": 600,
                      "size": 245123,
                      "mime": "image/jpeg"
                    }
                  ],
                  "createdAt": "2026-02-03T12:00:00",
                  "updatedAt": "2026-02-03T12:00:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_REPORT_CREATE = """
            {
              "success": true,
              "data": {
                "id": "report_uuid",
                "status": "PENDING",
                "createdAt": "2026-03-05T12:10:00"
              }
            }
            """;

    public static final String SUCCESS_APP_VERSION = """
            {
              "success": true,
              "data": {
                "platform": "ios",
                "minimumVersion": "1.5.0",
                "forceUpdate": false,
                "message": "새로운 기능이 추가되었습니다.",
                "title": "업데이트 안내",
                "showButton": true,
                "buttonText": "업데이트",
                "buttonUrl": "https://apps.apple.com/..."
              }
            }
            """;

    public static final String SUCCESS_CAFETERIA_MENU = """
            {
              "success": true,
              "data": {
                "weekId": "2026-W06",
                "weekStart": "2026-02-03",
                "weekEnd": "2026-02-07",
                "menus": {
                  "2026-02-03": {
                    "rollNoodles": ["우동", "김밥"],
                    "theBab": ["돈까스", "된장찌개"],
                    "fryRice": ["볶음밥", "짜장면"]
                  }
                },
                "categories": [
                  {
                    "code": "rollNoodles",
                    "label": "Roll & Noodles"
                  },
                  {
                    "code": "theBab",
                    "label": "The bab"
                  },
                  {
                    "code": "fryRice",
                    "label": "Fry & Rice"
                  }
                ],
                "menuEntries": {
                  "2026-02-03": {
                    "rollNoodles": [
                      {
                        "id": "2026-W06.rollNoodles.8851f2731beef1f0",
                        "title": "우동",
                        "badges": [],
                        "likeCount": 12,
                        "dislikeCount": 1,
                        "myReaction": null
                      },
                      {
                        "id": "2026-W06.rollNoodles.881b3d074ed4535d",
                        "title": "김밥",
                        "badges": [
                          {
                            "code": "TAKEOUT",
                            "label": "테이크아웃"
                          }
                        ],
                        "likeCount": 31,
                        "dislikeCount": 2,
                        "myReaction": "LIKE"
                      }
                    ],
                    "theBab": [
                      {
                        "id": "2026-W06.theBab.1f529546f2bf7ff3",
                        "title": "돈까스",
                        "badges": [],
                        "likeCount": 18,
                        "dislikeCount": 4,
                        "myReaction": "DISLIKE"
                      }
                    ],
                    "fryRice": []
                  }
                }
              }
            }
            """;

    public static final String SUCCESS_CAFETERIA_MENU_REACTION_LIKE = """
            {
              "success": true,
              "data": {
                "menuId": "2026-W08.rollNoodles.c4973864db4f8815",
                "myReaction": "LIKE",
                "likeCount": 13,
                "dislikeCount": 2
              }
            }
            """;

    public static final String SUCCESS_CAFETERIA_MENU_REACTION_CANCEL = """
            {
              "success": true,
              "data": {
                "menuId": "2026-W08.rollNoodles.c4973864db4f8815",
                "myReaction": null,
                "likeCount": 12,
                "dislikeCount": 2
              }
            }
            """;

    public static final String SUCCESS_ADMIN_INQUIRIES_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "inquiry_uuid",
                    "memberId": "user_uuid",
                    "type": "BUG",
                    "subject": "채팅 화면 오류",
                    "content": "채팅 진입 시 앱이 종료됩니다.",
                    "status": "PENDING",
                    "attachments": [
                      {
                        "url": "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0.jpg",
                        "thumbUrl": "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0_thumb.jpg",
                        "width": 800,
                        "height": 600,
                        "size": 245123,
                        "mime": "image/jpeg"
                      }
                    ],
                    "memo": null,
                    "userEmail": "user@sungkyul.ac.kr",
                    "userName": "스쿠리유저",
                    "userRealname": "홍길동",
                    "userStudentId": "20201234",
                    "createdAt": "2026-03-05T12:00:00",
                    "updatedAt": "2026-03-05T12:00:00"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 53,
                "totalPages": 3,
                "hasNext": true,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_ADMIN_INQUIRY_STATUS_UPDATE = """
            {
              "success": true,
              "data": {
                "id": "inquiry_uuid",
                "memberId": "user_uuid",
                "type": "BUG",
                "subject": "채팅 화면 오류",
                "content": "채팅 진입 시 앱이 종료됩니다.",
                "status": "RESOLVED",
                "attachments": [
                  {
                    "url": "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0.jpg",
                    "thumbUrl": "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0_thumb.jpg",
                    "width": 800,
                    "height": 600,
                    "size": 245123,
                    "mime": "image/jpeg"
                  }
                ],
                "memo": "재현 후 수정 배포 완료",
                "userEmail": "user@sungkyul.ac.kr",
                "userName": "스쿠리유저",
                "userRealname": "홍길동",
                "userStudentId": "20201234",
                "createdAt": "2026-03-05T12:00:00",
                "updatedAt": "2026-03-05T12:30:00"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_REPORTS_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "report_uuid",
                    "reporterId": "user_uuid",
                    "targetType": "CHAT_MESSAGE",
                    "targetId": "message_uuid",
                    "targetAuthorId": "target_user_uuid",
                    "category": "SPAM",
                    "reason": "광고성 메시지입니다.",
                    "status": "PENDING",
                    "action": null,
                    "memo": null,
                    "createdAt": "2026-03-29T12:10:00",
                    "updatedAt": "2026-03-29T12:10:00"
                  },
                  {
                    "id": "report_uuid_2",
                    "reporterId": "user_uuid",
                    "targetType": "CHAT_ROOM",
                    "targetId": "chat_room_uuid",
                    "targetAuthorId": "room_owner_uuid",
                    "category": "ABUSE",
                    "reason": "부적절한 목적의 채팅방입니다.",
                    "status": "PENDING",
                    "action": null,
                    "memo": null,
                    "createdAt": "2026-03-29T12:20:00",
                    "updatedAt": "2026-03-29T12:20:00"
                  },
                  {
                    "id": "report_uuid_3",
                    "reporterId": "user_uuid",
                    "targetType": "TAXI_PARTY",
                    "targetId": "party_uuid",
                    "targetAuthorId": "party_host_uuid",
                    "category": "FRAUD",
                    "reason": "운행/정산 방식이 부적절합니다.",
                    "status": "PENDING",
                    "action": null,
                    "memo": null,
                    "createdAt": "2026-03-29T12:30:00",
                    "updatedAt": "2026-03-29T12:30:00"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 3,
                "totalPages": 1,
                "hasNext": false,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_ADMIN_REPORT_STATUS_UPDATE = """
            {
              "success": true,
              "data": {
                "id": "report_uuid",
                "reporterId": "user_uuid",
                "targetType": "POST",
                "targetId": "post_uuid",
                "targetAuthorId": "target_user_uuid",
                "category": "SPAM",
                "reason": "광고성 게시글입니다.",
                "status": "ACTIONED",
                "action": "DELETE_POST",
                "memo": "광고성 게시물 삭제 및 사용자 경고",
                "createdAt": "2026-03-05T12:10:00",
                "updatedAt": "2026-03-05T12:20:00"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_APP_VERSION_UPDATE = """
            {
              "success": true,
              "data": {
                "platform": "ios",
                "minimumVersion": "1.6.0",
                "forceUpdate": true,
                "updatedAt": "2026-02-19T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_CAFETERIA_MENU_CREATE = SUCCESS_CAFETERIA_MENU;

    public static final String SUCCESS_ADMIN_CAFETERIA_MENU_UPDATE = SUCCESS_CAFETERIA_MENU;

    public static final String ERROR_INQUIRY_NOT_FOUND =
            "{\"success\":false,\"message\":\"문의를 찾을 수 없습니다.\",\"errorCode\":\"INQUIRY_NOT_FOUND\",\"timestamp\":\"2026-03-07T12:00:00\"}";

    public static final String ERROR_INVALID_INQUIRY_STATUS_TRANSITION =
            "{\"success\":false,\"message\":\"허용되지 않는 문의 상태 전이입니다.\",\"errorCode\":\"INVALID_INQUIRY_STATUS_TRANSITION\",\"timestamp\":\"2026-03-07T12:00:00\"}";

    public static final String ERROR_REPORT_ALREADY_SUBMITTED =
            "{\"success\":false,\"message\":\"동일 대상에 대한 중복 신고입니다.\",\"errorCode\":\"REPORT_ALREADY_SUBMITTED\",\"timestamp\":\"2026-03-07T12:00:00\"}";

    public static final String ERROR_CANNOT_REPORT_YOURSELF =
            "{\"success\":false,\"message\":\"자기 자신은 신고할 수 없습니다.\",\"errorCode\":\"CANNOT_REPORT_YOURSELF\",\"timestamp\":\"2026-03-07T12:00:00\"}";

    public static final String ERROR_REPORT_NOT_FOUND =
            "{\"success\":false,\"message\":\"신고를 찾을 수 없습니다.\",\"errorCode\":\"REPORT_NOT_FOUND\",\"timestamp\":\"2026-03-07T12:00:00\"}";

    public static final String ERROR_INVALID_REPORT_STATUS_TRANSITION =
            "{\"success\":false,\"message\":\"허용되지 않는 신고 상태 전이입니다.\",\"errorCode\":\"INVALID_REPORT_STATUS_TRANSITION\",\"timestamp\":\"2026-03-07T12:00:00\"}";

    public static final String ERROR_CAFETERIA_MENU_NOT_FOUND =
            "{\"success\":false,\"message\":\"학식 메뉴를 찾을 수 없습니다.\",\"errorCode\":\"CAFETERIA_MENU_NOT_FOUND\",\"timestamp\":\"2026-03-07T12:00:00\"}";

    public static final String ERROR_CAFETERIA_MENU_ENTRY_NOT_FOUND =
            "{\"success\":false,\"message\":\"학식 메뉴 항목을 찾을 수 없습니다.\",\"errorCode\":\"CAFETERIA_MENU_ENTRY_NOT_FOUND\",\"timestamp\":\"2026-03-29T12:00:00\"}";

    public static final String ERROR_CAFETERIA_MENU_ALREADY_EXISTS =
            "{\"success\":false,\"message\":\"이미 등록된 주차의 학식 메뉴입니다.\",\"errorCode\":\"CAFETERIA_MENU_ALREADY_EXISTS\",\"timestamp\":\"2026-03-07T12:00:00\"}";

    public static final String ERROR_CAFETERIA_MENU_MENU_ENTRIES_MISMATCH =
            "{\"success\":false,\"message\":\"menus와 menuEntries의 메뉴명이 일치하지 않습니다.\",\"errorCode\":\"INVALID_REQUEST\",\"timestamp\":\"2026-03-29T12:00:00\"}";

    public static final String ERROR_CAFETERIA_MENU_WEEKLY_METADATA_CONFLICT =
            "{\"success\":false,\"message\":\"같은 주차에서 동일 카테고리의 동일 메뉴는 날짜별 메타데이터가 동일해야 합니다. category=rollNoodles, title=존슨부대찌개, firstDate=2026-02-16, date=2026-02-17\",\"errorCode\":\"INVALID_REQUEST\",\"timestamp\":\"2026-03-29T12:00:00\"}";
}
