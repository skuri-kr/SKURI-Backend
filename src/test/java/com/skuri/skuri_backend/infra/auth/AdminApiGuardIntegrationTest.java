package com.skuri.skuri_backend.infra.auth;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.academic.controller.AcademicScheduleAdminController;
import com.skuri.skuri_backend.domain.academic.controller.CourseAdminController;
import com.skuri.skuri_backend.domain.academic.dto.response.AcademicScheduleResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkAcademicSchedulesResponse;
import com.skuri.skuri_backend.domain.academic.dto.response.AdminBulkCoursesResponse;
import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import com.skuri.skuri_backend.domain.academic.service.AcademicScheduleService;
import com.skuri.skuri_backend.domain.academic.service.CourseService;
import com.skuri.skuri_backend.domain.admin.dashboard.controller.AdminDashboardController;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardSummaryResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.service.AdminDashboardService;
import com.skuri.skuri_backend.domain.app.controller.AppNoticeAdminController;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeCreateResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.board.controller.BoardAdminController;
import com.skuri.skuri_backend.domain.board.dto.response.AdminPostSummaryResponse;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.service.BoardAdminService;
import com.skuri.skuri_backend.domain.campus.controller.CampusBannerAdminController;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerAdminResponse;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionTarget;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerActionType;
import com.skuri.skuri_backend.domain.campus.entity.CampusBannerPaletteKey;
import com.skuri.skuri_backend.domain.campus.service.CampusBannerService;
import com.skuri.skuri_backend.domain.chat.controller.ChatAdminRoomController;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminCreateChatRoomResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.service.ChatAdminService;
import com.skuri.skuri_backend.domain.member.controller.MemberAdminController;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberDetailResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberBankAccountResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberNotificationSettingResponse;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.member.service.MemberAdminService;
import com.skuri.skuri_backend.domain.notice.controller.NoticeAdminController;
import com.skuri.skuri_backend.domain.notice.dto.response.NoticeSyncResponse;
import com.skuri.skuri_backend.domain.notice.service.NoticeSyncService;
import com.skuri.skuri_backend.domain.support.controller.AppVersionAdminController;
import com.skuri.skuri_backend.domain.support.controller.CafeteriaMenuAdminController;
import com.skuri.skuri_backend.domain.support.controller.InquiryAdminController;
import com.skuri.skuri_backend.domain.support.controller.ReportAdminController;
import com.skuri.skuri_backend.domain.support.dto.response.AdminInquiryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.AdminReportResponse;
import com.skuri.skuri_backend.domain.support.dto.response.AppVersionAdminUpdateResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuCategoryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuEntryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.entity.InquiryAttachment;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.service.AppVersionService;
import com.skuri.skuri_backend.domain.support.service.CafeteriaMenuService;
import com.skuri.skuri_backend.domain.support.service.InquiryService;
import com.skuri.skuri_backend.domain.support.service.ReportService;
import com.skuri.skuri_backend.domain.taxiparty.controller.PartyAdminController;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyJoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyAdminService;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AcademicScheduleAdminController.class,
        CourseAdminController.class,
        AdminDashboardController.class,
        ChatAdminRoomController.class,
        BoardAdminController.class,
        NoticeAdminController.class,
        AppNoticeAdminController.class,
        CampusBannerAdminController.class,
        MemberAdminController.class,
        InquiryAdminController.class,
        ReportAdminController.class,
        AppVersionAdminController.class,
        CafeteriaMenuAdminController.class,
        PartyAdminController.class
})
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminApiGuardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcademicScheduleService academicScheduleService;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private ChatAdminService chatAdminService;

    @MockitoBean
    private BoardAdminService boardAdminService;

    @MockitoBean
    private NoticeSyncService noticeSyncService;

    @MockitoBean
    private AppNoticeService appNoticeService;

    @MockitoBean
    private CampusBannerService campusBannerService;

    @MockitoBean
    private MemberAdminService memberAdminService;

    @MockitoBean
    private InquiryService inquiryService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private AppVersionService appVersionService;

    @MockitoBean
    private CafeteriaMenuService cafeteriaMenuService;

    @MockitoBean
    private TaxiPartyAdminService taxiPartyAdminService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @ParameterizedTest(name = "[401] {0}")
    @MethodSource("adminEndpoints")
    void adminApi_미인증요청_401(String name, AdminEndpointCase endpointCase) throws Exception {
        mockMvc.perform(endpointCase.requestBuilder().get())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @ParameterizedTest(name = "[403] {0}")
    @MethodSource("adminEndpoints")
    void adminApi_비관리자요청_403_ADMIN_REQUIRED(String name, AdminEndpointCase endpointCase) throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(endpointCase.requestBuilder().get().header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @ParameterizedTest(name = "[2xx] {0}")
    @MethodSource("adminEndpoints")
    void adminApi_관리자요청_정상처리(String name, AdminEndpointCase endpointCase) throws Exception {
        mockToken("admin-token", true);
        endpointCase.stubber().run();

        mockMvc.perform(endpointCase.requestBuilder().get().header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(endpointCase.statusMatcher())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(endpointCase.extraMatcher());
    }

    private Stream<org.junit.jupiter.params.provider.Arguments> adminEndpoints() {
        return Stream.of(
                endpoint(
                        "dashboard summary",
                        () -> get("/v1/admin/dashboard/summary"),
                        () -> when(adminDashboardService.getSummary())
                                .thenReturn(new AdminDashboardSummaryResponse(
                                        12,
                                        4831,
                                        4,
                                        17,
                                        9,
                                        3,
                                        LocalDateTime.of(2026, 3, 29, 18, 0)
                                )),
                        status().isOk(),
                        jsonPath("$.data.totalMembers").value(4831)
                ),
                endpoint(
                        "academic schedule create",
                        () -> post("/v1/admin/academic-schedules")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "중간고사",
                                          "startDate": "2026-04-15",
                                          "endDate": "2026-04-21",
                                          "type": "MULTI",
                                          "isPrimary": true,
                                          "description": "2026학년도 1학기 중간고사"
                                        }
                                        """),
                        () -> when(academicScheduleService.createSchedule(any()))
                                .thenReturn(new AcademicScheduleResponse(
                                        "schedule-1",
                                        "중간고사",
                                        LocalDate.of(2026, 4, 15),
                                        LocalDate.of(2026, 4, 21),
                                        AcademicScheduleType.MULTI,
                                        true,
                                        "2026학년도 1학기 중간고사"
                                )),
                        status().isCreated(),
                        jsonPath("$.data.id").value("schedule-1")
                ),
                endpoint(
                        "academic schedule bulk sync",
                        () -> put("/v1/admin/academic-schedules/bulk")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "scopeStartDate": "2026-03-01",
                                          "scopeEndDate": "2027-02-28",
                                          "schedules": [
                                            {
                                              "title": "입학식 / 개강",
                                              "startDate": "2026-03-03",
                                              "endDate": "2026-03-03",
                                              "type": "single",
                                              "description": "정상수업",
                                              "isPrimary": true
                                            }
                                          ]
                                        }
                                        """),
                        () -> when(academicScheduleService.bulkSyncSchedules(any()))
                                .thenReturn(new AdminBulkAcademicSchedulesResponse(
                                        LocalDate.of(2026, 3, 1),
                                        LocalDate.of(2027, 2, 28),
                                        12,
                                        37,
                                        5
                                )),
                        status().isOk(),
                        jsonPath("$.data.created").value(12)
                ),
                endpoint(
                        "course delete",
                        () -> delete("/v1/admin/courses").param("semester", "2026-1"),
                        () -> when(courseService.deleteSemesterCourses("2026-1"))
                                .thenReturn(new AdminBulkCoursesResponse("2026-1", 0, 0, 120)),
                        status().isOk(),
                        jsonPath("$.data.semester").value("2026-1")
                ),
                endpoint(
                        "chat room create",
                        () -> post("/v1/admin/chat-rooms")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "운영 채팅방",
                                          "type": "CUSTOM",
                                          "description": "운영 공지 채팅방",
                                          "isPublic": true
                                        }
                                        """),
                        () -> when(chatAdminService.createPublicChatRoom(eq("admin-uid"), any()))
                                .thenReturn(new AdminCreateChatRoomResponse("room-1", "운영 채팅방", ChatRoomType.CUSTOM)),
                        status().isCreated(),
                        jsonPath("$.data.id").value("room-1")
                ),
                endpoint(
                        "board posts list",
                        () -> get("/v1/admin/posts"),
                        () -> when(boardAdminService.getAdminPosts(null, null, null, null, 0, 20))
                                .thenReturn(PageResponse.<AdminPostSummaryResponse>builder()
                                        .content(java.util.List.of(new AdminPostSummaryResponse(
                                                "post-1",
                                                PostCategory.GENERAL,
                                                "관리 대상 게시글",
                                                "member-1",
                                                "스쿠리유저",
                                                "홍길동",
                                                false,
                                                5,
                                                10,
                                                LocalDateTime.of(2026, 3, 29, 12, 0),
                                                LocalDateTime.of(2026, 3, 29, 12, 30),
                                                com.skuri.skuri_backend.domain.board.constant.BoardModerationStatus.VISIBLE,
                                                "https://example.com/post-1-thumb.jpg"
                                        )))
                                        .page(0)
                                        .size(20)
                                        .totalElements(1)
                                        .totalPages(1)
                                        .hasNext(false)
                                        .hasPrevious(false)
                                        .build()),
                        status().isOk(),
                        jsonPath("$.data.content[0].id").value("post-1")
                ),
                endpoint(
                        "notice sync",
                        () -> post("/v1/admin/notices/sync"),
                        () -> when(noticeSyncService.syncManually())
                                .thenReturn(new NoticeSyncResponse(3, 1, 8, 0, LocalDateTime.of(2026, 3, 10, 10, 0))),
                        status().isOk(),
                        jsonPath("$.data.created").value(3)
                ),
                endpoint(
                        "app notice create",
                        () -> post("/v1/admin/app-notices")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "서버 점검 안내",
                                          "content": "2월 20일 새벽 2시~4시 서버 점검이 있습니다.",
                                          "category": "MAINTENANCE",
                                          "priority": "HIGH",
                                          "imageUrls": [],
                                          "actionUrl": null,
                                          "publishedAt": "2026-02-20T00:00:00"
                                        }
                                        """),
                        () -> when(appNoticeService.createAppNotice(any()))
                                .thenReturn(new AppNoticeCreateResponse("app-notice-1", "서버 점검 안내", LocalDateTime.of(2026, 2, 19, 12, 0))),
                        status().isCreated(),
                        jsonPath("$.data.id").value("app-notice-1")
                ),
                endpoint(
                        "campus banner create",
                        () -> post("/v1/admin/campus-banners")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "badgeLabel": "택시 파티",
                                          "titleLabel": "택시 동승 매칭",
                                          "descriptionLabel": "같은 방향 가는 학생과 택시비를 함께 나눠요",
                                          "buttonLabel": "파티 찾기",
                                          "paletteKey": "GREEN",
                                          "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                                          "actionType": "IN_APP",
                                          "actionTarget": "TAXI_MAIN",
                                          "actionParams": null,
                                          "actionUrl": null,
                                          "isActive": true,
                                          "displayStartAt": "2026-03-25T00:00:00",
                                          "displayEndAt": null
                                        }
                                        """),
                        () -> when(campusBannerService.createBanner(any()))
                                .thenReturn(new CampusBannerAdminResponse(
                                        "campus-banner-1",
                                        "택시 파티",
                                        "택시 동승 매칭",
                                        "같은 방향 가는 학생과 택시비를 함께 나눠요",
                                        "파티 찾기",
                                        CampusBannerPaletteKey.GREEN,
                                        "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                                        CampusBannerActionType.IN_APP,
                                        CampusBannerActionTarget.TAXI_MAIN,
                                        null,
                                        null,
                                        true,
                                        LocalDateTime.of(2026, 3, 25, 0, 0),
                                        null,
                                        1,
                                        LocalDateTime.of(2026, 3, 25, 10, 0),
                                        LocalDateTime.of(2026, 3, 25, 10, 0)
                                )),
                        status().isCreated(),
                        jsonPath("$.data.id").value("campus-banner-1")
                ),
                endpoint(
                        "inquiry list",
                        AdminApiGuardIntegrationTest::inquiryListRequest,
                        () -> when(inquiryService.getAdminInquiries(null, 0, 20))
                                .thenReturn(PageResponse.<AdminInquiryResponse>builder()
                                        .content(java.util.List.of(new AdminInquiryResponse(
                                                "inquiry-1",
                                                "member-1",
                                                InquiryType.BUG,
                                                "채팅 오류",
                                                "앱이 종료됩니다.",
                                                InquiryStatus.PENDING,
                                                java.util.List.of(new InquiryAttachment(
                                                        "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image.jpg",
                                                        "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image_thumb.jpg",
                                                        800,
                                                        600,
                                                        245123,
                                                        "image/jpeg"
                                                )),
                                                null,
                                                "user@sungkyul.ac.kr",
                                                "스쿠리유저",
                                                "홍길동",
                                                "20201234",
                                                LocalDateTime.of(2026, 3, 10, 9, 0),
                                                LocalDateTime.of(2026, 3, 10, 9, 0)
                                        )))
                                        .page(0)
                                        .size(20)
                                        .totalElements(1)
                                        .totalPages(1)
                                        .hasNext(false)
                                        .hasPrevious(false)
                                        .build()),
                        status().isOk(),
                        jsonPath("$.data.content[0].id").value("inquiry-1")
                ),
                endpoint(
                        "member admin role update",
                        () -> patch("/v1/admin/members/member-1/admin-role")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "isAdmin": true
                                        }
                                        """),
                        () -> when(memberAdminService.updateAdminRole(eq("admin-uid"), eq("member-1"), any()))
                                .thenReturn(new AdminMemberDetailResponse(
                                        "member-1",
                                        "member-1@sungkyul.ac.kr",
                                        "스쿠리 유저",
                                        "홍길동",
                                        "2023112233",
                                        "컴퓨터공학과",
                                        "https://cdn.skuri.app/profiles/member-1.png",
                                        true,
                                        MemberStatus.ACTIVE,
                                        LocalDateTime.of(2025, 3, 1, 9, 0),
                                        LocalDateTime.of(2026, 3, 29, 10, 5),
                                        null,
                                        new MemberBankAccountResponse("신한은행", "110-123-456789", "홍길동", false),
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
                                                Map.of("academic", true, "event", false)
                                        )
                                )),
                        status().isOk(),
                        jsonPath("$.data.isAdmin").value(true)
                ),
                endpoint(
                        "report list",
                        () -> get("/v1/admin/reports"),
                        () -> when(reportService.getAdminReports(null, null, 0, 20))
                                .thenReturn(PageResponse.<AdminReportResponse>builder()
                                        .content(java.util.List.of(new AdminReportResponse(
                                                "report-1",
                                                "reporter-1",
                                                ReportTargetType.POST,
                                                "post-1",
                                                "author-1",
                                                "SPAM",
                                                "광고성 게시글입니다.",
                                                ReportStatus.PENDING,
                                                null,
                                                null,
                                                LocalDateTime.of(2026, 3, 10, 9, 10),
                                                LocalDateTime.of(2026, 3, 10, 9, 10)
                                        )))
                                        .page(0)
                                        .size(20)
                                        .totalElements(1)
                                        .totalPages(1)
                                        .hasNext(false)
                                        .hasPrevious(false)
                                        .build()),
                        status().isOk(),
                        jsonPath("$.data.content[0].id").value("report-1")
                ),
                endpoint(
                        "app version upsert",
                        () -> put("/v1/admin/app-versions/ios")
                                .contentType(APPLICATION_JSON)
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
                                        """),
                        () -> when(appVersionService.upsertAppVersion(eq("ios"), any()))
                                .thenReturn(new AppVersionAdminUpdateResponse("ios", "1.6.0", true, LocalDateTime.of(2026, 3, 10, 10, 0))),
                        status().isOk(),
                        jsonPath("$.data.platform").value("ios")
                ),
                endpoint(
                        "cafeteria menu create",
                        () -> post("/v1/admin/cafeteria-menus")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "weekId": "2026-W08",
                                          "weekStart": "2026-02-16",
                                          "weekEnd": "2026-02-20",
                                          "menus": {
                                            "2026-02-16": {
                                              "rollNoodles": ["우동", "김밥"]
                                            }
                                          }
                                        }
                                        """),
                        () -> when(cafeteriaMenuService.createMenu(any()))
                                .thenReturn(new CafeteriaMenuResponse(
                                        "2026-W08",
                                        LocalDate.of(2026, 2, 16),
                                        LocalDate.of(2026, 2, 20),
                                        Map.of("2026-02-16", Map.of("rollNoodles", java.util.List.of("우동", "김밥"))),
                                        java.util.List.of(
                                                new CafeteriaMenuCategoryResponse("rollNoodles", "Roll & Noodles"),
                                                new CafeteriaMenuCategoryResponse("theBab", "The bab"),
                                                new CafeteriaMenuCategoryResponse("fryRice", "Fry & Rice")
                                        ),
                                        Map.of(
                                                "2026-02-16",
                                                Map.of(
                                                        "rollNoodles",
                                                        java.util.List.of(new CafeteriaMenuEntryResponse(
                                                                "2026-W08.rollNoodles.8851f2731beef1f0",
                                                                "우동",
                                                                java.util.List.of(),
                                                                0,
                                                                0,
                                                                null
                                                        )),
                                                        "theBab",
                                                        java.util.List.of(),
                                                        "fryRice",
                                                        java.util.List.of()
                                                )
                                        )
                                )),
                        status().isCreated(),
                        jsonPath("$.data.weekId").value("2026-W08")
                ),
                endpoint(
                        "party list",
                        () -> get("/v1/admin/parties"),
                        () -> when(taxiPartyAdminService.getAdminParties(null, null, null, 0, 20))
                                .thenReturn(PageResponse.<AdminPartySummaryResponse>builder()
                                        .content(java.util.List.of(new AdminPartySummaryResponse(
                                                "party-1",
                                                PartyStatus.OPEN,
                                                "leader-1",
                                                "스쿠리 유저",
                                                "성결대학교 -> 안양역",
                                                LocalDateTime.of(2026, 3, 29, 18, 30),
                                                2,
                                                4,
                                                LocalDateTime.of(2026, 3, 29, 12, 0)
                                        )))
                                        .page(0)
                                        .size(20)
                                        .totalElements(1)
                                        .totalPages(1)
                                        .hasNext(false)
                                        .hasPrevious(false)
                                        .build()),
                        status().isOk(),
                        jsonPath("$.data.content[0].id").value("party-1")
                ),
                endpoint(
                        "party member removal",
                        () -> delete("/v1/admin/parties/party-1/members/member-1"),
                        () -> {
                        },
                        status().isOk(),
                        jsonPath("$.data").doesNotExist()
                ),
                endpoint(
                        "party system message create",
                        () -> post("/v1/admin/parties/party-1/messages/system")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "message": "관리자 안내 메시지"
                                        }
                                        """),
                        () -> when(taxiPartyAdminService.createPartySystemMessage("admin-uid", "party-1", "관리자 안내 메시지"))
                                .thenReturn(new ChatMessageResponse(
                                        "message-1",
                                        "party:party-1",
                                        "admin-uid",
                                        "관리자",
                                        null,
                                        ChatMessageType.SYSTEM,
                                        "관리자 안내 메시지",
                                        null,
                                        null,
                                        null,
                                        LocalDateTime.of(2026, 3, 29, 12, 10)
                                )),
                        status().isOk(),
                        jsonPath("$.data.id").value("message-1")
                ),
                endpoint(
                        "party join requests",
                        () -> get("/v1/admin/parties/party-1/join-requests"),
                        () -> when(taxiPartyAdminService.getPartyJoinRequests("party-1"))
                                .thenReturn(java.util.List.of(new AdminPartyJoinRequestResponse(
                                        "request-1",
                                        "member-1",
                                        "김철수",
                                        "김철수",
                                        null,
                                        "컴퓨터공학과",
                                        "20230001",
                                        LocalDateTime.of(2026, 3, 29, 12, 0)
                                ))),
                        status().isOk(),
                        jsonPath("$.data[0].requestId").value("request-1")
                )
        );
    }

    private static MockHttpServletRequestBuilder inquiryListRequest() {
        return get("/v1/admin/inquiries");
    }

    private org.junit.jupiter.params.provider.Arguments endpoint(
            String name,
            Supplier<MockHttpServletRequestBuilder> requestBuilder,
            Runnable stubber,
            ResultMatcher statusMatcher,
            ResultMatcher extraMatcher
    ) {
        AdminEndpointCase endpointCase = new AdminEndpointCase(requestBuilder, stubber, statusMatcher, extraMatcher);
        return org.junit.jupiter.params.provider.Arguments.of(Named.of(name, name), endpointCase);
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

    private record AdminEndpointCase(
            Supplier<MockHttpServletRequestBuilder> requestBuilder,
            Runnable stubber,
            ResultMatcher statusMatcher,
            ResultMatcher extraMatcher
    ) {
    }
}
