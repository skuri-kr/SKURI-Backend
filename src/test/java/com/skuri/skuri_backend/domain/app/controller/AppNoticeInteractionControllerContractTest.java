package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
}
