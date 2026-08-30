package com.skuri.skuri_backend.infra.auth;

import com.skuri.skuri_backend.domain.app.controller.AppNoticeController;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.support.controller.AppVersionController;
import com.skuri.skuri_backend.domain.support.controller.AppVersionAdminController;
import com.skuri.skuri_backend.domain.support.dto.response.AppVersionResponse;
import com.skuri.skuri_backend.domain.support.service.AppVersionService;
import com.skuri.skuri_backend.domain.chat.controller.ChatAdminRoomController;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminCreateChatRoomResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.service.ChatAdminService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.controller.MemberController;
import com.skuri.skuri_backend.domain.member.dto.response.MemberMeResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberNotificationSettingResponse;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.member.service.MemberLifecycleService;
import com.skuri.skuri_backend.domain.member.service.MemberService;
import com.skuri.skuri_backend.domain.notification.controller.FcmTokenController;
import com.skuri.skuri_backend.domain.notification.controller.NotificationController;
import com.skuri.skuri_backend.domain.notification.controller.NotificationSseController;
import com.skuri.skuri_backend.domain.notification.service.FcmTokenService;
import com.skuri.skuri_backend.domain.notification.service.NotificationService;
import com.skuri.skuri_backend.domain.notification.service.NotificationSseService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        MemberController.class,
        AppVersionController.class,
        AppVersionAdminController.class,
        AppNoticeController.class,
        ChatAdminRoomController.class,
        NotificationController.class,
        FcmTokenController.class,
        NotificationSseController.class
})
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberLifecycleService memberLifecycleService;

    @MockitoBean
    private AppNoticeService appNoticeService;

    @MockitoBean
    private AppVersionService appVersionService;

    @MockitoBean
    private ChatAdminService chatAdminService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private FcmTokenService fcmTokenService;

    @MockitoBean
    private NotificationSseService notificationSseService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void 보호Api_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 보호Api_membersById_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/members/target-uid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 보호Api_notifications_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 보호Api_fcmTokens_토큰없음_401() throws Exception {
        mockMvc.perform(
                        post("/v1/members/me/fcm-tokens")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "token": "dXZlbnQ6ZmNtLXRva2Vu",
                                          "platform": "ios"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 보호Api_notificationSse_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/sse/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 보호Api_도메인불일치_403() throws Exception {
        when(firebaseTokenVerifier.verify("invalid-domain-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@gmail.com",
                        "google.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));

        mockMvc.perform(
                        get("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer invalid-domain-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("EMAIL_DOMAIN_RESTRICTED"));
    }

    @Test
    void 보호Api_정상토큰_접근가능() throws Exception {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
        when(memberService.getMyProfile("firebase-uid"))
                .thenReturn(new MemberMeResponse(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "홍길동",
                        "20201234",
                        "컴퓨터공학과",
                        "https://example.com/profile.jpg",
                        "홍길동",
                        false,
                        null,
                        new MemberNotificationSettingResponse(
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                false,
                                Map.of("news", true)
                        ),
                        true,
                        "2026-08-30",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(
                        get("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("firebase-uid"));

        verify(firebaseTokenVerifier).verify("valid-token");
    }

    @Test
    void 공개Api_인증없이_접근가능() throws Exception {
        when(appNoticeService.getPublishedNotices()).thenReturn(java.util.List.of(appNoticeResponse()));
        when(appNoticeService.getPublishedNotice("app-notice-1")).thenReturn(appNoticeResponse());
        when(appVersionService.getAppVersion("ios"))
                .thenReturn(new AppVersionResponse(
                        "ios",
                        "1.5.0",
                        false,
                        "새로운 기능이 추가되었습니다.",
                        "업데이트 안내",
                        true,
                        "업데이트",
                        "https://apps.apple.com/..."
                ));

        mockMvc.perform(get("/v1/app-notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/v1/app-notices/app-notice-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("app-notice-1"));

        mockMvc.perform(get("/v1/app-versions/ios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.platform").value("ios"));
    }

    @Test
    void 관리자Api_비관리자토큰_403_ADMIN_REQUIRED() throws Exception {
        mockToken("user-token", "firebase-uid", false);

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType("application/json")
                                .content("{\"name\":\"운영 채팅방\",\"type\":\"CUSTOM\",\"isPublic\":true}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void support관리자Api_비관리자토큰_403_ADMIN_REQUIRED() throws Exception {
        mockToken("user-token", "firebase-uid", false);

        mockMvc.perform(
                        put("/v1/admin/app-versions/ios")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "minimumVersion": "1.6.0",
                                          "forceUpdate": true,
                                          "title": "필수 업데이트 안내",
                                          "message": "안정성 개선을 위한 필수 업데이트입니다.",
                                          "showButton": true,
                                          "buttonText": "업데이트",
                                          "buttonUrl": "https://apps.apple.com/..."
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void 관리자Api_관리자토큰_접근가능() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.createPublicChatRoom(org.mockito.ArgumentMatchers.eq("admin-uid"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminCreateChatRoomResponse("room:1", "운영 채팅방", ChatRoomType.CUSTOM));

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType("application/json")
                                .content("{\"name\":\"운영 채팅방\",\"type\":\"CUSTOM\",\"isPublic\":true}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("room:1"));
    }

    private void mockToken(String token, String uid, boolean isAdmin) {
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "google-provider-id",
                        "테스터",
                        "https://example.com/profile.jpg"
                ));
        if (!isAdmin) {
            when(memberRepository.findById(uid)).thenReturn(Optional.empty());
            return;
        }
        Member admin = Member.create(
                uid,
                uid + "@sungkyul.ac.kr",
                "관리자",
                LocalDateTime.now().minusDays(1)
        );
        ReflectionTestUtils.setField(admin, "isAdmin", true);
        when(memberRepository.findById(uid)).thenReturn(Optional.of(admin));
    }

    private AppNoticeResponse appNoticeResponse() {
        return new AppNoticeResponse(
                "app-notice-1",
                "앱 공지",
                "앱 공지 내용",
                AppNoticeCategory.MAINTENANCE,
                AppNoticePriority.HIGH,
                java.util.List.of(),
                null,
                LocalDateTime.of(2026, 2, 20, 0, 0),
                LocalDateTime.of(2026, 2, 19, 12, 0),
                LocalDateTime.of(2026, 2, 19, 12, 0)
        );
    }
}
