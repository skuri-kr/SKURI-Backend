package com.skuri.skuri_backend.domain.member.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "member_terms_consents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_terms_consents_member_version",
                columnNames = {"member_id", "terms_version"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTermsConsent extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "terms_version", nullable = false, length = 32)
    private String termsVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MemberTermsConsentSource source;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

}
