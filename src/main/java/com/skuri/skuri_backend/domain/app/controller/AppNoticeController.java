package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiMemberExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/app-notices")
@Tag(name = "App Notice API", description = "앱 공지 공개 API")
public class AppNoticeController {

    private final AppNoticeService appNoticeService;

    @GetMapping
    @Operation(
            summary = "앱 공지 목록 조회",
            description = "점검 화면에서도 사용하는 완전 공개 API이며 Authorization 헤더를 처리하지 않습니다.",
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAppSchemas.AppNoticeListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICES)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<AppNoticeResponse>>> getAppNotices() {
        return ResponseEntity.ok(ApiResponse.success(appNoticeService.getPublishedNotices()));
    }

    @GetMapping("/{appNoticeId}")
    @Operation(
            summary = "앱 공지 상세 조회",
            description = "익명으로 호출할 수 있으며 유효한 Firebase ID Token을 보내면 사용자별 좋아요 상태를 반환합니다.",
            security = {
                    @SecurityRequirement(name = ""),
                    @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAppSchemas.AppNoticeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_DETAIL)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "선택 인증 토큰 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한 또는 탈퇴 회원 접근 제한",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                                    @ExampleObject(name = "withdrawn_member", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "앱 공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<AppNoticeResponse>> getAppNotice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "앱 공지 ID", example = "app_notice_uuid")
            @PathVariable String appNoticeId
    ) {
        String memberId = authenticatedMember == null ? null : authenticatedMember.uid();
        AppNoticeResponse response = memberId == null
                ? appNoticeService.getPublishedNotice(appNoticeId)
                : appNoticeService.getPublishedNotice(memberId, appNoticeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
