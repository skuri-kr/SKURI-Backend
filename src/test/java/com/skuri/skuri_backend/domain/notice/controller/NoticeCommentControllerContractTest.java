package com.skuri.skuri_backend.domain.notice.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentLikeResponse;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeCommentResponse;
import com.skuri.skuri_backend.domain.notice.service.NoticeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NoticeCommentController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class NoticeCommentControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoticeService noticeService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void patchComment_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.updateComment(eq("firebase-uid"), eq("comment-1"), any()))
                .thenReturn(commentResponse());

        mockMvc.perform(
                        patch("/v1/notice-comments/comment-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "content": "수정된 댓글",
                                          "isAnonymous": true
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("notice-comment-1"))
                .andExpect(jsonPath("$.data.content").value("수정된 댓글"))
                .andExpect(jsonPath("$.data.isAnonymous").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(5))
                .andExpect(jsonPath("$.data.isLiked").value(true));
    }

    @Test
    void patchComment_작성자아님_403() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.NOT_NOTICE_COMMENT_AUTHOR))
                .when(noticeService)
                .updateComment(eq("firebase-uid"), eq("comment-1"), any());

        mockMvc.perform(
                        patch("/v1/notice-comments/comment-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "content": "수정된 댓글",
                                          "isAnonymous": true
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_NOTICE_COMMENT_AUTHOR"));
    }

    @Test
    void patchComment_댓글없음_404() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.NOTICE_COMMENT_NOT_FOUND))
                .when(noticeService)
                .updateComment(eq("firebase-uid"), eq("comment-1"), any());

        mockMvc.perform(
                        patch("/v1/notice-comments/comment-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "content": "수정된 댓글"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTICE_COMMENT_NOT_FOUND"));
    }

    @Test
    void patchComment_요청검증실패_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        patch("/v1/notice-comments/comment-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "content": ""
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteComment_정상요청_200() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/notice-comments/comment-1").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteComment_작성자아님_403() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.NOT_NOTICE_COMMENT_AUTHOR))
                .when(noticeService)
                .deleteComment("firebase-uid", "comment-1");

        mockMvc.perform(delete("/v1/notice-comments/comment-1").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_NOTICE_COMMENT_AUTHOR"));
    }

    @Test
    void deleteComment_댓글없음_404() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.NOTICE_COMMENT_NOT_FOUND))
                .when(noticeService)
                .deleteComment("firebase-uid", "comment-1");

        mockMvc.perform(delete("/v1/notice-comments/comment-1").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTICE_COMMENT_NOT_FOUND"));
    }

    @Test
    void postLike_정상요청_200() throws Exception {
        mockValidToken();
        when(noticeService.likeComment("firebase-uid", "comment-1"))
                .thenReturn(new NoticeCommentLikeResponse("comment-1", true, 5));

        mockMvc.perform(post("/v1/notice-comments/comment-1/like").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value("comment-1"))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(5));
    }

    @Test
    void deleteLike_이미삭제된댓글_409() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED))
                .when(noticeService)
                .unlikeComment("firebase-uid", "comment-1");

        mockMvc.perform(delete("/v1/notice-comments/comment-1/like").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_ALREADY_DELETED"));
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(delete("/v1/notice-comments/comment-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(noticeService);
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }

    private NoticeCommentResponse commentResponse() {
        return new NoticeCommentResponse(
                "notice-comment-1",
                null,
                0,
                "수정된 댓글",
                null,
                "익명1",
                null,
                true,
                1,
                true,
                5,
                true,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
