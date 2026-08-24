package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiTaxiPartyExamples {

    private OpenApiTaxiPartyExamples() {
    }

    public static final String SUCCESS_PARTY_CREATE = """
            {
              "success": true,
              "data": {
                "id": "party-20260304-001",
                "chatRoomId": "party:party-20260304-001"
              }
            }
            """;

    public static final String SUCCESS_PARTY_LIST_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "party-20260304-001",
                    "leaderId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                    "leaderName": "스쿠리 유저",
                    "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                    "participantSummaries": [
                      {
                        "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                        "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                        "nickname": "스쿠리 유저",
                        "isLeader": true
                      },
                      {
                        "id": "member-2",
                        "photoUrl": null,
                        "nickname": "김철수",
                        "isLeader": false
                      }
                    ],
                    "departure": {
                      "name": "성결대학교",
                      "lat": 37.382742,
                      "lng": 126.928031
                    },
                    "destination": {
                      "name": "안양역",
                      "lat": 37.401,
                      "lng": 126.922
                    },
                    "departureTime": "2026-03-04T21:00:00",
                    "maxMembers": 4,
                    "currentMembers": 2,
                    "tags": [
                      "빠른출발",
                      "정문"
                    ],
                    "detail": "정문 앞 택시승강장 집합",
                    "status": "OPEN",
                    "createdAt": "2026-03-04T19:00:00"
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

    public static final String SUCCESS_PARTY_DETAIL_OPEN = """
            {
              "success": true,
              "data": {
                "id": "party-20260304-001",
                "leaderId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "leaderName": "스쿠리 유저",
                "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                "departure": {
                  "name": "성결대학교",
                  "lat": 37.382742,
                  "lng": 126.928031
                },
                "destination": {
                  "name": "안양역",
                  "lat": 37.401,
                  "lng": 126.922
                },
                "departureTime": "2026-03-04T21:00:00",
                "maxMembers": 4,
                "members": [
                  {
                    "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                    "nickname": "스쿠리 유저",
                    "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                    "isLeader": true,
                    "joinedAt": "2026-03-04T19:00:00"
                  },
                  {
                    "id": "member-2",
                    "nickname": "김철수",
                    "photoUrl": null,
                    "isLeader": false,
                    "joinedAt": "2026-03-04T19:10:00"
                  }
                ],
                "tags": [
                  "빠른출발",
                  "정문"
                ],
                "detail": "정문 앞 택시승강장 집합",
                "status": "OPEN",
                "settlement": null,
                "createdAt": "2026-03-04T19:00:00"
              }
            }
            """;

    public static final String SUCCESS_PARTY_DETAIL_UPDATED = """
            {
              "success": true,
              "data": {
                "id": "party-20260304-001",
                "leaderId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "leaderName": "스쿠리 유저",
                "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                "departure": {
                  "name": "성결대학교",
                  "lat": 37.382742,
                  "lng": 126.928031
                },
                "destination": {
                  "name": "안양역",
                  "lat": 37.401,
                  "lng": 126.922
                },
                "departureTime": "2026-03-04T21:30:00",
                "maxMembers": 4,
                "members": [
                  {
                    "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                    "nickname": "스쿠리 유저",
                    "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                    "isLeader": true,
                    "joinedAt": "2026-03-04T19:00:00"
                  },
                  {
                    "id": "member-2",
                    "nickname": "김철수",
                    "photoUrl": null,
                    "isLeader": false,
                    "joinedAt": "2026-03-04T19:10:00"
                  }
                ],
                "tags": [
                  "빠른출발",
                  "정문"
                ],
                "detail": "출발시간 30분 연기, 정문 CU 앞 집합",
                "status": "CLOSED",
                "settlement": null,
                "createdAt": "2026-03-04T19:00:00"
              }
            }
            """;

    public static final String SUCCESS_PARTY_DETAIL_ARRIVED = """
            {
              "success": true,
              "data": {
                "id": "party-20260304-001",
                "leaderId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "leaderName": "스쿠리 유저",
                "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                "departure": {
                  "name": "성결대학교",
                  "lat": 37.382742,
                  "lng": 126.928031
                },
                "destination": {
                  "name": "안양역",
                  "lat": 37.401,
                  "lng": 126.922
                },
                "departureTime": "2026-03-04T21:00:00",
                "maxMembers": 4,
                "members": [
                  {
                    "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                    "nickname": "스쿠리 유저",
                    "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                    "isLeader": true,
                    "joinedAt": "2026-03-04T19:00:00"
                  },
                  {
                    "id": "member-2",
                    "nickname": "김철수",
                    "photoUrl": null,
                    "isLeader": false,
                    "joinedAt": "2026-03-04T19:10:00"
                  },
                  {
                    "id": "member-3",
                    "nickname": "이영희",
                    "photoUrl": "https://cdn.skuri.app/profiles/user-3.png",
                    "isLeader": false,
                    "joinedAt": "2026-03-04T19:15:00"
                  }
                ],
                "tags": [
                  "빠른출발",
                  "정문"
                ],
                "detail": "정문 앞 택시승강장 집합",
                "status": "ARRIVED",
                "settlement": {
                  "status": "PENDING",
                  "taxiFare": 15000,
                  "splitMemberCount": 3,
                  "perPersonAmount": 5000,
                  "settlementTargetMemberIds": [
                    "member-2",
                    "member-3"
                  ],
                  "account": {
                    "bankName": "카카오뱅크",
                    "accountNumber": "3333-01-1234567",
                    "accountHolder": "홍*동",
                    "hideName": true
                  },
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
                      "settledAt": "2026-03-04T21:20:00",
                      "leftParty": false,
                      "leftAt": null
                    }
                  ]
                },
                "createdAt": "2026-03-04T19:00:00"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_PARTY_LIST_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "party-20260304-001",
                    "status": "OPEN",
                    "leaderId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                    "leaderNickname": "스쿠리 유저",
                    "routeSummary": "성결대학교 -> 안양역",
                    "departureTime": "2026-03-04T21:00:00",
                    "currentMembers": 2,
                    "maxMembers": 4,
                    "createdAt": "2026-03-04T19:00:00"
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

    public static final String SUCCESS_ADMIN_PARTY_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "party-20260304-001",
                "status": "ARRIVED",
                "leaderId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "leaderNickname": "스쿠리 유저",
                "leader": {
                  "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                  "nickname": "스쿠리 유저",
                  "photoUrl": "https://cdn.skuri.app/profiles/user-1.png"
                },
                "routeSummary": "성결대학교 -> 안양역",
                "departure": {
                  "name": "성결대학교",
                  "lat": 37.382742,
                  "lng": 126.928031
                },
                "destination": {
                  "name": "안양역",
                  "lat": 37.401,
                  "lng": 126.922
                },
                "departureTime": "2026-03-04T21:00:00",
                "currentMembers": 3,
                "maxMembers": 4,
                "members": [
                  {
                    "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                    "nickname": "스쿠리 유저",
                    "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                    "isLeader": true,
                    "joinedAt": "2026-03-04T19:00:00"
                  },
                  {
                    "id": "member-2",
                    "nickname": "김철수",
                    "photoUrl": null,
                    "isLeader": false,
                    "joinedAt": "2026-03-04T19:10:00"
                  },
                  {
                    "id": "member-3",
                    "nickname": "이영희",
                    "photoUrl": "https://cdn.skuri.app/profiles/user-3.png",
                    "isLeader": false,
                    "joinedAt": "2026-03-04T19:15:00"
                  }
                ],
                "tags": [
                  "빠른출발",
                  "정문"
                ],
                "detail": "정문 앞 택시승강장 집합",
                "pendingJoinRequestCount": 2,
                "settlementStatus": "PENDING",
                "settlement": {
                  "status": "PENDING",
                  "taxiFare": 15000,
                  "splitMemberCount": 3,
                  "perPersonAmount": 5000,
                  "settlementTargetMemberIds": [
                    "member-2",
                    "member-3"
                  ],
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
                      "settledAt": "2026-03-04T21:20:00",
                      "leftParty": false,
                      "leftAt": null
                    }
                  ],
                  "account": {
                    "bankName": "카카오뱅크",
                    "accountNumber": "3333-01-1234567",
                    "accountHolder": "홍*동",
                    "hideName": true
                  }
                },
                "chatRoomId": "party:party-20260304-001",
                "createdAt": "2026-03-04T19:00:00",
                "updatedAt": "2026-03-04T21:10:00"
              }
            }
            """;

    public static final String SUCCESS_PARTY_STATUS_CLOSED = """
            {
              "success": true,
              "data": {
                "id": "party-20260304-001",
                "status": "CLOSED"
              }
            }
            """;

    public static final String SUCCESS_PARTY_STATUS_OPEN = """
            {
              "success": true,
              "data": {
                "id": "party-20260304-001",
                "status": "OPEN"
              }
            }
            """;

    public static final String SUCCESS_PARTY_STATUS_ENDED_FORCE = """
            {
              "success": true,
              "data": {
                "id": "party-20260304-001",
                "status": "ENDED",
                "endReason": "FORCE_ENDED"
              }
            }
            """;

    public static final String SUCCESS_PARTY_STATUS_ENDED_CANCELLED = """
            {
              "success": true,
              "data": {
                "id": "party-20260304-001",
                "status": "ENDED",
                "endReason": "CANCELLED"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_PARTY_MEMBER_REMOVAL = """
            {
              "success": true,
              "data": null
            }
            """;

    public static final String SUCCESS_ADMIN_PARTY_SYSTEM_MESSAGE = """
            {
              "success": true,
              "data": {
                "id": "msg-admin-system-1",
                "chatRoomId": "party:party-20260304-001",
                "senderId": "admin-uid",
                "senderName": "관리자",
                "senderPhotoUrl": null,
                "type": "SYSTEM",
                "text": "관리자 안내 메시지",
                "createdAt": "2026-03-29T12:10:00",
                "updatedAt": "2026-03-29T12:10:00",
                "isDeleted": false
              }
            }
            """;

    public static final String SUCCESS_ADMIN_PARTY_MESSAGES_PAGE = """
            {
              "success": true,
              "data": {
                "messages": [
                  {
                    "id": "msg-system-4",
                    "chatRoomId": "party:party-20260304-001",
                    "senderId": "member-2",
                    "senderName": "김철수",
                    "senderPhotoUrl": null,
                    "type": "SYSTEM",
                    "text": "김철수님이 파티에 합류했어요.",
                    "createdAt": "2026-03-04T19:10:00",
                    "updatedAt": "2026-03-04T19:10:00",
                    "isDeleted": false
                  },
                  {
                    "id": "msg-text-1",
                    "chatRoomId": "party:party-20260304-001",
                    "senderId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                    "senderName": "스쿠리 유저",
                    "senderPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                    "type": "TEXT",
                    "text": "정문 앞 택시승강장 집합이에요.",
                    "createdAt": "2026-03-04T19:12:00",
                    "updatedAt": "2026-03-04T19:12:00",
                    "isDeleted": false
                  }
                ],
                "hasNext": false,
                "nextCursor": null
              }
            }
            """;

    public static final String SUCCESS_ADMIN_PARTY_JOIN_REQUEST_LIST = """
            {
              "success": true,
              "data": [
                {
                  "requestId": "request-2",
                  "memberId": "member-2",
                  "nickname": "김철수",
                  "realname": "김철수",
                  "photoUrl": "https://cdn.skuri.app/profiles/member-2.png",
                  "department": "컴퓨터공학과",
                  "studentId": "20230001",
                  "requestedAt": "2026-03-29T12:05:00"
                },
                {
                  "requestId": "request-1",
                  "memberId": "member-1",
                  "nickname": "이영희",
                  "realname": "이영희",
                  "photoUrl": null,
                  "department": "미디어소프트웨어학과",
                  "studentId": "20230002",
                  "requestedAt": "2026-03-29T12:00:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_JOIN_REQUEST_CREATE = """
            {
              "success": true,
              "data": {
                "id": "request-20260304-001",
                "status": "PENDING",
                "expiryReason": null
              }
            }
            """;

    public static final String SUCCESS_JOIN_REQUEST_ACCEPT = """
            {
              "success": true,
              "data": {
                "id": "request-20260304-001",
                "status": "ACCEPTED",
                "partyId": "party-20260304-001"
              }
            }
            """;

    public static final String SUCCESS_JOIN_REQUEST_DECLINE = """
            {
              "success": true,
              "data": {
                "id": "request-20260304-001",
                "status": "DECLINED",
                "expiryReason": null
              }
            }
            """;

    public static final String SUCCESS_JOIN_REQUEST_CANCEL = """
            {
              "success": true,
              "data": {
                "id": "request-20260304-001",
                "status": "CANCELED",
                "expiryReason": null
              }
            }
            """;

    public static final String SUCCESS_JOIN_REQUEST_LIST_MY = """
            {
              "success": true,
              "data": [
                {
                  "id": "request-20260304-001",
                  "partyId": "party-20260304-001",
                  "requesterId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                  "requesterName": "스쿠리 유저",
                  "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                  "invitationInviterName": "김길동",
                  "status": "PENDING",
                  "expiryReason": null,
                  "createdAt": "2026-03-04T19:05:00"
                },
                {
                  "id": "request-20260304-002",
                  "partyId": "party-20260304-002",
                  "requesterId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                  "requesterName": "스쿠리 유저",
                  "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                  "invitationInviterName": null,
                  "status": "PENDING",
                  "expiryReason": null,
                  "createdAt": "2026-03-04T19:15:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_JOIN_REQUEST_LIST_PARTY = """
            {
              "success": true,
              "data": [
                {
                  "id": "request-20260304-001",
                  "partyId": "party-20260304-001",
                  "requesterId": "member-3",
                  "requesterName": "이영희",
                  "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
                  "invitationInviterName": "김길동",
                  "status": "PENDING",
                  "expiryReason": null,
                  "createdAt": "2026-03-04T19:12:00"
                },
                {
                  "id": "request-20260304-002",
                  "partyId": "party-20260304-001",
                  "requesterId": "member-4",
                  "requesterName": "박민수",
                  "requesterPhotoUrl": null,
                  "invitationInviterName": null,
                  "status": "DECLINED",
                  "expiryReason": null,
                  "createdAt": "2026-03-04T19:20:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_SETTLEMENT_CONFIRM = """
            {
              "success": true,
              "data": {
                "memberId": "member-2",
                "settled": true,
                "settledAt": "2026-03-04T21:25:00",
                "allSettled": false
              }
            }
            """;

    public static final String SUCCESS_MY_PARTIES_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "party-20260304-001",
                  "status": "OPEN",
                  "departure": {
                    "name": "성결대학교",
                    "lat": 37.382742,
                    "lng": 126.928031
                  },
                  "destination": {
                    "name": "안양역",
                    "lat": 37.401,
                    "lng": 126.922
                  },
                  "isLeader": true,
                  "settlement": null
                },
                {
                  "id": "party-20260303-101",
                  "status": "ARRIVED",
                  "departure": {
                    "name": "성결대학교",
                    "lat": 37.382742,
                    "lng": 126.928031
                  },
                  "destination": {
                    "name": "인덕원역",
                    "lat": 37.4019,
                    "lng": 126.9768
                  },
                  "isLeader": false,
                  "settlement": {
                    "status": "PENDING",
                    "taxiFare": 12600,
                    "splitMemberCount": 3,
                    "perPersonAmount": 4200,
                    "settlementTargetMemberIds": [
                      "member-2",
                      "member-3"
                    ],
                    "account": {
                      "bankName": "카카오뱅크",
                      "accountNumber": "3333-01-1234567",
                      "accountHolder": "홍*동",
                      "hideName": true
                    },
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
                        "settledAt": "2026-03-04T21:12:00",
                        "leftParty": true,
                        "leftAt": "2026-03-04T21:18:00"
                      }
                    ]
                  }
                }
              ]
            }
            """;

    public static final String SUCCESS_TAXI_HISTORY_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "party-20260304-001",
                  "departureLabel": "성결대학교",
                  "arrivalLabel": "안양역",
                  "dateTime": "2026-03-04T21:00:00",
                  "passengerCount": 3,
                  "paymentAmount": 5000,
                  "role": "LEADER",
                  "status": "COMPLETED"
                },
                {
                  "id": "party-20260303-101",
                  "departureLabel": "성결대학교",
                  "arrivalLabel": "범계역",
                  "dateTime": "2026-03-03T18:30:00",
                  "passengerCount": 2,
                  "paymentAmount": null,
                  "role": "MEMBER",
                  "status": "CANCELLED"
                }
              ]
            }
            """;

    public static final String SUCCESS_TAXI_HISTORY_SUMMARY = """
            {
              "success": true,
              "data": {
                "totalRideCount": 5,
                "completedRideCount": 4,
                "savedFareAmount": 9374
              }
            }
            """;

    public static final String ERROR_PARTY_NOT_FOUND =
            "{\"success\":false,\"message\":\"파티를 찾을 수 없습니다.\",\"errorCode\":\"PARTY_NOT_FOUND\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_REQUEST_NOT_FOUND =
            "{\"success\":false,\"message\":\"동승 요청을 찾을 수 없습니다.\",\"errorCode\":\"REQUEST_NOT_FOUND\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_NOT_PARTY_LEADER =
            "{\"success\":false,\"message\":\"리더 권한이 필요합니다.\",\"errorCode\":\"NOT_PARTY_LEADER\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_NOT_PARTY_MEMBER =
            "{\"success\":false,\"message\":\"파티 멤버가 아닙니다.\",\"errorCode\":\"NOT_PARTY_MEMBER\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_NOT_PARTY_MEMBER_SETTLEMENT_TARGET =
            "{\"success\":false,\"message\":\"정산 대상 멤버가 아닙니다.\",\"errorCode\":\"NOT_PARTY_MEMBER\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_ALREADY_IN_PARTY =
            "{\"success\":false,\"message\":\"이미 파티에 참여 중입니다.\",\"errorCode\":\"ALREADY_IN_PARTY\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_ALREADY_REQUESTED =
            "{\"success\":false,\"message\":\"이미 동승 요청을 보냈습니다.\",\"errorCode\":\"ALREADY_REQUESTED\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_REQUEST_ALREADY_PROCESSED =
            "{\"success\":false,\"message\":\"이미 처리된 요청입니다.\",\"errorCode\":\"REQUEST_ALREADY_PROCESSED\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_PARTY_CLOSED =
            "{\"success\":false,\"message\":\"모집이 마감된 파티입니다.\",\"errorCode\":\"PARTY_CLOSED\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_PARTY_ENDED =
            "{\"success\":false,\"message\":\"이미 종료된 파티입니다.\",\"errorCode\":\"PARTY_ENDED\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_PARTY_FULL =
            "{\"success\":false,\"message\":\"파티 정원이 가득 찼습니다.\",\"errorCode\":\"PARTY_FULL\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_PARTY_NOT_ARRIVABLE =
            "{\"success\":false,\"message\":\"도착 처리할 수 없는 파티 상태입니다.\",\"errorCode\":\"PARTY_NOT_ARRIVABLE\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_NO_MEMBERS_TO_SETTLE =
            "{\"success\":false,\"message\":\"정산 대상 멤버가 없습니다.\",\"errorCode\":\"NO_MEMBERS_TO_SETTLE\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_VALIDATION_SETTLEMENT_TARGET_MEMBER_IDS =
            "{\"success\":false,\"message\":\"settlementTargetMemberIds에는 현재 파티의 non-leader 멤버만 포함해야 합니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_PARTY_NOT_CANCELABLE =
            "{\"success\":false,\"message\":\"취소할 수 없는 파티 상태입니다.\",\"errorCode\":\"PARTY_NOT_CANCELABLE\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_CANNOT_KICK_LEADER =
            "{\"success\":false,\"message\":\"리더는 강퇴할 수 없습니다.\",\"errorCode\":\"CANNOT_KICK_LEADER\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_CANNOT_KICK_IN_ARRIVED =
            "{\"success\":false,\"message\":\"ARRIVED 상태에서는 멤버를 강퇴할 수 없습니다.\",\"errorCode\":\"CANNOT_KICK_IN_ARRIVED\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_MEMBER_NOT_FOUND =
            "{\"success\":false,\"message\":\"회원을 찾을 수 없습니다.\",\"errorCode\":\"MEMBER_NOT_FOUND\",\"timestamp\":\"2026-03-29T12:00:00\"}";

    public static final String ERROR_PARTY_MEMBER_NOT_FOUND =
            "{\"success\":false,\"message\":\"파티 멤버를 찾을 수 없습니다.\",\"errorCode\":\"PARTY_MEMBER_NOT_FOUND\",\"timestamp\":\"2026-03-29T12:00:00\"}";

    public static final String ERROR_PARTY_LEADER_REMOVAL_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"리더는 관리자 멤버 제거 API로 제거할 수 없습니다.\",\"errorCode\":\"PARTY_LEADER_REMOVAL_NOT_ALLOWED\",\"timestamp\":\"2026-03-29T12:00:00\"}";

    public static final String ERROR_LEADER_CANNOT_LEAVE =
            "{\"success\":false,\"message\":\"리더는 파티에서 나갈 수 없습니다.\",\"errorCode\":\"LEADER_CANNOT_LEAVE\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_INVALID_PARTY_STATE_TRANSITION_OPEN_CLOSED_ONLY =
            "{\"success\":false,\"message\":\"OPEN/CLOSED 상태에서만 수정할 수 있습니다.\",\"errorCode\":\"INVALID_PARTY_STATE_TRANSITION\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_INVALID_PARTY_STATE_TRANSITION_CLOSE_ONLY =
            "{\"success\":false,\"message\":\"OPEN 상태에서만 모집 마감할 수 있습니다.\",\"errorCode\":\"INVALID_PARTY_STATE_TRANSITION\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_INVALID_PARTY_STATE_TRANSITION_REOPEN_ONLY =
            "{\"success\":false,\"message\":\"CLOSED 상태에서만 모집 재개할 수 있습니다.\",\"errorCode\":\"INVALID_PARTY_STATE_TRANSITION\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_INVALID_PARTY_STATE_TRANSITION_FORCE_END_ONLY =
            "{\"success\":false,\"message\":\"ARRIVED 상태에서만 강제 종료할 수 있습니다.\",\"errorCode\":\"INVALID_PARTY_STATE_TRANSITION\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_INVALID_PARTY_STATE_TRANSITION_SETTLEMENT_ONLY =
            "{\"success\":false,\"message\":\"ARRIVED 상태에서만 정산 확인할 수 있습니다.\",\"errorCode\":\"INVALID_PARTY_STATE_TRANSITION\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_PARTY_CONCURRENT_MODIFICATION =
            "{\"success\":false,\"message\":\"동시 요청 충돌이 발생했습니다. 다시 시도해주세요.\",\"errorCode\":\"PARTY_CONCURRENT_MODIFICATION\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_VALIDATION_PARTY_UPDATE_REQUIRED_FIELD =
            "{\"success\":false,\"message\":\"departureTime 또는 detail 중 최소 하나는 입력해야 합니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_VALIDATION_ADMIN_PARTY_SYSTEM_MESSAGE_BLANK =
            "{\"success\":false,\"message\":\"message: message는 필수입니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-29T12:00:00\"}";

    public static final String ERROR_VALIDATION_ADMIN_PARTY_SYSTEM_MESSAGE_TOO_LONG =
            "{\"success\":false,\"message\":\"message: message는 500자 이하여야 합니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-29T12:00:00\"}";

    public static final String ERROR_FORBIDDEN_ONLY_REQUESTER_CAN_CANCEL_JOIN_REQUEST =
            "{\"success\":false,\"message\":\"요청자 본인만 취소할 수 있습니다.\",\"errorCode\":\"FORBIDDEN\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String ERROR_ALREADY_SETTLED =
            "{\"success\":false,\"message\":\"이미 정산 완료 처리된 멤버입니다.\",\"errorCode\":\"ALREADY_SETTLED\",\"timestamp\":\"2026-03-04T12:00:00\"}";

    public static final String SSE_PARTIES_STREAM_FULL = """
            event: SNAPSHOT
            data: {
              "parties": [
                {
                  "id": "party-1",
                  "leaderId": "member-1",
                  "leaderName": "스쿠리 유저",
                  "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                  "participantSummaries": [
                    {
                      "id": "member-1",
                      "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                      "nickname": "스쿠리 유저",
                      "isLeader": true
                    },
                    {
                      "id": "member-2",
                      "photoUrl": null,
                      "nickname": "김철수",
                      "isLeader": false
                    }
                  ],
                  "departure": {
                    "name": "성결대학교",
                    "lat": 37.3801,
                    "lng": 126.9282
                  },
                  "destination": {
                    "name": "안양역",
                    "lat": 37.4012,
                    "lng": 126.9228
                  },
                  "departureTime": "2026-03-04T18:30:00",
                  "maxMembers": 4,
                  "currentMembers": 2,
                  "tags": ["정문", "빠른출발"],
                  "detail": "정문 앞 출발",
                  "status": "OPEN",
                  "createdAt": "2026-03-04T17:50:00"
                }
              ]
            }

            event: PARTY_CREATED
            data: {
              "id": "party-2",
              "leaderId": "member-2",
              "leaderName": "김리더",
              "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-2.png",
              "participantSummaries": [
                {
                  "id": "member-2",
                  "photoUrl": "https://cdn.skuri.app/profiles/user-2.png",
                  "nickname": "김리더",
                  "isLeader": true
                }
              ],
              "departure": {
                "name": "성결대학교",
                "lat": 37.3801,
                "lng": 126.9282
              },
              "destination": {
                "name": "범계역",
                "lat": 37.3891,
                "lng": 126.9507
              },
              "departureTime": "2026-03-04T19:00:00",
              "maxMembers": 4,
              "currentMembers": 1,
              "tags": ["정문"],
              "detail": "정문 택시승강장",
              "status": "OPEN",
              "createdAt": "2026-03-04T18:00:00"
            }

            event: PARTY_UPDATED
            data: {
              "id": "party-1",
              "leaderId": "member-1",
              "leaderName": "스쿠리 유저",
              "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
              "participantSummaries": [
                {
                  "id": "member-1",
                  "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                  "nickname": "스쿠리 유저",
                  "isLeader": true
                },
                {
                  "id": "member-2",
                  "photoUrl": null,
                  "nickname": "김철수",
                  "isLeader": false
                }
              ],
              "departure": {
                "name": "성결대학교",
                "lat": 37.3801,
                "lng": 126.9282
              },
              "destination": {
                "name": "안양역",
                "lat": 37.4012,
                "lng": 126.9228
              },
              "departureTime": "2026-03-04T18:40:00",
              "maxMembers": 4,
              "currentMembers": 2,
              "tags": ["정문", "빠른출발"],
              "detail": "정문 편의점 앞 출발",
              "status": "CLOSED",
              "createdAt": "2026-03-04T17:50:00"
            }

            event: PARTY_STATUS_CHANGED
            data: {
              "id": "party-1",
              "status": "ARRIVED",
              "currentMembers": 3
            }

            event: PARTY_MEMBER_JOINED
            data: {
              "partyId": "party-1",
              "memberId": "member-3",
              "memberName": "김철수",
              "currentMembers": 3
            }

            event: PARTY_MEMBER_LEFT
            data: {
              "partyId": "party-1",
              "memberId": "member-3",
              "reason": "KICKED",
              "currentMembers": 2
            }

            event: PARTY_DELETED
            data: {
              "id": "party-9"
            }

            event: HEARTBEAT
            data: {
              "timestamp": "2026-03-04T18:05:00"
            }

            """;

    public static final String SSE_PARTIES_SNAPSHOT = """
            event: SNAPSHOT
            data: {
              "parties": [
                {
                  "id": "party-1",
                  "leaderId": "member-1",
                  "leaderName": "스쿠리 유저",
                  "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                  "participantSummaries": [
                    {
                      "id": "member-1",
                      "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                      "nickname": "스쿠리 유저",
                      "isLeader": true
                    },
                    {
                      "id": "member-2",
                      "photoUrl": null,
                      "nickname": "김철수",
                      "isLeader": false
                    }
                  ],
                  "departure": {
                    "name": "성결대학교",
                    "lat": 37.3801,
                    "lng": 126.9282
                  },
                  "destination": {
                    "name": "안양역",
                    "lat": 37.4012,
                    "lng": 126.9228
                  },
                  "departureTime": "2026-03-04T18:30:00",
                  "maxMembers": 4,
                  "currentMembers": 2,
                  "tags": ["정문", "빠른출발"],
                  "detail": "정문 앞 출발",
                  "status": "OPEN",
                  "createdAt": "2026-03-04T17:50:00"
                }
              ]
            }

            """;

    public static final String SSE_PARTIES_CREATED = """
            event: PARTY_CREATED
            data: {
              "id": "party-2",
              "leaderId": "member-2",
              "leaderName": "김리더",
              "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-2.png",
              "participantSummaries": [
                {
                  "id": "member-2",
                  "photoUrl": "https://cdn.skuri.app/profiles/user-2.png",
                  "nickname": "김리더",
                  "isLeader": true
                }
              ],
              "departure": {
                "name": "성결대학교",
                "lat": 37.3801,
                "lng": 126.9282
              },
              "destination": {
                "name": "범계역",
                "lat": 37.3891,
                "lng": 126.9507
              },
              "departureTime": "2026-03-04T19:00:00",
              "maxMembers": 4,
              "currentMembers": 1,
              "tags": ["정문"],
              "detail": "정문 택시승강장",
              "status": "OPEN",
              "createdAt": "2026-03-04T18:00:00"
            }

            """;

    public static final String SSE_PARTIES_UPDATED = """
            event: PARTY_UPDATED
            data: {
              "id": "party-1",
              "leaderId": "member-1",
              "leaderName": "스쿠리 유저",
              "leaderPhotoUrl": "https://cdn.skuri.app/profiles/user-1.png",
              "participantSummaries": [
                {
                  "id": "member-1",
                  "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                  "nickname": "스쿠리 유저",
                  "isLeader": true
                },
                {
                  "id": "member-2",
                  "photoUrl": null,
                  "nickname": "김철수",
                  "isLeader": false
                }
              ],
              "departure": {
                "name": "성결대학교",
                "lat": 37.3801,
                "lng": 126.9282
              },
              "destination": {
                "name": "안양역",
                "lat": 37.4012,
                "lng": 126.9228
              },
              "departureTime": "2026-03-04T18:40:00",
              "maxMembers": 4,
              "currentMembers": 2,
              "tags": ["정문", "빠른출발"],
              "detail": "정문 편의점 앞 출발",
              "status": "CLOSED",
              "createdAt": "2026-03-04T17:50:00"
            }

            """;

    public static final String SSE_PARTIES_STATUS_CHANGED = """
            event: PARTY_STATUS_CHANGED
            data: {
              "id": "party-1",
              "status": "ARRIVED",
              "currentMembers": 3
            }

            """;

    public static final String SSE_PARTIES_MEMBER_JOINED = """
            event: PARTY_MEMBER_JOINED
            data: {
              "partyId": "party-1",
              "memberId": "member-3",
              "memberName": "김철수",
              "currentMembers": 3
            }

            """;

    public static final String SSE_PARTIES_MEMBER_LEFT = """
            event: PARTY_MEMBER_LEFT
            data: {
              "partyId": "party-1",
              "memberId": "member-3",
              "reason": "KICKED",
              "currentMembers": 2
            }

            """;

    public static final String SSE_PARTIES_DELETED = """
            event: PARTY_DELETED
            data: {
              "id": "party-9"
            }

            """;

    public static final String SSE_PARTIES_HEARTBEAT = """
            event: HEARTBEAT
            data: {
              "timestamp": "2026-03-04T18:05:00"
            }

            """;

    public static final String SSE_PARTY_JOIN_REQUESTS_STREAM_FULL = """
            event: SNAPSHOT
            data: {
              "partyId": "party-1",
              "requests": [
                {
                  "id": "request-1",
                  "partyId": "party-1",
                  "requesterId": "member-2",
                  "requesterName": "김철수",
                  "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-2.png",
                  "invitationInviterName": null,
                  "status": "PENDING",
                  "expiryReason": null,
                  "createdAt": "2026-03-04T18:10:00"
                }
              ]
            }

            event: JOIN_REQUEST_CREATED
            data: {
              "id": "request-2",
              "partyId": "party-1",
              "requesterId": "member-3",
              "requesterName": "박민수",
              "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
              "invitationInviterName": "김길동",
              "status": "PENDING",
              "expiryReason": null,
              "createdAt": "2026-03-04T18:11:00"
            }

            event: JOIN_REQUEST_UPDATED
            data: {
              "id": "request-2",
              "partyId": "party-1",
              "requesterId": "member-3",
              "requesterName": "박민수",
              "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
              "invitationInviterName": "김길동",
              "status": "EXPIRED",
              "expiryReason": "CAPACITY_FULL",
              "createdAt": "2026-03-04T18:11:00"
            }

            event: HEARTBEAT
            data: {
              "timestamp": "2026-03-04T18:12:00"
            }

            """;

    public static final String SSE_PARTY_JOIN_REQUESTS_SNAPSHOT = """
            event: SNAPSHOT
            data: {
              "partyId": "party-1",
              "requests": [
                {
                  "id": "request-1",
                  "partyId": "party-1",
                  "requesterId": "member-2",
                  "requesterName": "김철수",
                  "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-2.png",
                  "invitationInviterName": null,
                  "status": "PENDING",
                  "expiryReason": null,
                  "createdAt": "2026-03-04T18:10:00"
                }
              ]
            }

            """;

    public static final String SSE_PARTY_JOIN_REQUESTS_CREATED = """
            event: JOIN_REQUEST_CREATED
            data: {
              "id": "request-2",
              "partyId": "party-1",
              "requesterId": "member-3",
              "requesterName": "박민수",
              "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
              "invitationInviterName": "김길동",
              "status": "PENDING",
              "expiryReason": null,
              "createdAt": "2026-03-04T18:11:00"
            }

            """;

    public static final String SSE_PARTY_JOIN_REQUESTS_UPDATED = """
            event: JOIN_REQUEST_UPDATED
            data: {
              "id": "request-2",
              "partyId": "party-1",
              "requesterId": "member-3",
              "requesterName": "박민수",
              "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
              "invitationInviterName": "김길동",
              "status": "EXPIRED",
              "expiryReason": "CAPACITY_FULL",
              "createdAt": "2026-03-04T18:11:00"
            }

            """;

    public static final String SSE_PARTY_JOIN_REQUESTS_HEARTBEAT = """
            event: HEARTBEAT
            data: {
              "timestamp": "2026-03-04T18:12:00"
            }

            """;

    public static final String SSE_MY_JOIN_REQUESTS_STREAM_FULL = """
            event: SNAPSHOT
            data: {
              "requests": [
                {
                  "id": "request-5",
                  "partyId": "party-7",
                  "requesterId": "member-3",
                  "requesterName": "박민수",
                  "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
                  "invitationInviterName": null,
                  "status": "PENDING",
                  "expiryReason": null,
                  "createdAt": "2026-03-04T18:20:00"
                }
              ]
            }

            event: MY_JOIN_REQUEST_CREATED
            data: {
              "id": "request-6",
              "partyId": "party-8",
              "requesterId": "member-3",
              "requesterName": "박민수",
              "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
              "invitationInviterName": "김길동",
              "status": "PENDING",
              "expiryReason": null,
              "createdAt": "2026-03-04T18:21:00"
            }

            event: MY_JOIN_REQUEST_UPDATED
            data: {
              "id": "request-6",
              "partyId": "party-8",
              "requesterId": "member-3",
              "requesterName": "박민수",
              "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
              "invitationInviterName": "김길동",
              "status": "EXPIRED",
              "expiryReason": "CAPACITY_FULL",
              "createdAt": "2026-03-04T18:21:00"
            }

            event: HEARTBEAT
            data: {
              "timestamp": "2026-03-04T18:22:00"
            }

            """;

    public static final String SSE_MY_JOIN_REQUESTS_SNAPSHOT = """
            event: SNAPSHOT
            data: {
              "requests": [
                {
                  "id": "request-5",
                  "partyId": "party-7",
                  "requesterId": "member-3",
                  "requesterName": "박민수",
                  "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
                  "invitationInviterName": null,
                  "status": "PENDING",
                  "expiryReason": null,
                  "createdAt": "2026-03-04T18:20:00"
                }
              ]
            }

            """;

    public static final String SSE_MY_JOIN_REQUESTS_CREATED = """
            event: MY_JOIN_REQUEST_CREATED
            data: {
              "id": "request-6",
              "partyId": "party-8",
              "requesterId": "member-3",
              "requesterName": "박민수",
              "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
              "invitationInviterName": "김길동",
              "status": "PENDING",
              "expiryReason": null,
              "createdAt": "2026-03-04T18:21:00"
            }

            """;

    public static final String SSE_MY_JOIN_REQUESTS_UPDATED = """
            event: MY_JOIN_REQUEST_UPDATED
            data: {
              "id": "request-6",
              "partyId": "party-8",
              "requesterId": "member-3",
              "requesterName": "박민수",
              "requesterPhotoUrl": "https://cdn.skuri.app/profiles/user-3.png",
              "invitationInviterName": "김길동",
              "status": "EXPIRED",
              "expiryReason": "CAPACITY_FULL",
              "createdAt": "2026-03-04T18:21:00"
            }

            """;

    public static final String SSE_MY_JOIN_REQUESTS_HEARTBEAT = """
            event: HEARTBEAT
            data: {
              "timestamp": "2026-03-04T18:22:00"
            }

            """;
}
