package com.skuri.skuri_backend.domain.board.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.dto.response.CommentLikeResponse;
import com.skuri.skuri_backend.domain.board.dto.response.CommentResponse;
import com.skuri.skuri_backend.domain.board.service.BoardService;
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
import java.util.List;

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

@WebMvcTest(controllers = CommentController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class CommentControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardService boardService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void patchComment_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.updateComment(eq("firebase-uid"), eq("comment-1"), any()))
                .thenReturn(commentResponse());

        mockMvc.perform(
                        patch("/v1/comments/comment-1")
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
                .andExpect(jsonPath("$.data.id").value("comment-1"))
                .andExpect(jsonPath("$.data.isAnonymous").value(true))
                .andExpect(jsonPath("$.data.isAuthorAdmin").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(3))
                .andExpect(jsonPath("$.data.isLiked").value(true));
    }

    @Test
    void patchComment_작성자아님_403() throws Exception {
        mockValidToken();
        when(boardService.updateComment(eq("firebase-uid"), eq("comment-1"), any()))
                .thenThrow(new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR));

        mockMvc.perform(
                        patch("/v1/comments/comment-1")
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
                .andExpect(jsonPath("$.errorCode").value("NOT_COMMENT_AUTHOR"));
    }

    @Test
    void patchComment_요청검증실패_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        patch("/v1/comments/comment-1")
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

        mockMvc.perform(
                        delete("/v1/comments/comment-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteComment_이미삭제된댓글_409() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.COMMENT_ALREADY_DELETED))
                .when(boardService)
                .deleteComment("firebase-uid", "comment-1");

        mockMvc.perform(
                        delete("/v1/comments/comment-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_ALREADY_DELETED"));
    }

    @Test
    void postLike_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.likeComment("firebase-uid", "comment-1"))
                .thenReturn(new CommentLikeResponse("comment-1", true, 3));

        mockMvc.perform(
                        post("/v1/comments/comment-1/like")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value("comment-1"))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(3));
    }

    @Test
    void deleteLike_댓글없음_404() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.COMMENT_NOT_FOUND))
                .when(boardService)
                .unlikeComment("firebase-uid", "comment-1");

        mockMvc.perform(
                        delete("/v1/comments/comment-1/like")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(delete("/v1/comments/comment-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(boardService);
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

    private CommentResponse commentResponse() {
        return new CommentResponse(
                "comment-1",
                null,
                0,
                "수정된 댓글",
                null,
                "익명1",
                null,
                false,
                true,
                1,
                true,
                false,
                3,
                true,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
