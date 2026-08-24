package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiInvitationExamples {

    private OpenApiInvitationExamples() {
    }

    public static final String SUCCESS_PARTY_ELIGIBLE = """
            {"success":true,"data":{"partyId":"party-1","targetName":"정문 → 안양역","remainingCapacity":2,"canInvite":true,"unavailableReason":null,"friends":[{"friendPublicId":"friend-public-1","nickname":"가람","department":"컴퓨터공학과","photoUrl":null,"favorite":true}],"alreadyMemberFriends":[{"friendPublicId":"friend-public-2","nickname":"나래","department":"컴퓨터공학과","photoUrl":null,"favorite":false}],"alreadyPendingFriends":[],"alreadyMemberCount":1,"alreadyPendingCount":0,"notEligibleCount":0}}
            """;
    public static final String SUCCESS_PARTY_BATCH = """
            {"success":true,"data":{"results":[{"friendPublicId":"friend-public-1","outcome":"SENT","invitationId":"invitation-1"},{"friendPublicId":"friend-public-2","outcome":"NOT_ELIGIBLE","invitationId":null}]}}
            """;
    public static final String SUCCESS_PARTY_RECEIVED = """
            {"success":true,"data":[{"invitationId":"invitation-1","invitationType":"PARTY","status":"PENDING","expiryReason":null,"inviter":{"friendPublicId":"friend-public-2","nickname":"나래","department":"컴퓨터공학과","photoUrl":null,"favorite":false},"target":{"partyId":"party-1","departureName":"정문","destinationName":"안양역","departureTime":"2026-08-24T18:00:00","currentMembers":2,"maxMembers":4,"status":"OPEN"},"createdAt":"2026-08-23T12:00:00","respondedAt":null}]}
            """;
    public static final String SUCCESS_PARTY_ACCEPT_JOINED = """
            {"success":true,"data":{"invitationId":"invitation-1","partyId":"party-1","status":"ACCEPTED","result":"JOINED","joinRequestId":null}}
            """;
    public static final String SUCCESS_PARTY_ACCEPT_LEADER_APPROVAL_PENDING = """
            {"success":true,"data":{"invitationId":"invitation-1","partyId":"party-1","status":"ACCEPTED","result":"LEADER_APPROVAL_PENDING","joinRequestId":"request-1"}}
            """;
    public static final String SUCCESS_PARTY_DECLINE = """
            {"success":true,"data":{"invitationId":"invitation-1","partyId":"party-1","status":"DECLINED","result":null,"joinRequestId":null}}
            """;
    public static final String SUCCESS_CHAT_ELIGIBLE = """
            {"success":true,"data":{"chatRoomId":"public:university","targetName":"성결대 전체 채팅방","remainingCapacity":null,"expiresInDays":7,"sameDepartmentOnly":false,"friends":[],"alreadyMemberFriends":[{"friendPublicId":"friend-public-1","nickname":"가람","department":"컴퓨터공학과","photoUrl":null,"favorite":true}],"alreadyPendingFriends":[],"alreadyMemberCount":1,"alreadyPendingCount":0,"notEligibleCount":0}}
            """;
    public static final String SUCCESS_CHAT_BATCH = """
            {"success":true,"data":{"results":[{"friendPublicId":"friend-public-1","outcome":"SENT","invitationId":"invitation-2"}]}}
            """;
    public static final String SUCCESS_CHAT_RECEIVED = """
            {"success":true,"data":[{"invitationId":"invitation-2","invitationType":"CHAT_ROOM","status":"PENDING","expiryReason":null,"inviter":{"friendPublicId":"friend-public-2","nickname":"나래","department":"컴퓨터공학과","photoUrl":null,"favorite":false},"target":{"chatRoomId":"public:university","name":"성결대 전체 채팅방","type":"UNIVERSITY","memberCount":10,"maxMembers":null},"createdAt":"2026-08-23T12:00:00","expiresAt":"2026-08-30T12:00:00","respondedAt":null}]}
            """;
    public static final String SUCCESS_CHAT_MUTATION = """
            {"success":true,"data":{"invitationId":"invitation-2","chatRoomId":"public:university","status":"ACCEPTED"}}
            """;
    public static final String SUCCESS_CHAT_DECLINE = """
            {"success":true,"data":{"invitationId":"invitation-2","chatRoomId":"public:university","status":"DECLINED"}}
            """;
    public static final String ERROR_PARTY_INVITATION_NOT_FOUND = """
            {"success":false,"message":"택시파티 초대를 찾을 수 없습니다.","errorCode":"PARTY_INVITATION_NOT_FOUND","timestamp":"2026-08-23T12:00:00"}
            """;
    public static final String ERROR_PARTY_INVITATION_STATE = """
            {"success":false,"message":"현재 상태에서는 택시파티 초대를 처리할 수 없습니다.","errorCode":"PARTY_INVITATION_STATE_NOT_ALLOWED","timestamp":"2026-08-23T12:00:00"}
            """;
    public static final String ERROR_PARTY_INVITATION_RECIPIENT_REQUIRED = """
            {"success":false,"message":"택시파티 초대 수신자만 처리할 수 있습니다.","errorCode":"PARTY_INVITATION_RECIPIENT_REQUIRED","timestamp":"2026-08-23T12:00:00"}
            """;
    public static final String ERROR_PARTY_INVITATION_INVITER_REQUIRED = """
            {"success":false,"message":"택시파티 초대 발송자 또는 만료된 초대 수신자만 처리할 수 있습니다.","errorCode":"PARTY_INVITATION_INVITER_REQUIRED","timestamp":"2026-08-23T12:00:00"}
            """;
    public static final String ERROR_CHAT_INVITATION_INVALID_ROOM = """
            {"success":false,"message":"공개 PARTY 이외의 채팅방만 친구를 초대할 수 있습니다.","errorCode":"INVALID_REQUEST","timestamp":"2026-08-23T12:00:00"}
            """;
    public static final String ERROR_CHAT_INVITATION_NOT_FOUND = """
            {"success":false,"message":"채팅방 초대를 찾을 수 없습니다.","errorCode":"CHAT_ROOM_INVITATION_NOT_FOUND","timestamp":"2026-08-23T12:00:00"}
            """;
    public static final String ERROR_CHAT_INVITATION_RECIPIENT_REQUIRED = """
            {"success":false,"message":"채팅방 초대 수신자만 처리할 수 있습니다.","errorCode":"CHAT_ROOM_INVITATION_RECIPIENT_REQUIRED","timestamp":"2026-08-23T12:00:00"}
            """;
    public static final String ERROR_CHAT_INVITATION_INVITER_REQUIRED = """
            {"success":false,"message":"채팅방 초대 발송자 또는 만료된 초대 수신자만 처리할 수 있습니다.","errorCode":"CHAT_ROOM_INVITATION_INVITER_REQUIRED","timestamp":"2026-08-23T12:00:00"}
            """;
    public static final String ERROR_CHAT_INVITATION_STATE = """
            {"success":false,"message":"현재 상태에서는 채팅방 초대를 처리할 수 없습니다.","errorCode":"CHAT_ROOM_INVITATION_STATE_NOT_ALLOWED","timestamp":"2026-08-23T12:00:00"}
            """;
}
