package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeLikeResponse;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static com.skuri.skuri_backend.domain.contentblock.compatibility.ContentBlockV20xHttpCompatibilityFixture.assertAppNoticeCommentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AppNoticeInteractionController.class, AppNoticeCommentController.class})
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class AppNoticeInteractionControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppNoticeService appNoticeService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void 댓글목록_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/app-notices/app-notice-1/comments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(appNoticeService);
    }

    @Test
    void 댓글목록_유효한토큰_200() throws Exception {
        mockValidToken();
        when(appNoticeService.getComments("firebase-uid", "app-notice-1"))
                .thenReturn(java.util.List.of(commentResponse()));

        mockMvc.perform(get("/v1/app-notices/app-notice-1/comments")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("app-comment-1"));
    }

    @Test
    void 댓글목록_차단placeholder는실제HTTP에서_2_0_1댓글계약으로해석가능하다() throws Exception {
        mockValidToken();
        when(appNoticeService.getComments("firebase-uid", "app-notice-1"))
                .thenReturn(java.util.List.of(blockedCommentResponse()));

        MvcResult mvcResult = mockMvc.perform(
                        get("/v1/app-notices/app-notice-1/comments")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andReturn();

        assertAppNoticeCommentResponse(mvcResult);
    }

    @Test
    void 댓글목록_앱공지없음_404() throws Exception {
        mockValidToken();
        when(appNoticeService.getComments("firebase-uid", "app-notice-1"))
                .thenThrow(new BusinessException(ErrorCode.APP_NOTICE_NOT_FOUND));

        mockMvc.perform(get("/v1/app-notices/app-notice-1/comments")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APP_NOTICE_NOT_FOUND"));
    }

    @Test
    void 댓글작성_정상요청_201() throws Exception {
        mockValidToken();
        when(appNoticeService.createComment(eq("firebase-uid"), eq("app-notice-1"), any()))
                .thenReturn(commentResponse());

        mockMvc.perform(post("/v1/app-notices/app-notice-1/comments")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"content":"새 댓글","isAnonymous":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("app-comment-1"));
    }

    @Test
    void 댓글작성_토큰없음_401() throws Exception {
        mockMvc.perform(post("/v1/app-notices/app-notice-1/comments")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"content":"새 댓글","isAnonymous":true}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(appNoticeService);
    }

    @Test
    void 댓글작성_요청검증실패_422() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/v1/app-notices/app-notice-1/comments")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"content":"","isAnonymous":true}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void 앱공지좋아요_정상요청_200() throws Exception {
        mockValidToken();
        when(appNoticeService.likeNotice("firebase-uid", "app-notice-1"))
                .thenReturn(new NoticeLikeResponse(true, 8));

        mockMvc.perform(post("/v1/app-notices/app-notice-1/like")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(8));
    }

    @Test
    void 앱공지좋아요_토큰없음_401() throws Exception {
        mockMvc.perform(post("/v1/app-notices/app-notice-1/like"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(appNoticeService);
    }

    @Test
    void 앱공지좋아요_앱공지없음_404() throws Exception {
        mockValidToken();
        when(appNoticeService.likeNotice("firebase-uid", "app-notice-1"))
                .thenThrow(new BusinessException(ErrorCode.APP_NOTICE_NOT_FOUND));

        mockMvc.perform(post("/v1/app-notices/app-notice-1/like")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APP_NOTICE_NOT_FOUND"));
    }

    @Test
    void 앱공지좋아요취소_정상요청_200() throws Exception {
        mockValidToken();
        when(appNoticeService.unlikeNotice("firebase-uid", "app-notice-1"))
                .thenReturn(new NoticeLikeResponse(false, 7));

        mockMvc.perform(delete("/v1/app-notices/app-notice-1/like")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(7));
    }

    @Test
    void 앱공지좋아요취소_토큰없음_401() throws Exception {
        mockMvc.perform(delete("/v1/app-notices/app-notice-1/like"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(appNoticeService);
    }

    @Test
    void 앱공지좋아요취소_앱공지없음_404() throws Exception {
        mockValidToken();
        when(appNoticeService.unlikeNotice("firebase-uid", "app-notice-1"))
                .thenThrow(new BusinessException(ErrorCode.APP_NOTICE_NOT_FOUND));

        mockMvc.perform(delete("/v1/app-notices/app-notice-1/like")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APP_NOTICE_NOT_FOUND"));
    }

    @Test
    void 댓글수정_정상요청_200() throws Exception {
        mockValidToken();
        when(appNoticeService.updateComment(eq("firebase-uid"), eq("app-comment-1"), any()))
                .thenReturn(commentResponse());

        mockMvc.perform(patch("/v1/app-notice-comments/app-comment-1")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"content":"수정 댓글"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("app-comment-1"));
    }

    @Test
    void 댓글수정_토큰없음_401() throws Exception {
        mockMvc.perform(patch("/v1/app-notice-comments/app-comment-1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"content":"수정 댓글"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(appNoticeService);
    }

    @Test
    void 댓글수정_작성자아님_403() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.NOT_APP_NOTICE_COMMENT_AUTHOR))
                .when(appNoticeService).updateComment(eq("firebase-uid"), eq("app-comment-1"), any());

        mockMvc.perform(patch("/v1/app-notice-comments/app-comment-1")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"content":"수정 댓글"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_APP_NOTICE_COMMENT_AUTHOR"));
    }

    @Test
    void 댓글삭제_정상요청_200() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/app-notice-comments/app-comment-1")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 댓글삭제_토큰없음_401() throws Exception {
        mockMvc.perform(delete("/v1/app-notice-comments/app-comment-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(appNoticeService);
    }

    @Test
    void 댓글삭제_작성자아님_403() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.NOT_APP_NOTICE_COMMENT_AUTHOR))
                .when(appNoticeService).deleteComment("firebase-uid", "app-comment-1");

        mockMvc.perform(delete("/v1/app-notice-comments/app-comment-1")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_APP_NOTICE_COMMENT_AUTHOR"));
    }

    @Test
    void 댓글좋아요_정상요청_200() throws Exception {
        mockValidToken();
        when(appNoticeService.likeComment("firebase-uid", "app-comment-1"))
                .thenReturn(new NoticeCommentLikeResponse("app-comment-1", true, 3));

        mockMvc.perform(post("/v1/app-notice-comments/app-comment-1/like")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value("app-comment-1"))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(3));
    }

    @Test
    void 댓글좋아요_토큰없음_401() throws Exception {
        mockMvc.perform(post("/v1/app-notice-comments/app-comment-1/like"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(appNoticeService);
    }

    @Test
    void 댓글좋아요_댓글없음_404() throws Exception {
        mockValidToken();
        when(appNoticeService.likeComment("firebase-uid", "app-comment-1"))
                .thenThrow(new BusinessException(ErrorCode.APP_NOTICE_COMMENT_NOT_FOUND));

        mockMvc.perform(post("/v1/app-notice-comments/app-comment-1/like")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APP_NOTICE_COMMENT_NOT_FOUND"));
    }

    @Test
    void 댓글좋아요취소_정상요청_200() throws Exception {
        mockValidToken();
        when(appNoticeService.unlikeComment("firebase-uid", "app-comment-1"))
                .thenReturn(new NoticeCommentLikeResponse("app-comment-1", false, 2));

        mockMvc.perform(delete("/v1/app-notice-comments/app-comment-1/like")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value("app-comment-1"))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(2));
    }

    @Test
    void 댓글좋아요취소_토큰없음_401() throws Exception {
        mockMvc.perform(delete("/v1/app-notice-comments/app-comment-1/like"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(appNoticeService);
    }

    @Test
    void 댓글좋아요취소_삭제된댓글_409() throws Exception {
        mockValidToken();
        when(appNoticeService.unlikeComment("firebase-uid", "app-comment-1"))
                .thenThrow(new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED));

        mockMvc.perform(delete("/v1/app-notice-comments/app-comment-1/like")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_ALREADY_DELETED"));
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token")).thenReturn(new FirebaseTokenClaims(
                "firebase-uid", "user@sungkyul.ac.kr", "google.com", "provider-id", "홍길동", null
        ));
    }

    private NoticeCommentResponse commentResponse() {
        return new NoticeCommentResponse(
                "app-comment-1", null, 0, "새 댓글", null, "익명1", null, false,
                true, 1, true, 0, false, false, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private NoticeCommentResponse blockedCommentResponse() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 8, 31, 18, 30);
        return new NoticeCommentResponse(
                "app-comment-1", null, 0, "차단한 사용자의 댓글입니다.", null, null, null, false,
                false, null, false, 0, false, true, occurredAt, occurredAt
        );
    }
}
