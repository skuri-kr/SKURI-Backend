package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.domain.academic.repository.TimetableShareOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableSharingRelationshipCleanupService {

    private final TimetableShareOverrideRepository timetableShareOverrideRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteOverridesForMemberPair(String firstMemberId, String secondMemberId) {
        timetableShareOverrideRepository.deleteByOwnerMemberIdAndFriendMemberId(firstMemberId, secondMemberId);
        timetableShareOverrideRepository.deleteByOwnerMemberIdAndFriendMemberId(secondMemberId, firstMemberId);
    }
}
