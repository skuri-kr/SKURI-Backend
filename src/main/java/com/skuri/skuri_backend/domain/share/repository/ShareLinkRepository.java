package com.skuri.skuri_backend.domain.share.repository;

import com.skuri.skuri_backend.domain.share.entity.ShareLink;
import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, String> {

    Optional<ShareLink> findByResourceTypeAndResourceId(ShareResourceType resourceType, String resourceId);

    Optional<ShareLink> findByCodeAndResourceType(String code, ShareResourceType resourceType);
}
