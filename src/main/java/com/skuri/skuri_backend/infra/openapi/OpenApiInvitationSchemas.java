package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationBatchResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationMutationResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationReceivedResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationBatchResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationMutationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyInvitationReceivedResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class OpenApiInvitationSchemas {

    private OpenApiInvitationSchemas() {
    }

    public record PartyEligibleApiResponse(
            boolean success,
            PartyInvitationEligibleFriendsResponse data,
            @Schema(nullable = true) String message,
            @Schema(nullable = true) String errorCode,
            @Schema(nullable = true) LocalDateTime timestamp
    ) {
    }

    public record PartyBatchApiResponse(
            boolean success,
            PartyInvitationBatchResponse data,
            @Schema(nullable = true) String message,
            @Schema(nullable = true) String errorCode,
            @Schema(nullable = true) LocalDateTime timestamp
    ) {
    }

    public record PartyReceivedApiResponse(
            boolean success,
            List<PartyInvitationReceivedResponse> data,
            @Schema(nullable = true) String message,
            @Schema(nullable = true) String errorCode,
            @Schema(nullable = true) LocalDateTime timestamp
    ) {
    }

    public record PartyMutationApiResponse(
            boolean success,
            PartyInvitationMutationResponse data,
            @Schema(nullable = true) String message,
            @Schema(nullable = true) String errorCode,
            @Schema(nullable = true) LocalDateTime timestamp
    ) {
    }

    public record ChatEligibleApiResponse(
            boolean success,
            ChatRoomInvitationEligibleFriendsResponse data,
            @Schema(nullable = true) String message,
            @Schema(nullable = true) String errorCode,
            @Schema(nullable = true) LocalDateTime timestamp
    ) {
    }

    public record ChatBatchApiResponse(
            boolean success,
            ChatRoomInvitationBatchResponse data,
            @Schema(nullable = true) String message,
            @Schema(nullable = true) String errorCode,
            @Schema(nullable = true) LocalDateTime timestamp
    ) {
    }

    public record ChatReceivedApiResponse(
            boolean success,
            List<ChatRoomInvitationReceivedResponse> data,
            @Schema(nullable = true) String message,
            @Schema(nullable = true) String errorCode,
            @Schema(nullable = true) LocalDateTime timestamp
    ) {
    }

    public record ChatMutationApiResponse(
            boolean success,
            ChatRoomInvitationMutationResponse data,
            @Schema(nullable = true) String message,
            @Schema(nullable = true) String errorCode,
            @Schema(nullable = true) LocalDateTime timestamp
    ) {
    }
}
