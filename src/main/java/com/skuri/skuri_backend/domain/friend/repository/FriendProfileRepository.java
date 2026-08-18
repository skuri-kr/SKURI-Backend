package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;

public interface FriendProfileRepository extends JpaRepository<FriendProfile, String> {

    Optional<FriendProfile> findByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FriendProfile p where p.memberId = :memberId")
    Optional<FriendProfile> findByMemberIdForUpdate(@Param("memberId") String memberId);

    long countByMemberIdIn(Collection<String> memberIds);
}
