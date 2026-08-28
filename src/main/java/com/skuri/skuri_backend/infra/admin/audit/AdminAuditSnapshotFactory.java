package com.skuri.skuri_backend.infra.admin.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.academic.dto.response.AcademicScheduleResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.CourseSchedule;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerAdminResponse;
import com.skuri.skuri_backend.domain.campus.entity.CampusBanner;
import com.skuri.skuri_backend.domain.campus.repository.CampusBannerRepository;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminResponse;
import com.skuri.skuri_backend.domain.support.entity.AppVersion;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.repository.AppVersionRepository;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuRepository;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import com.skuri.skuri_backend.domain.support.service.CafeteriaMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component("adminAuditSnapshots")
@RequiredArgsConstructor
public class AdminAuditSnapshotFactory {

    private final AcademicScheduleRepository academicScheduleRepository;
    private final CourseRepository courseRepository;
    private final AppNoticeRepository appNoticeRepository;
    private final CampusBannerRepository campusBannerRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final PartyRepository partyRepository;
    private final InquiryRepository inquiryRepository;
    private final ReportRepository reportRepository;
    private final AppVersionRepository appVersionRepository;
    private final LegalDocumentRepository legalDocumentRepository;
    private final CafeteriaMenuRepository cafeteriaMenuRepository;
    private final CafeteriaMenuService cafeteriaMenuService;

    public AcademicScheduleResponse academicSchedule(String scheduleId) {
        return academicScheduleRepository.findById(scheduleId)
                .map(schedule -> new AcademicScheduleResponse(
                        schedule.getId(),
                        schedule.getTitle(),
                        schedule.getStartDate(),
                        schedule.getEndDate(),
                        schedule.getType(),
                        schedule.isPrimary(),
                        schedule.getDescription()
                ))
                .orElse(null);
    }

    public CourseSemesterSnapshot courseSemester(String semester) {
        if (semester == null || semester.isBlank()) {
            return null;
        }
        List<Course> courses = courseRepository.findAllBySemesterWithSchedules(semester);
        return new CourseSemesterSnapshot(
                semester,
                courses.size(),
                courses.stream()
                        .map(course -> new CourseSummarySnapshot(
                                course.getId(),
                                course.semesterCourseKey(),
                                course.getName(),
                                course.getDepartment(),
                                course.getProfessor(),
                                course.getGrade(),
                                course.getCredits(),
                                course.getSchedules().stream().map(this::toCourseScheduleSnapshot).toList()
                        ))
                        .toList()
        );
    }

    public ChatRoomSnapshot chatRoom(String chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .map(room -> new ChatRoomSnapshot(
                        room.getId(),
                        room.getName(),
                        room.getType(),
                        room.getDescription(),
                        room.isPublic(),
                        room.getCreatedBy(),
                        room.getMemberCount()
                ))
                .orElse(null);
    }

    public ChatMessageSnapshot chatMessage(String chatMessageId) {
        return chatMessageRepository.findById(chatMessageId)
                .map(message -> new ChatMessageSnapshot(
                        message.getId(),
                        message.getChatRoomId(),
                        message.getSenderId(),
                        message.getSenderName(),
                        message.getType(),
                        message.getSource(),
                        message.getText(),
                        message.getCreatedAt()
                ))
                .orElse(null);
    }

    public AppNoticeResponse appNotice(String appNoticeId) {
        return appNoticeRepository.findById(appNoticeId)
                .map(this::toAppNoticeResponse)
                .orElse(null);
    }

    public CampusBannerAdminResponse campusBanner(String bannerId) {
        return campusBannerRepository.findById(bannerId)
                .map(this::toCampusBannerResponse)
                .orElse(null);
    }

    public List<CampusBannerOrderSnapshot> campusBannerOrder() {
        return campusBannerRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc().stream()
                .map(banner -> new CampusBannerOrderSnapshot(
                        banner.getId(),
                        banner.getTitleLabel(),
                        banner.getDisplayOrder()
                ))
                .toList();
    }

    public InquirySnapshot inquiry(String inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .map(inquiry -> new InquirySnapshot(
                        inquiry.getId(),
                        inquiry.getUserId(),
                        inquiry.getType(),
                        inquiry.getSubject(),
                        inquiry.getContent(),
                        inquiry.getStatus(),
                        inquiry.getAdminMemo(),
                        inquiry.getCreatedAt(),
                        inquiry.getUpdatedAt()
                ))
                .orElse(null);
    }

