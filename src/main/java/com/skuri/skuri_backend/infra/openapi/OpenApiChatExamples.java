package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiChatExamples {

    private OpenApiChatExamples() {
    }

    public static final String SUCCESS_CHAT_ROOM_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "public:university",
                  "type": "UNIVERSITY",
                  "name": "성결대학교 전체 채팅방",
                  "description": "성결대학교 전체 채팅방입니다.",
                  "isPublic": true,
                  "memberCount": 150,
                  "joined": false,
                  "unreadCount": 0,
                  "lastMessage": {
                    "type": "TEXT",
                    "text": "안녕하세요!",
                    "senderName": "홍길동",
                    "createdAt": "2026-03-05T21:10:00"
                  },
                  "lastMessageAt": "2026-03-05T21:10:00",
                  "isMuted": false
                },
                {
                  "id": "room:42c6ad65-c8e1-4c87-b796-3f0f6d0f0f0f",
                  "type": "CUSTOM",
                  "name": "시험기간 밤샘 메이트",
                  "description": "기말고사 기간 같이 공부할 사람들 모여요.",
                  "isPublic": true,
                  "memberCount": 24,
                  "joined": true,
                  "unreadCount": 3,
                  "lastMessage": {
                    "type": "TEXT",
                    "text": "중앙도서관 4층 자리 남아요.",
                    "senderName": "김성결",
                    "createdAt": "2026-03-05T22:10:00"
                  },
                  "lastMessageAt": "2026-03-05T22:10:00",
                  "isMuted": false
                }
              ]
            }
            """;

    public static final String SUCCESS_CHAT_ROOM_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "public:university",
                "type": "UNIVERSITY",
                "name": "성결대학교 전체 채팅방",
                "description": "성결대학교 전체 채팅방입니다.",
                "isPublic": true,
                "memberCount": 150,
                "joined": false,
                "unreadCount": 0,
                "lastMessage": {
                  "type": "TEXT",
                  "text": "안녕하세요!",
                  "senderName": "홍길동",
                  "createdAt": "2026-03-05T21:10:00"
                },
                "lastMessageAt": "2026-03-05T21:10:00",
                "isMuted": false
              }
            }
            """;

    public static final String SUCCESS_CHAT_ROOM_CREATE = """
            {
              "success": true,
              "data": {
                "id": "room:42c6ad65-c8e1-4c87-b796-3f0f6d0f0f0f",
                "type": "CUSTOM",
                "name": "시험기간 밤샘 메이트",
                "description": "기말고사 기간 같이 공부할 사람들 모여요.",
                "isPublic": true,
                "memberCount": 1,
                "joined": true,
                "unreadCount": 0,
                "lastMessage": null,
                "lastMessageAt": null,
                "isMuted": false,
                "lastReadAt": null
              }
            }
            """;

    public static final String SUCCESS_CHAT_ROOM_JOIN = """
            {
              "success": true,
              "data": {
                "id": "public:game:minecraft",
                "type": "GAME",
                "name": "마인크래프트 채팅방",
                "description": "스쿠리 서버 채팅방입니다.",
                "isPublic": true,
                "memberCount": 87,
                "joined": true,
                "unreadCount": 0,
                "lastMessage": {
                  "type": "TEXT",
                  "text": "오늘 저녁 9시에 이벤트 서버 열어요.",
                  "senderName": "스쿠리운영팀",
                  "createdAt": "2026-03-05T21:30:00"
                },
                "lastMessageAt": "2026-03-05T21:30:00",
                "isMuted": false,
                "lastReadAt": "2026-03-05T12:30:00Z"
              }
            }
            """;

    public static final String SUCCESS_CHAT_ROOM_LEAVE = """
            {
              "success": true,
              "data": {
                "id": "room:42c6ad65-c8e1-4c87-b796-3f0f6d0f0f0f",
                "type": "CUSTOM",
                "name": "시험기간 밤샘 메이트",
                "description": "기말고사 기간 같이 공부할 사람들 모여요.",
                "isPublic": true,
                "memberCount": 23,
                "joined": false,
                "unreadCount": 0,
                "lastMessage": {
                  "type": "TEXT",
                  "text": "중앙도서관 4층 자리 남아요.",
                  "senderName": "김성결",
                  "createdAt": "2026-03-05T22:10:00"
                },
                "lastMessageAt": "2026-03-05T22:10:00",
                "isMuted": false,
                "lastReadAt": null
              }
            }
            """;

    public static final String SUCCESS_CHAT_MESSAGES_PAGE = """
            {
              "success": true,
              "data": {
                "messages": [
                  {
                    "id": "msg-system-1",
                    "chatRoomId": "party:party-1",
                    "senderId": "leader-1",
                    "senderName": "파티 리더",
                    "senderPhotoUrl": "https://cdn.skuri.app/uploads/profiles/leader-1.jpg",
                    "type": "SYSTEM",
                    "text": "김철수님이 파티에 합류했어요.",
                    "createdAt": "2026-03-05T21:10:00"
                  },
                  {
                    "id": "msg-system-2",
                    "chatRoomId": "party:party-1",
                    "senderId": "leader-1",
                    "senderName": "파티 리더",
                    "senderPhotoUrl": "https://cdn.skuri.app/uploads/profiles/leader-1.jpg",
                    "type": "SYSTEM",
                    "text": "모집이 마감되었어요.",
                    "createdAt": "2026-03-05T21:11:00"
                  },
                  {
                    "id": "msg-system-3",
                    "chatRoomId": "party:party-1",
                    "senderId": "leader-1",
                    "senderName": "파티 리더",
                    "senderPhotoUrl": "https://cdn.skuri.app/uploads/profiles/leader-1.jpg",
                    "type": "SYSTEM",
                    "text": "모집이 재개되었어요.",
                    "createdAt": "2026-03-05T21:11:30"
                  },
                  {
                    "id": "msg-system-4",
                    "chatRoomId": "party:party-1",
                    "senderId": "member-2",
                    "senderName": "홍길동",
                    "senderPhotoUrl": null,
                    "type": "SYSTEM",
                    "text": "홍길동님이 파티에서 나갔어요.",
                    "createdAt": "2026-03-05T21:11:45"
                  },
                  {
                    "id": "msg-account-1",
                    "chatRoomId": "party:party-1",
                    "senderId": "leader-1",
                    "senderName": "파티 리더",
                    "senderPhotoUrl": "https://cdn.skuri.app/uploads/profiles/leader-1.jpg",
                    "type": "ACCOUNT",
                    "text": "계좌 정보를 공유했어요. (카카오뱅크 3333-01-1234567)",
                    "accountData": {
                      "bankName": "카카오뱅크",
                      "accountNumber": "3333-01-1234567",
                      "accountHolder": "홍*동",
                      "hideName": true
                    },
                    "createdAt": "2026-03-05T21:12:00"
                  },
                  {
                    "id": "msg-arrived-1",
                    "chatRoomId": "party:party-1",
                    "senderId": "leader-1",
                    "senderName": "파티 리더",
                    "senderPhotoUrl": "https://cdn.skuri.app/uploads/profiles/leader-1.jpg",
                    "type": "ARRIVED",
                    "text": "택시가 목적지에 도착했어요. 총 15000원, 3명 정산, 1인당 5000원입니다.",
                    "arrivalData": {
                      "taxiFare": 15000,
                      "splitMemberCount": 3,
                      "perPersonAmount": 5000,
                      "settlementTargetMemberIds": ["member-2", "member-3"],
                      "memberSettlements": [
                        {
                          "memberId": "member-2",
                          "displayName": "김철수",
                          "settled": false,
                          "settledAt": null,
                          "leftParty": false,
                          "leftAt": null
                        },
                        {
                          "memberId": "member-3",
                          "displayName": "이영희",
                          "settled": true,
                          "settledAt": "2026-03-05T21:32:00",
                          "leftParty": true,
                          "leftAt": "2026-03-05T21:40:00"
                        }
                      ],
                      "accountData": {
                        "bankName": "카카오뱅크",
                        "accountNumber": "3333-01-1234567",
                        "accountHolder": "홍*동",
                        "hideName": true
                      }
                    },
                    "createdAt": "2026-03-05T21:30:00"
                  },
                  {
                    "id": "msg-end-1",
                    "chatRoomId": "party:party-1",
                    "senderId": "leader-1",
                    "senderName": "파티 리더",
                    "senderPhotoUrl": "https://cdn.skuri.app/uploads/profiles/leader-1.jpg",
                    "type": "END",
                    "text": "리더가 파티를 종료했어요.",
                    "createdAt": "2026-03-05T21:45:00"
                  }
                ],
                "hasNext": false,
                "nextCursor": null
              }
            }
            """;

    public static final String SUCCESS_CHAT_READ_UPDATE = """
            {
              "success": true,
              "data": {
                "chatRoomId": "public:university",
                "lastReadAt": "2026-03-05T12:10:00Z",
                "updated": true
              }
            }
            """;

    public static final String REQUEST_CHAT_READ_UPDATE_LOCAL = """
            {
              "lastReadAt": "2026-03-25T21:36:29.837407"
            }
            """;

    public static final String REQUEST_CHAT_READ_UPDATE_UTC = """
            {
              "lastReadAt": "2026-03-25T12:36:29Z"
            }
            """;

    public static final String REQUEST_CHAT_READ_UPDATE_OFFSET = """
            {
              "lastReadAt": "2026-03-25T21:36:29+09:00"
            }
            """;

    public static final String SUCCESS_CHAT_SETTINGS_UPDATE = """
            {
              "success": true,
              "data": {
                "chatRoomId": "public:university",
                "muted": true
              }
            }
            """;

    public static final String SUCCESS_CHAT_MESSAGE_UPDATE = """
            {
              "success": true,
              "data": {
                "id": "msg-1",
                "chatRoomId": "public:university",
                "senderId": "member-1",
                "senderName": "홍길동",
                "senderPhotoUrl": null,
                "type": "TEXT",
                "text": "수정한 메시지입니다.",
                "createdAt": "2026-03-05T21:10:00",
                "updatedAt": "2026-03-05T21:12:00",
                "editedAt": "2026-03-05T21:12:00",
                "isDeleted": false
              }
            }
            """;

    public static final String SUCCESS_CHAT_MESSAGE_DELETE = """
            {
              "success": true,
              "data": {
                "id": "msg-1",
                "chatRoomId": "public:university",
                "senderId": "member-1",
                "senderName": "홍길동",
                "senderPhotoUrl": null,
                "type": "TEXT",
                "text": "삭제된 메시지입니다.",
                "createdAt": "2026-03-05T21:10:00",
                "updatedAt": "2026-03-05T21:13:00",
                "deletedAt": "2026-03-05T21:13:00",
                "isDeleted": true
              }
            }
            """;

    public static final String SUCCESS_ADMIN_CHAT_ROOM_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "public:game:minecraft",
                  "type": "GAME",
                  "name": "마인크래프트 채팅방",
                  "description": "스쿠리 서버 채팅방입니다.",
                  "isPublic": true,
                  "memberCount": 87,
                  "joined": false,
                  "unreadCount": 0,
                  "lastMessage": {
                    "type": "TEXT",
                    "text": "오늘 저녁 9시에 이벤트 서버 열어요.",
                    "senderName": "스쿠리운영팀",
                    "createdAt": "2026-03-05T21:30:00"
                  },
                  "lastMessageAt": "2026-03-05T21:30:00",
                  "isMuted": false
                },
                {
                  "id": "room:42c6ad65-c8e1-4c87-b796-3f0f6d0f0f0f",
                  "type": "CUSTOM",
                  "name": "시험기간 밤샘 메이트",
                  "description": "기말고사 기간 같이 공부할 사람들 모여요.",
                  "isPublic": true,
                  "memberCount": 24,
                  "joined": false,
                  "unreadCount": 0,
                  "lastMessage": {
                    "type": "TEXT",
                    "text": "중앙도서관 4층 자리 남아요.",
                    "senderName": "김성결",
                    "createdAt": "2026-03-05T22:10:00"
                  },
                  "lastMessageAt": "2026-03-05T22:10:00",
                  "isMuted": false
                }
              ]
            }
            """;

    public static final String SUCCESS_ADMIN_CHAT_ROOM_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "public:game:minecraft",
                "type": "GAME",
                "name": "마인크래프트 채팅방",
                "description": "스쿠리 서버 채팅방입니다.",
                "isPublic": true,
                "memberCount": 87,
                "joined": false,
                "unreadCount": 0,
                "lastMessage": {
                  "type": "TEXT",
                  "text": "오늘 저녁 9시에 이벤트 서버 열어요.",
                  "senderName": "스쿠리운영팀",
                  "createdAt": "2026-03-05T21:30:00"
                },
                "lastMessageAt": "2026-03-05T21:30:00",
                "isMuted": false
              }
            }
            """;

    public static final String SUCCESS_ADMIN_CHAT_ROOM_MEMBERS = """
            {
              "success": true,
              "data": [
                {
                  "memberId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                  "email": "user@sungkyul.ac.kr",
                  "nickname": "스쿠리 유저",
                  "realname": "홍길동",
                  "studentId": "2023112233",
                  "department": "컴퓨터공학과",
                  "photoUrl": "https://cdn.skuri.app/uploads/profiles/dw9rPtuticbjnaYPkeiF3RGPpqk1/2026/04/06/photo.jpg",
                  "joinedAt": "2026-03-05T12:00:00Z",
                  "lastReadAt": "2026-03-05T12:10:00Z",
                  "muted": false,
                  "status": "ACTIVE"
                },
                {
                  "memberId": "member-without-profile",
                  "email": null,
                  "nickname": null,
                  "realname": null,
                  "studentId": null,
                  "department": null,
                  "photoUrl": null,
                  "joinedAt": "2026-03-05T12:05:00Z",
                  "lastReadAt": null,
                  "muted": true,
                  "status": null
                }
              ]
            }
            """;

    public static final String SUCCESS_ADMIN_CHAT_ROOM_MESSAGES_PAGE = """
            {
              "success": true,
              "data": {
                "messages": [
                  {
                    "id": "mc-message-2",
                    "chatRoomId": "public:game:minecraft",
                    "senderId": "mc-admin",
                    "senderName": "스쿠리운영팀",
                    "senderPhotoUrl": null,
                    "type": "SYSTEM",
                    "text": "오늘 저녁 9시에 이벤트 서버를 엽니다.",
                    "createdAt": "2026-03-05T21:30:00"
                  },
                  {
                    "id": "mc-message-1",
                    "chatRoomId": "public:game:minecraft",
                    "senderId": "mc-player-1",
                    "senderName": "skuriPlayer",
                    "senderPhotoUrl": "https://minotar.net/avatar/8667ba71b85a4004af54457a9734eed7/64",
                    "type": "TEXT",
                    "text": "안녕하세요!",
                    "createdAt": "2026-03-05T21:29:00"
                  }
                ],
                "hasNext": false,
                "nextCursor": null
              }
            }
            """;

    public static final String SUCCESS_ADMIN_CHAT_ROOM_CREATE = """
            {
              "success": true,
              "data": {
                "id": "room:2e8f745a-c131-4e1d-9b8e-7e8d4bb686b3",
                "name": "성결대학교 전체 채팅방",
                "type": "UNIVERSITY"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_CHAT_ROOM_DELETE = """
            {
              "success": true,
              "data": null
            }
            """;

    public static final String ERROR_CHAT_ROOM_NOT_FOUND = """
            {
              "success": false,
              "errorCode": "CHAT_ROOM_NOT_FOUND",
              "message": "채팅방을 찾을 수 없습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_MESSAGE_NOT_FOUND = """
            {
              "success": false,
              "errorCode": "CHAT_MESSAGE_NOT_FOUND",
              "message": "채팅 메시지를 찾을 수 없습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_NOT_CHAT_MESSAGE_AUTHOR = """
            {
              "success": false,
              "errorCode": "NOT_CHAT_MESSAGE_AUTHOR",
              "message": "메시지 작성자만 처리할 수 있습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_MESSAGE_EDIT_NOT_ALLOWED = """
            {
              "success": false,
              "errorCode": "CHAT_MESSAGE_EDIT_NOT_ALLOWED",
              "message": "이 메시지는 수정할 수 없습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_MESSAGE_DELETE_NOT_ALLOWED = """
            {
              "success": false,
              "errorCode": "CHAT_MESSAGE_DELETE_NOT_ALLOWED",
              "message": "이 메시지는 삭제할 수 없습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_MESSAGE_EDIT_WINDOW_EXPIRED = """
            {
              "success": false,
              "errorCode": "CHAT_MESSAGE_EDIT_WINDOW_EXPIRED",
              "message": "메시지는 전송 후 15분 이내에만 수정할 수 있습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_MESSAGE_ALREADY_DELETED = """
            {
              "success": false,
              "errorCode": "CHAT_MESSAGE_ALREADY_DELETED",
              "message": "이미 삭제된 메시지입니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_NOT_CHAT_ROOM_MEMBER = """
            {
              "success": false,
              "errorCode": "NOT_CHAT_ROOM_MEMBER",
              "message": "채팅방 멤버가 아닙니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_ROOM_FULL = """
            {
              "success": false,
              "errorCode": "CHAT_ROOM_FULL",
              "message": "채팅방 정원이 가득 찼습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ALREADY_CHAT_ROOM_MEMBER = """
            {
              "success": false,
              "errorCode": "ALREADY_CHAT_ROOM_MEMBER",
              "message": "이미 채팅방에 참여 중입니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_ROOM_PUBLIC_MEMBERSHIP_ONLY = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "공개 채팅방만 이 API로 처리할 수 있습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_ROOM_JOIN_PARTY_API_ONLY = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "파티 채팅방 참여는 택시 파티 API로 처리해야 합니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_ROOM_LEAVE_PARTY_API_ONLY = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "파티 채팅방 나가기는 택시 파티 API로 처리해야 합니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_STOMP_AUTH_FAILED = """
            {
              "success": false,
              "errorCode": "STOMP_AUTH_FAILED",
              "message": "WebSocket 인증에 실패했습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_VALIDATION_CURSOR_PAIR = """
            {
              "success": false,
              "errorCode": "VALIDATION_ERROR",
              "message": "cursorCreatedAt와 cursorId는 함께 전달해야 합니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ADMIN_CHAT_ROOM_PARTY_TYPE_NOT_ALLOWED = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "PARTY 타입 채팅방은 파티 생성 시 자동 생성됩니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ADMIN_CHAT_ROOM_PUBLIC_ONLY = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "관리자 채팅방 생성 API는 isPublic=true만 허용합니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ADMIN_CHAT_ROOM_DELETE_PARTY_NOT_ALLOWED = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "파티 채팅방은 관리자 API로 삭제할 수 없습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ADMIN_CHAT_ROOM_DELETE_PUBLIC_ONLY = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "공개 채팅방만 관리자 API로 삭제할 수 있습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;
}
