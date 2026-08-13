package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.domain.support.dto.request.CreateInquiryRequest;
import com.skuri.skuri_backend.domain.support.dto.response.InquiryCreateResponse;
import com.skuri.skuri_backend.domain.support.dto.response.InquiryResponse;
import com.skuri.skuri_backend.domain.support.entity.InquiryAttachment;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.service.InquiryService;
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

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InquiryController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class InquiryControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InquiryService inquiryService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void createInquiry_정상요청_201() throws Exception {
        mockUserToken("user-token");
        when(inquiryService.createInquiry(any(), any(CreateInquiryRequest.class)))
                .thenReturn(new InquiryCreateResponse("inquiry-1", InquiryStatus.PENDING, LocalDateTime.of(2026, 2, 3, 12, 0)));

        mockMvc.perform(
                        post("/v1/inquiries")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "type": "BUG",
                                          "subject": "앱 오류 문의",
                                          "content": "채팅 화면에서 오류가 발생합니다."
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("inquiry-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createInquiry_attachmentsNull허용_201() throws Exception {
        mockUserToken("user-token");
        when(inquiryService.createInquiry(any(), any(CreateInquiryRequest.class)))
                .thenReturn(new InquiryCreateResponse("inquiry-1", InquiryStatus.PENDING, LocalDateTime.of(2026, 2, 3, 12, 0)));

        mockMvc.perform(
                        post("/v1/inquiries")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "type": "BUG",
                                          "subject": "앱 오류 문의",
                                          "content": "채팅 화면에서 오류가 발생합니다.",
                                          "attachments": null
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("inquiry-1"));
    }

    @Test
    void createInquiry_토큰없음_401() throws Exception {
        mockMvc.perform(
                        post("/v1/inquiries")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "type": "BUG",
                                          "subject": "앱 오류 문의",
                                          "content": "채팅 화면에서 오류가 발생합니다."
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void createInquiry_검증실패_422() throws Exception {
        mockUserToken("user-token");

        mockMvc.perform(
                        post("/v1/inquiries")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "type": null,
                                          "subject": "",
                                          "content": ""
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createInquiry_첨부구조검증실패_422() throws Exception {
        mockUserToken("user-token");

        mockMvc.perform(
                        post("/v1/inquiries")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "type": "BUG",
                                          "subject": "앱 오류 문의",
                                          "content": "채팅 화면에서 오류가 발생합니다.",
                                          "attachments": [
                                            {
                                              "url": "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image.jpg",
                                              "thumbUrl": "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image_thumb.jpg",
                                              "width": 800,
                                              "height": 600,
                                              "size": 245123,
                                              "mime": "application/pdf"
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void getMyInquiries_정상요청_200() throws Exception {
        mockUserToken("user-token");
        when(inquiryService.getMyInquiries("user-uid"))
                .thenReturn(List.of(new InquiryResponse(
                        "inquiry-1",
                        InquiryType.BUG,
                        "앱 오류 문의",
                        "채팅 화면에서 오류가 발생합니다.",
                        InquiryStatus.PENDING,
                        "재현 후 수정 배포 완료",
                        List.of(new InquiryAttachment(
                                "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image.jpg",
                                "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image_thumb.jpg",
                                800,
                                600,
                                245123,
                                "image/jpeg"
                        )),
                        LocalDateTime.of(2026, 2, 3, 12, 0),
                        LocalDateTime.of(2026, 2, 3, 12, 0)
                )));

        mockMvc.perform(get("/v1/inquiries/my").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("inquiry-1"))
                .andExpect(jsonPath("$.data[0].answer").value("재현 후 수정 배포 완료"))
                .andExpect(jsonPath("$.data[0].attachments[0].mime").value("image/jpeg"));
    }

    @Test
    void getMyInquiries_답변없음_null로응답한다() throws Exception {
        mockUserToken("user-token");
        when(inquiryService.getMyInquiries("user-uid"))
                .thenReturn(List.of(new InquiryResponse(
                        "inquiry-1",
                        InquiryType.BUG,
                        "앱 오류 문의",
                        "채팅 화면에서 오류가 발생합니다.",
                        InquiryStatus.PENDING,
                        null,
                        List.of(),
                        LocalDateTime.of(2026, 2, 3, 12, 0),
                        LocalDateTime.of(2026, 2, 3, 12, 0)
                )));

        mockMvc.perform(get("/v1/inquiries/my").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].answer").value(nullValue()));
    }

    @Test
    void getMyInquiries_도메인불일치_403() throws Exception {
        when(firebaseTokenVerifier.verify("invalid-domain-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "user-uid",
                        "user@gmail.com",
                        "google.com",
                        "provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));

        mockMvc.perform(get("/v1/inquiries/my").header(AUTHORIZATION, "Bearer invalid-domain-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_DOMAIN_RESTRICTED"));
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
