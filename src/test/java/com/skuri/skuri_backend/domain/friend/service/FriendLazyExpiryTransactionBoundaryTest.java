package com.skuri.skuri_backend.domain.friend.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
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
                .getMethod("getRelationshipState", String.class, String.class)
                .getAnnotation(Transactional.class))
                .isNull();
        assertThat(FriendCodeService.class
                .getMethod("preview", String.class, String.class)
                .getAnnotation(Transactional.class))
                .isNull();
    }

    @Test
    void 직접요청전이는_만료트랜잭션을호출하기전에_바깥트랜잭션을유지하지않는다() throws Exception {
        assertThat(FriendRequestTransitionService.class
                .getMethod("acceptRequest", String.class, String.class)
                .getAnnotation(Transactional.class))
                .isNull();
        assertThat(FriendRequestTransitionService.class
                .getMethod("declineRequest", String.class, String.class)
                .getAnnotation(Transactional.class))
                .isNull();
        assertThat(FriendRequestTransitionService.class
                .getMethod("cancelRequest", String.class, String.class)
                .getAnnotation(Transactional.class))
                .isNull();

        assertThat(FriendRequestTransitionPreflightService.class
                .getMethod("prepareForRecipient", String.class, String.class)
                .getAnnotation(Transactional.class))
                .isNotNull();
        assertThat(FriendRequestTransitionMutationService.class
                .getMethod("acceptRequest", String.class, String.class)
                .getAnnotation(Transactional.class))
                .isNotNull();
        assertThat(FriendRequestExpiryService.class
                .getMethod("expireRequestIfNeeded", String.class)
                .getAnnotation(Transactional.class)
                .propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }
}
