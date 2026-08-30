package com.skuri.skuri_backend.domain.support.repository;

import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d
            from LegalDocument d
            where d.documentKey = :documentKey
            """)
    Optional<LegalDocument> findByDocumentKeyForUpdate(@Param("documentKey") String documentKey);

    Optional<LegalDocument> findByDocumentKeyAndIsActiveTrue(String documentKey);

    List<LegalDocument> findAllByOrderByDocumentKeyAsc();
}
