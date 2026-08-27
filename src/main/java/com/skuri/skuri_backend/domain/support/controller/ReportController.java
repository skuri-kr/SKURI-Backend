package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.support.dto.request.CreateReportRequest;
import com.skuri.skuri_backend.domain.support.dto.response.ReportCreateResponse;
import com.skuri.skuri_backend.domain.support.service.ReportService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiBoardExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiMemberExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiNoticeExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiSupportExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiSupportSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiTaxiPartyExamples;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/reports")
@Tag(name = "Support Report API", description = "신고 접수 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "신고 접수", description = "게시글/게시글 댓글/공지 댓글/회원/채팅 메시지/일반 채팅방/택시파티 대상 신고를 접수합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSupportSchemas.ReportCreateApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiSupportExamples.SUCCESS_REPORT_CREATE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "자기 자신 신고",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "cannot_report_yourself", value = OpenApiSupportExamples.ERROR_CANNOT_REPORT_YOURSELF)
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
                    description = "이메일 도메인 제한",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "신고 대상 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND),
                                    @ExampleObject(name = "comment_not_found", value = OpenApiBoardExamples.ERROR_COMMENT_NOT_FOUND),
                                    @ExampleObject(name = "notice_comment_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_NOT_FOUND),
                                    @ExampleObject(name = "member_not_found", value = OpenApiMemberExamples.ERROR_MEMBER_NOT_FOUND),
                                    @ExampleObject(name = "chat_message_not_found", value = OpenApiChatExamples.ERROR_CHAT_MESSAGE_NOT_FOUND),
                                    @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND),
                                    @ExampleObject(name = "party_not_found", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "중복 신고",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "report_already_submitted", value = OpenApiSupportExamples.ERROR_REPORT_ALREADY_SUBMITTED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "신고 접수 요청",
            content = @Content(
                    schema = @Schema(implementation = CreateReportRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "post_report",
                                    value = """
                                            {
                                              "targetType": "POST",
                                              "targetId": "post_uuid",
                                              "category": "SPAM",
                                              "reason": "광고성 게시글입니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "notice_comment_report",
                                    value = """
                                            {
                                              "targetType": "NOTICE_COMMENT",
                                              "targetId": "notice_comment_uuid",
                                              "category": "ABUSE",
                                              "reason": "공지 댓글에 부적절한 표현이 있습니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "chat_message_report",
                                    value = """
                                            {
                                              "targetType": "CHAT_MESSAGE",
                                              "targetId": "message_uuid",
                                              "category": "SPAM",
                                              "reason": "광고성 메시지입니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "chat_room_report",
                                    value = """
                                            {
                                              "targetType": "CHAT_ROOM",
                                              "targetId": "chat_room_uuid",
                                              "category": "ABUSE",
                                              "reason": "부적절한 목적의 채팅방입니다."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "taxi_party_report",
                                    value = """
                                            {
                                              "targetType": "TAXI_PARTY",
                                              "targetId": "party_uuid",
                                              "category": "FRAUD",
                                              "reason": "운행/정산 방식이 부적절합니다."
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<ApiResponse<ReportCreateResponse>> createReport(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreateReportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        reportService.createReport(requireAuthenticatedMember(authenticatedMember).uid(), request)
                ));
    }
}
