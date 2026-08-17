package com.skuri.skuri_backend.domain.support.model;

import com.skuri.skuri_backend.domain.chat.entity.ChatAccountData;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageDirection;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "신고가 접수된 시점의 채팅 메시지 증거 스냅샷")
public record ChatMessageReportSnapshot(
        @Schema(description = "메시지 ID", example = "message_uuid")
        String messageId,
        @Schema(description = "채팅방 ID", example = "party:party-1")
        String chatRoomId,
        @Schema(description = "발신자 ID", example = "user_uuid")
        String senderId,
        @Schema(description = "발신자 이름", example = "스쿠리 유저", nullable = true)
        String senderName,
        @Schema(description = "신고 시점의 원래 메시지 타입", example = "IMAGE")
        ChatMessageType originalType,
        @Schema(description = "신고 시점의 텍스트 본문", nullable = true)
        String text,
        @Schema(description = "신고 시점의 이미지 URL", nullable = true)
        String imageUrl,
        @Schema(description = "신고 시점의 계좌 정보", nullable = true)
        ChatAccountData accountData,
        @Schema(description = "마인크래프트 메시지 방향", nullable = true)
        ChatMessageDirection direction,
        @Schema(description = "메시지 원본 source", nullable = true)
        String source,
        @Schema(description = "메시지 작성 시각", example = "2026-08-16T12:00:00")
        LocalDateTime createdAt,
        @Schema(description = "신고 전 마지막 수정 시각", nullable = true)
        LocalDateTime editedAt
) {
}
