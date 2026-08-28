package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
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

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppNoticeController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class AppNoticeControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppNoticeService appNoticeService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getAppNotices_비인증정상요청_200() throws Exception {
        when(appNoticeService.getPublishedNotices()).thenReturn(List.of(appNoticeResponse()));

        mockMvc.perform(get("/v1/app-notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("app-notice-1"));
    }

    @Test
    void getAppNotice_비인증정상요청_200() throws Exception {
        when(appNoticeService.getPublishedNotice("app-notice-1")).thenReturn(appNoticeResponse());

        mockMvc.perform(get("/v1/app-notices/app-notice-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("app-notice-1"));
    }

    @Test
    void getAppNotice_인증토큰이있으면_사용자별좋아요상태를조회한다() throws Exception {
        when(firebaseTokenVerifier.verify("valid-token")).thenReturn(new FirebaseTokenClaims(
                "firebase-uid", "user@sungkyul.ac.kr", "google.com", "provider-id", "홍길동", null
        ));
        when(appNoticeService.getPublishedNotice("firebase-uid", "app-notice-1"))
                .thenReturn(appNoticeResponse());

        mockMvc.perform(get("/v1/app-notices/app-notice-1")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("app-notice-1"));
    }

    @Test
    void getAppNotice_없는공지_404() throws Exception {
        when(appNoticeService.getPublishedNotice("missing"))
                .thenThrow(new BusinessException(ErrorCode.APP_NOTICE_NOT_FOUND));

        mockMvc.perform(get("/v1/app-notices/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APP_NOTICE_NOT_FOUND"));
    }

    private AppNoticeResponse appNoticeResponse() {
        return new AppNoticeResponse(
                "app-notice-1",
                "앱 공지",
                "앱 공지 내용",
                AppNoticeCategory.MAINTENANCE,
                AppNoticePriority.HIGH,
                List.of(),
                null,
                LocalDateTime.of(2026, 2, 20, 0, 0),
                LocalDateTime.of(2026, 2, 19, 12, 0),
                LocalDateTime.of(2026, 2, 19, 12, 0)
        );
    }
}
