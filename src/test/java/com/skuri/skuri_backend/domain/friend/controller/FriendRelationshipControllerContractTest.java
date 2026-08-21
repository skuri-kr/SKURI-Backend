package com.skuri.skuri_backend.domain.friend.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInboxCountsResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendRequestPageResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSearchPageResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendSummaryResponse;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipService;
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

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThat;
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
    void 친구관계Core의_모든경로는_토큰없으면_401이다() throws Exception {
        mockMvc.perform(get("/v1/friends/friend-public-id"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/friends/friend-public-id"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/v1/friends/friend-public-id/favorite")
                        .contentType(APPLICATION_JSON)
                        .content("{\"favorite\":true}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/friends/search").param("query", "가나"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/friend-requests").param("direction", "RECEIVED"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/friend-requests")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicId\":\"friend-public-id\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/friend-requests/request-id/accept"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/friend-requests/request-id/decline"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/friend-requests/request-id"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/friends/blocks"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/friends/blocks")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicId\":\"friend-public-id\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/v1/friends/blocks/friend-public-id"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/friends/inbox-counts"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(friendRelationshipService, friendRelationshipQueryService);
    }

    @Test
    void 가입한활성회원이없으면_MEMBER_NOT_FOUND_404를_반환한다() throws Exception {
        mockValidToken();
        doThrow(new MemberNotFoundException())
                .when(friendRelationshipQueryService).getFriends("firebase-uid");

        mockMvc.perform(get("/v1/friends").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void 친구상세조회는_친구공개요약을_반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipQueryService.getFriend("firebase-uid", "friend-public-id"))
                .thenReturn(new FriendSummaryResponse("friend-public-id", "스쿠리", "컴퓨터공학과", null, false));

        mockMvc.perform(get("/v1/friends/friend-public-id").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.friendPublicId").value("friend-public-id"));
    }

    @Test
    void 친구상세조회는_친구관계가없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND))
                .when(friendRelationshipQueryService).getFriend("firebase-uid", "friend-public-id");

        mockMvc.perform(get("/v1/friends/friend-public-id").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FRIENDSHIP_NOT_FOUND"));
    }

    @Test
    void 친구끊기는_204를_반환한다() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/friends/friend-public-id").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 친구끊기는_친구관계가없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND))
                .when(friendRelationshipService).removeFriendship("firebase-uid", "friend-public-id");

        mockMvc.perform(delete("/v1/friends/friend-public-id").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FRIENDSHIP_NOT_FOUND"));
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
                .andExpect(jsonPath("$.data.friend").doesNotExist())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).doesNotContain("\"friend\""));
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
    void 닉네임검색은_한글자검색어도허용한다() throws Exception {
        mockValidToken();
        when(friendRelationshipQueryService.search("firebase-uid", "가", null, null))
                .thenReturn(new FriendSearchPageResponse(List.of(), false, null));

        mockMvc.perform(get("/v1/friends/search")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("query", "가"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void 닉네임검색의_범위초과size는_VALIDATION_ERROR_422이다() throws Exception {
        mockValidToken();

        mockMvc.perform(get("/v1/friends/search")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("query", "가나")
                        .param("size", "21"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(friendRelationshipService, friendRelationshipQueryService);
    }

    @Test
    void 닉네임검색은_검색결과페이지를_반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipQueryService.search("firebase-uid", "가나", null, null))
                .thenReturn(new FriendSearchPageResponse(List.of(), false, null));

        mockMvc.perform(get("/v1/friends/search")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("query", "가나"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void 친구요청목록은_페이지를_반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipQueryService.getRequests(
                "firebase-uid", FriendRelationshipQueryService.FriendRequestDirection.RECEIVED, null, null
        )).thenReturn(new FriendRequestPageResponse(List.of(), false, null));

        mockMvc.perform(get("/v1/friend-requests")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("direction", "RECEIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void 친구요청목록의_잘못된방향은_INVALID_REQUEST_400이다() throws Exception {
        mockValidToken();

        mockMvc.perform(get("/v1/friend-requests")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("direction", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        verifyNoInteractions(friendRelationshipService, friendRelationshipQueryService);
    }

    @Test
    void 친구요청수락은_친구공개요약을_반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipService.acceptRequest("firebase-uid", "request-id"))
                .thenReturn(new FriendRelationshipService.FriendRequestAcceptResult(
                        new FriendSummaryResponse("friend-public-id", "스쿠리", null, null, false)
                ));

        mockMvc.perform(post("/v1/friend-requests/request-id/accept")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.friend.friendPublicId").value("friend-public-id"));

        verifyNoInteractions(friendRelationshipQueryService);
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
    void 프로필미완료회원의_친구API는_409를반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipQueryService.getFriends("firebase-uid"))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_PROFILE_INCOMPLETE));

        mockMvc.perform(get("/v1/friends").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_PROFILE_INCOMPLETE"));
    }

    @Test
    void 친구요청수락은_수신자가아니면_403이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.FRIEND_REQUEST_RECIPIENT_REQUIRED))
                .when(friendRelationshipService).acceptRequest("firebase-uid", "request-id");

        mockMvc.perform(post("/v1/friend-requests/request-id/accept")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_REQUEST_RECIPIENT_REQUIRED"));
    }

    @Test
    void 친구요청거절은_204를_반환한다() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/v1/friend-requests/request-id/decline")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 친구요청취소는_204를_반환한다() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/friend-requests/request-id")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 친구요청취소는_요청자가아니면_403이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.FRIEND_REQUEST_REQUESTER_REQUIRED))
                .when(friendRelationshipService).cancelRequest("firebase-uid", "request-id");

        mockMvc.perform(delete("/v1/friend-requests/request-id")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_REQUEST_REQUESTER_REQUIRED"));
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
    void 즐겨찾기변경은_친구관계가없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.FRIENDSHIP_NOT_FOUND))
                .when(friendRelationshipService).setFavorite("firebase-uid", "friend-public-id", true);

        mockMvc.perform(patch("/v1/friends/friend-public-id/favorite")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"favorite\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FRIENDSHIP_NOT_FOUND"));
    }

    @Test
    void 차단목록은_목록을_반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipQueryService.getBlocks("firebase-uid")).thenReturn(List.of());

        mockMvc.perform(get("/v1/friends/blocks").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void 차단목록은_가입한활성회원이없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new MemberNotFoundException())
                .when(friendRelationshipQueryService).getBlocks("firebase-uid");

        mockMvc.perform(get("/v1/friends/blocks").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void 회원차단은_204를_반환한다() throws Exception {
        mockValidToken();

        mockMvc.perform(post("/v1/friends/blocks")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicId\":\"friend-public-id\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 회원차단은_자기자신이면_400이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.FRIEND_SELF_BLOCK_NOT_ALLOWED))
                .when(friendRelationshipService).blockMember("firebase-uid", "friend-public-id");

        mockMvc.perform(post("/v1/friends/blocks")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"friendPublicId\":\"friend-public-id\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_SELF_BLOCK_NOT_ALLOWED"));
    }

    @Test
    void 차단해제는_204를_반환한다() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/friends/blocks/friend-public-id")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 차단해제는_대상이없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND))
                .when(friendRelationshipService).unblockMember("firebase-uid", "friend-public-id");

        mockMvc.perform(delete("/v1/friends/blocks/friend-public-id")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_TARGET_NOT_FOUND"));
    }

    @Test
    void 친구허브처리필요항목수는_응답을_반환한다() throws Exception {
        mockValidToken();
        when(friendRelationshipQueryService.getInboxCounts("firebase-uid"))
                .thenReturn(new FriendInboxCountsResponse(2, 0, 0, 2));

        mockMvc.perform(get("/v1/friends/inbox-counts").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incomingRequestCount").value(2))
                .andExpect(jsonPath("$.data.totalActionCount").value(2));
    }

    @Test
    void 친구허브처리필요항목수는_가입한활성회원이없으면_404이다() throws Exception {
        mockValidToken();
        doThrow(new MemberNotFoundException())
                .when(friendRelationshipQueryService).getInboxCounts("firebase-uid");

        mockMvc.perform(get("/v1/friends/inbox-counts").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
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
