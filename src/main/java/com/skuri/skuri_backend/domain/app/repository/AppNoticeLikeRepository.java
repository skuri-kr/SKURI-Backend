package com.skuri.skuri_backend.domain.app.repository;

import com.skuri.skuri_backend.domain.app.entity.AppNoticeLike;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppNoticeLikeRepository extends JpaRepository<AppNoticeLike, AppNoticeLikeId> {

    boolean existsById_UserIdAndId_AppNoticeId(String userId, String appNoticeId);

    Optional<AppNoticeLike> findById_UserIdAndId_AppNoticeId(String userId, String appNoticeId);

    List<AppNoticeLike> findById_UserId(String userId);

    List<AppNoticeLike> findById_AppNoticeId(String appNoticeId);
}
