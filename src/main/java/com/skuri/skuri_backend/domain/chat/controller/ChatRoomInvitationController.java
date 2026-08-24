package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.chat.dto.request.CreateChatRoomInvitationsRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationBatchResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationMutationResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationReceivedResponse;
import com.skuri.skuri_backend.domain.chat.service.ChatRoomInvitationService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiInvitationExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiInvitationSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
@Tag(name = "Chat Room Invitation API", description = "공개 채팅방 친구 초대 발송·조회·처리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ChatRoomInvitationController {

    private final ChatRoomInvitationService invitationService;

    @GetMapping("/chat-rooms/{chatRoomId}/invitations/eligible-friends")
    @Operation(summary = "공개 채팅방 초대 가능 친구 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.ChatEligibleApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.SUCCESS_CHAT_ELIGIBLE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "공개 non-PARTY 방이 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_INVALID_ROOM))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "방 참가자가 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방 또는 회원 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "CHAT_ROOM_NOT_FOUND", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND),
                    @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "정원 또는 회원 프로필 조건 불충족", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "CHAT_ROOM_FULL", value = OpenApiChatExamples.ERROR_CHAT_ROOM_FULL),
                    @ExampleObject(name = "MEMBER_PROFILE_INCOMPLETE", value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE)
            }))
    })
    public ResponseEntity<ApiResponse<ChatRoomInvitationEligibleFriendsResponse>> getEligibleFriends(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String chatRoomId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.getEligibleFriends(memberId(authenticatedMember), chatRoomId)
        ));
    }

    @PostMapping("/chat-rooms/{chatRoomId}/invitations")
    @Operation(summary = "공개 채팅방 친구 초대 발송", description = "요청 순서를 유지한 수신자별 부분 성공 결과를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 결과", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.ChatBatchApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.SUCCESS_CHAT_BATCH))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "공개 non-PARTY 방이 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_INVALID_ROOM))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "방 참가자가 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방 또는 회원 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "CHAT_ROOM_NOT_FOUND", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND),
                    @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "회원 프로필 조건 불충족", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "MEMBER_PROFILE_INCOMPLETE", value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "batch 입력 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION)))
    })
    public ResponseEntity<ApiResponse<ChatRoomInvitationBatchResponse>> send(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String chatRoomId,
            @Valid @RequestBody CreateChatRoomInvitationsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.send(memberId(authenticatedMember), chatRoomId, request.friendPublicIds())
        ));
    }

    @GetMapping("/chat-room-invitations/received")
    @Operation(summary = "받은 공개 채팅방 초대 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.ChatReceivedApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.SUCCESS_CHAT_RECEIVED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "회원가입 프로필 미완료", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_PROFILE_INCOMPLETE)))
    })
    public ResponseEntity<ApiResponse<List<ChatRoomInvitationReceivedResponse>>> getReceived(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(invitationService.getReceived(memberId(authenticatedMember))));
    }

    @PostMapping("/chat-room-invitations/{invitationId}/accept")
    @Operation(summary = "공개 채팅방 초대 수락")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수락 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.ChatMutationApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.SUCCESS_CHAT_MUTATION))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수신자가 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "CHAT_ROOM_INVITATION_RECIPIENT_REQUIRED", value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_RECIPIENT_REQUIRED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "초대 처리 불가", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_STATE)))
    })
    public ResponseEntity<ApiResponse<ChatRoomInvitationMutationResponse>> accept(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String invitationId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.accept(memberId(authenticatedMember), invitationId)
        ));
    }

    @PostMapping("/chat-room-invitations/{invitationId}/decline")
    @Operation(summary = "공개 채팅방 초대 거절")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거절 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiInvitationSchemas.ChatMutationApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.SUCCESS_CHAT_DECLINE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수신자가 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "CHAT_ROOM_INVITATION_RECIPIENT_REQUIRED", value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_RECIPIENT_REQUIRED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "초대 처리 불가", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_STATE)))
    })
    public ResponseEntity<ApiResponse<ChatRoomInvitationMutationResponse>> decline(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String invitationId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.decline(memberId(authenticatedMember), invitationId)
        ));
    }

    @DeleteMapping("/chat-room-invitations/{invitationId}")
    @Operation(summary = "공개 채팅방 초대 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "발송자가 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "CHAT_ROOM_INVITATION_INVITER_REQUIRED", value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_INVITER_REQUIRED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "초대 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "초대 처리 불가", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiInvitationExamples.ERROR_CHAT_INVITATION_STATE)))
    })
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String invitationId
    ) {
        invitationService.cancel(memberId(authenticatedMember), invitationId);
        return ResponseEntity.noContent().build();
    }

    private String memberId(AuthenticatedMember authenticatedMember) {
        return requireAuthenticatedMember(authenticatedMember).uid();
    }
}
