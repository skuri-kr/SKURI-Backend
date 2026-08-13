package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateInquiryStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminInquiryResponse;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.service.InquiryService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiSupportExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiSupportSchemas;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/inquiries")
@Tag(name = "Admin Support Inquiry API", description = "관리자 문의 운영 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class InquiryAdminController {

    private final InquiryService inquiryService;

    @GetMapping
    @Operation(summary = "문의 전체 목록 조회(관리자)", description = "문의 상태 필터와 페이지네이션으로 문의 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSupportSchemas.AdminInquiryPageApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiSupportExamples.SUCCESS_ADMIN_INQUIRIES_PAGE)
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
                    description = "잘못된 상태 필터",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "페이지네이션 파라미터 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "validation_error", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<AdminInquiryResponse>>> getInquiries(
            @Parameter(description = "문의 상태 필터", example = "PENDING")
            @RequestParam(name = "status", required = false) InquiryStatus status,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getAdminInquiries(status, page, size)));
    }

    @PatchMapping("/{inquiryId}/status")
    @Operation(summary = "문의 상태 처리(관리자)", description = "문의 상태와 사용자 공개 답변을 갱신합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSupportSchemas.AdminInquiryApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiSupportExamples.SUCCESS_ADMIN_INQUIRY_STATUS_UPDATE)
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
                    description = "문의 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "inquiry_not_found", value = OpenApiSupportExamples.ERROR_INQUIRY_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "허용되지 않는 상태 전이",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "invalid_inquiry_status_transition",
                                    value = OpenApiSupportExamples.ERROR_INVALID_INQUIRY_STATUS_TRANSITION
                            )
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
            description = "문의 상태 변경 요청",
            content = @Content(
                    schema = @Schema(implementation = UpdateInquiryStatusRequest.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "status": "RESOLVED",
                                      "memo": "재현 후 수정 배포 완료"
                                    }                                    
                                    """
                    )
            )
    )
    @AdminAudit(
            action = AdminAuditActions.INQUIRY_STATUS_UPDATED,
            targetType = AdminAuditTargetTypes.INQUIRY,
            targetId = "#inquiryId",
            before = "@adminAuditSnapshots.inquiry(#inquiryId)",
            after = "@adminAuditSnapshots.inquiry(#inquiryId)"
    )
    public ResponseEntity<ApiResponse<AdminInquiryResponse>> updateInquiryStatus(
            @Parameter(description = "문의 ID", example = "inquiry_uuid")
            @PathVariable String inquiryId,
            @Valid @RequestBody UpdateInquiryStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.updateInquiryStatus(inquiryId, request)));
    }
}
