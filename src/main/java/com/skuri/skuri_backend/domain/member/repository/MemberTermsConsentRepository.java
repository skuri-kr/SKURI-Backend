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
            on duplicate key update
                source = 'SIGNUP',
                accepted_at = case
                    when accepted_at is null or accepted_at > values(accepted_at)
                        then values(accepted_at)
                    else accepted_at
                end,
                updated_at = current_timestamp
            """, nativeQuery = true)
    int upsertSignupConsent(
            @Param("id") String id,
            @Param("memberId") String memberId,
            @Param("termsVersion") String termsVersion,
            @Param("acceptedAt") LocalDateTime acceptedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update member_terms_consents
            set source = 'SIGNUP',
                accepted_at = coalesce(accepted_at, :backfillAt),
                updated_at = current_timestamp
            where terms_version = :termsVersion
              and (source <> 'SIGNUP' or accepted_at is null)
            """, nativeQuery = true)
    int normalizeCurrentVersionConsents(
            @Param("termsVersion") String termsVersion,
            @Param("backfillAt") LocalDateTime backfillAt
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
                'SIGNUP',
                :backfillAt,
                current_timestamp,
                current_timestamp
            from members m
            on duplicate key update
                source = 'SIGNUP',
                accepted_at = case
                    when accepted_at is null or accepted_at > values(accepted_at)
                        then values(accepted_at)
                    else accepted_at
                end,
                updated_at = current_timestamp
            """, nativeQuery = true)
    int backfillAllMemberSignupConsents(
            @Param("termsVersion") String termsVersion,
            @Param("backfillAt") LocalDateTime backfillAt
    );
}
