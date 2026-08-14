package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.board.repository.PostSummaryProjection;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberAdminRoleRequest;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberActivityResponse;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberDetailResponse;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberSummaryResponse;
import com.skuri.skuri_backend.domain.member.constant.AdminMemberSortField;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.repository.AdminMemberSummaryProjection;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAdminServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private MemberAdminService memberAdminService;

    @Test
    void getAdminMembers_필터와정렬을반영한페이지를반환한다() {
        AdminMemberSummaryProjection member = summaryProjection(
                "member-1",
                "홍길동",
                "user@sungkyul.ac.kr",
                "스쿠리 유저",
                "2023112233",
                "컴퓨터공학과",
                LocalDateTime.of(2025, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 29, 10, 5),
                "ios",
                "1.4.2"
        );
        when(memberRepository.searchAdminMembers(
                eq("홍길동"),
                eq(MemberStatus.ACTIVE),
                eq(false),
                eq("컴퓨터공학과"),
                eq(AdminMemberSortField.CURRENT_APP_VERSION),
                eq(Sort.Direction.DESC),
                any()
        )).thenReturn(new PageImpl<>(List.of(member)));
        when(departmentService.normalizeSupported("컴퓨터공학과")).thenReturn("컴퓨터공학과");

        var response = memberAdminService.getAdminMembers(
                "홍길동",
                MemberStatus.ACTIVE,
                false,
                "컴퓨터공학과",
                "currentAppVersion",
                "desc",
                0,
                20
        );

        assertEquals(1, response.getContent().size());
        AdminMemberSummaryResponse content = response.getContent().getFirst();
        assertEquals("member-1", content.id());
        assertEquals("컴퓨터공학과", content.department());
        assertEquals("홍길동", content.realname());
        assertEquals("ios", content.lastLoginOs());
        assertEquals("1.4.2", content.currentAppVersion());
        verify(memberRepository).searchAdminMembers(
                eq("홍길동"),
                eq(MemberStatus.ACTIVE),
                eq(false),
                eq("컴퓨터공학과"),
                eq(AdminMemberSortField.CURRENT_APP_VERSION),
                eq(Sort.Direction.DESC),
                argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 20
                )
        );
    }

    @Test
    void getAdminMembers_지원하지않는sortBy면_VALIDATION_ERROR() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberAdminService.getAdminMembers(null, null, null, null, "providerDisplayName", null, 0, 20)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("지원하지 않는 sortBy입니다.", exception.getMessage());
        verify(memberRepository, never()).searchAdminMembers(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAdminMembers_지원하지않는sortDirection이면_VALIDATION_ERROR() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberAdminService.getAdminMembers(null, null, null, null, "joinedAt", "sideways", 0, 20)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("지원하지 않는 sortDirection입니다.", exception.getMessage());
        verify(memberRepository, never()).searchAdminMembers(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAdminMember_탈퇴회원도상세조회할수있다() {
        Member withdrawnMember = member("withdrawn-member");
        withdrawnMember.withdraw(LocalDateTime.of(2026, 3, 29, 10, 15));
        when(memberRepository.findById("withdrawn-member")).thenReturn(Optional.of(withdrawnMember));

        AdminMemberDetailResponse response = memberAdminService.getAdminMember("withdrawn-member");

        assertEquals("withdrawn-member", response.id());
        assertEquals(MemberStatus.WITHDRAWN, response.status());
        assertNotNull(response.withdrawnAt());
        assertNotNull(response.notificationSetting());
    }

    @Test
    void getAdminMemberActivity_ACTIVE회원_성공() {
        Member member = member("member-1");
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(member));

        PageRequest recentPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(postRepository.findActiveSummariesByAuthorId(eq("member-1"), any()))
                .thenReturn(new PageImpl<>(
                        List.of(
                                postSummary("post-1", "택시 파티 구해요", LocalDateTime.of(2026, 3, 28, 14, 0)),
                                postSummary("post-2", "정문 출발", LocalDateTime.of(2026, 3, 27, 14, 0))
                        ),
                        recentPageable,
                        12
                ));
        when(commentRepository.findActiveByAuthorId(eq("member-1"), any()))
                .thenReturn(new PageImpl<>(
                        List.of(
                                comment("comment-1", "post-1", "택시 파티 구해요", "저도 참여하고 싶어요", "member-1",
                                        LocalDateTime.of(2026, 3, 28, 14, 10)),
                                comment("comment-2", "post-2", "정문 출발", "몇 시 출발인가요?", "member-1",
                                        LocalDateTime.of(2026, 3, 27, 15, 0))
                        ),
                        recentPageable,
                        34
                ));
        when(partyRepository.findByLeaderId(eq("member-1"), any()))
                .thenReturn(new PageImpl<>(
                        List.of(
                                leaderParty("party-1", "member-1", "성결대 정문", "안양역", PartyStatus.OPEN,
                                        LocalDateTime.of(2026, 3, 29, 9, 0)),
                                leaderParty("party-2", "member-1", "성결대 정문", "범계역", PartyStatus.CLOSED,
                                        LocalDateTime.of(2026, 3, 28, 20, 0)),
                                leaderParty("party-3", "member-1", "성결대 정문", "인덕원역", PartyStatus.ARRIVED,
                                        LocalDateTime.of(2026, 3, 27, 12, 0))
                        ),
                        recentPageable,
                        3
                ));
        when(partyRepository.findJoinedPartiesExcludingLeader(eq("member-1"), any()))
                .thenReturn(new PageImpl<>(
                        List.of(
                                joinedParty("party-4", "leader-2", "member-1", "성결대 정문", "의왕역", PartyStatus.OPEN,
                                        LocalDateTime.of(2026, 3, 29, 10, 0)),
                                joinedParty("party-5", "leader-3", "member-1", "성결대 정문", "산본역", PartyStatus.CLOSED,
                                        LocalDateTime.of(2026, 3, 28, 21, 0)),
                                joinedParty("party-6", "leader-4", "member-1", "성결대 정문", "금정역", PartyStatus.ENDED,
                                        LocalDateTime.of(2026, 3, 26, 11, 0))
                        ),
                        recentPageable,
                        7
                ));
        when(inquiryRepository.findByUserId(eq("member-1"), any()))
                .thenReturn(new PageImpl<>(
                        List.of(
                                inquiry("inquiry-1", "member-1", InquiryType.ACCOUNT, InquiryStatus.PENDING,
                                        LocalDateTime.of(2026, 3, 28, 11, 0)),
                                inquiry("inquiry-2", "member-1", InquiryType.BUG, InquiryStatus.RESOLVED,
                                        LocalDateTime.of(2026, 3, 27, 11, 0))
                        ),
                        recentPageable,
                        2
                ));
        when(reportRepository.findByReporterId(eq("member-1"), any()))
                .thenReturn(new PageImpl<>(
                        List.of(
                                report("report-1", "member-1", ReportTargetType.POST, "post-9", "SPAM",
                                        ReportStatus.REVIEWING, LocalDateTime.of(2026, 3, 27, 20, 0))
                        ),
                        recentPageable,
                        1
                ));

        AdminMemberActivityResponse response = memberAdminService.getAdminMemberActivity("member-1");

        assertEquals("member-1", response.memberId());
        assertEquals(12, response.counts().posts());
        assertEquals(34, response.counts().comments());
        assertEquals(3, response.counts().partiesCreated());
        assertEquals(7, response.counts().partiesJoined());
        assertEquals(2, response.counts().inquiries());
        assertEquals(1, response.counts().reportsSubmitted());
        assertEquals(2, response.recentPosts().size());
        assertEquals("comment-1", response.recentComments().getFirst().id());
        assertEquals(5, response.recentParties().size());
        assertEquals("party-4", response.recentParties().get(0).id());
        assertEquals(AdminMemberActivityResponse.PartyRole.JOINED, response.recentParties().get(0).role());
        assertEquals("party-1", response.recentParties().get(1).id());
        verify(postRepository).findActiveSummariesByAuthorId(
                eq("member-1"),
                argThat(pageable -> pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 5
                        && pageable.getSort().getOrderFor("createdAt") != null
                        && pageable.getSort().getOrderFor("createdAt").isDescending())
        );
        verify(commentRepository).findActiveByAuthorId(
                eq("member-1"),
                argThat(pageable -> pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 5
                        && pageable.getSort().getOrderFor("createdAt") != null
                        && pageable.getSort().getOrderFor("createdAt").isDescending())
        );
    }

    @Test
    void updateAdminRole_성공() {
        Member member = member("member-1");
        when(memberRepository.findByIdForUpdate("member-1")).thenReturn(Optional.of(member));
        when(memberRepository.saveAndFlush(member)).thenReturn(member);

        AdminMemberDetailResponse response = memberAdminService.updateAdminRole(
                "admin-uid",
                "member-1",
                new UpdateMemberAdminRoleRequest(true)
        );

        assertEquals("member-1", response.id());
        assertEquals(true, response.isAdmin());
        verify(memberRepository).saveAndFlush(member);
    }

    @Test
    void updateAdminRole_자기자신이면_SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberAdminService.updateAdminRole("admin-uid", "admin-uid", new UpdateMemberAdminRoleRequest(false))
        );

        assertEquals(ErrorCode.SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED, exception.getErrorCode());
        verify(memberRepository, never()).findByIdForUpdate(any());
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateAdminRole_탈퇴회원이면_CONFLICT() {
        Member withdrawnMember = member("withdrawn-member");
        withdrawnMember.withdraw(LocalDateTime.now());
        when(memberRepository.findByIdForUpdate("withdrawn-member")).thenReturn(Optional.of(withdrawnMember));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberAdminService.updateAdminRole("admin-uid", "withdrawn-member", new UpdateMemberAdminRoleRequest(false))
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        assertEquals("탈퇴한 회원의 관리자 권한은 변경할 수 없습니다.", exception.getMessage());
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void getAdminMemberActivity_WITHDRAWN회원이면_CONFLICT() {
        Member withdrawnMember = member("withdrawn-member");
        withdrawnMember.withdraw(LocalDateTime.of(2026, 3, 29, 10, 15));
        when(memberRepository.findById("withdrawn-member")).thenReturn(Optional.of(withdrawnMember));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberAdminService.getAdminMemberActivity("withdrawn-member")
        );

        assertEquals(ErrorCode.MEMBER_ACTIVITY_NOT_AVAILABLE_FOR_WITHDRAWN, exception.getErrorCode());
        verifyNoInteractions(postRepository, commentRepository, partyRepository, inquiryRepository, reportRepository);
    }

    @Test
    void getAdminMembers_지원하지않는학과면_VALIDATION_ERROR() {
        when(departmentService.normalizeSupported("없는학과")).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberAdminService.getAdminMembers(null, null, null, "없는학과", null, null, 0, 20)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    private Member member(String id) {
        Member member = Member.create(id, id + "@sungkyul.ac.kr", "홍길동", LocalDateTime.of(2025, 3, 1, 9, 0));
        ReflectionTestUtils.setField(member, "lastLogin", LocalDateTime.of(2026, 3, 29, 10, 5));
        return member;
    }

    private AdminMemberSummaryProjection summaryProjection(
            String id,
            String realname,
            String email,
            String nickname,
            String studentId,
            String department,
            LocalDateTime joinedAt,
            LocalDateTime lastLogin,
            String lastLoginOs,
            String currentAppVersion
    ) {
        return new AdminMemberSummaryProjection(
                id,
                email,
                nickname,
                realname,
                studentId,
                department,
                false,
                joinedAt,
                lastLogin,
                lastLoginOs,
                currentAppVersion,
                MemberStatus.ACTIVE
        );
    }

    private PostSummaryProjection postSummary(String id, String title, LocalDateTime createdAt) {
        return new PostSummaryProjection() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getContent() {
                return "본문";
            }

            @Override
            public String getAuthorId() {
                return "member-1";
            }

            @Override
            public String getAuthorName() {
                return "홍길동";
            }

            @Override
            public String getAuthorProfileImage() {
                return null;
            }

            @Override
            public boolean isAnonymous() {
                return false;
            }

            @Override
            public PostCategory getCategory() {
                return PostCategory.GENERAL;
            }

            @Override
            public int getViewCount() {
                return 0;
            }

            @Override
            public int getLikeCount() {
                return 0;
            }

            @Override
            public int getCommentCount() {
                return 0;
            }

            @Override
            public int getBookmarkCount() {
                return 0;
            }

            @Override
            public boolean isPinned() {
                return false;
            }

            @Override
            public LocalDateTime getCreatedAt() {
                return createdAt;
            }

            @Override
            public boolean isHasImage() {
                return false;
            }
        };
    }

    private Comment comment(
            String id,
            String postId,
            String postTitle,
            String content,
            String authorId,
            LocalDateTime createdAt
    ) {
        Post post = Post.create(postTitle, "본문", authorId, "홍길동", null, false, PostCategory.GENERAL);
        ReflectionTestUtils.setField(post, "id", postId);
        Comment comment = Comment.create(post, content, authorId, "홍길동", null, false, null, null, null);
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "createdAt", createdAt);
        return comment;
    }

    private Party leaderParty(
            String id,
            String leaderId,
            String departure,
            String destination,
            PartyStatus status,
            LocalDateTime createdAt
    ) {
        Party party = Party.create(
                leaderId,
                Location.of(departure, null, null),
                Location.of(destination, null, null),
                createdAt.plusHours(1),
                4,
                List.of(),
                null
        );
        ReflectionTestUtils.setField(party, "id", id);
        ReflectionTestUtils.setField(party, "status", status);
        ReflectionTestUtils.setField(party, "createdAt", createdAt);
        return party;
    }

    private Party joinedParty(
            String id,
            String leaderId,
            String memberId,
            String departure,
            String destination,
            PartyStatus status,
            LocalDateTime createdAt
    ) {
        Party party = leaderParty(id, leaderId, departure, destination, PartyStatus.OPEN, createdAt);
        party.addMember(memberId);
        ReflectionTestUtils.setField(party, "status", status);
        return party;
    }

    private Inquiry inquiry(
            String id,
            String userId,
            InquiryType type,
            InquiryStatus status,
            LocalDateTime createdAt
    ) {
        Inquiry inquiry = Inquiry.create(type, "계정 문의", "본문", List.of(), userId, userId + "@sungkyul.ac.kr", "닉네임", "홍길동", "2023112233");
        ReflectionTestUtils.setField(inquiry, "id", id);
        ReflectionTestUtils.setField(inquiry, "status", status);
        ReflectionTestUtils.setField(inquiry, "createdAt", createdAt);
        return inquiry;
    }

    private Report report(
            String id,
            String reporterId,
            ReportTargetType targetType,
            String targetId,
            String category,
            ReportStatus status,
            LocalDateTime createdAt
    ) {
        Report report = Report.create(targetType, targetId, "target-author", category, "신고 사유", reporterId);
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "status", status);
        ReflectionTestUtils.setField(report, "createdAt", createdAt);
        return report;
    }
}
