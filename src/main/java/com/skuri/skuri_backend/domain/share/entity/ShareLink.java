package com.skuri.skuri_backend.domain.share.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.share.model.ShareResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "share_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_share_links_resource",
                columnNames = {"resource_type", "resource_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareLink extends BaseTimeEntity {

    @Id
    @Column(length = 8)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 16)
    private ShareResourceType resourceType;

    @Column(name = "resource_id", nullable = false, length = 160)
    private String resourceId;

    private ShareLink(String code, ShareResourceType resourceType, String resourceId) {
        this.code = code;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public static ShareLink create(String code, ShareResourceType resourceType, String resourceId) {
        return new ShareLink(code, resourceType, resourceId);
    }
}
