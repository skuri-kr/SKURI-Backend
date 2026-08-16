package com.skuri.skuri_backend.domain.support.repository;

import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, String> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(String reporterId, ReportTargetType targetType, String targetId);

    List<Report> findByTargetType(ReportTargetType targetType);

    boolean existsByTargetTypeAndTargetImageAssetKey(
            ReportTargetType targetType,
            String targetImageAssetKey
    );

    List<Report> findByTargetImageAssetKeyIsNull(Pageable pageable);

    List<Report> findByTargetTypeAndTargetImageAssetKeyIsNull(ReportTargetType targetType);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Report r
            set r.targetImageAssetKey = :targetImageAssetKey
            where r.targetType = :targetType
              and r.targetId = :targetId
              and r.targetImageAssetKey is null
            """)
    int fillMissingTargetImageAssetKey(
            @Param("targetType") ReportTargetType targetType,
            @Param("targetId") String targetId,
            @Param("targetImageAssetKey") String targetImageAssetKey
    );

    long countByStatus(ReportStatus status);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start,
            LocalDateTime endExclusive
    );

    Page<Report> findByReporterId(String reporterId, Pageable pageable);

    @Query("""
            select r
            from Report r
            where (:status is null or r.status = :status)
              and (:targetType is null or r.targetType = :targetType)
            order by r.createdAt desc
            """)
    Page<Report> search(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            Pageable pageable
    );
}
