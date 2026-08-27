package com.skuri.skuri_backend.domain.admin.dashboard.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardActivityResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardRecentItemResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardRecentItemType;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardSummaryResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Seoul");
    static final String DASHBOARD_TIMEZONE = "Asia/Seoul";
    private static final int DEFAULT_ACTIVITY_DAYS = 7;
    private static final int EXTENDED_ACTIVITY_DAYS = 30;
    private static final int DEFAULT_RECENT_ITEM_LIMIT = 10;
    private static final int MAX_RECENT_ITEM_LIMIT = 30;

    private final MemberRepository memberRepository;
    private final PartyRepository partyRepository;
    private final InquiryRepository inquiryRepository;
    private final ReportRepository reportRepository;
    private final AppNoticeRepository appNoticeRepository;

    private Clock clock = Clock.system(DASHBOARD_ZONE);

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary() {
        LocalDateTime now = now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();

        return new AdminDashboardSummaryResponse(
                memberRepository.countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(todayStart, currentEndExclusive(now)),
                memberRepository.count(),
                memberRepository.countByIsAdminTrue(),
                partyRepository.countByStatus(PartyStatus.OPEN),
                inquiryRepository.countByStatus(InquiryStatus.PENDING),
                reportRepository.countByStatus(ReportStatus.PENDING),
                now
        );
    }

    @Transactional(readOnly = true)
    public AdminDashboardActivityResponse getActivity(int days) {
        validateDays(days);

        LocalDateTime now = now();
        LocalDate today = now.toLocalDate();
        LocalDate startDate = today.minusDays(days - 1L);

        List<AdminDashboardActivityResponse.ActivitySeriesItem> series = startDate.datesUntil(today.plusDays(1))
                .map(date -> toSeriesItem(date, today, now))
                .toList();

        return new AdminDashboardActivityResponse(days, DASHBOARD_TIMEZONE, series);
    }

    @Transactional(readOnly = true)
    public List<AdminDashboardRecentItemResponse> getRecentItems(Integer limit) {
        int resolvedLimit = limit == null ? DEFAULT_RECENT_ITEM_LIMIT : limit;
        validateLimit(resolvedLimit);

        LocalDateTime now = now();
        PageRequest pageable = PageRequest.of(0, resolvedLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        return Stream.of(
                        inquiryRepository.findAllByOrderByCreatedAtDesc(pageable).getContent().stream()
                                .map(this::toInquiryRecentItem),
                        reportRepository.findAll(pageable).getContent().stream()
                                .map(this::toReportRecentItem),
                        appNoticeRepository.findRecentPublishedForAdmin(now, pageable).stream()
                                .map(this::toAppNoticeRecentItem),
                        partyRepository.findAll(pageable).getContent().stream()
                                .map(this::toPartyRecentItem)
                )
                .flatMap(stream -> stream)
                .sorted(Comparator.comparing(AdminDashboardRecentItemResponse::createdAt).reversed())
                .limit(resolvedLimit)
                .toList();
    }

    private AdminDashboardActivityResponse.ActivitySeriesItem toSeriesItem(
            LocalDate date,
            LocalDate today,
            LocalDateTime now
    ) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime endExclusive = date.equals(today)
                ? currentEndExclusive(now)
                : date.plusDays(1).atStartOfDay();

        return new AdminDashboardActivityResponse.ActivitySeriesItem(
                date,
                memberRepository.countByJoinedAtGreaterThanEqualAndJoinedAtLessThan(start, endExclusive),
                inquiryRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, endExclusive),
                reportRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, endExclusive),
                partyRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, endExclusive)
        );
    }

    private AdminDashboardRecentItemResponse toInquiryRecentItem(Inquiry inquiry) {
        return new AdminDashboardRecentItemResponse(
                AdminDashboardRecentItemType.INQUIRY,
                inquiry.getId(),
                inquiry.getSubject(),
                inquiry.getStatus().name() + " · " + inquiry.getUserId(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt()
        );
    }

    private AdminDashboardRecentItemResponse toReportRecentItem(Report report) {
        return new AdminDashboardRecentItemResponse(
                AdminDashboardRecentItemType.REPORT,
                report.getId(),
                reportTitle(report.getTargetType()),
                report.getStatus().name() + " · " + report.getTargetType().name(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }

    private AdminDashboardRecentItemResponse toAppNoticeRecentItem(AppNotice appNotice) {
        return new AdminDashboardRecentItemResponse(
                AdminDashboardRecentItemType.APP_NOTICE,
                appNotice.getId(),
                appNotice.getTitle(),
                appNotice.getPriority().name(),
                "PUBLISHED",
                appNotice.getCreatedAt()
        );
    }

    private AdminDashboardRecentItemResponse toPartyRecentItem(Party party) {
        return new AdminDashboardRecentItemResponse(
                AdminDashboardRecentItemType.PARTY,
                party.getId(),
                party.getDeparture().getName() + " -> " + party.getDestination().getName(),
                party.getStatus().name() + " · " + party.getLeaderId(),
                party.getStatus().name(),
                party.getCreatedAt()
        );
    }

    private String reportTitle(ReportTargetType targetType) {
        return switch (targetType) {
            case POST -> "게시글 신고";
            case COMMENT -> "댓글 신고";
            case NOTICE_COMMENT -> "공지 댓글 신고";
            case MEMBER -> "회원 신고";
            case CHAT_MESSAGE -> "채팅 메시지 신고";
            case CHAT_ROOM -> "채팅방 신고";
            case TAXI_PARTY -> "택시 파티 신고";
        };
    }

    private void validateDays(int days) {
        if (days != DEFAULT_ACTIVITY_DAYS && days != EXTENDED_ACTIVITY_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "days는 7 또는 30만 허용합니다.");
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_RECENT_ITEM_LIMIT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "limit는 1 이상 30 이하여야 합니다.");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private LocalDateTime currentEndExclusive(LocalDateTime now) {
        return now.plusNanos(1);
    }
}
