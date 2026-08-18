package com.skuri.skuri_backend.domain.friend.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class FriendLazyExpiryTransactionBoundaryTest {

    @Test
    void lazy만료정리조회는_독립만료트랜잭션앞에_바깥트랜잭션을유지하지않는다() throws Exception {
        assertThat(FriendRelationshipQueryService.class
                .getMethod("search", String.class, String.class, String.class, Integer.class)
                .getAnnotation(Transactional.class))
                .isNull();
        assertThat(FriendRelationshipQueryService.class
                .getMethod("getRequests", String.class, FriendRelationshipQueryService.FriendRequestDirection.class, String.class, Integer.class)
                .getAnnotation(Transactional.class))
                .isNull();
        assertThat(FriendRelationshipQueryService.class
                .getMethod("getInboxCounts", String.class)
                .getAnnotation(Transactional.class))
                .isNull();
        assertThat(FriendRelationshipQueryService.class
                .getMethod("canSendFriendRequest", String.class, String.class)
                .getAnnotation(Transactional.class))
                .isNull();
        assertThat(FriendCodeService.class
                .getMethod("preview", String.class, String.class)
                .getAnnotation(Transactional.class))
                .isNull();
    }
}
