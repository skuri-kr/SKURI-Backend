package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.notice.dto.request.CreateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeLikeResponse;
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
@RequestMapping("/v1/app-notices")
@Tag(name = "App Notice API", description = "앱 공지 조회/댓글/좋아요 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AppNoticeInteractionController {

    private final AppNoticeService appNoticeService;

    @GetMapping("/{appNoticeId}/comments")
    @Operation(summary = "앱 공지 댓글 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentListApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_COMMENTS_LIST))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앱 공지 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND)))
    })
    public ResponseEntity<ApiResponse<List<NoticeCommentResponse>>> getComments(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String appNoticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.getComments(requireAuthenticatedMember(member).uid(), appNoticeId)
        ));
    }

    @PostMapping("/{appNoticeId}/comments")
    @Operation(summary = "앱 공지 댓글 작성")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeCommentApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_COMMENT_CREATE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앱 공지 또는 부모 댓글 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = {
                    @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND),
                    @ExampleObject(name = "parent_comment_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_NOT_FOUND)
            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "부모 댓글이 이미 삭제됨", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "comment_already_deleted", value = OpenApiAppExamples.ERROR_APP_NOTICE_COMMENT_ALREADY_DELETED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)))
    })
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> createComment(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String appNoticeId,
            @Valid @RequestBody CreateNoticeCommentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                appNoticeService.createComment(requireAuthenticatedMember(member).uid(), appNoticeId, request)
        ));
    }

    @PostMapping("/{appNoticeId}/like")
    @Operation(summary = "앱 공지 좋아요")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeLikeApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_LIKE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앱 공지 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND)))
    })
    public ResponseEntity<ApiResponse<NoticeLikeResponse>> like(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String appNoticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.likeNotice(requireAuthenticatedMember(member).uid(), appNoticeId)
        ));
    }

    @DeleteMapping("/{appNoticeId}/like")
    @Operation(summary = "앱 공지 좋아요 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 취소 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenApiNoticeSchemas.NoticeLikeApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_UNLIKE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앱 공지 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND)))
    })
    public ResponseEntity<ApiResponse<NoticeLikeResponse>> unlike(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String appNoticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.unlikeNotice(requireAuthenticatedMember(member).uid(), appNoticeId)
        ));
    }
}
