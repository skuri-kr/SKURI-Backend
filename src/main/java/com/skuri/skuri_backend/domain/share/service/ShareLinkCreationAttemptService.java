package com.skuri.skuri_backend.domain.share.service;

import com.skuri.skuri_backend.domain.share.entity.ShareLink;
import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import com.skuri.skuri_backend.domain.share.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ShareLinkCreationAttemptService {

    private final ShareLinkRepository shareLinkRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShareLink getOrCreate(ShareResourceType resourceType, String resourceId, String code) {
        return shareLinkRepository.findByResourceTypeAndResourceId(resourceType, resourceId)
                .orElseGet(() -> shareLinkRepository.saveAndFlush(ShareLink.create(code, resourceType, resourceId)));
    }
}
