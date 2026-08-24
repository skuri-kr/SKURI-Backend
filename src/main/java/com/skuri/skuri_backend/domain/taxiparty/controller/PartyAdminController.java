package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.CreateAdminPartySystemMessageRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.UpdateAdminPartyStatusRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyJoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyAdminService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiTaxiPartyExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiTaxiPartySchemas;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/parties")
@Tag(name = "TaxiParty Admin API", description = "관리자 택시 파티 운영 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class PartyAdminController {

    private final TaxiPartyAdminService taxiPartyAdminService;

    @GetMapping
    @Operation(
            summary = "파티 목록 조회(관리자)",
            description = "관리자용 파티 목록을 검색/필터/페이지네이션으로 조회합니다. 기본 정렬은 departureTime DESC입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.AdminPartySummaryPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_ADMIN_PARTY_LIST_PAGE)
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
                    description = "잘못된 enum 필터 값",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "페이지네이션 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<AdminPartySummaryResponse>>> getAdminParties(
            @Parameter(description = "파티 상태 필터", example = "OPEN")
            @RequestParam(name = "status", required = false) PartyStatus status,
            @Parameter(description = "출발일 필터(yyyy-MM-dd)", example = "2026-03-29")
            @RequestParam(name = "departureDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate departureDate,
            @Parameter(description = "출발지/목적지/leader uid/leader nickname 검색어", example = "안양역")
            @RequestParam(name = "query", required = false) String query,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                taxiPartyAdminService.getAdminParties(status, departureDate, query, page, size)
        ));
    }

    @GetMapping("/{partyId}")
    @Operation(summary = "파티 상세 조회(관리자)", description = "관리자용 파티 상세와 운영 메타데이터를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.AdminPartyDetailApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_ADMIN_PARTY_DETAIL)
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
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "party_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<AdminPartyDetailResponse>> getAdminParty(
            @Parameter(description = "파티 ID", example = "party-20260304-001")
            @PathVariable String partyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(taxiPartyAdminService.getAdminParty(partyId)));
    }

    @GetMapping("/{partyId}/messages")
    @Operation(
            summary = "파티 채팅 메시지 조회(관리자)",
            description = "관리자는 현재 파티 멤버가 아니어도 `party:{partyId}` 채팅방 메시지를 createdAt DESC 커서 기준으로 조회할 수 있습니다. 파티 존재 여부는 TaxiParty를 기준으로 검증하고, 파티는 있지만 채팅방이 없으면 `CHAT_ROOM_NOT_FOUND` 를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.ChatMessagePageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_ADMIN_PARTY_MESSAGES_PAGE)
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
                    description = "파티 또는 채팅방 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND),
                                    @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                            }
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
    public ResponseEntity<ApiResponse<ChatMessagePageResponse>> getAdminPartyMessages(
            @Parameter(description = "파티 ID", example = "party-20260304-001")
            @PathVariable String partyId,
            @Parameter(description = "다음 페이지 시작 기준 createdAt", example = "2026-03-05T21:10:00")
            @RequestParam(name = "cursorCreatedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorCreatedAt,
            @Parameter(description = "다음 페이지 시작 기준 메시지 ID", example = "msg-account-1")
            @RequestParam(name = "cursorId", required = false) String cursorId,
            @Parameter(description = "페이지 크기(1~100)", example = "50")
            @RequestParam(name = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                taxiPartyAdminService.getAdminPartyMessages(partyId, cursorCreatedAt, cursorId, size)
        ));
    }

    @DeleteMapping("/{partyId}/members/{memberId}")
    @Operation(
            summary = "파티 멤버 제거(관리자)",
            description = "관리자가 일반 참여자를 파티에서 제거합니다. 리더 제거와 ARRIVED/ENDED 상태 제거는 허용하지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "제거 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.VoidApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_ADMIN_PARTY_MEMBER_REMOVAL)
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
                    description = "파티 또는 멤버 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND),
                                    @ExampleObject(name = "member_not_found", value = OpenApiTaxiPartyExamples.ERROR_MEMBER_NOT_FOUND),
                                    @ExampleObject(name = "party_member_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_MEMBER_NOT_FOUND)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "리더 제거 불가 또는 상태상 제거 불가",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "leader_removal_not_allowed", value = OpenApiTaxiPartyExamples.ERROR_PARTY_LEADER_REMOVAL_NOT_ALLOWED),
                                    @ExampleObject(name = "cannot_kick_in_arrived", value = OpenApiTaxiPartyExamples.ERROR_CANNOT_KICK_IN_ARRIVED),
                                    @ExampleObject(name = "party_ended", value = OpenApiTaxiPartyExamples.ERROR_PARTY_ENDED)
                            }
                    )
            )
    })
    @AdminAudit(
            action = AdminAuditActions.PARTY_MEMBER_REMOVED,
            targetType = AdminAuditTargetTypes.PARTY_MEMBER,
            targetId = "#memberId",
            before = "@adminAuditSnapshots.partyMember(#partyId, #memberId)",
            after = "@adminAuditSnapshots.partyMember(#partyId, #memberId)"
    )
    public ResponseEntity<ApiResponse<Void>> removePartyMember(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "파티 ID", example = "party-20260304-001")
            @PathVariable String partyId,
            @Parameter(description = "제거할 멤버 ID", example = "member-2")
            @PathVariable String memberId
    ) {
        taxiPartyAdminService.removePartyMember(requireAuthenticatedMember(authenticatedMember).uid(), partyId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{partyId}/messages/system")
    @Operation(
            summary = "파티 시스템 메시지 전송(관리자)",
            description = "관리자가 파티 채팅방에 운영용 시스템 메시지를 전송합니다. 메시지는 관리자 시스템 메시지로 기록됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "전송 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.ChatMessageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_ADMIN_PARTY_SYSTEM_MESSAGE)
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
                    description = "파티 또는 채팅방 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND),
                                    @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "메시지 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "message_blank", value = OpenApiTaxiPartyExamples.ERROR_VALIDATION_ADMIN_PARTY_SYSTEM_MESSAGE_BLANK),
                                    @ExampleObject(name = "message_too_long", value = OpenApiTaxiPartyExamples.ERROR_VALIDATION_ADMIN_PARTY_SYSTEM_MESSAGE_TOO_LONG)
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "관리자 시스템 메시지 전송 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateAdminPartySystemMessageRequest.class),
                    examples = @ExampleObject(
                            name = "default",
                            value = """
                                    {
                                      "message": "관리자 안내 메시지"
                                    }
                                    """
                    )
            )
    )
    @AdminAudit(
            action = AdminAuditActions.PARTY_SYSTEM_MESSAGE_CREATED,
            targetType = AdminAuditTargetTypes.CHAT_MESSAGE,
            targetId = "#responseBody['data']['id']",
            after = "@adminAuditSnapshots.chatMessage(#responseBody['data']['id'])"
    )
    public ResponseEntity<ApiResponse<ChatMessageResponse>> createPartySystemMessage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "파티 ID", example = "party-20260304-001")
            @PathVariable String partyId,
            @Valid @RequestBody CreateAdminPartySystemMessageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                taxiPartyAdminService.createPartySystemMessage(
                        requireAuthenticatedMember(authenticatedMember).uid(),
                        partyId,
                        request.message()
                )
        ));
    }

    @GetMapping("/{partyId}/join-requests")
    @Operation(
            summary = "파티 대기 동승 요청 조회(관리자)",
            description = "관리자가 특정 파티의 현재 PENDING join request 목록을 최신 요청순으로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.AdminPartyJoinRequestListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.SUCCESS_ADMIN_PARTY_JOIN_REQUEST_LIST)
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
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "party_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<AdminPartyJoinRequestResponse>>> getAdminPartyJoinRequests(
            @Parameter(description = "파티 ID", example = "party-20260304-001")
            @PathVariable String partyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                taxiPartyAdminService.getPartyJoinRequests(partyId)
        ));
    }

    @PatchMapping("/{partyId}/status")
    @Operation(
            summary = "파티 상태 변경(관리자)",
            description = "관리자가 기존 파티 상태 머신의 허용 전이만 재사용해 파티 상태를 강제 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiTaxiPartySchemas.PartyStatusApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "close", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_CLOSED),
                                    @ExampleObject(name = "reopen", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_OPEN),
                                    @ExampleObject(name = "cancel", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_ENDED_CANCELLED),
                                    @ExampleObject(name = "end", value = OpenApiTaxiPartyExamples.SUCCESS_PARTY_STATUS_ENDED_FORCE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "party_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않은 상태 전이/동시성 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "close_only", value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_CLOSE_ONLY),
                                    @ExampleObject(name = "reopen_only", value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_REOPEN_ONLY),
                                    @ExampleObject(name = "end_only", value = OpenApiTaxiPartyExamples.ERROR_INVALID_PARTY_STATE_TRANSITION_FORCE_END_ONLY),
                                    @ExampleObject(name = "party_not_cancelable", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_CANCELABLE),
                                    @ExampleObject(name = "party_ended", value = OpenApiTaxiPartyExamples.ERROR_PARTY_ENDED),
                                    @ExampleObject(name = "party_concurrent_modification", value = OpenApiTaxiPartyExamples.ERROR_PARTY_CONCURRENT_MODIFICATION)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 enum 액션 값",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "관리자 파티 상태 변경 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateAdminPartyStatusRequest.class),
                    examples = {
                            @ExampleObject(name = "close", value = "{\"action\":\"CLOSE\"}"),
                            @ExampleObject(name = "reopen", value = "{\"action\":\"REOPEN\"}"),
                            @ExampleObject(name = "cancel", value = "{\"action\":\"CANCEL\"}"),
                            @ExampleObject(name = "end", value = "{\"action\":\"END\"}")
                    }
            )
    )
    @AdminAudit(
            action = AdminAuditActions.PARTY_STATUS_UPDATED,
            targetType = AdminAuditTargetTypes.PARTY,
            targetId = "#partyId",
            before = "@adminAuditSnapshots.partyStatus(#partyId)",
            after = "@adminAuditSnapshots.partyStatus(#partyId)"
    )
    public ResponseEntity<ApiResponse<PartyStatusResponse>> updatePartyStatus(
            @Parameter(description = "파티 ID", example = "party-20260304-001")
            @PathVariable String partyId,
            @Valid @RequestBody UpdateAdminPartyStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                taxiPartyAdminService.updatePartyStatus(partyId, request.action())
        ));
    }
}
