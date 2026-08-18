package com.skuri.skuri_backend.domain.friend.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.friend.dto.request.FriendCodePreviewRequest;
import com.skuri.skuri_backend.domain.friend.dto.request.UpdateFriendPrivacyRequest;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodePreviewResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodeResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendPrivacyResponse;
import com.skuri.skuri_backend.domain.friend.service.FriendCodeService;
import com.skuri.skuri_backend.domain.friend.service.FriendPrivacyService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@Tag(name = "Friend API", description = "친구 코드와 친구 검색 공개 설정 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class FriendFoundationController {

    private final FriendCodeService friendCodeService;
    private final FriendPrivacyService friendPrivacyService;

    @GetMapping("/v1/friends/me/code")
    @Operation(summary = "내 친구 코드 조회", description = "현재 활성 친구 코드를 조회합니다. 과도기 누락 프로필은 안전하게 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendCodeApiResponse.class),
                            examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_MY_FRIEND_CODE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendCodeResponse>> getMyCode(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                friendCodeService.getMyCode(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @PostMapping("/v1/friends/me/code/regenerate")
    @Operation(summary = "내 친구 코드 재발급", description = "24시간에 한 번만 재발급할 수 있습니다. 제한 중이면 429와 Retry-After 헤더를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendCodeApiResponse.class),
                            examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_REGENERATED_FRIEND_CODE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "재발급 제한",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = OpenApiFriendExamples.ERROR_FRIEND_CODE_REGENERATION_COOLDOWN))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendCodeResponse>> regenerateMyCode(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                friendCodeService.regenerateMyCode(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @PostMapping("/v1/friend-codes/preview")
    @Operation(summary = "친구 코드 미리보기", description = "코드 또는 QR에서 해석한 코드를 부작용 없이 확인합니다. 친구 요청은 생성하지 않습니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FriendCodePreviewRequest.class),
                    examples = @ExampleObject(value = OpenApiFriendExamples.REQUEST_FRIEND_CODE_PREVIEW)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "미리보기 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendCodePreviewApiResponse.class),
                            examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_CODE_PREVIEW))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신의 코드",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = OpenApiFriendExamples.ERROR_FRIEND_SELF_NOT_ALLOWED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "코드 없음 또는 폐기됨",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = OpenApiFriendExamples.ERROR_FRIEND_CODE_NOT_FOUND))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION)))
    })
    public ResponseEntity<ApiResponse<FriendCodePreviewResponse>> previewCode(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody FriendCodePreviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                friendCodeService.preview(requireAuthenticatedMember(authenticatedMember).uid(), request.friendCode())
        ));
    }

    @GetMapping("/v1/friends/me/privacy")
    @Operation(summary = "내 친구 검색 공개 설정 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendPrivacyApiResponse.class),
                            examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_PRIVACY))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)))
    })
    public ResponseEntity<ApiResponse<FriendPrivacyResponse>> getMyPrivacy(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                friendPrivacyService.getMyPrivacy(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @PatchMapping("/v1/friends/me/privacy")
    @Operation(summary = "내 친구 검색 공개 설정 변경")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateFriendPrivacyRequest.class),
                    examples = @ExampleObject(value = OpenApiFriendExamples.REQUEST_UPDATE_PRIVACY)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiFriendSchemas.FriendPrivacyApiResponse.class),
                            examples = @ExampleObject(value = OpenApiFriendExamples.SUCCESS_FRIEND_PRIVACY))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION)))
    })
    public ResponseEntity<ApiResponse<FriendPrivacyResponse>> updateMyPrivacy(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody UpdateFriendPrivacyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                friendPrivacyService.updateMyPrivacy(
                        requireAuthenticatedMember(authenticatedMember).uid(),
                        request.nicknameSearchable()
                )
        ));
    }
}
