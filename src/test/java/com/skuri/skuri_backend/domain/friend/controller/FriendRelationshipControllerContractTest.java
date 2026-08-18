package com.skuri.skuri_backend.domain.friend.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSummaryResponse;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipService;
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

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FriendRelationshipController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        MinecraftInternalSecretFilter.class,
        FriendFoundationControllerTestConfig.class
})
class FriendRelationshipControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendRelationshipService friendRelationshipService;

    @MockitoBean
    private FriendRelationshipQueryService friendRelationshipQueryService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void 친구목록은_공개식별자와_Core필드만_반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipQueryService.getFriends("firebase-uid"))
                .thenReturn(List.of(new FriendSummaryResponse("friend-public-id", "스쿠리", "컴퓨터공학과", null, true)));

        mockMvc.perform(get("/v1/friends").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].friendPublicId").value("friend-public-id"))
                .andExpect(jsonPath("$.data[0].favorite").value(true))
                .andExpect(jsonPath("$.data[0].effectiveTimetableScope").doesNotExist())
                .andExpect(jsonPath("$.data[0].minecraftAccountCount").doesNotExist());
    }

    @Test
    void 친구목록은_토큰없으면_401이고_서비스를호출하지않는다() throws Exception {
        mockMvc.perform(get("/v1/friends"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(friendRelationshipService, friendRelationshipQueryService);
    }

    @Test
    void 친구요청생성은_PENDING과_요청식별자를_반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipService.createRequest("firebase-uid", "friend-public-id"))
                .thenReturn(new FriendRelationshipService.FriendRequestCreationResult("request-id", null, false));

        mockMvc.perform(post("/v1/friend-requests")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicId\":\"friend-public-id\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.requestId").value("request-id"))
                .andExpect(jsonPath("$.data.friend").doesNotExist());
    }

    @Test
    void 친구요청생성은_필수공개식별자가없으면_422이다() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/v1/friend-requests")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(friendRelationshipService, friendRelationshipQueryService);
    }

    @Test
    void 친구요청수락은_친구공개요약을_반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipService.acceptRequest("firebase-uid", "request-id")).thenReturn("friend-member-id");
        when(friendRelationshipQueryService.getFriendByMemberId("firebase-uid", "friend-member-id"))
                .thenReturn(new FriendSummaryResponse("friend-public-id", "스쿠리", null, null, false));

        mockMvc.perform(post("/v1/friend-requests/request-id/accept")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.friend.friendPublicId").value("friend-public-id"));
    }

    @Test
    void 친구요청terminal상태는_409공통포맷으로_반환한다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.FRIEND_REQUEST_STATE_NOT_ALLOWED))
                .when(friendRelationshipService).declineRequest("firebase-uid", "request-id");

        mockMvc.perform(post("/v1/friend-requests/request-id/decline")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_REQUEST_STATE_NOT_ALLOWED"));
    }

    @Test
    void 즐겨찾기변경은_204를_반환한다() throws Exception {
        mockValidToken();

        mockMvc.perform(patch("/v1/friends/friend-public-id/favorite")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"favorite\":true}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 차단해제는_204를_반환한다() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/friends/blocks/friend-public-id")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());
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