    public MemberAdminRoleSnapshot memberAdminRole(String memberId) {
        return memberRepository.findById(memberId)
                .map(this::toMemberAdminRoleSnapshot)
                .orElse(null);
    }

    public PostModerationSnapshot postModeration(String postId) {
        return postRepository.findById(postId)
                .map(this::toPostModerationSnapshot)
                .orElse(null);
    }

    public CommentModerationSnapshot commentModeration(String commentId) {
        return commentRepository.findById(commentId)
                .map(this::toCommentModerationSnapshot)
                .orElse(null);
    }

    public PartyStatusSnapshot partyStatus(String partyId) {
        return partyRepository.findById(partyId)
                .map(this::toPartyStatusSnapshot)
                .orElse(null);
    }

    public PartyMemberSnapshot partyMember(String partyId, String memberId) {
        return partyRepository.findDetailById(partyId)
                .flatMap(party -> party.getMembers().stream()
                        .filter(member -> member.getMemberId().equals(memberId))
                        .findFirst()
                        .map(member -> new PartyMemberSnapshot(
                                party.getId(),
                                member.getMemberId(),
                                party.isLeader(member.getMemberId()),
                                member.getJoinedAt()
                        )))
                .orElse(null);
    }

    public ReportSnapshot report(String reportId) {
        return reportRepository.findById(reportId)
                .map(report -> new ReportSnapshot(
                        report.getId(),
                        report.getReporterId(),
                        report.getTargetType(),
                        report.getTargetId(),
                        report.getTargetAuthorId(),
                        report.getCategory(),
                        report.getReason(),
                        report.getStatus(),
                        report.getAction(),
                        report.getAdminMemo(),
                        report.getCreatedAt(),
                        report.getUpdatedAt()
                ))
                .orElse(null);
    }

    public AppVersionSnapshot appVersion(String platform) {
        return appVersionRepository.findById(platform)
                .map(appVersion -> new AppVersionSnapshot(
                        appVersion.getPlatform(),
                        appVersion.getMinimumVersion(),
                        appVersion.isForceUpdate(),
                        appVersion.getMessage(),
                        appVersion.getTitle(),
                        appVersion.isShowButton(),
                        appVersion.getButtonText(),
                        appVersion.getButtonUrl(),
                        appVersion.getUpdatedAt()
                ))
                .orElse(null);
    }

    public LegalDocumentAdminResponse legalDocument(String documentKey) {
        return legalDocumentRepository.findById(documentKey)
                .map(legalDocument -> new LegalDocumentAdminResponse(
                        legalDocument.getDocumentKey(),
                        legalDocument.getTitle(),
                        new com.skuri.skuri_backend.domain.support.model.LegalDocumentBanner(
                                legalDocument.getBannerIconKey(),
                                List.copyOf(legalDocument.getBannerLines()),
                                legalDocument.getBannerTitle(),
                                legalDocument.getBannerTone()
                        ),
                        List.copyOf(legalDocument.getSections()),
                        List.copyOf(legalDocument.getFooterLines()),
                        legalDocument.isActive(),
                        legalDocument.getCreatedAt(),
                        legalDocument.getUpdatedAt()
                ))
                .orElse(null);
    }

    public CafeteriaMenuResponse cafeteriaMenu(String weekId) {
        return cafeteriaMenuRepository.findById(weekId)
                .map(cafeteriaMenuService::toResponse)
                .orElse(null);
    }

    private CourseScheduleSnapshot toCourseScheduleSnapshot(CourseSchedule schedule) {
        return new CourseScheduleSnapshot(schedule.getDayOfWeek(), schedule.getStartPeriod(), schedule.getEndPeriod());
    }

    private AppNoticeResponse toAppNoticeResponse(AppNotice appNotice) {
        return new AppNoticeResponse(
                appNotice.getId(),
                appNotice.getTitle(),
                appNotice.getContent(),
                appNotice.getCategory(),
                appNotice.getPriority(),
                List.copyOf(appNotice.getImageUrls()),
                appNotice.getActionUrl(),
                appNotice.getActionLabel(),
                appNotice.getViewCount(),
                appNotice.getLikeCount(),
                appNotice.getCommentCount(),
                false,
                appNotice.getPublishedAt(),
                appNotice.getCreatedAt(),
                appNotice.getUpdatedAt()
        );
    }

