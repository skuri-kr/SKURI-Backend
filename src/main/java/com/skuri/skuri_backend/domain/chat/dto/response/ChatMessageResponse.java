package com.skuri.skuri_backend.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageResponse(
        @Schema(description = "메시지 ID", example = "9f9efc3b-4d55-44e7-a86f-93d5101938ec")
        String id,
        @Schema(description = "채팅방 ID", example = "party:party-1")
        String chatRoomId,
        @Schema(description = "발신자 ID", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
        String senderId,
        @Schema(description = "발신자 닉네임", example = "스쿠리 유저")
        String senderName,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Schema(description = "발신자 프로필 사진 URL(members.photo_url, null 허용, linked_accounts fallback 없음)", example = "https://cdn.skuri.app/uploads/profiles/profile.jpg", nullable = true)
        String senderPhotoUrl,
        @Schema(description = "메시지 타입(TEXT/IMAGE/ACCOUNT는 클라이언트 전송, SYSTEM/ARRIVED/END는 서버 생성)", example = "TEXT")
        ChatMessageType type,
        @Schema(description = "텍스트 본문(TEXT/SYSTEM/END 등)", example = "안녕하세요!", nullable = true)
        String text,
        @Schema(description = "이미지 URL(IMAGE 타입)", example = "https://cdn.skuri.app/chat/2026/03/05/image-1.jpg", nullable = true)
        String imageUrl,
        @Schema(description = "계좌 정보(ACCOUNT 타입)", nullable = true)
        ChatAccountDataResponse accountData,
        @Schema(description = "도착 정보(ARRIVED 타입)", nullable = true)
        ChatArrivalDataResponse arrivalData,
        @Schema(description = "메시지 생성 시각", example = "2026-03-05T21:10:00")
        LocalDateTime createdAt,
        @Schema(description = "메시지 최신 변경 시각", example = "2026-03-05T21:12:00", nullable = true)
        LocalDateTime updatedAt,
        @Schema(description = "메시지 수정 시각(TEXT 수정 시)", example = "2026-03-05T21:12:00", nullable = true)
        LocalDateTime editedAt,
        @Schema(description = "메시지 삭제 시각(삭제 tombstone일 때)", example = "2026-03-05T21:13:00", nullable = true)
        LocalDateTime deletedAt,
        @JsonProperty("isDeleted")
        @Schema(description = "삭제된 메시지 여부", example = "false")
        boolean isDeleted
) {

    public ChatMessageResponse(
            String id,
            String chatRoomId,
            String senderId,
            String senderName,
            String senderPhotoUrl,
            ChatMessageType type,
            String text,
            String imageUrl,
            ChatAccountDataResponse accountData,
            ChatArrivalDataResponse arrivalData,
            LocalDateTime createdAt
    ) {
        this(
                id,
                chatRoomId,
                senderId,
                senderName,
                senderPhotoUrl,
                type,
                text,
                imageUrl,
                accountData,
                arrivalData,
                createdAt,
                null,
                null,
                null,
                false
        );
    }
}
