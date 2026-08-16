package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.chat.dto.request.CreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.request.UpdateChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.dto.request.UpdateChatRoomReadRequest;
import com.skuri.skuri_backend.domain.chat.dto.request.UpdateChatRoomSettingsRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatReadUpdateResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSettingsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiMemberExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/chat-rooms")
@Tag(name = "Chat API", description = "공개 채팅방 조회/생성/참여/설정/읽음 처리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ChatRoomController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "채팅방 목록 조회", description = "타입/참여 여부 조건으로 공개 채팅방과 내가 참여 중인 비공개 채팅방(PARTY 포함)을 함께 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatRoomSummaryListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_ROOM_LIST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> getChatRooms(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(name = "type", required = false) ChatRoomType type,
            @RequestParam(name = "joined", required = false) Boolean joined
    ) {
        List<ChatRoomSummaryResponse> response = chatService.getChatRooms(
                requireAuthenticatedMember(authenticatedMember).uid(),
                type,
                joined
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "커스텀 공개 채팅방 생성", description = "커스텀 공개 채팅방을 생성하고, 생성자는 즉시 joined=true 상태가 됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatRoomDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_ROOM_CREATE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "member_not_found", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "커스텀 공개 채팅방 생성 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateChatRoomRequest.class),
                    examples = @ExampleObject(
                            value = "{\"name\":\"시험기간 밤샘 메이트\",\"description\":\"기말고사 기간 같이 공부할 사람들 모여요.\"}"
                    )
            )
    )
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> createChatRoom(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreateChatRoomRequest request
    ) {
        ChatRoomDetailResponse response = chatService.createChatRoom(
                requireAuthenticatedMember(authenticatedMember).uid(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "채팅방 상세 조회", description = "공개 채팅방은 joined=false여도 상세 조회할 수 있고, 비공개 채팅방은 멤버만 조회할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatRoomDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_ROOM_DETAIL)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "비공개 채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_chat_room_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음 또는 접근 불가한 학과방",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getChatRoom(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId
    ) {
        ChatRoomDetailResponse response = chatService.getChatRoomDetail(requireAuthenticatedMember(authenticatedMember).uid(), chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "공개 채팅방 참여", description = "공개 채팅방에 즉시 참여합니다. 이미 참여 중이면 409를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "참여 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatRoomDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_ROOM_JOIN)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 방 타입",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "public_membership_only", value = OpenApiChatExamples.ERROR_CHAT_ROOM_PUBLIC_MEMBERSHIP_ONLY),
                                    @ExampleObject(name = "party_api_only", value = OpenApiChatExamples.ERROR_CHAT_ROOM_JOIN_PARTY_API_ONLY)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원 없음, 채팅방 없음 또는 접근 불가한 학과방",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "member_not_found", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND),
                                    @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 참여 중 또는 정원 초과",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "already_member", value = OpenApiChatExamples.ERROR_ALREADY_CHAT_ROOM_MEMBER),
                                    @ExampleObject(name = "chat_room_full", value = OpenApiChatExamples.ERROR_CHAT_ROOM_FULL)
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> joinChatRoom(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId
    ) {
        ChatRoomDetailResponse response = chatService.joinChatRoom(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}/members/me")
    @Operation(summary = "공개 채팅방 나가기", description = "참여 중인 공개 채팅방에서 나갑니다. 나간 뒤에도 공개방 상세 조회는 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "나가기 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatRoomDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_ROOM_LEAVE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 방 타입",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "public_membership_only", value = OpenApiChatExamples.ERROR_CHAT_ROOM_PUBLIC_MEMBERSHIP_ONLY),
                                    @ExampleObject(name = "party_api_only", value = OpenApiChatExamples.ERROR_CHAT_ROOM_LEAVE_PARTY_API_ONLY)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_chat_room_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음 또는 접근 불가한 학과방",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> leaveChatRoom(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId
    ) {
        ChatRoomDetailResponse response = chatService.leaveChatRoom(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/messages")
    @Operation(
            summary = "채팅 메시지 조회",
            description = "메시지를 createdAt DESC 기준으로 커서 기반 조회합니다. 같은 createdAt에서는 서버 내부 저장 순서 tie-breaker를 사용합니다. 공개방이라도 joined=false면 조회할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatMessagePageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_MESSAGES_PAGE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_chat_room_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음 또는 접근 불가한 학과방",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "cursor_pair_required", value = OpenApiChatExamples.ERROR_VALIDATION_CURSOR_PAIR),
                                    @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION)
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatMessagePageResponse>> getMessages(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId,
            @RequestParam(name = "cursorCreatedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorCreatedAt,
            @RequestParam(name = "cursorId", required = false) String cursorId,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        ChatMessagePageResponse response = chatService.getMessages(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId,
                cursorCreatedAt,
                cursorId,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/messages/{messageId}")
    @Operation(summary = "내 텍스트 메시지 수정", description = "작성자 본인만 전송 뒤 15분 이내의 TEXT 메시지를 수정할 수 있습니다. 마인크래프트 연동 메시지와 삭제된 메시지는 수정할 수 없습니다. 성공하면 REST 응답과 별도로 /topic/chat/{roomId}/events에 MESSAGE_UPDATED 이벤트가 발행됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatMessageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_MESSAGE_UPDATE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "채팅방 멤버가 아니거나 메시지 작성자가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "not_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER),
                            @ExampleObject(name = "not_author", value = OpenApiChatExamples.ERROR_NOT_CHAT_MESSAGE_AUTHOR)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 또는 메시지 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "message_not_found", value = OpenApiChatExamples.ERROR_CHAT_MESSAGE_NOT_FOUND))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "수정할 수 없는 메시지 상태",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "not_allowed", value = OpenApiChatExamples.ERROR_CHAT_MESSAGE_EDIT_NOT_ALLOWED),
                            @ExampleObject(name = "expired", value = OpenApiChatExamples.ERROR_CHAT_MESSAGE_EDIT_WINDOW_EXPIRED),
                            @ExampleObject(name = "already_deleted", value = OpenApiChatExamples.ERROR_CHAT_MESSAGE_ALREADY_DELETED)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION))
            )
    })
    public ResponseEntity<ApiResponse<ChatMessageResponse>> updateMessage(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId,
            @PathVariable String messageId,
            @Valid @RequestBody UpdateChatMessageRequest request
    ) {
        ChatMessageResponse response = chatService.updateMessage(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId,
                messageId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}/messages/{messageId}")
    @Operation(summary = "내 메시지 삭제", description = "작성자 본인이 TEXT·IMAGE 메시지와 파티 채팅방의 ACCOUNT 메시지를 tombstone으로 삭제합니다. 삭제된 자리와 커서는 유지하며, /topic/chat/{roomId}/events에 MESSAGE_DELETED 이벤트가 발행됩니다. 이미지 파일은 활성 참조·신고 증거가 없을 때에만 재시도 큐로 정리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공 또는 이미 삭제됨(멱등)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiChatSchemas.ChatMessageApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_MESSAGE_DELETE))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "채팅방 멤버가 아니거나 메시지 작성자가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "not_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER),
                            @ExampleObject(name = "not_author", value = OpenApiChatExamples.ERROR_NOT_CHAT_MESSAGE_AUTHOR)
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 또는 메시지 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "message_not_found", value = OpenApiChatExamples.ERROR_CHAT_MESSAGE_NOT_FOUND))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "삭제할 수 없는 메시지 타입 또는 마인크래프트 연동 메시지",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "not_allowed", value = OpenApiChatExamples.ERROR_CHAT_MESSAGE_DELETE_NOT_ALLOWED))
            )
    })
    public ResponseEntity<ApiResponse<ChatMessageResponse>> deleteMessage(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId,
            @PathVariable String messageId
    ) {
        ChatMessageResponse response = chatService.deleteMessage(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId,
                messageId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/read")
    @Operation(
            summary = "읽음 처리",
            description = "채팅 메시지 createdAt처럼 timezone 없는 LocalDateTime 문자열과 ISO 8601 Z/offset 문자열을 모두 받아 읽음 시각을 갱신합니다. timezone 없는 값은 Asia/Seoul 기준으로 해석하고, 응답은 기존처럼 UTC Instant로 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatReadUpdateApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_READ_UPDATE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_chat_room_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음 또는 접근 불가한 학과방",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "읽음 처리 요청. 프론트가 메시지 createdAt을 그대로 전달해도 되며, timezone 없는 값은 Asia/Seoul 기준으로 해석됩니다.",
            content = @Content(
                    schema = @Schema(implementation = UpdateChatRoomReadRequest.class),
                    examples = {
                            @ExampleObject(name = "local_datetime_microseconds", value = OpenApiChatExamples.REQUEST_CHAT_READ_UPDATE_LOCAL),
                            @ExampleObject(name = "utc_datetime", value = OpenApiChatExamples.REQUEST_CHAT_READ_UPDATE_UTC),
                            @ExampleObject(name = "offset_datetime", value = OpenApiChatExamples.REQUEST_CHAT_READ_UPDATE_OFFSET)
                    }
            )
    )
    public ResponseEntity<ApiResponse<ChatReadUpdateResponse>> markAsRead(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId,
            @Valid @RequestBody UpdateChatRoomReadRequest request
    ) {
        ChatReadUpdateResponse response = chatService.markAsRead(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId,
                request.lastReadAt()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/settings")
    @Operation(summary = "채팅방 설정 수정", description = "음소거 등 채팅방 사용자 설정을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatRoomSettingsApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_SETTINGS_UPDATE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_chat_room_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음 또는 접근 불가한 학과방",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatRoomSettingsResponse>> updateSettings(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId,
            @Valid @RequestBody UpdateChatRoomSettingsRequest request
    ) {
        ChatRoomSettingsResponse response = chatService.updateSettings(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId,
                request.muted()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
