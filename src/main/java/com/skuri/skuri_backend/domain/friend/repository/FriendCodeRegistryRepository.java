package com.skuri.skuri_backend.domain.friend.repository;

import com.skuri.skuri_backend.domain.friend.entity.FriendCodeRegistry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FriendCodeRegistryRepository extends JpaRepository<FriendCodeRegistry, String> {

    Optional<FriendCodeRegistry> findByNormalizedCode(String normalizedCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FriendCodeRegistry c where c.id = :codeId")
    Optional<FriendCodeRegistry> findByIdForUpdate(@Param("codeId") String codeId);
}
