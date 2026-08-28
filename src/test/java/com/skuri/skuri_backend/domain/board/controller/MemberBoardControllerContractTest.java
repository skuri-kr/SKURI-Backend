package com.skuri.skuri_backend.domain.board.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostSummaryResponse;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberBoardController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class MemberBoardControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardService boardService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getMyPosts_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.getMyPosts("firebase-uid", 0, 20))
                .thenReturn(PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(postSummaryResponse()))));

        mockMvc.perform(
                        get("/v1/members/me/posts")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("post-1"))
                .andExpect(jsonPath("$.data.content[0].isAuthorAdmin").value(true))
                .andExpect(jsonPath("$.data.content[0].isLiked").value(true))
                .andExpect(jsonPath("$.data.content[0].isCommentedByMe").value(true))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value("https://example.com/post-1-thumb.jpg"));
    }

    @Test
    void getMyBookmarks_정상요청_200() throws Exception {
        mockValidToken();
        when(boardService.getMyBookmarks("firebase-uid", 0, 20))
                .thenReturn(PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(postSummaryResponse()))));

        mockMvc.perform(
                        get("/v1/members/me/bookmarks")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("post-1"))
                .andExpect(jsonPath("$.data.content[0].isBookmarked").value(true));
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/members/me/posts"))
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

    private PostSummaryResponse postSummaryResponse() {
        return new PostSummaryResponse(
                "post-1",
                "제목",
                "내용",
                "firebase-uid",
                "홍길동",
                "https://example.com/profile.jpg",
                true,
                false,
                PostCategory.GENERAL,
                10,
                2,
                1,
                3,
                true,
                true,
                true,
                false,
                "https://example.com/post-1-thumb.jpg",
                false,
                LocalDateTime.now()
        );
    }
}
