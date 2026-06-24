package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminChatRoomMemberResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminCreateChatRoomResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomLastMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.service.ChatAdminService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatAdminRoomController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class ChatAdminRoomControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatAdminService chatAdminService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void getPublicChatRooms_관리자요청_200() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.getPublicChatRooms(ChatRoomType.GAME))
                .thenReturn(List.of(new ChatRoomSummaryResponse(
                        "public:game:minecraft",
                        ChatRoomType.GAME,
                        "마인크래프트 채팅방",
                        "스쿠리 서버 채팅방입니다.",
                        true,
                        87,
                        false,
                        0,
                        new ChatRoomLastMessageResponse("TEXT", "오늘 이벤트 있어요.", "운영팀", LocalDateTime.of(2026, 4, 6, 10, 0)),
                        LocalDateTime.of(2026, 4, 6, 10, 0),
                        false
                )));

        mockMvc.perform(
                        get("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("type", "GAME")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("public:game:minecraft"))
                .andExpect(jsonPath("$.data[0].joined").value(false))
                .andExpect(jsonPath("$.data[0].unreadCount").value(0))
                .andExpect(jsonPath("$.data[0].isMuted").value(false));
    }

    @Test
    void getPublicChatRoom_관리자요청_200() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.getPublicChatRoomDetail("public:game:minecraft"))
                .thenReturn(new ChatRoomDetailResponse(
                        "public:game:minecraft",
                        ChatRoomType.GAME,
                        "마인크래프트 채팅방",
                        "스쿠리 서버 채팅방입니다.",
                        true,
                        87,
                        false,
                        0,
                        new ChatRoomLastMessageResponse("TEXT", "오늘 이벤트 있어요.", "운영팀", LocalDateTime.of(2026, 4, 6, 10, 0)),
                        LocalDateTime.of(2026, 4, 6, 10, 0),
                        false,
                        null
                ));

        mockMvc.perform(
                        get("/v1/admin/chat-rooms/public:game:minecraft")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("public:game:minecraft"))
                .andExpect(jsonPath("$.data.joined").value(false))
                .andExpect(jsonPath("$.data.lastReadAt").doesNotExist());
    }

    @Test
    void getPublicChatRoom_채팅방없음_404() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.getPublicChatRoomDetail("party:party-1"))
                .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        mockMvc.perform(
                        get("/v1/admin/chat-rooms/party:party-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    void getPublicChatRoomMembers_관리자요청_200() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.getPublicChatRoomMembers("public:game:minecraft"))
                .thenReturn(List.of(new AdminChatRoomMemberResponse(
                        "member-1",
                        "member1@sungkyul.ac.kr",
                        "스쿠리 유저",
                        "홍길동",
                        "2023112233",
                        "컴퓨터공학과",
                        "https://example.com/member-1.jpg",
                        Instant.parse("2026-03-05T12:00:00Z"),
                        Instant.parse("2026-03-05T12:10:00Z"),
                        false,
                        MemberStatus.ACTIVE
                )));

        mockMvc.perform(
                        get("/v1/admin/chat-rooms/public:game:minecraft/members")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].memberId").value("member-1"))
                .andExpect(jsonPath("$.data[0].nickname").value("스쿠리 유저"))
                .andExpect(jsonPath("$.data[0].department").value("컴퓨터공학과"))
                .andExpect(jsonPath("$.data[0].joinedAt").value("2026-03-05T12:00:00Z"))
                .andExpect(jsonPath("$.data[0].muted").value(false))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }

    @Test
    void getPublicChatRoomMembers_채팅방없음_404() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.getPublicChatRoomMembers("party:party-1"))
                .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        mockMvc.perform(
                        get("/v1/admin/chat-rooms/party:party-1/members")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    void getPublicChatRoomMembers_비관리자요청_403_ADMIN_REQUIRED() throws Exception {
        mockToken("user-token", "user-uid", false);

        mockMvc.perform(
                        get("/v1/admin/chat-rooms/public:game:minecraft/members")
                                .header(AUTHORIZATION, "Bearer user-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void getPublicChatRoomMessages_관리자요청_200() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.getPublicChatRoomMessages("public:game:minecraft", null, null, 50))
                .thenReturn(new ChatMessagePageResponse(
                        List.of(new ChatMessageResponse(
                                "message-1",
                                "public:game:minecraft",
                                "system",
                                "운영팀",
                                null,
                                ChatMessageType.SYSTEM,
                                "관리 공지입니다.",
                                null,
                                null,
                                null,
                                LocalDateTime.of(2026, 4, 6, 10, 5)
                        )),
                        false,
                        null
                ));

        mockMvc.perform(
                        get("/v1/admin/chat-rooms/public:game:minecraft/messages")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("size", "50")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].id").value("message-1"))
                .andExpect(jsonPath("$.data.messages[0].senderName").value("운영팀"))
                .andExpect(jsonPath("$.data.messages[0].senderPhotoUrl").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void getPublicChatRoomMessages_커서쌍불일치_422() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.getPublicChatRoomMessages("public:game:minecraft", null, "message-1", null))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "cursorCreatedAt와 cursorId는 함께 전달해야 합니다."));

        mockMvc.perform(
                        get("/v1/admin/chat-rooms/public:game:minecraft/messages")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .param("cursorId", "message-1")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("cursorCreatedAt와 cursorId는 함께 전달해야 합니다."));
    }

    @Test
    void createPublicChatRoom_관리자요청_201() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.createPublicChatRoom(eq("admin-uid"), any()))
                .thenReturn(new AdminCreateChatRoomResponse(
                        "room:1",
                        "성결대 전체 채팅방",
                        ChatRoomType.UNIVERSITY
                ));

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"성결대 전체 채팅방\",\"type\":\"UNIVERSITY\",\"description\":\"설명\",\"isPublic\":true}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("room:1"));
    }

    @Test
    void createPublicChatRoom_비관리자요청_403_ADMIN_REQUIRED() throws Exception {
        mockToken("user-token", "user-uid", false);

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"일반 채팅방\",\"type\":\"CUSTOM\",\"isPublic\":true}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void createPublicChatRoom_요청검증실패_422() throws Exception {
        mockToken("admin-token", "admin-uid", true);

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"\",\"type\":\"UNIVERSITY\",\"isPublic\":true}")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createPublicChatRoom_party타입요청_400() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.createPublicChatRoom(eq("admin-uid"), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "PARTY 타입 채팅방은 파티 생성 시 자동 생성됩니다."));

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"파티 채팅방\",\"type\":\"PARTY\",\"isPublic\":true}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void deletePublicChatRoom_채팅방없음_404() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        doThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                .when(chatAdminService)
                .deletePublicChatRoom("unknown-room");

        mockMvc.perform(
                        delete("/v1/admin/chat-rooms/unknown-room")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    void deletePublicChatRoom_관리자요청_200() throws Exception {
        mockToken("admin-token", "admin-uid", true);

        mockMvc.perform(
                        delete("/v1/admin/chat-rooms/room-public")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"성결대 전체 채팅방\",\"type\":\"UNIVERSITY\",\"isPublic\":true}")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(chatAdminService);
    }

    private void mockToken(String token, String uid, boolean isAdmin) {
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
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
}
