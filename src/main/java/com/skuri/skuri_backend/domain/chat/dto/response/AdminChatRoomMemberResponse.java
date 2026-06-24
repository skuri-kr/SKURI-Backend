package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "관리자 채팅방 멤버 응답")
public record AdminChatRoomMemberResponse(
        @Schema(description = "회원 ID(Firebase UID)", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
        String memberId,
        @Schema(description = "성결대 이메일", example = "user@sungkyul.ac.kr", nullable = true)
        String email,
        @Schema(description = "앱 내 닉네임", example = "스쿠리 유저", nullable = true)
        String nickname,
        @Schema(description = "이름 컬럼에 사용하는 실명(members.realname)", example = "홍길동", nullable = true)
        String realname,
        @Schema(description = "학번", example = "2023112233", nullable = true)
        String studentId,
        @Schema(description = "학과", example = "컴퓨터공학과", nullable = true)
        String department,
        @Schema(
                description = "앱 내 프로필 이미지 URL",
                example = "https://cdn.skuri.app/uploads/profiles/dw9rPtuticbjnaYPkeiF3RGPpqk1/2026/04/06/photo.jpg",
                nullable = true
        )
        String photoUrl,
        @Schema(description = "채팅방 참여 시각(ISO 8601 UTC)", example = "2026-03-05T12:00:00Z")
        Instant joinedAt,
        @Schema(description = "마지막 읽음 시각(ISO 8601 UTC)", example = "2026-03-05T12:10:00Z", nullable = true)
        Instant lastReadAt,
        @Schema(description = "채팅방 음소거 여부", example = "false")
        boolean muted,
        @Schema(description = "회원 상태", example = "ACTIVE", nullable = true)
        MemberStatus status
) {
}
