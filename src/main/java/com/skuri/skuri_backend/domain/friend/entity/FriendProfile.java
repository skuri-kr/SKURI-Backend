package com.skuri.skuri_backend.domain.friend.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "friend_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendProfile extends BaseTimeEntity {

    @Id
    @Column(name = "member_id", length = 36)
    private String memberId;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "active_friend_code_id", nullable = false, unique = true, length = 36)
    private String activeFriendCodeId;

    @Column(name = "nickname_searchable", nullable = false)
    private boolean nicknameSearchable;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    private FriendProfile(String memberId, String publicId, String activeFriendCodeId) {
        this.memberId = memberId;
        this.publicId = publicId;
        this.activeFriendCodeId = activeFriendCodeId;
        this.nicknameSearchable = true;
    }

    public static FriendProfile create(String memberId, String publicId, String activeFriendCodeId) {
        return new FriendProfile(memberId, publicId, activeFriendCodeId);
    }

    public void replaceActiveFriendCode(String activeFriendCodeId, LocalDateTime rotatedAt) {
        this.activeFriendCodeId = activeFriendCodeId;
        this.rotatedAt = rotatedAt;
    }

    public boolean canRegenerateAt(LocalDateTime now) {
        return rotatedAt == null || !rotatedAt.plusHours(24).isAfter(now);
    }

    public LocalDateTime nextRegenerationAt() {
        return rotatedAt == null ? null : rotatedAt.plusHours(24);
    }

    public void updateNicknameSearchable(boolean nicknameSearchable) {
        this.nicknameSearchable = nicknameSearchable;
    }
}
