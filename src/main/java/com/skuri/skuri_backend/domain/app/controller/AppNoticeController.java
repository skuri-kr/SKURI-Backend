package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppSchemas;
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
            description = "로그인 전에도 호출 가능한 공개 API입니다.",
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
            description = "로그인 전에도 호출 가능한 공개 API입니다.",
            security = @SecurityRequirement(name = "")
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
