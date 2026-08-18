package com.skuri.skuri_backend.domain.friend.controller;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodePreviewResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodeResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendPrivacyResponse;
import com.skuri.skuri_backend.domain.friend.exception.FriendCodeNotFoundException;
import com.skuri.skuri_backend.domain.friend.exception.FriendCodeRegenerationCooldownException;
import com.skuri.skuri_backend.domain.friend.service.FriendCodeService;
import com.skuri.skuri_backend.domain.friend.service.FriendPrivacyService;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftInternalSecretFilter;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FriendFoundationController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        MinecraftInternalSecretFilter.class,
        FriendFoundationControllerTestConfig.class
})
class FriendFoundationControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendCodeService friendCodeService;

    @MockitoBean
    private FriendPrivacyService friendPrivacyService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void 내친구코드_정상조회_200() throws Exception {
        mockValidToken();
        when(friendCodeService.getMyCode("firebase-uid"))
                .thenReturn(new FriendCodeResponse("SKR-7K4M-9Q2D", true, null));

        mockMvc.perform(get("/v1/friends/me/code").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.friendCode").value("SKR-7K4M-9Q2D"));
    }

    @Test
    void 내친구코드_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/friends/me/code"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(friendCodeService);
    }

    @Test
    void 내친구코드_가입한활성회원없음_404() throws Exception {
        mockValidToken();
        when(friendCodeService.getMyCode("firebase-uid"))
                .thenThrow(new MemberNotFoundException());

        mockMvc.perform(get("/v1/friends/me/code").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void 친구코드_재발급_성공_200() throws Exception {
        mockValidToken();
        when(friendCodeService.regenerateMyCode("firebase-uid"))
                .thenReturn(new FriendCodeResponse("SKR-5H2P-8X3K", false, LocalDateTime.of(2026, 8, 19, 12, 0)));

        mockMvc.perform(post("/v1/friends/me/code/regenerate").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.friendCode").value("SKR-5H2P-8X3K"));
    }

    @Test
    void 친구코드_재발급_토큰없음_401() throws Exception {
        mockMvc.perform(post("/v1/friends/me/code/regenerate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(friendCodeService);
    }

    @Test
    void 재발급_제한은_429와_retryAfter를_반환한다() throws Exception {
        mockValidToken();
        when(friendCodeService.regenerateMyCode("firebase-uid"))
                .thenThrow(new FriendCodeRegenerationCooldownException(
                        LocalDateTime.now(),
                        LocalDateTime.now().plusHours(23)
                ));

        mockMvc.perform(post("/v1/friends/me/code/regenerate").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errorCode").value("FRIEND_CODE_REGENERATION_COOLDOWN"));
    }

    @Test
    void 친구코드_preview는_부작용없는_프로필만_반환한다() throws Exception {
        mockValidToken();
        when(friendCodeService.preview("firebase-uid", "SKR-7K4M-9Q2D"))
                .thenReturn(new FriendCodePreviewResponse(
                        "friend-public-id", "스쿠리", null, "컴퓨터공학과", true
                ));

        mockMvc.perform(post("/v1/friend-codes/preview")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendCode\":\"SKR-7K4M-9Q2D\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.friendPublicId").value("friend-public-id"))
                .andExpect(jsonPath("$.data.canSendFriendRequest").value(true));
    }

    @Test
    void 친구코드_preview_빈값은_422이고_서비스를호출하지않는다() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/v1/friend-codes/preview")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendCode\":\"\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(friendCodeService);
    }

    @Test
    void 친구코드_preview_토큰없음_401() throws Exception {
        mockMvc.perform(post("/v1/friend-codes/preview")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendCode\":\"SKR-7K4M-9Q2D\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(friendCodeService);
    }

    @Test
    void 친구코드_preview_존재하지않는코드는_404() throws Exception {
        mockValidToken();
        when(friendCodeService.preview("firebase-uid", "SKR-7K4M-9Q2D"))
                .thenThrow(new FriendCodeNotFoundException());

        mockMvc.perform(post("/v1/friend-codes/preview")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendCode\":\"SKR-7K4M-9Q2D\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_CODE_NOT_FOUND"));
    }

    @Test
    void privacy_정상조회_200() throws Exception {
        mockValidToken();
        when(friendPrivacyService.getMyPrivacy("firebase-uid"))
                .thenReturn(new FriendPrivacyResponse(true));

        mockMvc.perform(get("/v1/friends/me/privacy").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nicknameSearchable").value(true));
    }

    @Test
    void privacy_조회_가입한활성회원없음_404() throws Exception {
        mockValidToken();
        when(friendPrivacyService.getMyPrivacy("firebase-uid"))
                .thenThrow(new MemberNotFoundException());

        mockMvc.perform(get("/v1/friends/me/privacy").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void privacy_변경은_저장결과를_반환한다() throws Exception {
        mockValidToken();
        when(friendPrivacyService.updateMyPrivacy("firebase-uid", true))
                .thenReturn(new FriendPrivacyResponse(true));

        mockMvc.perform(patch("/v1/friends/me/privacy")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nicknameSearchable\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nicknameSearchable").value(true));

        verify(friendPrivacyService).updateMyPrivacy("firebase-uid", true);
    }

    @Test
    void privacy_조회_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/friends/me/privacy"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void privacy_변경_토큰없음_401() throws Exception {
        mockMvc.perform(patch("/v1/friends/me/privacy")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nicknameSearchable\":true}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(friendPrivacyService);
    }

    @Test
    void privacy_변경_필수값누락은_422이고_서비스를호출하지않는다() throws Exception {
        mockValidToken();

        mockMvc.perform(patch("/v1/friends/me/privacy")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(friendPrivacyService);
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }
}
