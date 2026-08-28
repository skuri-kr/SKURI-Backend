package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.academic.entity.AcademicSchedule;
import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentRepository;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeComment;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostInteractionRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
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
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementAccountSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private JoinRequestRepository joinRequestRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostInteractionRepository postInteractionRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private NoticeCommentRepository noticeCommentRepository;
    @Mock
    private AppNoticeRepository appNoticeRepository;
    @Mock
    private AppNoticeCommentRepository appNoticeCommentRepository;
    @Mock
    private AcademicScheduleRepository academicScheduleRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private FriendNotificationDeliveryService friendNotificationDeliveryService;
    @Mock
    private InvitationNotificationDeliveryService invitationNotificationDeliveryService;

    @InjectMocks
    private NotificationEventHandler notificationEventHandler;

    @Test
    void handlePartyCreated_partyNotifications켜진사용자만수신한다() {
        Party party = Party.create(
                "leader-1",
                Location.of("정문", null, null),
                Location.of("역", null, null),
                LocalDateTime.of(2026, 3, 8, 18, 30),
                4,
                List.of("역"),
                "상세"
        );
        ReflectionTestUtils.setField(party, "id", "party-1");

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findPartyNotificationRecipientIdsExcluding("leader-1"))
                .thenReturn(List.of("member-1"));

        notificationEventHandler.handle(new NotificationDomainEvent.PartyCreated("party-1"));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        verify(pushNotificationService).send(captor.getValue());
        assertEquals(NotificationType.PARTY_CREATED, captor.getValue().type());
        assertEquals(List.of("member-1"), captor.getValue().recipientIds().stream().toList());
        assertEquals("정문 → 역 택시 파티 등장", captor.getValue().title());
        assertFalse(captor.getValue().inboxEnabled());
    }

    @Test
    void handleFriendRequestCreated_전용전달서비스에위임한다() {
        notificationEventHandler.handle(new NotificationDomainEvent.FriendRequestCreated("friend-request-1"));

        verify(friendNotificationDeliveryService).deliverFriendRequestCreated("friend-request-1");
    }

    @Test
    void handleFriendRequestAccepted_전용전달서비스에위임한다() {
        notificationEventHandler.handle(new NotificationDomainEvent.FriendRequestAccepted("friend-request-1"));

        verify(friendNotificationDeliveryService).deliverFriendRequestAccepted("friend-request-1");
    }

    @Test
    void handleFriendRequestDeclined_전용전달서비스에위임한다() {
        notificationEventHandler.handle(new NotificationDomainEvent.FriendRequestDeclined("friend-request-1"));

        verify(friendNotificationDeliveryService).deliverFriendRequestDeclined("friend-request-1");
    }

    @Test
    void handlePartyInvitationCreated_잠금전용전달서비스에위임한다() {
        notificationEventHandler.handle(new NotificationDomainEvent.PartyInvitationCreated("party-invitation-1"));

        verify(invitationNotificationDeliveryService).deliverPartyInvitationCreated("party-invitation-1");
    }

    @Test
    void handleChatRoomInvitationCreated_잠금전용전달서비스에위임한다() {
        notificationEventHandler.handle(new NotificationDomainEvent.ChatRoomInvitationCreated("chat-invitation-1"));

        verify(invitationNotificationDeliveryService).deliverChatRoomInvitationCreated("chat-invitation-1");
    }

    @Test
    void handleBoardCommentCreated_중복대상자는한번만수신한다() {
        Post post = Post.create("게시글", "내용", "target-1", "작성자", null, false, PostCategory.GENERAL);
        ReflectionTestUtils.setField(post, "id", "post-1");
        Comment parent = Comment.create(post, "부모", "target-1", "작성자", null, false, null, null, null);
        ReflectionTestUtils.setField(parent, "id", "comment-parent");
        Comment created = Comment.create(post, "새 댓글", "actor-1", "작성자", null, false, null, null, parent);
        ReflectionTestUtils.setField(created, "id", "comment-new");

        Member target = Member.create("target-1", "target-1@sungkyul.ac.kr", "작성자", LocalDateTime.now());
        Member bookmarker = Member.create("target-2", "target-2@sungkyul.ac.kr", "북마크", LocalDateTime.now());

        when(commentRepository.findActiveById("comment-new")).thenReturn(Optional.of(created));
        when(postInteractionRepository.findBookmarkedUserIdsByPostId("post-1")).thenReturn(List.of("target-1", "target-2"));
        when(memberRepository.findById("target-1")).thenReturn(Optional.of(target));
        when(memberRepository.findById("target-2")).thenReturn(Optional.of(bookmarker));

        notificationEventHandler.handle(new NotificationDomainEvent.BoardCommentCreated("comment-new"));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService, times(2)).createInboxNotifications(captor.capture());
        List<NotificationDispatchRequest> requests = captor.getAllValues();

        assertEquals(2, requests.size());
        assertTrue(requests.stream().anyMatch(request ->
                request.recipientIds().contains("target-1") && request.title().equals("내 댓글에 답글이 달렸어요")
        ));
        assertTrue(requests.stream().anyMatch(request ->
                request.recipientIds().contains("target-2") && request.title().equals("북마크한 게시글에 새 댓글이 달렸어요")
        ));
    }

    @Test
    void handleAppNoticeCommentCreated_작성자를제외한활성운영자모두에게알린다() {
        AppNotice appNotice = appNotice("app-notice-1");
        AppNoticeComment comment = AppNoticeComment.create(
                appNotice, "actor-1", "작성자", "새 댓글", false, null, null, null
        );
        ReflectionTestUtils.setField(comment, "id", "app-comment-1");

        when(appNoticeCommentRepository.findNotificationAggregateById("app-comment-1")).thenReturn(Optional.of(comment));
        when(memberRepository.findActiveAdminIdsExcluding("actor-1"))
                .thenReturn(List.of("admin-1", "admin-2"));

        notificationEventHandler.handle(new NotificationDomainEvent.AppNoticeCommentCreated("app-comment-1"));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        verify(pushNotificationService).send(captor.getValue());
        assertEquals(NotificationType.COMMENT_CREATED, captor.getValue().type());
        assertEquals(Set.of("admin-1", "admin-2"), captor.getValue().recipientIds());
        assertEquals("app-notice-1", captor.getValue().data().appNoticeId());
        assertEquals("app-comment-1", captor.getValue().data().commentId());
    }

    @Test
    void handleAppNoticeCommentCreated_부모작성자가운영자면답글알림으로한번만수신한다() {
        AppNotice appNotice = appNotice("app-notice-1");
        AppNoticeComment parent = AppNoticeComment.create(
                appNotice, "admin-1", "운영자", "부모 댓글", false, null, null, null
        );
        ReflectionTestUtils.setField(parent, "id", "app-comment-parent");
        AppNoticeComment reply = AppNoticeComment.create(
                appNotice, "actor-1", "작성자", "새 답글", false, null, null, parent
        );
        ReflectionTestUtils.setField(reply, "id", "app-comment-reply");

        when(appNoticeCommentRepository.findNotificationAggregateById("app-comment-reply")).thenReturn(Optional.of(reply));
        when(memberRepository.findActiveAdminIdsExcluding("actor-1"))
                .thenReturn(List.of("admin-1", "admin-2"));
        when(memberRepository.findActiveById("admin-1")).thenReturn(Optional.of(
                Member.create("admin-1", "admin-1@sungkyul.ac.kr", "운영자", LocalDateTime.now())
        ));

        notificationEventHandler.handle(new NotificationDomainEvent.AppNoticeCommentCreated("app-comment-reply"));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService, times(2)).createInboxNotifications(captor.capture());
        List<NotificationDispatchRequest> requests = captor.getAllValues();
        assertEquals(List.of("admin-1"), requests.get(0).recipientIds().stream().toList());
        assertEquals("내 댓글에 답글이 달렸어요", requests.get(0).title());
        assertEquals(List.of("admin-2"), requests.get(1).recipientIds().stream().toList());
    }

    @Test
    void handleAcademicScheduleReminder_옵션에따라수신대상이결정된다() {
        AcademicSchedule schedule = AcademicSchedule.create(
                "수강신청",
                LocalDate.of(2026, 3, 9),
                LocalDate.of(2026, 3, 9),
                AcademicScheduleType.SINGLE,
                false,
                "설명"
        );
        ReflectionTestUtils.setField(schedule, "id", "schedule-1");

        when(academicScheduleRepository.findById("schedule-1")).thenReturn(Optional.of(schedule));
        when(memberRepository.findAcademicScheduleReminderRecipientIds(true, true))
                .thenReturn(List.of("member-1"));

        notificationEventHandler.handle(new NotificationDomainEvent.AcademicScheduleReminder(
                "schedule-1",
                NotificationDomainEvent.ReminderTiming.DAY_BEFORE
        ));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        assertEquals(NotificationType.ACADEMIC_SCHEDULE, captor.getValue().type());
        assertEquals(List.of("member-1"), captor.getValue().recipientIds().stream().toList());
    }

    private AppNotice appNotice(String id) {
        AppNotice appNotice = AppNotice.create(
                "앱 공지", "내용", AppNoticeCategory.GENERAL, AppNoticePriority.NORMAL,
                List.of(), null, null, LocalDateTime.now().minusMinutes(1)
        );
        ReflectionTestUtils.setField(appNotice, "id", id);
        return appNotice;
    }

    @Test
    void handleBoardCommentCreated_allNotifications가꺼져있으면댓글알림을받지않는다() {
        Post post = Post.create("게시글", "내용", "target-1", "작성자", null, false, PostCategory.GENERAL);
        ReflectionTestUtils.setField(post, "id", "post-1");
        Comment created = Comment.create(post, "새 댓글", "actor-1", "작성자", null, false, null, null, null);
        ReflectionTestUtils.setField(created, "id", "comment-new");

        Member postAuthor = Member.create("target-1", "target-1@sungkyul.ac.kr", "작성자", LocalDateTime.now());
        postAuthor.updateNotificationSetting(false, null, null, null, true, null, null, null, null, null, null, null);

        when(commentRepository.findActiveById("comment-new")).thenReturn(Optional.of(created));
        when(memberRepository.findById("target-1")).thenReturn(Optional.of(postAuthor));
        when(postInteractionRepository.findBookmarkedUserIdsByPostId("post-1")).thenReturn(List.of());

        notificationEventHandler.handle(new NotificationDomainEvent.BoardCommentCreated("comment-new"));

        verify(notificationService, times(0)).createInboxNotifications(org.mockito.ArgumentMatchers.any());
        verify(pushNotificationService, times(0)).send(org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest(name = "[파티 CHAT_MESSAGE] {0}")
    @MethodSource("partyChatMessages")
    void handleChatMessageCreated_파티일반메시지는_경로제목과발신자본문으로보낸다(
            ChatMessageType messageType,
            String text,
            String expectedTitle,
            String expectedBody
    ) {
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        Party party = party("party-1", "명학역", "성결대학교");
        ChatRoomMember actor = membership(room, "party:party-1", "leader-1", false);
        ChatRoomMember recipient = membership(room, "party:party-1", "member-1", false);
        ChatRoomMember mutedRecipient = membership(room, "party:party-1", "member-2", true);
        ChatMessage message = ChatMessage.create(
                "party:party-1",
                "leader-1",
                "리더",
                text,
                messageType,
                null,
                null
        );
        ReflectionTestUtils.setField(message, "id", "message-1");

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatMessageRepository.findById("message-1")).thenReturn(Optional.of(message));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1"))
                .thenReturn(List.of(actor, recipient, mutedRecipient));

        notificationEventHandler.handle(new NotificationDomainEvent.ChatMessageCreated("party:party-1", "message-1"));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        verify(pushNotificationService).send(captor.getValue());
        assertEquals(NotificationType.CHAT_MESSAGE, captor.getValue().type());
        assertEquals(List.of("member-1"), captor.getValue().recipientIds().stream().toList());
        assertEquals(expectedTitle, captor.getValue().title());
        assertEquals(expectedBody, captor.getValue().message());
        assertEquals("party:party-1", captor.getValue().data().chatRoomId());
        assertEquals(null, captor.getValue().data().partyId());
        assertFalse(captor.getValue().inboxEnabled());
    }

    @ParameterizedTest(name = "[파티 상태중복 CHAT_MESSAGE 억제] {0}")
    @MethodSource("duplicatedPartyStatusMessages")
    void handleChatMessageCreated_파티상태변화메시지는_CHAT_MESSAGE푸시를생략한다(
            ChatMessageType messageType,
            String text
    ) {
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatRoomMember actor = membership(room, "party:party-1", "leader-1", false);
        ChatRoomMember recipient = membership(room, "party:party-1", "member-1", false);
        ChatMessage message = ChatMessage.create(
                "party:party-1",
                "leader-1",
                "리더",
                text,
                messageType,
                null,
                null
        );
        ReflectionTestUtils.setField(message, "id", "message-1");

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatMessageRepository.findById("message-1")).thenReturn(Optional.of(message));

        notificationEventHandler.handle(new NotificationDomainEvent.ChatMessageCreated("party:party-1", "message-1"));

        verify(notificationService, never()).createInboxNotifications(org.mockito.ArgumentMatchers.any());
        verify(pushNotificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handleChatMessageCreated_중복이아닌파티안내시스템메시지는_CHAT_MESSAGE알림을보낸다() {
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatRoomMember actor = membership(room, "party:party-1", "leader-1", false);
        ChatRoomMember recipient = membership(room, "party:party-1", "member-1", false);
        ChatMessage message = ChatMessage.create(
                "party:party-1",
                "leader-1",
                "리더",
                "정산 계좌를 확인해주세요.",
                ChatMessageType.SYSTEM,
                null,
                null
        );
        ReflectionTestUtils.setField(message, "id", "message-1");

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatMessageRepository.findById("message-1")).thenReturn(Optional.of(message));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1"))
                .thenReturn(List.of(actor, recipient));

        notificationEventHandler.handle(new NotificationDomainEvent.ChatMessageCreated("party:party-1", "message-1"));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        verify(pushNotificationService).send(captor.getValue());
        assertEquals(NotificationType.CHAT_MESSAGE, captor.getValue().type());
        assertEquals("파티 안내 메시지", captor.getValue().title());
        assertEquals("정산 계좌를 확인해주세요.", captor.getValue().message());
    }

    @Test
    void handleChatMessageCreated_파티멤버입퇴장시스템메시지는_CHAT_MESSAGE푸시를생략한다() {
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatMessage message = ChatMessage.create(
                "party:party-1",
                "leader-1",
                "리더",
                "김철수님이 나갔어요.",
                ChatMessageType.SYSTEM,
                null,
                null
        );
        ReflectionTestUtils.setField(message, "id", "message-1");
        ReflectionTestUtils.setField(message, "source", ChatMessage.SOURCE_MEMBER_LEAVE);

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatMessageRepository.findById("message-1")).thenReturn(Optional.of(message));

        notificationEventHandler.handle(new NotificationDomainEvent.ChatMessageCreated("party:party-1", "message-1"));

        verify(notificationService, never()).createInboxNotifications(org.mockito.ArgumentMatchers.any());
        verify(pushNotificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handleChatMessageCreated_GAME공개채팅_멤버입퇴장시스템메시지는_CHAT_MESSAGE푸시를생략한다() {
        ChatRoom room = ChatRoom.create("public:game:minecraft", "마인크래프트 채팅방", ChatRoomType.GAME, null, null, null, true, null);
        ChatMessage message = ChatMessage.create(
                "public:game:minecraft",
                "member-1",
                "홍길동",
                "홍길동님이 입장했어요.",
                ChatMessageType.SYSTEM,
                null,
                null
        );
        ReflectionTestUtils.setField(message, "id", "message-2");
        ReflectionTestUtils.setField(message, "source", ChatMessage.SOURCE_MEMBER_JOIN);

        when(chatRoomRepository.findById("public:game:minecraft")).thenReturn(Optional.of(room));
        when(chatMessageRepository.findById("message-2")).thenReturn(Optional.of(message));

        notificationEventHandler.handle(new NotificationDomainEvent.ChatMessageCreated("public:game:minecraft", "message-2"));

        verify(notificationService, never()).createInboxNotifications(org.mockito.ArgumentMatchers.any());
        verify(pushNotificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handlePartyStatusChanged_CLOSED알림을별도도메인알림으로보낸다() {
        Party party = party("party-1", "명학역", "성결대학교");
        party.addMember("member-1");

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findPartyNotificationRecipientIds(List.of("member-1")))
                .thenReturn(List.of("member-1"));

        notificationEventHandler.handle(new NotificationDomainEvent.PartyStatusChanged(
                "party-1",
                PartyStatus.OPEN,
                PartyStatus.CLOSED
        ));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        assertEquals(NotificationType.PARTY_CLOSED, captor.getValue().type());
        assertEquals("파티 모집이 마감되었어요", captor.getValue().title());
        assertEquals("리더가 파티 모집을 마감했습니다.", captor.getValue().message());
        assertFalse(captor.getValue().inboxEnabled());
    }

    @Test
    void handlePartyStatusChanged_REOPENED알림을별도도메인알림으로보낸다() {
        Party party = party("party-1", "명학역", "성결대학교");
        party.addMember("member-1");
        party.close();
        party.reopen();

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findPartyNotificationRecipientIds(List.of("member-1")))
                .thenReturn(List.of("member-1"));

        notificationEventHandler.handle(new NotificationDomainEvent.PartyStatusChanged(
                "party-1",
                PartyStatus.CLOSED,
                PartyStatus.OPEN
        ));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        assertEquals(NotificationType.PARTY_REOPENED, captor.getValue().type());
        assertEquals("파티 모집이 재개되었어요", captor.getValue().title());
        assertEquals("리더가 파티 모집을 다시 시작했습니다.", captor.getValue().message());
        assertFalse(captor.getValue().inboxEnabled());
    }

    @Test
    void handlePartyStatusChanged_CANCELLED종료알림은사유를보존한다() {
        Party party = party("party-1", "명학역", "성결대학교");
        party.addMember("member-1");
        party.cancel();

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findPartyNotificationRecipientIds(List.of("member-1")))
                .thenReturn(List.of("member-1"));

        notificationEventHandler.handle(new NotificationDomainEvent.PartyStatusChanged(
                "party-1",
                PartyStatus.CLOSED,
                PartyStatus.ENDED
        ));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        assertEquals(NotificationType.PARTY_ENDED, captor.getValue().type());
        assertEquals("파티가 해체되었어요", captor.getValue().title());
        assertEquals("리더가 파티를 취소했어요.", captor.getValue().message());
        assertTrue(captor.getValue().inboxEnabled());
    }

    @Test
    void handlePartyStatusChanged_ARRIVED알림은정산대상멤버에게만보낸다() {
        Party party = Party.create(
                "leader-1",
                Location.of("정문", null, null),
                Location.of("역", null, null),
                LocalDateTime.of(2026, 3, 8, 18, 30),
                4,
                List.of("역"),
                "상세"
        );
        ReflectionTestUtils.setField(party, "id", "party-1");
        party.addMember("member-1");
        party.addMember("member-2");
        party.arrive(
                14000,
                List.of("member-1"),
                SettlementAccountSnapshot.of("카카오뱅크", "3333-03-1234567", "홍길동", true)
        );

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findPartyNotificationRecipientIds(List.of("member-1")))
                .thenReturn(List.of("member-1"));

        notificationEventHandler.handle(new NotificationDomainEvent.PartyStatusChanged(
                "party-1",
                PartyStatus.CLOSED,
                PartyStatus.ARRIVED
        ));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        verify(pushNotificationService).send(captor.getValue());
        assertEquals(NotificationType.PARTY_ARRIVED, captor.getValue().type());
        assertEquals(List.of("member-1"), captor.getValue().recipientIds().stream().toList());
    }

    private static Stream<Arguments> partyChatMessages() {
        return Stream.of(
                Arguments.of(
                        ChatMessageType.TEXT,
                        "안녕하세요",
                        "명학역 → 성결대학교 파티 채팅방",
                        "리더 : 안녕하세요"
                ),
                Arguments.of(
                        ChatMessageType.IMAGE,
                        "https://cdn.skuri.app/chat/image-1.jpg",
                        "명학역 → 성결대학교 파티 채팅방",
                        "리더 : 사진을 보냈어요."
                ),
                Arguments.of(
                        ChatMessageType.ACCOUNT,
                        "계좌 정보를 공유했어요. (카카오뱅크 3333-01-1234567)",
                        "명학역 → 성결대학교 파티 채팅방",
                        "리더 : 계좌 정보를 공유했어요. (카카오뱅크 3333-01-1234567)"
                )
        );
    }

    private static Stream<Arguments> duplicatedPartyStatusMessages() {
        return Stream.of(
                Arguments.of(ChatMessageType.SYSTEM, "모집이 마감되었어요."),
                Arguments.of(ChatMessageType.SYSTEM, "모집이 재개되었어요."),
                Arguments.of(ChatMessageType.ARRIVED, "택시가 목적지에 도착했어요."),
                Arguments.of(ChatMessageType.END, "리더가 파티를 취소했어요.")
        );
    }

    private Party party(String id, String departureName, String destinationName) {
        Party party = Party.create(
                "leader-1",
                Location.of(departureName, null, null),
                Location.of(destinationName, null, null),
                LocalDateTime.of(2026, 3, 8, 18, 30),
                4,
                List.of(destinationName),
                "상세"
        );
        ReflectionTestUtils.setField(party, "id", id);
        return party;
    }

    private ChatRoomMember membership(ChatRoom room, String chatRoomId, String memberId, boolean muted) {
        ChatRoomMember membership = ChatRoomMember.create(room, memberId, LocalDateTime.now().minusMinutes(5));
        ReflectionTestUtils.setField(membership, "id", ChatRoomMemberId.of(chatRoomId, memberId));
        membership.updateMuted(muted);
        return membership;
    }
}
