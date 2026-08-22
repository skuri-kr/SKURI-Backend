package com.skuri.skuri_backend.domain.academic.controller;

import com.skuri.skuri_backend.domain.academic.dto.request.UpdateTimetableShareOverrideRequest;
import com.skuri.skuri_backend.domain.academic.dto.request.UpdateTimetableSharingSettingsRequest;
import com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.FriendTimetableSlotResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableShareOverrideResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.TimetableSharingSettingsResponse;
import com.skuri.skuri_backend.domain.academic.entity.TimetableShareScope;
import com.skuri.skuri_backend.domain.academic.service.TimetableSharingService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TimetableSharingController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class TimetableSharingControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimetableSharingService timetableSharingService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getMySharingSettings_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableSharingService.getMySharingSettings("firebase-uid"))
                .thenReturn(sharingSettingsResponse());

        mockMvc.perform(get("/v1/timetables/my/sharing-settings")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultScope").value("BUSY_ONLY"))
                .andExpect(jsonPath("$.data.overrides[0].friendPublicId").value("friend-public-id"))
                .andExpect(jsonPath("$.data.overrides[0].scope").value("DETAILS"));
    }

    @Test
    void updateMySharingSettings_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableSharingService.updateMySharingSettings(eq("firebase-uid"), any(UpdateTimetableSharingSettingsRequest.class)))
                .thenReturn(sharingSettingsResponse());

        mockMvc.perform(patch("/v1/timetables/my/sharing-settings")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"defaultScope\":\"BUSY_ONLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultScope").value("BUSY_ONLY"));

        verify(timetableSharingService).updateMySharingSettings(
                eq("firebase-uid"), any(UpdateTimetableSharingSettingsRequest.class)
        );
    }

    @Test
    void updateMySharingSettings_공개범위누락_422() throws Exception {
        mockValidToken();

        mockMvc.perform(patch("/v1/timetables/my/sharing-settings")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void updateShareOverride_정상요청_200() throws Exception {
        mockValidToken();
        when(timetableSharingService.updateShareOverride(
                eq("firebase-uid"), eq("friend-public-id"), any(UpdateTimetableShareOverrideRequest.class)
        )).thenReturn(new TimetableShareOverrideResponse("friend-public-id", TimetableShareScope.DETAILS));

        mockMvc.perform(put("/v1/timetables/my/sharing-overrides/friend-public-id")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"scope\":\"DETAILS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.friendPublicId").value("friend-public-id"))
                .andExpect(jsonPath("$.data.scope").value("DETAILS"));
    }

    @Test
    void deleteShareOverride_정상요청_204() throws Exception {
        mockValidToken();

        mockMvc.perform(delete("/v1/timetables/my/sharing-overrides/friend-public-id")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(timetableSharingService).deleteShareOverride("firebase-uid", "friend-public-id");
    }

    @Test
    void getFriendTimetable_BUSY_ONLY은과목상세없이점유시간만_200() throws Exception {
        mockValidToken();
        when(timetableSharingService.getFriendTimetable("firebase-uid", "friend-public-id", "2026-1"))
                .thenReturn(new FriendTimetableResponse(
                        "2026-1",
                        TimetableShareScope.BUSY_ONLY,
                        true,
                        List.of(),
                        List.of(new FriendTimetableSlotResponse(1, 3, 4))
                ));

        mockMvc.perform(get("/v1/timetables/friends/friend-public-id")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("semester", "2026-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveScope").value("BUSY_ONLY"))
                .andExpect(jsonPath("$.data.courses").isEmpty())
                .andExpect(jsonPath("$.data.slots[0].dayOfWeek").value(1));
    }

    @Test
    void getFriendTimetable_학기누락_400() throws Exception {
        mockValidToken();

        mockMvc.perform(get("/v1/timetables/friends/friend-public-id")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void sharingSettings_비인증요청_401() throws Exception {
        mockMvc.perform(get("/v1/timetables/my/sharing-settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private TimetableSharingSettingsResponse sharingSettingsResponse() {
        return new TimetableSharingSettingsResponse(
                TimetableShareScope.BUSY_ONLY,
                List.of(new TimetableShareOverrideResponse("friend-public-id", TimetableShareScope.DETAILS))
        );
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "테스터",
                        "https://example.com/profile.jpg"
                ));
    }
}
