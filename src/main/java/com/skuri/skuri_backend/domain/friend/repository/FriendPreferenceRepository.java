package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.FriendPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface FriendPreferenceRepository extends JpaRepository<FriendPreference, FriendPreference.Key> {

    Optional<FriendPreference> findByOwnerMemberIdAndFriendMemberId(String ownerMemberId, String friendMemberId);

    List<FriendPreference> findAllByOwnerMemberIdAndFriendMemberIdIn(String ownerMemberId, Collection<String> friendMemberIds);

    void deleteByOwnerMemberIdAndFriendMemberId(String ownerMemberId, String friendMemberId);

    long deleteByOwnerMemberIdOrFriendMemberId(String ownerMemberId, String friendMemberId);
}
