package com.skuri.skuri_backend.domain.contentblock.service;

import com.skuri.skuri_backend.domain.contentblock.repository.ContentBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentBlockQueryService {

    private final ContentBlockRepository contentBlockRepository;

    @Transactional(readOnly = true)
    public boolean isBlockedBy(String blockerId, String candidateBlockedId) {
        if (!hasText(blockerId) || !hasText(candidateBlockedId) || blockerId.equals(candidateBlockedId)) {
            return false;
        }
        return contentBlockRepository.existsByBlockerIdAndBlockedId(blockerId, candidateBlockedId);
    }

    @Transactional(readOnly = true)
    public Set<String> findBlockedMemberIds(String blockerId, Collection<String> candidateMemberIds) {
        if (!hasText(blockerId) || candidateMemberIds == null || candidateMemberIds.isEmpty()) {
            return Set.of();
        }
        Set<String> candidates = candidateMemberIds.stream()
                .filter(ContentBlockQueryService::hasText)
                .filter(candidate -> !blockerId.equals(candidate))
                .collect(Collectors.toSet());
        if (candidates.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(contentBlockRepository.findBlockedMemberIds(blockerId, candidates));
    }

    @Transactional(readOnly = true)
    public Set<String> findMemberIdsBlocking(
            String blockedMemberId,
            Collection<String> candidateBlockerIds
    ) {
        if (!hasText(blockedMemberId) || candidateBlockerIds == null || candidateBlockerIds.isEmpty()) {
            return Set.of();
        }
        Set<String> candidates = candidateBlockerIds.stream()
                .filter(ContentBlockQueryService::hasText)
                .filter(candidate -> !blockedMemberId.equals(candidate))
                .collect(Collectors.toSet());
        if (candidates.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(contentBlockRepository.findBlockerMemberIds(blockedMemberId, candidates));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
