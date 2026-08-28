package com.skuri.skuri_backend.domain.share.controller;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftInternalSecretFilter;
import com.skuri.skuri_backend.domain.share.dto.response.BoardSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.CafeteriaSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.NoticeSharePreviewResponse;
import com.skuri.skuri_backend.domain.share.dto.response.ShareLinkResolveResponse;
import com.skuri.skuri_backend.domain.share.dto.response.ShareLinkResponse;
import com.skuri.skuri_backend.domain.share.exception.ShareLinkNotFoundException;
import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import com.skuri.skuri_backend.domain.share.service.ShareLinkService;
import com.skuri.skuri_backend.domain.support.exception.CafeteriaMenuNotFoundException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    void 공지미리보기는_만료된토큰이포함돼도_공개조회된다() throws Exception {
        when(shareLinkService.getNoticePreview("7Kp3mQxA")).thenReturn(new NoticeSharePreviewResponse(
                "7Kp3mQxA", "공지 제목", "학사", "교무처", "성결대학교",
                LocalDateTime.of(2026, 8, 28, 9, 0), List.of(), false
        ));

        mockMvc.perform(get("/v1/share-links/notice/7Kp3mQxA/preview")
                        .header(AUTHORIZATION, "Bearer expired-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("공지 제목"));

        verifyNoInteractions(firebaseTokenVerifier);
    }

    @Test
    void 학식미리보기는_인증없이_이번주메뉴를반환한다() throws Exception {
        when(shareLinkService.getCafeteriaPreview()).thenReturn(new CafeteriaSharePreviewResponse(
                "2026-W35",
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 30),
                List.of(new CafeteriaSharePreviewResponse.Category("rollNoodles", "Roll & Noodles")),
                Map.of("2026-08-28", Map.of(
                        "rollNoodles",
                        List.of(new CafeteriaSharePreviewResponse.MenuEntry("제육덮밥", List.of()))
                ))
        ));

        mockMvc.perform(get("/v1/share-links/cafeteria/preview")
                        .header(AUTHORIZATION, "Bearer expired-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekId").value("2026-W35"))
                .andExpect(jsonPath("$.data.days['2026-08-28'].rollNoodles[0].title").value("제육덮밥"));

        verifyNoInteractions(firebaseTokenVerifier);
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
    void 링크해석은_인증후_앱내부ID를반환한다() throws Exception {
        mockValidToken();
        when(shareLinkService.resolve("notice", "7Kp3mQxA")).thenReturn(new ShareLinkResolveResponse(
                ShareResourceType.NOTICE, "7Kp3mQxA", "notice-1"
        ));

        mockMvc.perform(get("/v1/share-links/notice/7Kp3mQxA/resolve")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceType").value("NOTICE"))
                .andExpect(jsonPath("$.data.resourceId").value("notice-1"));
    }

    @Test
    void 링크해석은_인증이없으면_401이다() throws Exception {
        mockMvc.perform(get("/v1/share-links/notice/7Kp3mQxA/resolve"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(shareLinkService);
    }

    @Test
    void 존재하지않는공유코드는_해석할때_404다() throws Exception {
        mockValidToken();
        when(shareLinkService.resolve("notice", "7Kp3mQxA")).thenThrow(new ShareLinkNotFoundException());

        mockMvc.perform(get("/v1/share-links/notice/7Kp3mQxA/resolve")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SHARE_LINK_NOT_FOUND"));
    }

    @Test
    void 유효하지않은긴코드는_422로_거부한다() throws Exception {
        mockMvc.perform(get("/v1/share-links/notice/aHR0cHM6Ly93d3cuc3VuZ2t5dWw/preview"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/v1/share-links/board/aHR0cHM6Ly93d3cuc3VuZ2t5dWw/preview"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockValidToken();
        mockMvc.perform(get("/v1/share-links/notice/aHR0cHM6Ly93d3cuc3VuZ2t5dWw/resolve")
                        .header(AUTHORIZATION, "Bearer valid-token"))
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

    @Test
    void 공지공유링크가없으면_404다() throws Exception {
        when(shareLinkService.getNoticePreview("7Kp3mQxA")).thenThrow(new ShareLinkNotFoundException());

        mockMvc.perform(get("/v1/share-links/notice/7Kp3mQxA/preview"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SHARE_LINK_NOT_FOUND"));
    }

    @Test
    void 이번주학식이없으면_404다() throws Exception {
        when(shareLinkService.getCafeteriaPreview()).thenThrow(new CafeteriaMenuNotFoundException());

        mockMvc.perform(get("/v1/share-links/cafeteria/preview"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAFETERIA_MENU_NOT_FOUND"));
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token")).thenReturn(new FirebaseTokenClaims(
                "firebase-uid", "user@sungkyul.ac.kr", "google.com", "provider-id", "홍길동", null
        ));
    }
}
