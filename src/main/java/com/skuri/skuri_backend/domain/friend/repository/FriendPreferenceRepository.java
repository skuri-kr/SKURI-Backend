package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.FriendPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FriendPreferenceRepository extends JpaRepository<FriendPreference, FriendPreference.Key> {

    Optional<FriendPreference> findByOwnerMemberIdAndFriendMemberId(String ownerMemberId, String friendMemberId);

    void deleteByOwnerMemberIdAndFriendMemberId(String ownerMemberId, String friendMemberId);
}
