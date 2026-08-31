package com.skuri.skuri_backend.domain.contentblock.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "content_blocks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_content_blocks_blocker_blocked",
                columnNames = {"blocker_id", "blocked_id"}
        ),
        indexes = {
                @Index(name = "idx_content_blocks_blocker_created", columnList = "blocker_id, created_at"),
                @Index(name = "idx_content_blocks_blocked", columnList = "blocked_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "blocker_id", nullable = false, length = 36)
    private String blockerId;

    @Column(name = "blocked_id", nullable = false, length = 36)
    private String blockedId;

    private ContentBlock(String blockerId, String blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }

    public static ContentBlock create(String blockerId, String blockedId) {
        return new ContentBlock(blockerId, blockedId);
    }
}
