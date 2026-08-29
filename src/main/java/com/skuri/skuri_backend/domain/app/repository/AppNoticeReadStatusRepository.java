package com.skuri.skuri_backend.domain.app.repository;

import com.skuri.skuri_backend.domain.app.entity.AppNoticeReadStatus;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeReadStatusId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppNoticeReadStatusRepository extends JpaRepository<AppNoticeReadStatus, AppNoticeReadStatusId> {

    Optional<AppNoticeReadStatus> findById_UserIdAndId_AppNoticeId(String userId, String appNoticeId);

    long deleteById_UserId(String userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from AppNoticeReadStatus status where status.id.appNoticeId = :appNoticeId")
    long deleteById_AppNoticeId(@Param("appNoticeId") String appNoticeId);
}
