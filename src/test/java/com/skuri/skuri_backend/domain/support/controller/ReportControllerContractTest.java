package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.CreateReportRequest;
import com.skuri.skuri_backend.domain.support.dto.response.ReportCreateResponse;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class ReportControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void createReport_정상요청_201() throws Exception {
        mockUserToken("user-token");
        when(reportService.createReport(org.mockito.ArgumentMatchers.eq("user-uid"), any(CreateReportRequest.class)))
                .thenReturn(new ReportCreateResponse("report-1", ReportStatus.PENDING, LocalDateTime.of(2026, 3, 5, 12, 10)));

        mockMvc.perform(
                        post("/v1/reports")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "targetType": "POST",
                                          "targetId": "post_uuid",
                                          "category": "SPAM",
                                          "reason": "광고성 게시글입니다."
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("report-1"));
    }

    @Test
    void createReport_채팅메시지정상요청_201() throws Exception {
        mockUserToken("user-token");
        when(reportService.createReport(org.mockito.ArgumentMatchers.eq("user-uid"), any(CreateReportRequest.class)))
                .thenReturn(new ReportCreateResponse("report-2", ReportStatus.PENDING, LocalDateTime.of(2026, 3, 29, 12, 10)));

        mockMvc.perform(
                        post("/v1/reports")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "targetType": "CHAT_MESSAGE",
                                          "targetId": "message_uuid",
                                          "category": "SPAM",
                                          "reason": "광고성 메시지입니다."
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("report-2"));
    }

    @Test
    void createReport_공지댓글정상요청_201() throws Exception {
        mockUserToken("user-token");
        when(reportService.createReport(org.mockito.ArgumentMatchers.eq("user-uid"), any(CreateReportRequest.class)))
                .thenReturn(new ReportCreateResponse("report-3", ReportStatus.PENDING, LocalDateTime.of(2026, 8, 27, 12, 10)));

        mockMvc.perform(
                        post("/v1/reports")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "targetType": "NOTICE_COMMENT",
                                          "targetId": "notice_comment_uuid",
                                          "category": "ABUSE",
                                          "reason": "공지 댓글에 부적절한 표현이 있습니다."
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("report-3"));
    }

    @Test
    void createReport_토큰없음_401() throws Exception {
        mockMvc.perform(
                        post("/v1/reports")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "targetType": "POST",
                                          "targetId": "post_uuid",
                                          "category": "SPAM",
                                          "reason": "광고성 게시글입니다."
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void createReport_검증실패_422() throws Exception {
        mockUserToken("user-token");

        mockMvc.perform(
                        post("/v1/reports")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "targetType": null,
                                          "targetId": "",
                                          "category": "",
                                          "reason": ""
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createReport_중복신고_409() throws Exception {
        mockUserToken("user-token");
        when(reportService.createReport(org.mockito.ArgumentMatchers.eq("user-uid"), any(CreateReportRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.REPORT_ALREADY_SUBMITTED));

        mockMvc.perform(
                        post("/v1/reports")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "targetType": "POST",
                                          "targetId": "post_uuid",
                                          "category": "SPAM",
                                          "reason": "광고성 게시글입니다."
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REPORT_ALREADY_SUBMITTED"));
    }

    @Test
    void createReport_자기자신신고_400() throws Exception {
        mockUserToken("user-token");
        when(reportService.createReport(org.mockito.ArgumentMatchers.eq("user-uid"), any(CreateReportRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.CANNOT_REPORT_YOURSELF));

        mockMvc.perform(
                        post("/v1/reports")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "targetType": "MEMBER",
                                          "targetId": "user-uid",
                                          "category": "ABUSE",
                                          "reason": "자기 자신 테스트"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_REPORT_YOURSELF"));
    }

    @Test
    void createReport_채팅메시지없음_404() throws Exception {
        mockUserToken("user-token");
        when(reportService.createReport(org.mockito.ArgumentMatchers.eq("user-uid"), any(CreateReportRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        mockMvc.perform(
                        post("/v1/reports")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "targetType": "CHAT_MESSAGE",
                                          "targetId": "missing_message",
                                          "category": "SPAM",
                                          "reason": "광고성 메시지입니다."
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAT_MESSAGE_NOT_FOUND"));
    }

    @Test
    void createReport_공지댓글없음_404() throws Exception {
        mockUserToken("user-token");
        when(reportService.createReport(org.mockito.ArgumentMatchers.eq("user-uid"), any(CreateReportRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.NOTICE_COMMENT_NOT_FOUND));

        mockMvc.perform(
                        post("/v1/reports")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "targetType": "NOTICE_COMMENT",
                                          "targetId": "missing_notice_comment",
                                          "category": "ABUSE",
                                          "reason": "공지 댓글을 찾을 수 없습니다."
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTICE_COMMENT_NOT_FOUND"));
    }

    private void mockUserToken(String token) {
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        "user-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }
}
