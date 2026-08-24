package com.skuri.skuri_backend.domain.academic.repository;

import com.skuri.skuri_backend.domain.academic.entity.TimetableSharingSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableSharingSettingRepository extends JpaRepository<TimetableSharingSetting, String> {

    long deleteByOwnerMemberId(String ownerMemberId);
}
