package com.skuri.skuri_backend.domain.share.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.share.dto.request.CreateShareLinkRequest;
import com.skuri.skuri_backend.domain.share.dto.response.BoardSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.CafeteriaSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.NoticeSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.ShareLinkResolveResponse;
import com.skuri.skuri_backend.domain.share.dto.response.ShareLinkResponse;
import com.skuri.skuri_backend.domain.share.service.ShareLinkService;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiBoardExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiNoticeExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiShareExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiShareSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/share-links")
@Tag(name = "Share Link API", description = "짧은 공유 링크 발급, 앱 내부 ID 해석, 공개 미리보기 API")
public class ShareLinkController {

    private static final String CODE_PATTERN = "[1-9A-HJ-NP-Za-km-z]{8}";

    private final ShareLinkService shareLinkService;

    @PostMapping
    @Operation(summary = "짧은 공유 링크 발급", security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateShareLinkRequest.class),
                    examples = @ExampleObject(value = OpenApiShareExamples.REQUEST_CREATE)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "발급 성공",
                    content = @Content(schema = @Schema(implementation = OpenApiShareSchemas.ShareLinkApiResponse.class), examples = @ExampleObject(value = OpenApiShareExamples.SUCCESS_CREATE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "원본 콘텐츠 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class), examples = {
                            @ExampleObject(name = "NOTICE_NOT_FOUND", value = OpenApiNoticeExamples.ERROR_NOTICE_NOT_FOUND),
                            @ExampleObject(name = "POST_NOT_FOUND", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION)))
    })
    public ResponseEntity<ApiResponse<ShareLinkResponse>> create(@Valid @RequestBody CreateShareLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(shareLinkService.create(request)));
    }

    @GetMapping("/{resourceType}/{code}/resolve")
    @Operation(summary = "공유 코드를 앱 내부 콘텐츠 ID로 해석", security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "해석 성공",
                    content = @Content(schema = @Schema(implementation = OpenApiShareSchemas.ShareLinkResolveApiResponse.class), examples = @ExampleObject(value = OpenApiShareExamples.SUCCESS_RESOLVE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공유 링크 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiShareExamples.ERROR_NOT_FOUND)))
    })
    public ResponseEntity<ApiResponse<ShareLinkResolveResponse>> resolve(
            @PathVariable String resourceType,
            @PathVariable @Pattern(regexp = CODE_PATTERN) String code
    ) {
        return ResponseEntity.ok(ApiResponse.success(shareLinkService.resolve(resourceType, code)));
    }

    @GetMapping("/notice/{code}/preview")
    @Operation(summary = "학교 공지 공개 미리보기", security = {})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = OpenApiShareSchemas.NoticeSharePreviewApiResponse.class), examples = @ExampleObject(value = OpenApiShareExamples.SUCCESS_NOTICE_PREVIEW))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공유 링크 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiShareExamples.ERROR_NOT_FOUND)))
    })
    public ResponseEntity<ApiResponse<NoticeSharePreviewResponse>> getNoticePreview(
            @PathVariable @Pattern(regexp = CODE_PATTERN) String code
    ) {
        return ResponseEntity.ok(ApiResponse.success(shareLinkService.getNoticePreview(code)));
    }

    @GetMapping("/board/{code}/preview")
    @Operation(summary = "커뮤니티 게시물 공개 미리보기", security = {})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = OpenApiShareSchemas.BoardSharePreviewApiResponse.class), examples = @ExampleObject(value = OpenApiShareExamples.SUCCESS_BOARD_PREVIEW))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공유 링크 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiShareExamples.ERROR_NOT_FOUND)))
    })
    public ResponseEntity<ApiResponse<BoardSharePreviewResponse>> getBoardPreview(
            @PathVariable @Pattern(regexp = CODE_PATTERN) String code
    ) {
        return ResponseEntity.ok(ApiResponse.success(shareLinkService.getBoardPreview(code)));
    }

    @GetMapping("/cafeteria/preview")
    @Operation(summary = "이번 주 학식 공개 미리보기", security = {})
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = OpenApiShareSchemas.CafeteriaSharePreviewApiResponse.class), examples = @ExampleObject(value = OpenApiShareExamples.SUCCESS_CAFETERIA_PREVIEW)))
    public ResponseEntity<ApiResponse<CafeteriaSharePreviewResponse>> getCafeteriaPreview() {
        return ResponseEntity.ok(ApiResponse.success(shareLinkService.getCafeteriaPreview()));
    }
}
