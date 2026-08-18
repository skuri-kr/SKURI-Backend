package com.skuri.skuri_backend.domain.friend.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.friend.dto.request.CreateFriendRequestRequest;
import com.skuri.skuri_backend.domain.friend.dto.request.CreateMemberBlockRequest;
import com.skuri.skuri_backend.domain.friend.dto.request.UpdateFriendFavoriteRequest;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendBlockResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInboxCountsResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendRequestMutationResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendRequestPageResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSearchPageResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSummaryResponse;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService.FriendRequestDirection;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiFriendExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiFriendSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "Friend API", description = "친구 관계, 요청, 차단 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class FriendRelationshipController {

    private final FriendRelationshipService friendRelationshipService;
    private final FriendRelationshipQueryService friendRelationshipQueryService;

    @GetMapping("/v1/friends")
    @Operation(summary = "내 친구 목록 조회", description = "즐겨찾기 우선, 닉네임 가나다순으로 현재 친구를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendSummaryListApiResponse.class),
                            examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_LIST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<List<FriendSummaryResponse>>> getFriends(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendRelationshipQueryService.getFriends(memberId(authenticatedMember))));
    }

    @GetMapping("/v1/friends/{friendPublicId}")
    @Operation(summary = "친구 상세 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendSummaryApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_DETAIL))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음, 대상 없음 또는 친구 관계가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND),
                            @ExampleObject(name = "FRIENDSHIP_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIENDSHIP_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendSummaryResponse>> getFriend(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String friendPublicId
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendRelationshipQueryService.getFriend(memberId(authenticatedMember), friendPublicId)));
    }

    @DeleteMapping("/v1/friends/{friendPublicId}")
    @Operation(summary = "친구 끊기", description = "상호 친구 관계와 양방향 즐겨찾기를 제거합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "친구 끊기 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음, 대상 없음 또는 친구 관계가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND),
                            @ExampleObject(name = "FRIENDSHIP_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIENDSHIP_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<Void> removeFriendship(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String friendPublicId
    ) {
        friendRelationshipService.removeFriendship(memberId(authenticatedMember), friendPublicId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/v1/friends/{friendPublicId}/favorite")
    @Operation(summary = "친구 즐겨찾기 변경")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateFriendFavoriteRequest.class), examples = @ExampleObject(value = OpenApiFriendExamples.REQUEST_UPDATE_FAVORITE)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음, 대상 없음 또는 친구 관계가 아님",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND),
                            @ExampleObject(name = "FRIENDSHIP_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIENDSHIP_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<Void> updateFavorite(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String friendPublicId,
            @Valid @RequestBody UpdateFriendFavoriteRequest request
    ) {
        friendRelationshipService.setFavorite(memberId(authenticatedMember), friendPublicId, request.favorite());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/v1/friends/search")
    @Operation(summary = "닉네임으로 친구 검색", description = "검색 허용 회원만 반환하고 양방향 차단 대상은 숨깁니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendSearchPageApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_SEARCH))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "cursor가 유효하지 않음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_INVALID_REQUEST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "검색어 또는 size 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendSearchPageResponse>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam @NotBlank @Size(min = 2, max = 50) String query,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @Min(1) @Max(20) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendRelationshipQueryService.search(memberId(authenticatedMember), query, cursor, size)));
    }

    @GetMapping("/v1/friend-requests")
    @Operation(summary = "현재 친구 요청 목록", description = "받은 요청 또는 보낸 요청의 유효 PENDING 항목만 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendRequestPageApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_REQUEST_PAGE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "direction 또는 cursor가 유효하지 않음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_INVALID_REQUEST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "size 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendRequestPageResponse>> getRequests(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam FriendRequestDirection direction,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @Min(1) @Max(20) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendRelationshipQueryService.getRequests(memberId(authenticatedMember), direction, cursor, size)));
    }

    @PostMapping("/v1/friend-requests")
    @Operation(summary = "친구 요청 생성", description = "역방향 유효 PENDING 요청이 있으면 새 요청 없이 즉시 수락합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateFriendRequestRequest.class), examples = @ExampleObject(value = OpenApiFriendExamples.REQUEST_CREATE_FRIEND_REQUEST)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "요청 생성 또는 역방향 요청 자동 수락 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendRequestMutationApiResponse.class), examples = {
                            @ExampleObject(name = "PENDING", value = OpenApiFriendExamples.SUCCESS_FRIEND_REQUEST_PENDING),
                            @ExampleObject(name = "ACCEPTED", value = OpenApiFriendExamples.SUCCESS_FRIEND_REQUEST_ACCEPTED)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신에게 요청",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = "{\"success\":false,\"message\":\"자기 자신에게 친구 요청을 보낼 수 없습니다.\",\"errorCode\":\"FRIEND_SELF_REQUEST_NOT_ALLOWED\",\"timestamp\":\"2026-08-18T12:00:00\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음, 대상 없음 또는 차단 관계",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "기존 친구 또는 동일 방향 PENDING 요청",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "FRIEND_ALREADY_EXISTS", value = "{\"success\":false,\"message\":\"이미 친구입니다.\",\"errorCode\":\"FRIEND_ALREADY_EXISTS\",\"timestamp\":\"2026-08-18T12:00:00\"}"),
                            @ExampleObject(name = "FRIEND_REQUEST_ALREADY_PENDING", value = OpenApiFriendExamples.ERROR_FRIEND_REQUEST_ALREADY_PENDING)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendRequestMutationResponse>> createRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreateFriendRequestRequest request
    ) {
        String memberId = memberId(authenticatedMember);
        FriendRelationshipService.FriendRequestCreationResult result = friendRelationshipService.createRequest(memberId, request.friendPublicId());
        return ResponseEntity.ok(ApiResponse.success(new FriendRequestMutationResponse(
                result.accepted() ? "ACCEPTED" : "PENDING", result.requestId(), result.friend()
        )));
    }

    @PostMapping("/v1/friend-requests/{requestId}/accept")
    @Operation(summary = "친구 요청 수락", description = "이미 같은 friendship이 있으면 동일한 친구 요약을 반환하는 멱등 성공입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수락 성공 또는 멱등 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendRequestMutationApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_REQUEST_ACCEPTED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수신자만 처리 가능",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = "{\"success\":false,\"message\":\"친구 요청 수신자만 처리할 수 있습니다.\",\"errorCode\":\"FRIEND_REQUEST_RECIPIENT_REQUIRED\",\"timestamp\":\"2026-08-18T12:00:00\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음, 요청 없음 또는 차단 관계",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_REQUEST_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_REQUEST_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "현재 PENDING이 아닌 요청",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.ERROR_FRIEND_REQUEST_STATE_NOT_ALLOWED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendRequestMutationResponse>> acceptRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String requestId
    ) {
        FriendRelationshipService.FriendRequestAcceptResult result = friendRelationshipService
                .acceptRequest(memberId(authenticatedMember), requestId);
        return ResponseEntity.ok(ApiResponse.success(new FriendRequestMutationResponse(
                "ACCEPTED", requestId, result.friend()
        )));
    }

    @PostMapping("/v1/friend-requests/{requestId}/decline")
    @Operation(summary = "친구 요청 거절")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "거절 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수신자만 처리 가능",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = "{\"success\":false,\"message\":\"친구 요청 수신자만 처리할 수 있습니다.\",\"errorCode\":\"FRIEND_REQUEST_RECIPIENT_REQUIRED\",\"timestamp\":\"2026-08-18T12:00:00\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음, 요청 없음 또는 대상 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_REQUEST_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_REQUEST_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "현재 PENDING이 아닌 요청",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.ERROR_FRIEND_REQUEST_STATE_NOT_ALLOWED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<Void> declineRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String requestId
    ) {
        friendRelationshipService.declineRequest(memberId(authenticatedMember), requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/v1/friend-requests/{requestId}")
    @Operation(summary = "보낸 친구 요청 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "요청자만 취소 가능",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = "{\"success\":false,\"message\":\"친구 요청자만 취소할 수 있습니다.\",\"errorCode\":\"FRIEND_REQUEST_REQUESTER_REQUIRED\",\"timestamp\":\"2026-08-18T12:00:00\"}"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음, 요청 없음 또는 대상 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_REQUEST_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_REQUEST_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "현재 PENDING이 아닌 요청",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.ERROR_FRIEND_REQUEST_STATE_NOT_ALLOWED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<Void> cancelRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String requestId
    ) {
        friendRelationshipService.cancelRequest(memberId(authenticatedMember), requestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/v1/friends/blocks")
    @Operation(summary = "내 차단 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendBlockListApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_BLOCK_LIST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<List<FriendBlockResponse>>> getBlocks(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendRelationshipQueryService.getBlocks(memberId(authenticatedMember))));
    }

    @PostMapping("/v1/friends/blocks")
    @Operation(summary = "회원 차단", description = "이미 차단한 대상이면 같은 결과로 멱등 처리합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateMemberBlockRequest.class), examples = @ExampleObject(value = OpenApiFriendExamples.REQUEST_CREATE_FRIEND_REQUEST)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "차단 성공 또는 멱등 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신 차단",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.ERROR_FRIEND_SELF_BLOCK_NOT_ALLOWED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음 또는 대상 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<Void> blockMember(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreateMemberBlockRequest request
    ) {
        friendRelationshipService.blockMember(memberId(authenticatedMember), request.friendPublicId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/v1/friends/blocks/{friendPublicId}")
    @Operation(summary = "회원 차단 해제", description = "차단이 없더라도 같은 결과로 멱등 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "차단 해제 성공 또는 멱등 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음 또는 대상 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "MEMBER_NOT_FOUND", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                            @ExampleObject(name = "FRIEND_TARGET_NOT_FOUND", value = OpenApiFriendExamples.ERROR_FRIEND_TARGET_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<Void> unblockMember(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String friendPublicId
    ) {
        friendRelationshipService.unblockMember(memberId(authenticatedMember), friendPublicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/v1/friends/inbox-counts")
    @Operation(summary = "친구 허브 처리 필요 항목 수 조회", description = "현재 Core에서는 친구 요청 수만 계산하고 초대 수는 0입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendInboxCountsApiResponse.class), examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_INBOX_COUNTS))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가입한 활성 회원 없음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendInboxCountsResponse>> getInboxCounts(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendRelationshipQueryService.getInboxCounts(memberId(authenticatedMember))));
    }

    private String memberId(AuthenticatedMember authenticatedMember) {
        return requireAuthenticatedMember(authenticatedMember).uid();
    }
}
