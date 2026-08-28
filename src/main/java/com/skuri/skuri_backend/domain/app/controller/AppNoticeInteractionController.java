package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.notice.dto.request.CreateNoticeCommentRequest;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeLikeResponse;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    public ResponseEntity<ApiResponse<NoticeLikeResponse>> unlike(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable String appNoticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.unlikeNotice(requireAuthenticatedMember(member).uid(), appNoticeId)
        ));
    }
}
