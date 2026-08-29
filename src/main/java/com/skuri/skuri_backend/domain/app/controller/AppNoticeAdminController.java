package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.app.dto.request.CreateAppNoticeRequest;
import com.skuri.skuri_backend.domain.app.dto.request.UpdateAppNoticeRequest;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeCreateResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppSchemas;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/app-notices")
@Tag(name = "App Notice Admin API", description = "관리자 앱 공지 관리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class AppNoticeAdminController {

    private final AppNoticeService appNoticeService;

    @PostMapping
    @Operation(summary = "앱 공지 생성(관리자)", description = "관리자가 앱 공지를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAppSchemas.AppNoticeCreateApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_ADMIN_APP_NOTICE_CREATE)
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
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION),
                                    @ExampleObject(name = "action_label_requires_url", value = OpenApiAppExamples.ERROR_VALIDATION_APP_NOTICE_ACTION_LABEL_REQUIRES_URL),
                                    @ExampleObject(name = "action_url_requires_https", value = OpenApiAppExamples.ERROR_VALIDATION_APP_NOTICE_ACTION_URL_HTTPS)
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "앱 공지 생성 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateAppNoticeRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "title": "서버 점검 안내",
                                      "content": "2월 20일 새벽 2시~4시 서버 점검이 있습니다.",
                                      "category": "MAINTENANCE",
                                      "priority": "HIGH",
                                      "imageUrls": [],
                                      "actionUrl": null,
                                      "publishedAt": "2026-02-20T00:00:00"
                                    }
                                    """
                    )
            )
    )
    @AdminAudit(
            action = AdminAuditActions.APP_NOTICE_CREATED,
            targetType = AdminAuditTargetTypes.APP_NOTICE,
            targetId = "#responseBody['data']['id']",
            after = "@adminAuditSnapshots.appNotice(#responseBody['data']['id'])"
    )
    public ResponseEntity<ApiResponse<AppNoticeCreateResponse>> createAppNotice(
            @Valid @RequestBody CreateAppNoticeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(appNoticeService.createAppNotice(request)));
    }

    @PatchMapping("/{appNoticeId}")
    @Operation(summary = "앱 공지 부분 수정(관리자)", description = "관리자가 전달한 필드만 앱 공지에 반영합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAppSchemas.AppNoticeApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_ADMIN_APP_NOTICE_UPDATE)
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
                    description = "앱 공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION),
                                    @ExampleObject(name = "action_label_requires_url", value = OpenApiAppExamples.ERROR_VALIDATION_APP_NOTICE_ACTION_LABEL_REQUIRES_URL),
                                    @ExampleObject(name = "action_url_requires_https", value = OpenApiAppExamples.ERROR_VALIDATION_APP_NOTICE_ACTION_URL_HTTPS)
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "앱 공지 부분 수정 요청. 전달한 필드만 반영되며 누락된 필드는 유지됩니다.",
            content = @Content(
                    schema = @Schema(implementation = UpdateAppNoticeRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "title": "서버 점검 안내 (수정)",
                                      "content": "점검 시간이 변경되었습니다.",
                                      "priority": "HIGH"
                                    }
                                    """
                    )
            )
    )
    @AdminAudit(
            action = AdminAuditActions.APP_NOTICE_UPDATED,
            targetType = AdminAuditTargetTypes.APP_NOTICE,
            targetId = "#appNoticeId",
            before = "@adminAuditSnapshots.appNotice(#appNoticeId)",
            after = "@adminAuditSnapshots.appNotice(#appNoticeId)"
    )
    public ResponseEntity<ApiResponse<AppNoticeResponse>> updateAppNotice(
            @Parameter(description = "앱 공지 ID", example = "app_notice_uuid")
            @PathVariable String appNoticeId,
            @Valid @RequestBody UpdateAppNoticeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(appNoticeService.updateAppNotice(appNoticeId, request)));
    }

    @DeleteMapping("/{appNoticeId}")
    @Operation(summary = "앱 공지 삭제(관리자)", description = "관리자가 앱 공지를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL)
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
                    description = "앱 공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND)
                    )
            )
    })
    @AdminAudit(
            action = AdminAuditActions.APP_NOTICE_DELETED,
            targetType = AdminAuditTargetTypes.APP_NOTICE,
            targetId = "#appNoticeId",
            before = "@adminAuditSnapshots.appNotice(#appNoticeId)"
    )
    public ResponseEntity<ApiResponse<Void>> deleteAppNotice(
            @Parameter(description = "앱 공지 ID", example = "app_notice_uuid")
            @PathVariable String appNoticeId
    ) {
        appNoticeService.deleteAppNotice(appNoticeId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
