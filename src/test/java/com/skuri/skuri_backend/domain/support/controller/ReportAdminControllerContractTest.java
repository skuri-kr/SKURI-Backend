package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateReportStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminReportResponse;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.model.ChatMessageReportSnapshot;
import com.skuri.skuri_backend.domain.support.service.ReportService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class ReportAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void getReports_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(reportService.getAdminReports(ReportStatus.PENDING, ReportTargetType.POST, 0, 20))
                .thenReturn(PageResponse.<AdminReportResponse>builder()
                        .content(java.util.List.of(adminReportResponse(ReportStatus.PENDING, null, null)))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());

        mockMvc.perform(
                        get("/v1/admin/reports")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("status", "PENDING")
                                .param("targetType", "POST")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("report-1"));
    }

    @Test
    void getReports_새타입필터정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(reportService.getAdminReports(ReportStatus.PENDING, ReportTargetType.CHAT_MESSAGE, 0, 20))
                .thenReturn(PageResponse.<AdminReportResponse>builder()
                        .content(java.util.List.of(new AdminReportResponse(
                                "report-chat-1",
                                "user-uid",
                                ReportTargetType.CHAT_MESSAGE,
                                "message-1",
                                "sender-1",
                                new ChatMessageReportSnapshot(
                                        "message-1",
                                        "room-1",
                                        "sender-1",
                                        "보낸이",
                                        com.skuri.skuri_backend.domain.chat.entity.ChatMessageType.TEXT,
                                        "광고 메시지",
                                        null,
                                        null,
                                        null,
                                        null,
                                        LocalDateTime.of(2026, 3, 29, 12, 0),
                                        null
                                ),
                                "SPAM",
                                "광고성 메시지입니다.",
                                ReportStatus.PENDING,
                                null,
                                null,
                                LocalDateTime.of(2026, 3, 29, 12, 10),
                                LocalDateTime.of(2026, 3, 29, 12, 10)
                        )))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());

        mockMvc.perform(
                        get("/v1/admin/reports")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("status", "PENDING")
                                .param("targetType", "CHAT_MESSAGE")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].targetType").value("CHAT_MESSAGE"))
                .andExpect(jsonPath("$.data.content[0].targetSnapshot.text").value("광고 메시지"));
    }

    @Test
    void getReports_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(get("/v1/admin/reports").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void getReports_잘못된타입필터_400() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        get("/v1/admin/reports")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("targetType", "INVALID")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void getReports_잘못된페이지파라미터_422() throws Exception {
        mockToken("admin-token", true);
        when(reportService.getAdminReports(null, null, 0, 0))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1 이상 100 이하여야 합니다."));

                mockMvc.perform(
                        get("/v1/admin/reports")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("size", "0")
                )
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void updateReportStatus_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(reportService.updateReportStatus(eq("report-1"), any(UpdateReportStatusRequest.class)))
                .thenReturn(adminReportResponse(ReportStatus.ACTIONED, "DELETE_POST", "게시글 삭제 완료"));

        mockMvc.perform(
                        patch("/v1/admin/reports/report-1/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "ACTIONED",
                                          "action": "DELETE_POST",
                                          "memo": "게시글 삭제 완료"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIONED"));
    }

    @Test
    void updateReportStatus_없는신고_404() throws Exception {
        mockToken("admin-token", true);
        when(reportService.updateReportStatus(eq("missing"), any(UpdateReportStatusRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        mockMvc.perform(
                        patch("/v1/admin/reports/missing/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "ACTIONED",
                                          "action": "DELETE_POST",
                                          "memo": "게시글 삭제 완료"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("REPORT_NOT_FOUND"));
    }

    @Test
    void updateReportStatus_허용되지않는상태전이_409() throws Exception {
        mockToken("admin-token", true);
        when(reportService.updateReportStatus(eq("report-1"), any(UpdateReportStatusRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REPORT_STATUS_TRANSITION));

        mockMvc.perform(
                        patch("/v1/admin/reports/report-1/status")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "REVIEWING",
                                          "action": null,
                                          "memo": null
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REPORT_STATUS_TRANSITION"));
    }

    private AdminReportResponse adminReportResponse(ReportStatus status, String action, String memo) {
        return new AdminReportResponse(
                "report-1",
                "user-uid",
                ReportTargetType.POST,
                "post-1",
                "author-1",
                "SPAM",
                "광고성 게시글입니다.",
                status,
                action,
                memo,
                LocalDateTime.of(2026, 3, 5, 12, 10),
                LocalDateTime.of(2026, 3, 5, 12, 20)
        );
    }

    private void mockToken(String token, boolean admin) {
        String uid = admin ? "admin-uid" : "user-uid";
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        admin ? "관리자" : "일반유저",
                        "https://example.com/profile.jpg"
                ));
        if (!admin) {
            when(memberRepository.findById(uid)).thenReturn(Optional.empty());
            return;
        }
        Member member = Member.create(uid, uid + "@sungkyul.ac.kr", "관리자", LocalDateTime.now());
        ReflectionTestUtils.setField(member, "isAdmin", true);
        when(memberRepository.findById(uid)).thenReturn(Optional.of(member));
    }
}