    private CampusBannerAdminResponse toCampusBannerResponse(CampusBanner campusBanner) {
        return new CampusBannerAdminResponse(
                campusBanner.getId(),
                campusBanner.getBadgeLabel(),
                campusBanner.getTitleLabel(),
                campusBanner.getDescriptionLabel(),
                campusBanner.getButtonLabel(),
                campusBanner.getPaletteKey(),
                campusBanner.getImageUrl(),
                campusBanner.getActionType(),
                campusBanner.getActionTarget(),
                campusBanner.getActionParams() == null
                        ? null
                        : ObjectMapperConfig.SHARED_OBJECT_MAPPER.convertValue(
                                campusBanner.getActionParams().deepCopy(),
                                new TypeReference<Map<String, Object>>() {
                                }
                        ),
                campusBanner.getActionUrl(),
                campusBanner.isActive(),
                campusBanner.getDisplayStartAt(),
                campusBanner.getDisplayEndAt(),
                campusBanner.getDisplayOrder(),
                campusBanner.getCreatedAt(),
                campusBanner.getUpdatedAt()
        );
    }

    private MemberAdminRoleSnapshot toMemberAdminRoleSnapshot(Member member) {
        return new MemberAdminRoleSnapshot(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.isAdmin(),
                member.getStatus()
        );
    }

    private PartyStatusSnapshot toPartyStatusSnapshot(Party party) {
        return new PartyStatusSnapshot(
                party.getId(),
                party.getStatus(),
                party.getEndReason(),
                party.getSettlementStatus(),
                party.getEndedAt()
        );
    }

    private PostModerationSnapshot toPostModerationSnapshot(Post post) {
        return new PostModerationSnapshot(
                post.getId(),
                post.getAuthorId(),
                post.getCategory(),
                post.isAnonymous(),
                post.isHidden(),
                post.isDeleted()
        );
    }

    private CommentModerationSnapshot toCommentModerationSnapshot(Comment comment) {
        return new CommentModerationSnapshot(
                comment.getId(),
                comment.getPost().getId(),
                comment.getAuthorId(),
                comment.hasParent() ? comment.getParent().getId() : null,
                comment.isAnonymous(),
                comment.isHidden(),
                comment.isDeleted()
        );
    }

    public record CourseSemesterSnapshot(
            String semester,
            int totalCourses,
            List<CourseSummarySnapshot> courses
    ) {
    }

    public record CourseSummarySnapshot(
            String id,
            String courseKey,
            String name,
            String department,
            String professor,
            Integer grade,
            Integer credits,
            List<CourseScheduleSnapshot> schedules
    ) {
    }

    public record CourseScheduleSnapshot(
            int dayOfWeek,
            int startPeriod,
            int endPeriod
    ) {
    }

    public record ChatRoomSnapshot(
            String id,
            String name,
            Object type,
            String description,
            boolean isPublic,
            String createdBy,
            int memberCount
    ) {
    }

    public record ChatMessageSnapshot(
            String id,
            String chatRoomId,
            String senderId,
            String senderName,
            Object type,
            String source,
            String text,
            LocalDateTime createdAt
    ) {
    }

    public record InquirySnapshot(
            String id,
            String memberId,
            Object type,
            String subject,
            String content,
            Object status,
            String memo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ReportSnapshot(
            String id,
            String reporterId,
            Object targetType,
            String targetId,
            String targetAuthorId,
            String category,
            String reason,
            Object status,
            String action,
            String memo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record MemberAdminRoleSnapshot(
            String id,
            String email,
            String nickname,
            boolean isAdmin,
            Object status
    ) {
    }

    public record PostModerationSnapshot(
            String id,
            String authorId,
            Object category,
            boolean anonymous,
            boolean hidden,
            boolean deleted
    ) {
    }

    public record CommentModerationSnapshot(
            String id,
            String postId,
            String authorId,
            String parentId,
            boolean anonymous,
            boolean hidden,
            boolean deleted
    ) {
    }

    public record PartyStatusSnapshot(
            String id,
            Object status,
            Object endReason,
            Object settlementStatus,
            LocalDateTime endedAt
    ) {
    }

    public record PartyMemberSnapshot(
            String partyId,
            String memberId,
            boolean isLeader,
            LocalDateTime joinedAt
    ) {
    }

    public record AppVersionSnapshot(
            String platform,
            String minimumVersion,
            boolean forceUpdate,
            String message,
            String title,
            boolean showButton,
            String buttonText,
            String buttonUrl,
            LocalDateTime updatedAt
    ) {
    }

    public record CampusBannerOrderSnapshot(
            String id,
            String titleLabel,
            int displayOrder
    ) {
    }
}
