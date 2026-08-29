package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.notice.dto.request.UpdateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiNoticeSchemas;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/app-notice-comments")
@Tag(name = "App Notice API", description = "앱 공지 조회/댓글/좋아요 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AppNoticeCommentController {

    private final AppNoticeService appNoticeService;

    @PatchMapping("/{commentId}")
    @Operation(summary = "앱 공지 댓글 수정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_COMMENT_UPDATE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "not_app_notice_comment_author", value = OpenApiAppExamples.ERROR_NOT_APP_NOTICE_COMMENT_AUTHOR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앱 공지 또는 댓글 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND),
                    @ExampleObject(name = "app_notice_comment_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_NOT_FOUND)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 삭제된 댓글", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "comment_already_deleted", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_ALREADY_DELETED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)))
    })
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String commentId,
            @Valid @RequestBody UpdateNoticeCommentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.updateComment(requireAuthenticatedMember(member).uid(), commentId, request)
        ));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "앱 공지 댓글 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 아님", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "not_app_notice_comment_author", value = OpenApiAppExamples.ERROR_NOT_APP_NOTICE_COMMENT_AUTHOR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앱 공지 또는 댓글 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND),
                    @ExampleObject(name = "app_notice_comment_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_NOT_FOUND)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 삭제된 댓글", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "comment_already_deleted", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_ALREADY_DELETED)))
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String commentId
    ) {
        appNoticeService.deleteComment(requireAuthenticatedMember(member).uid(), commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{commentId}/like")
    @Operation(summary = "앱 공지 댓글 좋아요")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentLikeApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_COMMENT_LIKE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앱 공지 또는 댓글 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND),
                    @ExampleObject(name = "app_notice_comment_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_NOT_FOUND)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 삭제된 댓글", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "comment_already_deleted", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_ALREADY_DELETED)))
    })
    public ResponseEntity<ApiResponse<NoticeCommentLikeResponse>> like(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.likeComment(requireAuthenticatedMember(member).uid(), commentId)
        ));
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "앱 공지 댓글 좋아요 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 취소 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentLikeApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_COMMENT_UNLIKE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앱 공지 또는 댓글 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND),
                    @ExampleObject(name = "app_notice_comment_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_NOT_FOUND)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 삭제된 댓글", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "comment_already_deleted", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_ALREADY_DELETED)))
    })
    public ResponseEntity<ApiResponse<NoticeCommentLikeResponse>> unlike(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.unlikeComment(requireAuthenticatedMember(member).uid(), commentId)
        ));
    }
}
