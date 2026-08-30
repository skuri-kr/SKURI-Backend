package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.entity.MemberTermsConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MemberTermsConsentRepository extends JpaRepository<MemberTermsConsent, String> {

    boolean existsByMember_IdAndTermsVersion(String memberId, String termsVersion);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into member_terms_consents (
                id,
                member_id,
                terms_version,
                source,
                accepted_at,
                created_at,
                updated_at
            ) values (
                :id,
                :memberId,
                :termsVersion,
                'SIGNUP',
                :acceptedAt,
                current_timestamp,
                current_timestamp
            )
            on duplicate key update id = id
            """, nativeQuery = true)
    int insertSignupConsentIfAbsent(
            @Param("id") String id,
            @Param("memberId") String memberId,
            @Param("termsVersion") String termsVersion,
            @Param("acceptedAt") LocalDateTime acceptedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            insert into member_terms_consents (
                id,
                member_id,
                terms_version,
                source,
                accepted_at,
                created_at,
                updated_at
            )
            select
                uuid(),
                m.id,
                :termsVersion,
                'EMAIL_BACKFILL',
                null,
                current_timestamp,
                current_timestamp
            from members m
            where m.status = 'ACTIVE'
              and m.joined_at <= :joinedAtCutoff
              and not exists (
                  select 1
                  from member_terms_consents c
                  where c.member_id = m.id
                    and c.terms_version = :termsVersion
              )
            """, nativeQuery = true)
    int backfillEmailConsents(
            @Param("termsVersion") String termsVersion,
            @Param("joinedAtCutoff") LocalDateTime joinedAtCutoff
    );
}
