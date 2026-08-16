package com.skuri.skuri_backend.domain.support.repository;

import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ReportRepositoryDataJpaTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void fillMissingTargetImageAssetKeyById_관리자검토결과를보존한다() {
        Report report = Report.create(
                ReportTargetType.CHAT_MESSAGE,
                "message-1",
                "member-1",
                "SPAM",
                "광고성 이미지입니다.",
                "reporter-1"
        );
        ReflectionTestUtils.setField(report, "targetImageAssetKey", null);
        report = reportRepository.saveAndFlush(report);
        report.updateReview(ReportStatus.ACTIONED, "DELETE_CHAT_MESSAGE", "처리 완료");
        reportRepository.saveAndFlush(report);
        entityManager.clear();

        int updated = reportRepository.fillMissingTargetImageAssetKeyById(
                report.getId(),
                "chat/2026/08/reported-image"
        );
        entityManager.clear();

        Report persisted = reportRepository.findById(report.getId()).orElseThrow();
        assertEquals(1, updated);
        assertEquals(ReportStatus.ACTIONED, persisted.getStatus());
        assertEquals("DELETE_CHAT_MESSAGE", persisted.getAction());
        assertEquals("처리 완료", persisted.getAdminMemo());
        assertEquals("chat/2026/08/reported-image", persisted.getTargetImageAssetKey());
    }
}
