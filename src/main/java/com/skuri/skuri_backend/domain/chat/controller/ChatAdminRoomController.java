package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.chat.dto.request.AdminCreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminChatRoomMemberResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminCreateChatRoomResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.service.ChatAdminService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/v1/admin/chat-rooms")
@Tag(name = "Chat Admin API", description = "관리자 공개 채팅방 조회/관리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class ChatAdminRoomController {

    private final ChatAdminService chatAdminService;

    @GetMapping
    @Operation(
            summary = "공개 채팅방 목록 조회(관리자)",
            description = "관리자는 현재 참여 여부와 관계없이 `isPublic=true` 이고 `type!=PARTY` 인 모든 공개 채팅방을 조회할 수 있습니다. 관리자 개인 상태 필드인 `joined`, `unreadCount`, `isMuted` 는 각각 `false`, `0`, `false` 고정값으로 내려갑니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatRoomSummaryListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_ADMIN_CHAT_ROOM_LIST)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> getPublicChatRooms(
            @Parameter(description = "공개 채팅방 타입 필터(PARTY 지정 시 결과는 비어 있음)", example = "GAME")
            @RequestParam(name = "type", required = false) ChatRoomType type
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatAdminService.getPublicChatRooms(type)));
    }

    @GetMapping("/{chatRoomId}")
    @Operation(
            summary = "공개 채팅방 상세 조회(관리자)",
            description = "관리자는 현재 참여 여부와 관계없이 공개 채팅방 상세를 조회할 수 있습니다. `PARTY` 타입과 비공개 채팅방은 이 API에서 제공하지 않으며 `CHAT_ROOM_NOT_FOUND` 를 반환합니다. 관리자 개인 상태 필드 `joined`, `unreadCount`, `isMuted`, `lastReadAt` 는 각각 `false`, `0`, `false`, `null` 로 고정됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatRoomDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_ADMIN_CHAT_ROOM_DETAIL)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공개 채팅방이 없거나 PARTY/비공개 채팅방임",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getPublicChatRoom(
            @Parameter(description = "채팅방 ID", example = "public:game:minecraft")
            @PathVariable String chatRoomId
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatAdminService.getPublicChatRoomDetail(chatRoomId)));
    }

    @GetMapping("/{chatRoomId}/members")
    @Operation(
            summary = "공개 채팅방 멤버 조회(관리자)",
            description = "관리자는 현재 참여 여부와 관계없이 공개 채팅방의 멤버십과 회원 표시 정보를 조회할 수 있습니다. `PARTY` 타입과 비공개 채팅방은 이 API에서 제공하지 않으며 `CHAT_ROOM_NOT_FOUND` 를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.AdminChatRoomMemberListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_ADMIN_CHAT_ROOM_MEMBERS)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공개 채팅방이 없거나 PARTY/비공개 채팅방임",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<AdminChatRoomMemberResponse>>> getPublicChatRoomMembers(
            @Parameter(description = "채팅방 ID", example = "public:game:minecraft")
            @PathVariable String chatRoomId
    ) {
        return ResponseEntity.ok(ApiResponse.success(chatAdminService.getPublicChatRoomMembers(chatRoomId)));
    }

    @GetMapping("/{chatRoomId}/messages")
    @Operation(
            summary = "공개 채팅방 메시지 조회(관리자)",
            description = "관리자는 현재 참여 여부와 관계없이 공개 채팅방 메시지를 createdAt DESC 커서 기준으로 조회할 수 있습니다. `PARTY` 타입과 비공개 채팅방은 이 API에서 제공하지 않으며 `CHAT_ROOM_NOT_FOUND` 를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatMessagePageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_ADMIN_CHAT_ROOM_MESSAGES_PAGE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "공개 채팅방이 없거나 PARTY/비공개 채팅방임",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "커서/페이지네이션 검증 실패",
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
    public ResponseEntity<ApiResponse<ChatMessagePageResponse>> getPublicChatRoomMessages(
            @Parameter(description = "채팅방 ID", example = "public:game:minecraft")
            @PathVariable String chatRoomId,
            @Parameter(description = "다음 페이지 시작 기준 createdAt", example = "2026-03-05T21:10:00")
            @RequestParam(name = "cursorCreatedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorCreatedAt,
            @Parameter(description = "다음 페이지 시작 기준 메시지 ID", example = "msg-system-4")
            @RequestParam(name = "cursorId", required = false) String cursorId,
            @Parameter(description = "페이지 크기(1~100)", example = "50")
            @RequestParam(name = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                chatAdminService.getPublicChatRoomMessages(chatRoomId, cursorCreatedAt, cursorId, size)
        ));
    }

    @PostMapping
    @Operation(summary = "공개 채팅방 생성(관리자)", description = "관리자가 공개 채팅방을 생성합니다. PARTY 타입은 생성할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.AdminCreateChatRoomApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_ADMIN_CHAT_ROOM_CREATE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "비즈니스 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_type_not_allowed", value = OpenApiChatExamples.ERROR_ADMIN_CHAT_ROOM_PARTY_TYPE_NOT_ALLOWED),
                                    @ExampleObject(name = "public_only", value = OpenApiChatExamples.ERROR_ADMIN_CHAT_ROOM_PUBLIC_ONLY)
                            }
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
            description = "공개 채팅방 생성 요청",
            content = @Content(
                    schema = @Schema(implementation = AdminCreateChatRoomRequest.class),
                    examples = @ExampleObject(
                            value = "{\"name\":\"성결대 전체 채팅방\",\"type\":\"UNIVERSITY\",\"description\":\"성결대학교 학생들의 소통 공간\",\"isPublic\":true}"
                    )
            )
    )
    @AdminAudit(
            action = AdminAuditActions.CHAT_ROOM_CREATED,
            targetType = AdminAuditTargetTypes.CHAT_ROOM,
            targetId = "#responseBody['data']['id']",
            after = "@adminAuditSnapshots.chatRoom(#responseBody['data']['id'])"
    )
    public ResponseEntity<ApiResponse<AdminCreateChatRoomResponse>> createPublicChatRoom(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody AdminCreateChatRoomRequest request
    ) {
        AdminCreateChatRoomResponse response = chatAdminService.createPublicChatRoom(
                requireAuthenticatedMember(authenticatedMember).uid(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{chatRoomId}")
    @Operation(summary = "공개 채팅방 삭제(관리자)", description = "관리자가 공개 채팅방을 삭제합니다. PARTY/비공개 채팅방은 삭제할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_ADMIN_CHAT_ROOM_DELETE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "비즈니스 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_type_not_allowed", value = OpenApiChatExamples.ERROR_ADMIN_CHAT_ROOM_DELETE_PARTY_NOT_ALLOWED),
                                    @ExampleObject(name = "public_only", value = OpenApiChatExamples.ERROR_ADMIN_CHAT_ROOM_DELETE_PUBLIC_ONLY)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            )
    })
    @AdminAudit(
            action = AdminAuditActions.CHAT_ROOM_DELETED,
            targetType = AdminAuditTargetTypes.CHAT_ROOM,
            targetId = "#chatRoomId",
            before = "@adminAuditSnapshots.chatRoom(#chatRoomId)"
    )
    public ResponseEntity<ApiResponse<Void>> deletePublicChatRoom(
            @PathVariable String chatRoomId
    ) {
        chatAdminService.deletePublicChatRoom(chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
