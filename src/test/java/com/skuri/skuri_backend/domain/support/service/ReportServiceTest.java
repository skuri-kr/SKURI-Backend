package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.image.service.MediaCleanupTaskService;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.request.CreateReportRequest;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateReportStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminReportResponse;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private StorageRepository storageRepository;

    @Mock
    private MediaCleanupTaskService mediaCleanupTaskService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void updateReportStatus_reviewingToActioned_성공() {
        Report report = Report.create(
                ReportTargetType.POST,
                "post-1",
                "author-1",
                "SPAM",
                "광고성 게시글입니다.",
                "user-1"
        );
        ReflectionTestUtils.setField(report, "id", "report-1");
        report.updateReview(ReportStatus.REVIEWING, null, null);
        when(reportRepository.findById("report-1")).thenReturn(Optional.of(report));

        AdminReportResponse response = reportService.updateReportStatus(
                "report-1",
                new UpdateReportStatusRequest(ReportStatus.ACTIONED, "DELETE_POST", "게시글 삭제 완료")
        );

        assertEquals(ReportStatus.ACTIONED, response.status());
        assertEquals("DELETE_POST", response.action());
        assertEquals("게시글 삭제 완료", response.memo());
    }

    @Test
    void updateReportStatus_actionedToReviewing_실패() {
        Report report = Report.create(
                ReportTargetType.POST,
                "post-1",
                "author-1",
                "SPAM",
                "광고성 게시글입니다.",
                "user-1"
        );
        ReflectionTestUtils.setField(report, "id", "report-1");
        report.updateReview(ReportStatus.ACTIONED, "DELETE_POST", "처리 완료");
        when(reportRepository.findById("report-1")).thenReturn(Optional.of(report));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reportService.updateReportStatus(
                        "report-1",
                        new UpdateReportStatusRequest(ReportStatus.REVIEWING, null, null)
                )
        );

        assertEquals(ErrorCode.INVALID_REPORT_STATUS_TRANSITION, exception.getErrorCode());
    }

    @Test
    void createReport_저장시중복충돌_실패() {
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId("user-1", ReportTargetType.POST, "post-1"))
                .thenReturn(false);
        when(postRepository.findByIdAndDeletedFalse("post-1"))
                .thenReturn(Optional.of(Post.create(
                        "제목",
                        "내용",
                        "author-1",
                        "작성자",
                        null,
                        false,
                        PostCategory.GENERAL
                )));
        when(reportRepository.saveAndFlush(any(Report.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate report"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reportService.createReport(
                        "user-1",
                        new CreateReportRequest(
                                ReportTargetType.POST,
                                "post-1",
                                "SPAM",
                                "광고성 게시글입니다."
                        )
                )
        );

        assertEquals(ErrorCode.REPORT_ALREADY_SUBMITTED, exception.getErrorCode());
    }

    @Test
    void createReport_chatMessage_발신자를대상작성자로저장() {
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        ChatMessage message = ChatMessage.create(
                "room-1",
                "sender-1",
                "보낸이",
                "광고 메시지",
                ChatMessageType.TEXT,
                null,
                null
        );
        ReflectionTestUtils.setField(message, "id", "message-1");
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 8, 16, 10, 0));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId("user-1", ReportTargetType.CHAT_MESSAGE, "message-1"))
                .thenReturn(false);
        when(chatMessageRepository.findByIdForUpdate("message-1")).thenReturn(Optional.of(message));
        when(reportRepository.saveAndFlush(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reportService.createReport(
                "user-1",
                new CreateReportRequest(
                        ReportTargetType.CHAT_MESSAGE,
                        "message-1",
                        "spam",
                        "광고성 메시지입니다."
                )
        );

        assertEquals("sender-1", captor.getValue().getTargetAuthorId());
        assertEquals("SPAM", captor.getValue().getCategory());
        assertEquals("광고 메시지", captor.getValue().getTargetSnapshot().text());
        assertEquals(LocalDateTime.of(2026, 8, 16, 10, 0), captor.getValue().getTargetSnapshot().createdAt());
    }

    @Test
    void createReport_채팅이미지는정리작업잠금을확보한뒤스냅샷을저장한다() {
        String imageUrl = "https://cdn.skuri.app/chat/2026/08/image.png";
        ChatMessage message = ChatMessage.create(
                "room-1",
                "sender-1",
                "보낸이",
                imageUrl,
                ChatMessageType.IMAGE,
                null,
                null
        );
        ReflectionTestUtils.setField(message, "id", "message-1");
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId("user-1", ReportTargetType.CHAT_MESSAGE, "message-1"))
                .thenReturn(false);
        when(chatMessageRepository.findByIdForUpdate("message-1")).thenReturn(Optional.of(message));
        when(storageRepository.resolveRelativePath(imageUrl)).thenReturn(Optional.of("chat/2026/08/image.png"));
        when(reportRepository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reportService.createReport(
                "user-1",
                new CreateReportRequest(
                        ReportTargetType.CHAT_MESSAGE,
                        "message-1",
                        "SPAM",
                        "광고성 이미지입니다."
                )
        );

        ArgumentCaptor<java.util.Collection<String>> pathsCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        InOrder verificationOrder = inOrder(mediaCleanupTaskService, reportRepository);
        verificationOrder.verify(mediaCleanupTaskService).retain(pathsCaptor.capture());
        verificationOrder.verify(reportRepository).saveAndFlush(any(Report.class));
        assertEquals(
                List.of(
                        "chat/2026/08/image.jpg",
                        "chat/2026/08/image.png",
                        "chat/2026/08/image.webp",
                        "chat/2026/08/image_thumb.jpg",
                        "chat/2026/08/image_thumb.png",
                        "chat/2026/08/image_thumb.webp"
                ),
                List.copyOf(pathsCaptor.getValue())
        );
    }

    @Test
    void createReport_삭제된채팅메시지는신고할수없다() {
        ChatMessage deletedMessage = ChatMessage.create(
                "room-1",
                "sender-1",
                "보낸이",
                "이미 지운 메시지",
                ChatMessageType.TEXT,
                null,
                null
        );
        deletedMessage.delete(LocalDateTime.of(2026, 8, 16, 10, 5));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId("user-1", ReportTargetType.CHAT_MESSAGE, "message-1"))
                .thenReturn(false);
        when(chatMessageRepository.findByIdForUpdate("message-1")).thenReturn(Optional.of(deletedMessage));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reportService.createReport(
                        "user-1",
                        new CreateReportRequest(
                                ReportTargetType.CHAT_MESSAGE,
                                "message-1",
                                "SPAM",
                                "삭제된 메시지 신고"
                        )
                )
        );

        assertEquals(ErrorCode.CHAT_MESSAGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createReport_chatRoom_seedRoom_createdByNull허용() {
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId("user-1", ReportTargetType.CHAT_ROOM, "public:university"))
                .thenReturn(false);
        when(chatRoomRepository.findById("public:university"))
                .thenReturn(Optional.of(ChatRoom.create(
                        "public:university",
                        "성결대학교 전체 채팅방",
                        ChatRoomType.UNIVERSITY,
                        null,
                        "성결대학교 전체 채팅방입니다.",
                        null,
                        true,
                        null
                )));
        when(reportRepository.saveAndFlush(captor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reportService.createReport(
                "user-1",
                new CreateReportRequest(
                        ReportTargetType.CHAT_ROOM,
                        "public:university",
                        "ABUSE",
                        "부적절한 목적의 채팅방입니다."
                )
        );

        assertEquals(null, captor.getValue().getTargetAuthorId());
    }

    @Test
    void createReport_chatRoom_partyRoom은신고대상아님() {
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId("user-1", ReportTargetType.CHAT_ROOM, "party:party-1"))
                .thenReturn(false);
        when(chatRoomRepository.findById("party:party-1"))
                .thenReturn(Optional.of(ChatRoom.createPartyRoom("party-1")));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reportService.createReport(
                        "user-1",
                        new CreateReportRequest(
                                ReportTargetType.CHAT_ROOM,
                                "party:party-1",
                                "ABUSE",
                                "파티 채팅방 신고 테스트"
                        )
                )
        );

        assertEquals(ErrorCode.CHAT_ROOM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createReport_taxiParty_자기자신신고실패() {
        Party party = mock(Party.class);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId("leader-1", ReportTargetType.TAXI_PARTY, "party-1"))
                .thenReturn(false);
        when(partyRepository.findById("party-1")).thenReturn(Optional.of(party));
        when(party.getLeaderId()).thenReturn("leader-1");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reportService.createReport(
                        "leader-1",
                        new CreateReportRequest(
                                ReportTargetType.TAXI_PARTY,
                                "party-1",
                                "FRAUD",
                                "운행/정산 방식이 부적절합니다."
                        )
                )
        );

        assertEquals(ErrorCode.CANNOT_REPORT_YOURSELF, exception.getErrorCode());
        verify(party).getLeaderId();
    }

    @Test
    void getAdminReports_잘못된사이즈_실패() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> reportService.getAdminReports(null, null, 0, 0)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("size는 1 이상 100 이하여야 합니다.", exception.getMessage());
    }
}
