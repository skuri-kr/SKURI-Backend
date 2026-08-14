package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.CreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.request.SendChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatReadUpdateResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatAccountData;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMemberId;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftAvatarService;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftBridgeOutboxService;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PartyMessageService partyMessageService;

    @Mock
    private ChatMessageOrderGenerator chatMessageOrderGenerator;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @Mock
    private MinecraftAvatarService minecraftAvatarService;

    @Mock
    private MinecraftBridgeOutboxService minecraftBridgeOutboxService;

    @Mock
    private MinecraftBridgeProperties minecraftBridgeProperties;

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        lenient().when(minecraftBridgeProperties.normalizedRoomId()).thenReturn("public:game:minecraft");
    }

    @Test
    void getChatRooms_다른학과공개방은_목록에서숨긴다() {
        Member member = activeMember("member-1", "컴퓨터공학과");
        ChatRoom universityRoom = ChatRoom.create("public:university", "성결대학교 전체 채팅방", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ChatRoom departmentRoom = ChatRoom.create("public:department:cs", "컴퓨터공학과 채팅방", ChatRoomType.DEPARTMENT, "컴퓨터공학과", null, null, true, null);
        ChatRoom otherDepartmentRoom = ChatRoom.create("public:department:law", "법학과 채팅방", ChatRoomType.DEPARTMENT, "법학과", null, null, true, null);

        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(chatRoomRepository.findAll()).thenReturn(List.of(universityRoom, departmentRoom, otherDepartmentRoom));
        when(chatRoomMemberRepository.findById_MemberId("member-1")).thenReturn(List.of());

        List<ChatRoomSummaryResponse> rooms = chatService.getChatRooms("member-1", null, null);

        assertEquals(2, rooms.size());
        assertEquals(List.of("public:university", "public:department:cs"), rooms.stream().map(ChatRoomSummaryResponse::id).toList());
        assertFalse(rooms.get(0).joined());
        verify(memberRepository, times(1)).findActiveById("member-1");
    }

    @Test
    void getChatRooms_legacy학과별칭은_DB추가조회없이canonical공개방과일치한다() {
        Member member = activeMember("member-1", "소프트웨어학과");
        ChatRoom departmentRoom = ChatRoom.create(
                "public:department:software",
                "미디어소프트웨어학과 채팅방",
                ChatRoomType.DEPARTMENT,
                "미디어소프트웨어학과",
                null,
                null,
                true,
                null
        );

        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(chatRoomRepository.findAll()).thenReturn(List.of(departmentRoom));
        when(chatRoomMemberRepository.findById_MemberId("member-1")).thenReturn(List.of());

        List<ChatRoomSummaryResponse> rooms = chatService.getChatRooms("member-1", null, null);

        assertEquals(List.of("public:department:software"), rooms.stream().map(ChatRoomSummaryResponse::id).toList());
        verify(memberRepository, times(1)).findActiveById("member-1");
    }

    @Test
    void getAdminPublicChatRooms_참여여부와무관하게_public_non_party만반환한다() {
        ChatRoom universityRoom = ChatRoom.create("public:university", "성결대학교 전체 채팅방", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ChatRoom departmentRoom = ChatRoom.create("public:department:law", "법학과 채팅방", ChatRoomType.DEPARTMENT, "법학과", null, null, true, null);
        ChatRoom customRoom = ChatRoom.create("room-1", "시험기간 밤샘 메이트", ChatRoomType.CUSTOM, null, null, null, true, null);
        ChatRoom privateRoom = ChatRoom.create("room-private", "비공개방", ChatRoomType.CUSTOM, null, null, null, false, null);
        ChatRoom partyRoom = ChatRoom.createPartyRoom("party-1");

        when(chatRoomRepository.findAll()).thenReturn(List.of(privateRoom, partyRoom, universityRoom, departmentRoom, customRoom));

        List<ChatRoomSummaryResponse> rooms = chatService.getAdminPublicChatRooms(null);

        assertEquals(
                Set.of("public:university", "public:department:law", "room-1"),
                Set.copyOf(rooms.stream().map(ChatRoomSummaryResponse::id).toList())
        );
        assertTrue(rooms.stream().allMatch(room -> !room.joined()));
        assertTrue(rooms.stream().allMatch(room -> room.unreadCount() == 0));
        assertTrue(rooms.stream().allMatch(room -> !room.isMuted()));
        verifyNoInteractions(chatRoomMemberRepository, memberRepository);
    }

    @Test
    void getChatRoomDetail_미참여공개방도_조회할수있다() {
        ChatRoom publicRoom = ChatRoom.create("public:university", "성결대학교 전체 채팅방", ChatRoomType.UNIVERSITY, null, "설명", null, true, null);
        ReflectionTestUtils.setField(publicRoom, "lastMessageTimestamp", LocalDateTime.of(2026, 3, 5, 21, 10, 0));
        ReflectionTestUtils.setField(publicRoom, "lastMessageText", "안녕하세요");
        ReflectionTestUtils.setField(publicRoom, "lastMessageSenderName", "홍길동");
        ReflectionTestUtils.setField(publicRoom, "lastMessageType", ChatMessageType.TEXT);

        when(chatRoomRepository.findById("public:university")).thenReturn(Optional.of(publicRoom));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("public:university", "member-1"))
                .thenReturn(Optional.empty());

        ChatRoomDetailResponse response = chatService.getChatRoomDetail("member-1", "public:university");

        assertFalse(response.joined());
        assertEquals(0, response.unreadCount());
        assertNotNull(response.lastMessage());
        assertEquals(LocalDateTime.of(2026, 3, 5, 21, 10, 0), response.lastMessageAt());
    }

    @Test
    void getMessages_미참여공개방이면_NOT_CHAT_ROOM_MEMBER() {
        ChatRoom publicRoom = ChatRoom.create("public:university", "성결대학교 전체 채팅방", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        when(chatRoomRepository.findById("public:university")).thenReturn(Optional.of(publicRoom));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("public:university", "member-1"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.getMessages("member-1", "public:university", null, null, 50)
        );

        assertEquals(ErrorCode.NOT_CHAT_ROOM_MEMBER, exception.getErrorCode());
    }

    @Test
    void getAdminPublicChatRoomMessages_멤버십없이조회가능하다() {
        ChatRoom publicRoom = ChatRoom.create("public:game:minecraft", "마인크래프트 채팅방", ChatRoomType.GAME, null, null, null, true, null);
        ChatMessage message = ChatMessage.create("public:game:minecraft", "sender-1", "운영팀", 2L, "관리 공지입니다.", ChatMessageType.SYSTEM, null, null);
        ReflectionTestUtils.setField(message, "id", "message-1");
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 4, 6, 10, 5, 0));

        when(chatRoomRepository.findById("public:game:minecraft")).thenReturn(Optional.of(publicRoom));
        when(chatMessageRepository.findByCursor("public:game:minecraft", null, null, null, PageRequest.of(0, 51)))
                .thenReturn(List.of(message));
        when(memberRepository.findAllById(List.of("sender-1")))
                .thenReturn(List.of(activeMember("sender-1", "컴퓨터공학과", null)));

        ChatMessagePageResponse response = chatService.getAdminPublicChatRoomMessages("public:game:minecraft", null, null, 50);

        assertEquals(1, response.messages().size());
        assertEquals("message-1", response.messages().get(0).id());
        verifyNoInteractions(chatRoomMemberRepository);
    }

    @Test
    void getAdminPartyChatMessages_멤버십없이조회가능하다() {
        ChatRoom partyRoom = ChatRoom.createPartyRoom("party-1");
        ChatMessage message = ChatMessage.create("party:party-1", "leader-1", "파티 리더", 3L, "정문 앞에서 만나요.", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(message, "id", "message-1");
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 4, 6, 10, 10, 0));

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(partyRoom));
        when(chatMessageRepository.findByCursor("party:party-1", null, null, null, PageRequest.of(0, 51)))
                .thenReturn(List.of(message));
        when(memberRepository.findAllById(List.of("leader-1")))
                .thenReturn(List.of(activeMember("leader-1", "컴퓨터공학과", "https://cdn.skuri.app/uploads/profiles/leader-1.jpg")));

        ChatMessagePageResponse response = chatService.getAdminPartyChatMessages("party:party-1", null, null, 50);

        assertEquals(1, response.messages().size());
        assertEquals("party:party-1", response.messages().get(0).chatRoomId());
        verifyNoInteractions(chatRoomMemberRepository);
    }

    @Test
    void getMessages_senderPhotoUrl은_membersPhotoUrl기준으로내려간다() {
        ChatRoom room = ChatRoom.create("room-1", "시험기간 밤샘 메이트", ChatRoomType.CUSTOM, null, null, null, true, null);
        ChatRoomMember membership = membership(room, "room-1", "member-1");
        ChatMessage messageWithPhoto = ChatMessage.create("room-1", "sender-1", "홍길동", 2L, "안녕하세요", ChatMessageType.TEXT, null, null);
        ChatMessage messageWithoutPhoto = ChatMessage.create("room-1", "sender-2", "김성결", 1L, "반가워요", ChatMessageType.TEXT, null, null);
        ReflectionTestUtils.setField(messageWithPhoto, "id", "message-2");
        ReflectionTestUtils.setField(messageWithPhoto, "createdAt", LocalDateTime.of(2026, 3, 5, 22, 10, 0));
        ReflectionTestUtils.setField(messageWithoutPhoto, "id", "message-1");
        ReflectionTestUtils.setField(messageWithoutPhoto, "createdAt", LocalDateTime.of(2026, 3, 5, 22, 9, 0));

        Member senderWithPhoto = activeMember("sender-1", "컴퓨터공학과", "https://cdn.skuri.app/uploads/profiles/sender-1.jpg");
        Member senderWithoutPhoto = activeMember("sender-2", "컴퓨터공학과", null);

        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1"))
                .thenReturn(Optional.of(membership));
        when(chatMessageRepository.findByCursor("room-1", null, null, null, PageRequest.of(0, 51)))
                .thenReturn(List.of(messageWithPhoto, messageWithoutPhoto));
        when(memberRepository.findAllById(List.of("sender-1", "sender-2")))
                .thenReturn(List.of(senderWithPhoto, senderWithoutPhoto));

        ChatMessagePageResponse response = chatService.getMessages("member-1", "room-1", null, null, 50);

        assertEquals(2, response.messages().size());
        assertEquals("https://cdn.skuri.app/uploads/profiles/sender-1.jpg", response.messages().get(0).senderPhotoUrl());
        assertNull(response.messages().get(1).senderPhotoUrl());
    }

    @Test
    void createMinecraftInboundMessage_같은SourceEventId면_기존메시지를재사용한다() {
        ChatMessage existingMessage = ChatMessage.create(
                "public:game:minecraft",
                "mc-sender",
                "skuriPlayer",
                10L,
                "안녕하세요!",
                ChatMessageType.TEXT,
                null,
                null
        );
        ReflectionTestUtils.setField(existingMessage, "id", "message-1");
        ReflectionTestUtils.setField(existingMessage, "createdAt", LocalDateTime.of(2026, 3, 30, 13, 20));
        existingMessage.markSource(ChatMessage.SOURCE_MINECRAFT);
        existingMessage.markMinecraftUuid("8667ba71b85a4004af54457a9734eed7");
        existingMessage.markSourceEventId("event-1");

        when(chatMessageRepository.findBySourceEventId("event-1")).thenReturn(Optional.of(existingMessage));

        ChatMessageResponse response = chatService.createMinecraftInboundMessage(
                "mc-sender",
                "skuriPlayer",
                "https://minotar.net/avatar/8667ba71b85a4004af54457a9734eed7/64",
                "안녕하세요!",
                ChatMessageType.TEXT,
                null,
                "8667ba71b85a4004af54457a9734eed7",
                "event-1"
        );

        assertEquals("message-1", response.id());
        verify(chatRoomRepository, times(0)).findById(anyString());
        verify(chatMessageRepository, times(0)).save(any(ChatMessage.class));
        verify(chatMessageRepository, times(0)).saveAndFlush(any(ChatMessage.class));
    }

    @Test
    void createChatRoom_생성자는_즉시참여상태가된다() {
        AtomicReference<ChatRoomMember> savedMemberRef = new AtomicReference<>();
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(activeMember("member-1", "컴퓨터공학과")));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> {
            ChatRoomMember member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", ChatRoomMemberId.of(member.getChatRoom().getId(), member.getMemberId()));
            savedMemberRef.set(member);
            return member;
        });
        when(chatRoomMemberRepository.findById_ChatRoomId(anyString())).thenAnswer(invocation -> List.of(savedMemberRef.get()));

        ChatRoomDetailResponse response = chatService.createChatRoom(
                "member-1",
                new CreateChatRoomRequest("시험기간 밤샘 메이트", "기말고사 기간 같이 공부할 사람들 모여요.")
        );

        assertEquals(ChatRoomType.CUSTOM, response.type());
        assertTrue(response.joined());
        assertEquals(1, response.memberCount());
        assertEquals("시험기간 밤샘 메이트", response.name());
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), any());
    }

    @Test
    void createChatRoom_활성회원이없으면_MEMBER_NOT_FOUND() {
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.createChatRoom(
                        "member-1",
                        new CreateChatRoomRequest("시험기간 밤샘 메이트", "기말고사 기간 같이 공부할 사람들 모여요.")
                )
        );

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void joinChatRoom_기존메시지가있으면_초기unread를0으로시작한다() {
        ChatRoom room = ChatRoom.create("room-1", "시험기간 밤샘 메이트", ChatRoomType.CUSTOM, null, null, null, true, null);
        ReflectionTestUtils.setField(room, "memberCount", 10);
        ReflectionTestUtils.setField(room, "lastMessageTimestamp", LocalDateTime.of(2026, 3, 5, 22, 0, 0));
        AtomicReference<ChatRoomMember> savedMemberRef = new AtomicReference<>();
        Member joinedMember = activeMember("member-1", "컴퓨터공학과", "https://cdn.skuri.app/uploads/profiles/member-1.jpg");

        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(joinedMember));
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1")).thenReturn(Optional.empty());
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> {
            ChatRoomMember member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", ChatRoomMemberId.of("room-1", "member-1"));
            savedMemberRef.set(member);
            return member;
        });
        when(chatMessageOrderGenerator.nextOrder()).thenReturn(42L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", "message-join-1");
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 3, 5, 22, 5, 0));
            return message;
        });
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("room-1")).thenAnswer(invocation -> List.of(savedMemberRef.get()));

        ChatRoomDetailResponse response = chatService.joinChatRoom("member-1", "room-1");

        assertTrue(response.joined());
        assertEquals(0, response.unreadCount());
        assertEquals(toInstant(LocalDateTime.of(2026, 3, 5, 22, 5, 0)), response.lastReadAt());
        assertEquals(11, response.memberCount());
        ArgumentCaptor<ChatMessage> joinMessageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(joinMessageCaptor.capture());
        assertEquals(Long.valueOf(42L), joinMessageCaptor.getValue().getMessageOrder());
        assertEquals(ChatMessageType.SYSTEM, joinMessageCaptor.getValue().getType());
        assertEquals(ChatMessage.SOURCE_MEMBER_JOIN, joinMessageCaptor.getValue().getSource());
        assertEquals("홍길동님이 입장했어요.", joinMessageCaptor.getValue().getText());
        ArgumentCaptor<ChatMessageResponse> joinPayloadCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/room-1"), joinPayloadCaptor.capture());
        assertEquals("https://cdn.skuri.app/uploads/profiles/member-1.jpg", joinPayloadCaptor.getValue().senderPhotoUrl());
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), any());
    }

    @Test
    void joinChatRoom_활성회원이없으면_MEMBER_NOT_FOUND() {
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.joinChatRoom("member-1", "room-1")
        );

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void leaveChatRoom_멤버십을제거하고_removed이벤트를보낸다() {
        ChatRoom room = ChatRoom.create("room-1", "시험기간 밤샘 메이트", ChatRoomType.CUSTOM, null, null, null, true, null);
        ReflectionTestUtils.setField(room, "memberCount", 2);
        ChatRoomMember membership = membership(room, "room-1", "member-1");
        ChatRoomMember remainingMember = membership(room, "room-1", "member-2");

        when(memberRepository.findActiveById("member-1"))
                .thenReturn(Optional.of(activeMember("member-1", "컴퓨터공학과", "https://cdn.skuri.app/uploads/profiles/member-1.jpg")));
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1")).thenReturn(Optional.of(membership));
        when(chatMessageOrderGenerator.nextOrder()).thenReturn(43L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", "message-leave-1");
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 3, 5, 22, 6, 0));
            return message;
        });
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("room-1")).thenReturn(List.of(remainingMember));

        ChatRoomDetailResponse response = chatService.leaveChatRoom("member-1", "room-1");

        assertFalse(response.joined());
        assertNull(response.lastReadAt());
        assertEquals(1, response.memberCount());
        verify(chatRoomMemberRepository).delete(membership);
        ArgumentCaptor<ChatMessage> leaveMessageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(leaveMessageCaptor.capture());
        assertEquals(Long.valueOf(43L), leaveMessageCaptor.getValue().getMessageOrder());
        assertEquals(ChatMessageType.SYSTEM, leaveMessageCaptor.getValue().getType());
        assertEquals(ChatMessage.SOURCE_MEMBER_LEAVE, leaveMessageCaptor.getValue().getSource());
        assertEquals("홍길동님이 나갔어요.", leaveMessageCaptor.getValue().getText());
        ArgumentCaptor<ChatMessageResponse> leavePayloadCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/room-1"), leavePayloadCaptor.capture());
        assertEquals("https://cdn.skuri.app/uploads/profiles/member-1.jpg", leavePayloadCaptor.getValue().senderPhotoUrl());
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/chat-rooms"), any());
    }

    @Test
    void removeMemberFromDepartmentChatRooms_학과방멤버십만정리한다() {
        ChatRoom departmentRoom = ChatRoom.create("public:department:cs", "컴퓨터공학과 채팅방", ChatRoomType.DEPARTMENT, "컴퓨터공학과", null, null, true, null);
        ReflectionTestUtils.setField(departmentRoom, "memberCount", 1);
        ChatRoom customRoom = ChatRoom.create("room-1", "시험기간 밤샘 메이트", ChatRoomType.CUSTOM, null, null, null, true, null);
        ReflectionTestUtils.setField(customRoom, "memberCount", 1);
        ChatRoomMember departmentMembership = membership(departmentRoom, "public:department:cs", "member-1");
        ChatRoomMember customMembership = membership(customRoom, "room-1", "member-1");

        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(activeMember("member-1", "컴퓨터공학과")));
        when(chatRoomMemberRepository.findById_MemberId("member-1")).thenReturn(List.of(departmentMembership, customMembership));
        when(chatMessageOrderGenerator.nextOrder()).thenReturn(44L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", "message-dept-leave-1");
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 3, 5, 22, 7, 0));
            return message;
        });
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("public:department:cs")).thenReturn(List.of());

        chatService.removeMemberFromDepartmentChatRooms("member-1");

        verify(chatRoomMemberRepository).delete(departmentMembership);
        verify(chatRoomMemberRepository, times(1)).delete(any(ChatRoomMember.class));
        ArgumentCaptor<ChatMessage> departmentLeaveCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(departmentLeaveCaptor.capture());
        assertEquals(ChatMessage.SOURCE_MEMBER_LEAVE, departmentLeaveCaptor.getValue().getSource());
        assertEquals("홍길동님이 나갔어요.", departmentLeaveCaptor.getValue().getText());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/public:department:cs"), any(ChatMessageResponse.class));
    }

    @Test
    void markAsRead_과거시각요청이면_단조증가유지() {
        ChatRoom room = ChatRoom.create("room-1", "테스트", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ChatRoomMember roomMember = membership(room, "room-1", "member-1");
        roomMember.advanceLastReadAt(LocalDateTime.of(2026, 3, 5, 21, 0, 0));

        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1"))
                .thenReturn(Optional.of(roomMember));

        ChatReadUpdateResponse response = chatService.markAsRead(
                "member-1",
                "room-1",
                toInstant(LocalDateTime.of(2026, 3, 5, 20, 59, 0))
        );

        assertFalse(response.updated());
        assertEquals(toInstant(LocalDateTime.of(2026, 3, 5, 21, 0, 0)), response.lastReadAt());
    }

    @Test
    void markAsRead_미래시각요청이면_마지막메시지시각으로보정한다() {
        ChatRoom room = ChatRoom.create("room-1", "테스트", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ReflectionTestUtils.setField(room, "lastMessageTimestamp", LocalDateTime.of(2026, 3, 5, 21, 30, 0));
        ChatRoomMember roomMember = membership(room, "room-1", "member-1");

        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1"))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatReadUpdateResponse response = chatService.markAsRead(
                "member-1",
                "room-1",
                Instant.parse("2099-03-05T12:00:00Z")
        );

        assertEquals(toInstant(LocalDateTime.of(2026, 3, 5, 21, 30, 0)), response.lastReadAt());
        assertTrue(response.updated());
    }

    @Test
    void markAsRead_UTC요청후_상세와목록재조회에도_unread가복원되지않는다() {
        ChatRoom room = ChatRoom.create("room-1", "테스트", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        LocalDateTime lastMessageAt = LocalDateTime.of(2026, 3, 5, 21, 30, 0);
        ReflectionTestUtils.setField(room, "lastMessageTimestamp", lastMessageAt);
        ReflectionTestUtils.setField(room, "messageCount", 5);
        ChatRoomMember roomMember = membership(room, "room-1", "member-1");
        Member member = activeMember("member-1", "컴퓨터공학과");

        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomRepository.findAll()).thenReturn(List.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1"))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findById_MemberId("member-1")).thenReturn(List.of(roomMember));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.countByChatRoomIdAndCreatedAtAfter("room-1", lastMessageAt)).thenReturn(0L);

        Instant readAt = Instant.parse("2026-03-05T12:30:00Z");

        ChatReadUpdateResponse updateResponse = chatService.markAsRead("member-1", "room-1", readAt);
        ChatRoomDetailResponse detailResponse = chatService.getChatRoomDetail("member-1", "room-1");
        List<ChatRoomSummaryResponse> summaryResponses = chatService.getChatRooms("member-1", null, null);

        assertTrue(updateResponse.updated());
        assertEquals(readAt, updateResponse.lastReadAt());
        assertEquals(0, detailResponse.unreadCount());
        assertEquals(readAt, detailResponse.lastReadAt());
        assertEquals(0, summaryResponses.get(0).unreadCount());
    }

    @Test
    void sendMessage_파티ACCOUNT타입이면_특수페이로드저장및브로드캐스트() {
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatRoomMember roomMember = membership(room, "party:party-1", "member-1");
        Member sender = activeMember("member-1", "컴퓨터공학과", "https://cdn.skuri.app/uploads/profiles/member-1.jpg");
        SendChatMessageRequest request = new SendChatMessageRequest(
                ChatMessageType.ACCOUNT,
                null,
                null,
                new SendChatMessageRequest.AccountPayload(
                        "카카오뱅크",
                        "3333-01-1234567",
                        "홍길동",
                        false,
                        false
                )
        );

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("party:party-1", "member-1"))
                .thenReturn(Optional.of(roomMember));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(sender));
        when(partyMessageService.buildClientPayload(eq("party:party-1"), eq("member-1"), eq(request)))
                .thenReturn(new PartySpecialMessagePayload(
                        "계좌 정보를 공유했어요. (카카오뱅크 3333-01-1234567)",
                        new ChatAccountData("카카오뱅크", "3333-01-1234567", "홍길동", false),
                        null
                ));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", "message-1");
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 3, 5, 21, 10, 0));
            return message;
        });
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1")).thenReturn(List.of(roomMember));

        ChatMessageResponse response = chatService.sendMessage(
                "party:party-1",
                "member-1",
                request
        );

        assertEquals(ChatMessageType.ACCOUNT, response.type());
        assertNotNull(response.accountData());
        assertEquals("카카오뱅크", response.accountData().bankName());
        assertEquals("https://cdn.skuri.app/uploads/profiles/member-1.jpg", response.senderPhotoUrl());
        ArgumentCaptor<ChatMessageResponse> sendPayloadCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/party:party-1"), sendPayloadCaptor.capture());
        assertEquals("https://cdn.skuri.app/uploads/profiles/member-1.jpg", sendPayloadCaptor.getValue().senderPhotoUrl());
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), any());
    }

    @Test
    void createPartySystemMessage_저장후_파티채팅히스토리와브로드캐스트에사용한다() {
        Party party = Party.create(
                "leader-1",
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(2),
                4,
                List.of("빠른출발"),
                "테스트"
        );
        ReflectionTestUtils.setField(party, "id", "party-1");
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatRoomMember leaderMember = membership(room, "party:party-1", "leader-1");
        Member leader = activeMember("leader-1", "컴퓨터공학과", "https://cdn.skuri.app/uploads/profiles/leader-1.jpg");

        when(memberRepository.findById("leader-1")).thenReturn(Optional.of(leader));
        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", "message-system-1");
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 3, 5, 21, 10, 0));
            return message;
        });
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1")).thenReturn(List.of(leaderMember));
        when(chatMessageOrderGenerator.nextOrder()).thenReturn(42L);

        ChatMessageResponse response = chatService.createPartySystemMessage(party, "leader-1", "모집이 마감되었어요.");

        assertEquals(ChatMessageType.SYSTEM, response.type());
        assertEquals("모집이 마감되었어요.", response.text());
        assertEquals("https://cdn.skuri.app/uploads/profiles/leader-1.jpg", response.senderPhotoUrl());
        verify(chatMessageRepository).save(argThat(message ->
                Long.valueOf(42L).equals(message.getMessageOrder())
                        && message.getType() == ChatMessageType.SYSTEM
        ));
        ArgumentCaptor<ChatMessageResponse> systemPayloadCaptor = ArgumentCaptor.forClass(ChatMessageResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/party:party-1"), systemPayloadCaptor.capture());
        assertEquals("https://cdn.skuri.app/uploads/profiles/leader-1.jpg", systemPayloadCaptor.getValue().senderPhotoUrl());
        verify(messagingTemplate).convertAndSendToUser(eq("leader-1"), eq("/queue/chat-rooms"), any());
        verify(eventPublisher).publish(argThat(event ->
                event instanceof com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent.ChatMessageCreated created
                        && created.chatRoomId().equals("party:party-1")
                        && created.messageId().equals("message-system-1")
        ));
    }

    @Test
    void createPartyChatRoom_파티멤버수만큼_chatRoomMember생성() {
        Party party = Party.create(
                "leader-1",
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(2),
                4,
                List.of("빠른출발"),
                "테스트"
        );
        party.addMember("member-2");
        ReflectionTestUtils.setField(party, "id", "party-1");

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1")).thenReturn(List.of());

        chatService.createPartyChatRoom(party);

        verify(chatRoomRepository, times(2)).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }

    @Test
    void syncPartyChatRoomMembers_파티탈퇴멤버는_채팅멤버에서제거된다() {
        Party party = Party.create(
                "leader-1",
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(2),
                4,
                List.of("빠른출발"),
                "테스트"
        );
        ReflectionTestUtils.setField(party, "id", "party-1");

        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatRoomMember leaderMember = membership(room, "party:party-1", "leader-1");
        ChatRoomMember removedMember = membership(room, "party:party-1", "member-2");

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1")).thenReturn(List.of(leaderMember, removedMember));

        chatService.syncPartyChatRoomMembers(party);

        verify(chatRoomMemberRepository).delete(removedMember);
        verify(chatRoomRepository).save(room);
        assertEquals(1, room.getMemberCount());
    }

    private Member activeMember(String memberId, String department) {
        return activeMember(memberId, department, null);
    }

    private Member activeMember(String memberId, String department, String photoUrl) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", "홍길동", LocalDateTime.now().minusDays(1));
        member.updateProfile("홍길동", null, department, photoUrl);
        return member;
    }

    private ChatRoomMember membership(ChatRoom room, String chatRoomId, String memberId) {
        ChatRoomMember membership = ChatRoomMember.create(room, memberId, LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(membership, "id", ChatRoomMemberId.of(chatRoomId, memberId));
        return membership;
    }

    private Instant toInstant(LocalDateTime value) {
        return value.atZone(ZoneId.of("Asia/Seoul")).toInstant();
    }
}
