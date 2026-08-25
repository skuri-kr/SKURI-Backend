package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.board.repository.PostSummaryProjection;
import com.skuri.skuri_backend.domain.member.constant.AdminMemberSortField;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberAdminRoleRequest;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberActivityResponse;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberDetailResponse;
import com.skuri.skuri_backend.domain.member.dto.response.AdminMemberSummaryResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberBankAccountResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberNotificationSettingResponse;
import com.skuri.skuri_backend.domain.member.entity.BankAccount;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.entity.NotificationSetting;
import com.skuri.skuri_backend.domain.member.exception.MemberActivityNotAvailableForWithdrawnException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.member.repository.AdminMemberSummaryProjection;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.exception.SelfAdminRoleChangeNotAllowedException;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import com.skuri.skuri_backend.infra.admin.list.AdminPageRequestPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MemberAdminService {

    private static final AdminMemberSortField ADMIN_MEMBER_DEFAULT_SORT_FIELD = AdminMemberSortField.JOINED_AT;
    private static final Sort.Direction ADMIN_MEMBER_DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;
    private static final Pageable ADMIN_MEMBER_ACTIVITY_RECENT_PAGEABLE =
            PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PartyRepository partyRepository;
    private final InquiryRepository inquiryRepository;
    private final ReportRepository reportRepository;
    private final DepartmentService departmentService;

    @Transactional(readOnly = true)
    public PageResponse<AdminMemberSummaryResponse> getAdminMembers(
            String query,
            MemberStatus status,
            Boolean isAdmin,
            String department,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        Pageable pageable = resolvePageable(page, size);
        AdminMemberSortField sortField = resolveSortField(sortBy);
        Sort.Direction resolvedSortDirection = resolveSortDirection(sortDirection);
        Page<AdminMemberSummaryResponse> memberPage = memberRepository.searchAdminMembers(
                        normalizeQuery(query),
                        status,
                        isAdmin,
                        normalizeDepartment(department),
                        sortField,
                        resolvedSortDirection,
                        pageable
                )
                .map(this::toSummaryResponse);
        return PageResponse.from(memberPage);
    }

    @Transactional(readOnly = true)
    public AdminMemberDetailResponse getAdminMember(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        return toDetailResponse(member);
    }

    @Transactional(readOnly = true)
    public AdminMemberActivityResponse getAdminMemberActivity(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (member.isWithdrawn()) {
            throw new MemberActivityNotAvailableForWithdrawnException();
        }

        Page<PostSummaryProjection> recentPostPage =
                postRepository.findActiveSummariesByAuthorId(memberId, ADMIN_MEMBER_ACTIVITY_RECENT_PAGEABLE);
        Page<Comment> recentCommentPage =
                commentRepository.findActiveByAuthorId(memberId, ADMIN_MEMBER_ACTIVITY_RECENT_PAGEABLE);
        Page<Party> recentCreatedPartyPage =
                partyRepository.findByLeaderId(memberId, ADMIN_MEMBER_ACTIVITY_RECENT_PAGEABLE);
        Page<Party> recentJoinedPartyPage =
                partyRepository.findJoinedPartiesExcludingLeader(memberId, ADMIN_MEMBER_ACTIVITY_RECENT_PAGEABLE);
        Page<Inquiry> recentInquiryPage =
                inquiryRepository.findByUserId(memberId, ADMIN_MEMBER_ACTIVITY_RECENT_PAGEABLE);
        Page<Report> recentReportPage =
                reportRepository.findByReporterId(memberId, ADMIN_MEMBER_ACTIVITY_RECENT_PAGEABLE);

        return new AdminMemberActivityResponse(
                memberId,
                LocalDateTime.now(),
                new AdminMemberActivityResponse.ActivityCounts(
                        recentPostPage.getTotalElements(),
                        recentCommentPage.getTotalElements(),
                        recentCreatedPartyPage.getTotalElements(),
                        recentJoinedPartyPage.getTotalElements(),
                        recentInquiryPage.getTotalElements(),
                        recentReportPage.getTotalElements()
                ),
                recentPostPage.getContent().stream()
                        .map(this::toRecentPostResponse)
                        .toList(),
                recentCommentPage.getContent().stream()
                        .map(this::toRecentCommentResponse)
                        .toList(),
                mergeRecentParties(recentCreatedPartyPage.getContent(), recentJoinedPartyPage.getContent()),
                recentInquiryPage.getContent().stream()
                        .map(this::toRecentInquiryResponse)
                        .toList(),
                recentReportPage.getContent().stream()
                        .map(this::toRecentReportResponse)
                        .toList()
        );
    }

    @Transactional
    public AdminMemberDetailResponse updateAdminRole(String actorId, String memberId, UpdateMemberAdminRoleRequest request) {
        if (Objects.equals(actorId, memberId)) {
            throw new SelfAdminRoleChangeNotAllowedException();
        }

        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (member.isWithdrawn()) {
            throw new BusinessException(ErrorCode.CONFLICT, "탈퇴한 회원의 관리자 권한은 변경할 수 없습니다.");
        }

        member.updateAdminRole(Boolean.TRUE.equals(request.isAdmin()));
        memberRepository.saveAndFlush(member);
        return toDetailResponse(member);
    }

    private Pageable resolvePageable(int page, int size) {
        Pageable validatedPageable = AdminPageRequestPolicy.of(page, size);
        return PageRequest.of(validatedPageable.getPageNumber(), validatedPageable.getPageSize());
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim();
    }

    private String normalizeDepartment(String department) {
        if (!StringUtils.hasText(department)) {
            return null;
        }
        String normalizedDepartment = departmentService.normalizeSupported(department);
        if (normalizedDepartment == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 department입니다.");
        }
        return normalizedDepartment;
    }

    private AdminMemberSortField resolveSortField(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return ADMIN_MEMBER_DEFAULT_SORT_FIELD;
        }
        return AdminMemberSortField.from(sortBy);
    }

    private Sort.Direction resolveSortDirection(String sortDirection) {
        if (!StringUtils.hasText(sortDirection)) {
            return ADMIN_MEMBER_DEFAULT_SORT_DIRECTION;
        }
        try {
            return Sort.Direction.fromString(sortDirection.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 sortDirection입니다.");
        }
    }

    private AdminMemberSummaryResponse toSummaryResponse(AdminMemberSummaryProjection member) {
        return new AdminMemberSummaryResponse(
                member.id(),
                member.email(),
                member.nickname(),
                member.realname(),
                member.studentId(),
                member.department(),
                member.isAdmin(),
                member.joinedAt(),
                member.lastLogin(),
                member.lastLoginOs(),
                member.currentAppVersion(),
                member.status()
        );
    }

    private AdminMemberActivityResponse.RecentPost toRecentPostResponse(PostSummaryProjection post) {
        return new AdminMemberActivityResponse.RecentPost(
                post.getId(),
                post.getTitle(),
                post.getCategory(),
                post.getCreatedAt()
        );
    }

    private AdminMemberActivityResponse.RecentComment toRecentCommentResponse(Comment comment) {
        return new AdminMemberActivityResponse.RecentComment(
                comment.getId(),
                comment.getPost().getId(),
                comment.getPost().getTitle(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    private List<AdminMemberActivityResponse.RecentParty> mergeRecentParties(
            List<Party> createdParties,
            List<Party> joinedParties
    ) {
        return Stream.concat(
                        createdParties.stream()
                                .map(party -> toRecentPartyResponse(party, AdminMemberActivityResponse.PartyRole.LEADER)),
                        joinedParties.stream()
                                .map(party -> toRecentPartyResponse(party, AdminMemberActivityResponse.PartyRole.JOINED))
                )
                .sorted(Comparator.comparing(AdminMemberActivityResponse.RecentParty::createdAt).reversed())
                .limit(5)
                .toList();
    }

    private AdminMemberActivityResponse.RecentParty toRecentPartyResponse(
            Party party,
            AdminMemberActivityResponse.PartyRole role
    ) {
        return new AdminMemberActivityResponse.RecentParty(
                party.getId(),
                role,
                party.getStatus(),
                party.getDeparture().getName() + " → " + party.getDestination().getName(),
                party.getDepartureTime(),
                party.getCreatedAt()
        );
    }

    private AdminMemberActivityResponse.RecentInquiry toRecentInquiryResponse(Inquiry inquiry) {
        return new AdminMemberActivityResponse.RecentInquiry(
                inquiry.getId(),
                inquiry.getType(),
                inquiry.getSubject(),
                inquiry.getStatus(),
                inquiry.getCreatedAt()
        );
    }

    private AdminMemberActivityResponse.RecentReport toRecentReportResponse(Report report) {
        return new AdminMemberActivityResponse.RecentReport(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getCategory(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }

    private AdminMemberDetailResponse toDetailResponse(Member member) {
        return new AdminMemberDetailResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRealname(),
                member.getStudentId(),
                member.getDepartment(),
                member.getPhotoUrl(),
                member.isAdmin(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getLastLogin(),
                member.getWithdrawnAt(),
                toBankAccountResponse(member.getBankAccount()),
                toNotificationSettingResponse(member.getNotificationSetting())
        );
    }

    private MemberBankAccountResponse toBankAccountResponse(BankAccount bankAccount) {
        if (bankAccount == null) {
            return null;
        }
        return new MemberBankAccountResponse(
                bankAccount.getBankName(),
                bankAccount.getAccountNumber(),
                bankAccount.getAccountHolder(),
                bankAccount.getHideName()
        );
    }

    private MemberNotificationSettingResponse toNotificationSettingResponse(NotificationSetting notificationSetting) {
        if (notificationSetting == null) {
            notificationSetting = NotificationSetting.defaultSetting();
        }
        Map<String, Boolean> detail = notificationSetting.getNoticeNotificationsDetail() != null
                ? new HashMap<>(notificationSetting.getNoticeNotificationsDetail())
                : Map.of();

        return new MemberNotificationSettingResponse(
                notificationSetting.isAllNotifications(),
                notificationSetting.isPartyNotifications(),
                notificationSetting.isNoticeNotifications(),
                notificationSetting.isBoardLikeNotifications(),
                notificationSetting.isCommentNotifications(),
                notificationSetting.isBookmarkedPostCommentNotifications(),
                notificationSetting.isSystemNotifications(),
                notificationSetting.isFriendAndInvitationNotifications(),
                notificationSetting.isAcademicScheduleNotifications(),
                notificationSetting.isAcademicScheduleDayBeforeEnabled(),
                notificationSetting.isAcademicScheduleAllEventsEnabled(),
                detail
        );
    }
}
