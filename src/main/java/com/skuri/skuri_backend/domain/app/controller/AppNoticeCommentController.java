package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.notice.dto.request.UpdateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String commentId
    ) {
        appNoticeService.deleteComment(requireAuthenticatedMember(member).uid(), commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{commentId}/like")
    @Operation(summary = "앱 공지 댓글 좋아요")
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
    public ResponseEntity<ApiResponse<NoticeCommentLikeResponse>> unlike(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.unlikeComment(requireAuthenticatedMember(member).uid(), commentId)
        ));
    }
}
