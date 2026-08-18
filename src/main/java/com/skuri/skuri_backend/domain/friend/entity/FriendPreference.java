package com.skuri.skuri_backend.domain.friend.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Entity
@Table(name = "friend_preferences")
@IdClass(FriendPreference.Key.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendPreference extends BaseTimeEntity {

    @Id
    @Column(name = "owner_member_id", length = 36)
    private String ownerMemberId;

    @Id
    @Column(name = "friend_member_id", length = 36)
    private String friendMemberId;

    @Column(nullable = false)
    private boolean favorite;

    private FriendPreference(String ownerMemberId, String friendMemberId, boolean favorite) {
        this.ownerMemberId = ownerMemberId;
        this.friendMemberId = friendMemberId;
        this.favorite = favorite;
    }

    public static FriendPreference create(String ownerMemberId, String friendMemberId, boolean favorite) {
        return new FriendPreference(ownerMemberId, friendMemberId, favorite);
    }

    public void updateFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String ownerMemberId;
        private String friendMemberId;
    }
}
