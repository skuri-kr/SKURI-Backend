package com.skuri.skuri_backend.domain.academic.repository;

import com.skuri.skuri_backend.domain.academic.entity.TimetableShareOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TimetableShareOverrideRepository extends JpaRepository<TimetableShareOverride, TimetableShareOverride.Key> {

    List<TimetableShareOverride> findAllByOwnerMemberId(String ownerMemberId);

    List<TimetableShareOverride> findAllByOwnerMemberIdAndFriendMemberIdIn(
            String ownerMemberId,
            Collection<String> friendMemberIds
    );

    void deleteByOwnerMemberIdAndFriendMemberId(String ownerMemberId, String friendMemberId);
}
