package com.skuri.skuri_backend.domain.share.controller;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftInternalSecretFilter;
import com.skuri.skuri_backend.domain.share.dto.response.BoardSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.ShareLinkResponse;
import com.skuri.skuri_backend.domain.share.exception.ShareLinkNotFoundException;
import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import com.skuri.skuri_backend.domain.share.service.ShareLinkService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShareLinkController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        MinecraftInternalSecretFilter.class
})
class ShareLinkControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShareLinkService shareLinkService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void 공개미리보기는_토큰없이_조회된다() throws Exception {
        when(shareLinkService.getBoardPreview("5Rm2Qn8B")).thenReturn(new BoardSharePreviewResponse(
                "5Rm2Qn8B", "제목", PostCategory.GENERAL, "익명", LocalDateTime.of(2026, 8, 28, 10, 30), "본문", false
        ));

        mockMvc.perform(get("/v1/share-links/board/5Rm2Qn8B/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.author").value("익명"));
    }

    @Test
    void 링크발급은_인증이필요하다() throws Exception {
        mockMvc.perform(post("/v1/share-links")
                        .contentType(APPLICATION_JSON)
                        .content("{\"resourceType\":\"NOTICE\",\"resourceId\":\"notice-1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(shareLinkService);
    }

    @Test
    void 링크발급은_201과_짧은URL을_반환한다() throws Exception {
        mockValidToken();
        when(shareLinkService.create(any())).thenReturn(new ShareLinkResponse(
                ShareResourceType.NOTICE, "7Kp3mQxA", "https://link.skuri.kr/notice/7Kp3mQxA"
        ));

        mockMvc.perform(post("/v1/share-links")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"resourceType\":\"NOTICE\",\"resourceId\":\"notice-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("7Kp3mQxA"))
                .andExpect(jsonPath("$.data.url").value("https://link.skuri.kr/notice/7Kp3mQxA"));
    }

    @Test
    void 유효하지않은긴코드는_422로_거부한다() throws Exception {
        mockMvc.perform(get("/v1/share-links/notice/aHR0cHM6Ly93d3cuc3VuZ2t5dWw/preview"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(shareLinkService);
    }

    @Test
    void 존재하지않는공유링크는_404다() throws Exception {
        when(shareLinkService.getBoardPreview("5Rm2Qn8B")).thenThrow(new ShareLinkNotFoundException());

        mockMvc.perform(get("/v1/share-links/board/5Rm2Qn8B/preview"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SHARE_LINK_NOT_FOUND"));
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token")).thenReturn(new FirebaseTokenClaims(
                "firebase-uid", "user@sungkyul.ac.kr", "google.com", "provider-id", "홍길동", null
        ));
    }
}
