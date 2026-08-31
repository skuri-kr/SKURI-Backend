package com.skuri.skuri_backend.domain.contentblock.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.contentblock.dto.request.CreateContentBlockRequest;
import com.skuri.skuri_backend.domain.contentblock.dto.response.ContentBlockResponse;
import com.skuri.skuri_backend.domain.contentblock.service.ContentBlockService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContentBlockController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class ContentBlockControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentBlockService contentBlockService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void create_콘텐츠식별자로차단하고_실제회원정보를노출하지않는다() throws Exception {
        mockValidToken();
        when(contentBlockService.create(eq("firebase-uid"), any(CreateContentBlockRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/v1/content-blocks")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "COMMENT",
                                  "targetId": "comment-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.blockId").value("block-id"))
                .andExpect(jsonPath("$.data.label").value("차단한 사용자"))
                .andExpect(jsonPath("$.data.blockedAt").value("2026-08-31T18:30:00"))
                .andExpect(jsonPath("$.data.memberId").doesNotExist())
                .andExpect(jsonPath("$.data.nickname").doesNotExist())
                .andExpect(jsonPath("$.data.department").doesNotExist());
    }

    @Test
    void getMyBlocks_비페이지목록을반환한다() throws Exception {
        mockValidToken();
        when(contentBlockService.getMyBlocks("firebase-uid")).thenReturn(List.of(response()));

        mockMvc.perform(get("/v1/content-blocks").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].blockId").value("block-id"))
                .andExpect(jsonPath("$.data[0].label").value("차단한 사용자"));
    }

    @Test
    void unblock_opaque식별자로204멱등처리한다() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/content-blocks/block-id")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 모든경로는_인증없으면401이고서비스를호출하지않는다() throws Exception {
        mockMvc.perform(post("/v1/content-blocks")
                        .contentType(APPLICATION_JSON)
                        .content("{\"targetType\":\"POST\",\"targetId\":\"post-1\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/content-blocks"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/content-blocks/block-id"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(contentBlockService);
    }

    @Test
    void create_필수필드가없으면422이다() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/v1/content-blocks")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(contentBlockService);
    }

    @Test
    void create_자기콘텐츠차단이면400이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.CONTENT_BLOCK_SELF_NOT_ALLOWED))
                .when(contentBlockService).create(eq("firebase-uid"), any(CreateContentBlockRequest.class));

        mockMvc.perform(post("/v1/content-blocks")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"targetType\":\"POST\",\"targetId\":\"post-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CONTENT_BLOCK_SELF_NOT_ALLOWED"));
    }

    private ContentBlockResponse response() {
        return new ContentBlockResponse(
                "block-id",
                "차단한 사용자",
                LocalDateTime.of(2026, 8, 31, 18, 30)
        );
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
}
