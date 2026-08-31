package com.skuri.skuri_backend.domain.contentblock.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.contentblock.dto.request.CreateContentBlockRequest;
import com.skuri.skuri_backend.domain.contentblock.dto.response.ContentBlockResponse;
import com.skuri.skuri_backend.domain.contentblock.service.ContentBlockService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiBoardExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiContentBlockExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiContentBlockSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiMemberExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiNoticeExamples;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/content-blocks")
@Tag(name = "Content Block API", description = "게시글·게시판 댓글·학교 공지 댓글·앱 공지 댓글 작성자 차단 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ContentBlockController {

    private final ContentBlockService contentBlockService;

    @PostMapping
    @Operation(
            summary = "콘텐츠 작성자 차단",
            description = "콘텐츠 ID를 서버 내부에서 작성자로 해석해 단방향 차단합니다. 작성자 회원 ID는 응답하지 않으며 중복 요청은 기존 차단을 반환합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateContentBlockRequest.class),
                    examples = @ExampleObject(value = OpenApiContentBlockExamples.REQUEST_CREATE)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "차단 생성 또는 멱등 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiContentBlockSchemas.ContentBlockApiResponse.class), examples = @ExampleObject(value = OpenApiContentBlockExamples.SUCCESS_CREATE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신 차단", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "self_block_not_allowed", value = OpenApiContentBlockExamples.ERROR_SELF_BLOCK_NOT_ALLOWED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 제한", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 또는 대상 콘텐츠 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "member_not_found", value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND),
                    @ExampleObject(name = "post_not_found", value = OpenApiBoardExamples.ERROR_POST_NOT_FOUND),
                    @ExampleObject(name = "comment_not_found", value = OpenApiBoardExamples.ERROR_COMMENT_NOT_FOUND),
                    @ExampleObject(name = "notice_comment_not_found", value = OpenApiNoticeExamples.ERROR_NOTICE_COMMENT_NOT_FOUND),
                    @ExampleObject(name = "app_notice_comment_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_NOT_FOUND)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_VALIDATION)))
    })
    public ResponseEntity<ApiResponse<ContentBlockResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreateContentBlockRequest request
    ) {
        ContentBlockResponse response = contentBlockService.create(
                requireAuthenticatedMember(authenticatedMember).uid(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "내 콘텐츠 차단 목록", description = "작성자 신원 없이 차단 해제용 blockId, 고정 라벨, 차단 시각만 최신순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiContentBlockSchemas.ContentBlockListApiResponse.class), examples = @ExampleObject(value = OpenApiContentBlockExamples.SUCCESS_LIST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 제한", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND)))
    })
    public ResponseEntity<ApiResponse<List<ContentBlockResponse>>> getMyBlocks(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                contentBlockService.getMyBlocks(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @DeleteMapping("/{blockId}")
    @Operation(summary = "콘텐츠 작성자 차단 해제", description = "내 차단이 없거나 다른 사용자의 blockId여도 신원 열거를 막기 위해 204로 멱등 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "차단 해제 또는 멱등 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 제한", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = OpenApiCommonExamples.ERROR_MEMBER_NOT_FOUND)))
    })
    public ResponseEntity<Void> unblock(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable String blockId
    ) {
        contentBlockService.unblock(requireAuthenticatedMember(authenticatedMember).uid(), blockId);
        return ResponseEntity.noContent().build();
    }
}
