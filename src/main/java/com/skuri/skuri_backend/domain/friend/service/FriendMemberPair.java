package com.skuri.skuri_backend.domain.friend.service;

/**
 * 친구 관계를 저장하고 조회할 때 사용하는 정규화된 회원 ID 쌍이다.
 *
 * <p>DB collation과 무관하게 Java의 {@link String#compareTo(String)} 결과를
 * 단일 기준으로 사용한다. DB 정렬은 비관적 잠금 획득 순서에만 사용한다.</p>
 */
public record FriendMemberPair(String lowMemberId, String highMemberId) {

    public static FriendMemberPair of(String firstMemberId, String secondMemberId) {
        if (firstMemberId.compareTo(secondMemberId) <= 0) {
            return new FriendMemberPair(firstMemberId, secondMemberId);
        }
        return new FriendMemberPair(secondMemberId, firstMemberId);
    }

    public String activePairKey() {
        return lowMemberId + ":" + highMemberId;
    }
}
