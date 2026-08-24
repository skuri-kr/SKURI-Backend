package com.skuri.skuri_backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 요청/인증/인가 에러.
    // 특정 도메인과 무관하게 모든 API에서 재사용되는 기본 오류 코드다.
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "관리자 권한이 필요합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "리소스 충돌이 발생했습니다."),
    RESOURCE_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "RESOURCE_CONCURRENT_MODIFICATION", "동시 수정 충돌이 발생했습니다. 다시 시도해주세요."),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR", "입력값 검증에 실패했습니다."),

    // 회원(Member) 도메인 에러.
    // 회원 식별, 재가입 제한, 탈퇴 상태, 계좌 등록 요건처럼 사용자 계정 상태와 직접 연결된 오류를 다룬다.
    EMAIL_DOMAIN_RESTRICTED(HttpStatus.FORBIDDEN, "EMAIL_DOMAIN_RESTRICTED", "성결대학교 이메일(@sungkyul.ac.kr)만 사용 가능합니다."),
    MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "MEMBER_WITHDRAWN", "탈퇴한 회원은 서비스에 접근할 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED(HttpStatus.CONFLICT, "WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED", "탈퇴한 계정은 같은 인증 계정으로 재가입할 수 없습니다."),
    MEMBER_WITHDRAWAL_NOT_ALLOWED(HttpStatus.CONFLICT, "MEMBER_WITHDRAWAL_NOT_ALLOWED", "현재 상태에서는 회원 탈퇴를 진행할 수 없습니다."),
    SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED", "자기 자신의 관리자 권한은 변경할 수 없습니다."),
    MEMBER_ACTIVITY_NOT_AVAILABLE_FOR_WITHDRAWN(HttpStatus.CONFLICT, "MEMBER_ACTIVITY_NOT_AVAILABLE_FOR_WITHDRAWN", "탈퇴한 회원의 활동 요약은 조회할 수 없습니다."),
    MEMBER_PROFILE_INCOMPLETE(HttpStatus.CONFLICT, "MEMBER_PROFILE_INCOMPLETE", "회원가입을 완료한 후 친구 기능을 이용할 수 있습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "NICKNAME_ALREADY_EXISTS", "이미 사용 중인 닉네임입니다."),
    NICKNAME_RESERVED(HttpStatus.UNPROCESSABLE_CONTENT, "NICKNAME_RESERVED", "사용할 수 없는 닉네임입니다."),
    BANK_ACCOUNT_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, "BANK_ACCOUNT_REQUIRED", "계좌 정보 등록 후 이용 가능합니다."),

    // 친구(Friend) 도메인 에러.
    // 친구 코드와 공개 프로필의 안전한 발급·조회에 사용한다.
    FRIEND_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND_CODE_NOT_FOUND", "친구 코드를 찾을 수 없습니다."),
    FRIEND_CODE_REGENERATION_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "FRIEND_CODE_REGENERATION_COOLDOWN", "친구 코드는 24시간에 한 번만 재발급할 수 있습니다."),
    FRIEND_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FRIEND_SELF_NOT_ALLOWED", "자기 자신의 친구 코드는 확인할 수 없습니다."),
    FRIEND_SELF_REQUEST_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FRIEND_SELF_REQUEST_NOT_ALLOWED", "자기 자신에게 친구 요청을 보낼 수 없습니다."),
    FRIEND_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND_TARGET_NOT_FOUND", "친구 대상을 찾을 수 없습니다."),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND_REQUEST_NOT_FOUND", "친구 요청을 찾을 수 없습니다."),
    FRIENDSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIENDSHIP_NOT_FOUND", "친구 관계를 찾을 수 없습니다."),
    FRIEND_REQUEST_ALREADY_PENDING(HttpStatus.CONFLICT, "FRIEND_REQUEST_ALREADY_PENDING", "이미 친구 요청을 보냈습니다."),
    FRIEND_ALREADY_EXISTS(HttpStatus.CONFLICT, "FRIEND_ALREADY_EXISTS", "이미 친구입니다."),
    FRIEND_REQUEST_STATE_NOT_ALLOWED(HttpStatus.CONFLICT, "FRIEND_REQUEST_STATE_NOT_ALLOWED", "현재 상태에서는 친구 요청을 처리할 수 없습니다."),
    FRIEND_REQUEST_RECIPIENT_REQUIRED(HttpStatus.FORBIDDEN, "FRIEND_REQUEST_RECIPIENT_REQUIRED", "친구 요청 수신자만 처리할 수 있습니다."),
    FRIEND_REQUEST_REQUESTER_REQUIRED(HttpStatus.FORBIDDEN, "FRIEND_REQUEST_REQUESTER_REQUIRED", "친구 요청자만 취소할 수 있습니다."),
    FRIEND_SELF_BLOCK_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FRIEND_SELF_BLOCK_NOT_ALLOWED", "자기 자신은 차단할 수 없습니다."),

    // 이미지(Image) 업로드/미디어 도메인 에러.
    // 파일 크기, 해상도, 포맷, 저장 실패처럼 공통 업로드 인프라에서 발생하는 예외를 담당한다.
    IMAGE_DIMENSIONS_EXCEEDED(HttpStatus.UNPROCESSABLE_CONTENT, "IMAGE_DIMENSIONS_EXCEEDED", "이미지 해상도가 허용 범위를 초과했습니다."),
    IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "IMAGE_TOO_LARGE", "이미지 파일은 최대 10MB까지 업로드할 수 있습니다."),
    IMAGE_INVALID_FORMAT(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE_INVALID_FORMAT", "지원하지 않는 이미지 형식입니다. JPEG, PNG, WebP만 업로드할 수 있습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_UPLOAD_FAILED", "이미지 업로드에 실패했습니다."),

    // 알림(Notification) 도메인 에러.
    // 알림 조회와 소유자 접근 제어처럼 사용자 알림 기능에서 발생하는 오류를 정의한다.
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    NOT_NOTIFICATION_OWNER(HttpStatus.FORBIDDEN, "NOT_NOTIFICATION_OWNER", "본인 알림만 접근할 수 있습니다."),

    // 학사/시간표(Academic) 도메인 에러.
    // 강의, 학사 일정, 시간표 중복 검증 등 학사 데이터 관리에서 사용하는 오류 코드다.
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "강의를 찾을 수 없습니다."),
    ACADEMIC_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "ACADEMIC_SCHEDULE_NOT_FOUND", "학사 일정을 찾을 수 없습니다."),
    TIMETABLE_CONFLICT(HttpStatus.CONFLICT, "TIMETABLE_CONFLICT", "시간이 겹치는 강의가 이미 시간표에 있습니다."),
    COURSE_ALREADY_IN_TIMETABLE(HttpStatus.CONFLICT, "COURSE_ALREADY_IN_TIMETABLE", "이미 시간표에 추가된 강의입니다."),

    // 택시파티(TaxiParty) 도메인 에러.
    // 파티 상태 전이, 정원/멤버십, 정산, 리더 권한처럼 핵심 도메인 규칙 위반을 표현한다.
    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY_NOT_FOUND", "파티를 찾을 수 없습니다."),
    PARTY_FULL(HttpStatus.CONFLICT, "PARTY_FULL", "파티 정원이 가득 찼습니다."),
    PARTY_CLOSED(HttpStatus.CONFLICT, "PARTY_CLOSED", "모집이 마감된 파티입니다."),
    PARTY_ENDED(HttpStatus.CONFLICT, "PARTY_ENDED", "이미 종료된 파티입니다."),
    NOT_PARTY_LEADER(HttpStatus.FORBIDDEN, "NOT_PARTY_LEADER", "리더 권한이 필요합니다."),
    NOT_PARTY_MEMBER(HttpStatus.FORBIDDEN, "NOT_PARTY_MEMBER", "파티 멤버가 아닙니다."),
    ALREADY_IN_PARTY(HttpStatus.CONFLICT, "ALREADY_IN_PARTY", "이미 파티에 참여 중입니다."),
    ALREADY_REQUESTED(HttpStatus.CONFLICT, "ALREADY_REQUESTED", "이미 동승 요청을 보냈습니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "동승 요청을 찾을 수 없습니다."),
    REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "REQUEST_ALREADY_PROCESSED", "이미 처리된 요청입니다."),
    SETTLEMENT_NOT_COMPLETED(HttpStatus.CONFLICT, "SETTLEMENT_NOT_COMPLETED", "정산이 완료되지 않았습니다."),
    ALREADY_SETTLED(HttpStatus.CONFLICT, "ALREADY_SETTLED", "이미 정산 완료 처리된 멤버입니다."),
    PARTY_NOT_ARRIVABLE(HttpStatus.CONFLICT, "PARTY_NOT_ARRIVABLE", "도착 처리할 수 없는 파티 상태입니다."),
    PARTY_NOT_CANCELABLE(HttpStatus.CONFLICT, "PARTY_NOT_CANCELABLE", "취소할 수 없는 파티 상태입니다."),
    NO_MEMBERS_TO_SETTLE(HttpStatus.CONFLICT, "NO_MEMBERS_TO_SETTLE", "정산 대상 멤버가 없습니다."),
    LEADER_CANNOT_LEAVE(HttpStatus.CONFLICT, "LEADER_CANNOT_LEAVE", "리더는 파티에서 나갈 수 없습니다."),
    CANNOT_LEAVE_ARRIVED_PARTY(HttpStatus.CONFLICT, "CANNOT_LEAVE_ARRIVED_PARTY", "ARRIVED 상태에서는 파티를 나갈 수 없습니다."),
    CANNOT_KICK_IN_ARRIVED(HttpStatus.CONFLICT, "CANNOT_KICK_IN_ARRIVED", "ARRIVED 상태에서는 멤버를 강퇴할 수 없습니다."),
    CANNOT_KICK_LEADER(HttpStatus.BAD_REQUEST, "CANNOT_KICK_LEADER", "리더는 강퇴할 수 없습니다."),
    PARTY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY_MEMBER_NOT_FOUND", "파티 멤버를 찾을 수 없습니다."),
    PARTY_LEADER_REMOVAL_NOT_ALLOWED(HttpStatus.CONFLICT, "PARTY_LEADER_REMOVAL_NOT_ALLOWED", "리더는 관리자 멤버 제거 API로 제거할 수 없습니다."),
    INVALID_PARTY_STATE_TRANSITION(HttpStatus.CONFLICT, "INVALID_PARTY_STATE_TRANSITION", "허용되지 않는 파티 상태 전이입니다."),
    PARTY_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "PARTY_CONCURRENT_MODIFICATION", "동시 요청 충돌이 발생했습니다. 다시 시도해주세요."),
    PARTY_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY_INVITATION_NOT_FOUND", "택시파티 초대를 찾을 수 없습니다."),
    PARTY_INVITATION_STATE_NOT_ALLOWED(HttpStatus.CONFLICT, "PARTY_INVITATION_STATE_NOT_ALLOWED", "현재 상태에서는 택시파티 초대를 처리할 수 없습니다."),
    PARTY_INVITATION_RECIPIENT_REQUIRED(HttpStatus.FORBIDDEN, "PARTY_INVITATION_RECIPIENT_REQUIRED", "택시파티 초대 수신자만 처리할 수 있습니다."),
    PARTY_INVITATION_INVITER_REQUIRED(HttpStatus.FORBIDDEN, "PARTY_INVITATION_INVITER_REQUIRED", "택시파티 초대 발송자 또는 만료된 초대 수신자만 처리할 수 있습니다."),

    // 채팅(Chat) 도메인 에러.
    // 채팅방 참여 자격, 정원 제한, WebSocket 인증 실패 등 채팅 통신 관련 오류를 다룬다.
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_MESSAGE_NOT_FOUND", "채팅 메시지를 찾을 수 없습니다."),
    NOT_CHAT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "NOT_CHAT_ROOM_MEMBER", "채팅방 멤버가 아닙니다."),
    NOT_CHAT_MESSAGE_AUTHOR(HttpStatus.FORBIDDEN, "NOT_CHAT_MESSAGE_AUTHOR", "메시지 작성자만 처리할 수 있습니다."),
    CHAT_ROOM_FULL(HttpStatus.CONFLICT, "CHAT_ROOM_FULL", "채팅방 정원이 가득 찼습니다."),
    ALREADY_CHAT_ROOM_MEMBER(HttpStatus.CONFLICT, "ALREADY_CHAT_ROOM_MEMBER", "이미 채팅방에 참여 중입니다."),
    CHAT_MESSAGE_EDIT_NOT_ALLOWED(HttpStatus.CONFLICT, "CHAT_MESSAGE_EDIT_NOT_ALLOWED", "이 메시지는 수정할 수 없습니다."),
    CHAT_MESSAGE_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "CHAT_MESSAGE_DELETE_NOT_ALLOWED", "이 메시지는 삭제할 수 없습니다."),
    CHAT_MESSAGE_EDIT_WINDOW_EXPIRED(HttpStatus.CONFLICT, "CHAT_MESSAGE_EDIT_WINDOW_EXPIRED", "메시지는 전송 후 15분 이내에만 수정할 수 있습니다."),
    CHAT_MESSAGE_ALREADY_DELETED(HttpStatus.CONFLICT, "CHAT_MESSAGE_ALREADY_DELETED", "이미 삭제된 메시지입니다."),
    CHAT_MESSAGE_MUTATION_NOT_ALLOWED(HttpStatus.CONFLICT, "CHAT_MESSAGE_MUTATION_NOT_ALLOWED", "이 메시지는 수정하거나 삭제할 수 없습니다."),
    CHAT_IMAGE_UNAVAILABLE(HttpStatus.CONFLICT, "CHAT_IMAGE_UNAVAILABLE", "정리된 채팅 이미지는 새로 업로드해야 합니다."),
    STOMP_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "STOMP_AUTH_FAILED", "WebSocket 인증에 실패했습니다."),
    CHAT_ROOM_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_INVITATION_NOT_FOUND", "채팅방 초대를 찾을 수 없습니다."),
    CHAT_ROOM_INVITATION_STATE_NOT_ALLOWED(HttpStatus.CONFLICT, "CHAT_ROOM_INVITATION_STATE_NOT_ALLOWED", "현재 상태에서는 채팅방 초대를 처리할 수 없습니다."),
    CHAT_ROOM_INVITATION_RECIPIENT_REQUIRED(HttpStatus.FORBIDDEN, "CHAT_ROOM_INVITATION_RECIPIENT_REQUIRED", "채팅방 초대 수신자만 처리할 수 있습니다."),
    CHAT_ROOM_INVITATION_INVITER_REQUIRED(HttpStatus.FORBIDDEN, "CHAT_ROOM_INVITATION_INVITER_REQUIRED", "채팅방 초대 발송자 또는 만료된 초대 수신자만 처리할 수 있습니다."),

    // 마인크래프트(Minecraft) 도메인 에러.
    // 마인크래프트 계정 등록, 화이트리스트 브리지 인증, 서버 상태 조회에서 발생하는 오류를 다룬다.
    MINECRAFT_ACCOUNT_LIMIT_EXCEEDED(
            HttpStatus.CONFLICT,
            "MINECRAFT_ACCOUNT_LIMIT_EXCEEDED",
            "등록 가능한 마인크래프트 계정 수를 초과했습니다."
    ),
    MINECRAFT_SELF_ACCOUNT_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "MINECRAFT_SELF_ACCOUNT_ALREADY_EXISTS",
            "본인 마인크래프트 계정은 1개만 등록할 수 있습니다."
    ),
    MINECRAFT_FRIEND_ACCOUNT_LIMIT_EXCEEDED(
            HttpStatus.CONFLICT,
            "MINECRAFT_FRIEND_ACCOUNT_LIMIT_EXCEEDED",
            "친구 마인크래프트 계정은 최대 3개까지만 등록할 수 있습니다."
    ),
    MINECRAFT_PARENT_ACCOUNT_REQUIRED(
            HttpStatus.CONFLICT,
            "MINECRAFT_PARENT_ACCOUNT_REQUIRED",
            "친구 계정을 등록하려면 본인 계정을 먼저 등록해야 합니다."
    ),
    MINECRAFT_PARENT_ACCOUNT_DELETE_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "MINECRAFT_PARENT_ACCOUNT_DELETE_NOT_ALLOWED",
            "친구 계정이 연결된 본인 계정은 삭제할 수 없습니다."
    ),
    MINECRAFT_ACCOUNT_DUPLICATED(
            HttpStatus.CONFLICT,
            "MINECRAFT_ACCOUNT_DUPLICATED",
            "이미 등록된 마인크래프트 계정입니다."
    ),
    MINECRAFT_SECRET_INVALID(
            HttpStatus.FORBIDDEN,
            "MINECRAFT_SECRET_INVALID",
            "마인크래프트 플러그인 shared secret이 올바르지 않습니다."
    ),
    MINECRAFT_SERVER_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "MINECRAFT_SERVER_UNAVAILABLE",
            "마인크래프트 서버 상태를 아직 확인할 수 없습니다."
    ),

    // 게시판(Board) 도메인 에러.
    // 게시글/댓글 존재 여부와 작성자 권한 검증에 사용되는 오류 코드다.
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "NOT_POST_AUTHOR", "게시글 작성자만 수정/삭제할 수 있습니다."),
    NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "NOT_COMMENT_AUTHOR", "댓글 작성자만 수정/삭제할 수 있습니다."),
    INVALID_POST_MODERATION_STATUS_TRANSITION(
            HttpStatus.CONFLICT,
            "INVALID_POST_MODERATION_STATUS_TRANSITION",
            "허용되지 않는 게시글 moderation 상태 전이입니다."
    ),
    INVALID_COMMENT_MODERATION_STATUS_TRANSITION(
            HttpStatus.CONFLICT,
            "INVALID_COMMENT_MODERATION_STATUS_TRANSITION",
            "허용되지 않는 댓글 moderation 상태 전이입니다."
    ),

    // 공지(Notice/AppNotice) 도메인 에러.
    // 공지, 앱 공지, 공지 댓글 등 공지성 콘텐츠 관리에서 발생하는 오류를 정의한다.
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다."),
    APP_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "APP_NOTICE_NOT_FOUND", "앱 공지를 찾을 수 없습니다."),
    LEGAL_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "LEGAL_DOCUMENT_NOT_FOUND", "법적 문서를 찾을 수 없습니다."),
    CAMPUS_BANNER_NOT_FOUND(HttpStatus.NOT_FOUND, "CAMPUS_BANNER_NOT_FOUND", "캠퍼스 홈 배너를 찾을 수 없습니다."),
    NOTICE_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_COMMENT_NOT_FOUND", "공지 댓글을 찾을 수 없습니다."),
    NOT_NOTICE_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "NOT_NOTICE_COMMENT_AUTHOR", "공지 댓글 작성자만 수정/삭제할 수 있습니다."),
    COMMENT_ALREADY_DELETED(HttpStatus.CONFLICT, "COMMENT_ALREADY_DELETED", "이미 삭제된 댓글입니다."),

    // 지원/문의/신고(Support) 도메인 에러.
    // 사용자 문의 처리와 신고 상태 관리에서 발생하는 비즈니스 규칙 위반을 표현한다.
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다."),
    INVALID_INQUIRY_STATUS_TRANSITION(HttpStatus.CONFLICT, "INVALID_INQUIRY_STATUS_TRANSITION", "허용되지 않는 문의 상태 전이입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "신고를 찾을 수 없습니다."),
    REPORT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "REPORT_ALREADY_SUBMITTED", "동일 대상에 대한 중복 신고입니다."),
    CANNOT_REPORT_YOURSELF(HttpStatus.BAD_REQUEST, "CANNOT_REPORT_YOURSELF", "자기 자신은 신고할 수 없습니다."),
    INVALID_REPORT_STATUS_TRANSITION(HttpStatus.CONFLICT, "INVALID_REPORT_STATUS_TRANSITION", "허용되지 않는 신고 상태 전이입니다."),

    // 학식(Cafeteria) 도메인 에러.
    // 주차별 학식 메뉴 등록과 조회에서 사용하는 오류 코드다.
    CAFETERIA_MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "CAFETERIA_MENU_NOT_FOUND", "학식 메뉴를 찾을 수 없습니다."),
    CAFETERIA_MENU_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "CAFETERIA_MENU_ENTRY_NOT_FOUND", "학식 메뉴 항목을 찾을 수 없습니다."),
    CAFETERIA_MENU_ALREADY_EXISTS(HttpStatus.CONFLICT, "CAFETERIA_MENU_ALREADY_EXISTS", "이미 등록된 주차의 학식 메뉴입니다."),

    // 공통 시스템 에러.
    // 도메인 구분 없이 예기치 못한 서버 내부 오류를 표현하는 최후 수단 코드다.
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
