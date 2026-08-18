package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendRequestTransitionService {

    private final FriendRequestTransitionPreflightService preflightService;
    private final FriendRequestExpiryService friendRequestExpiryService;
    private final FriendRequestTransitionMutationService mutationService;

    public FriendRequestAcceptAttempt acceptRequest(String recipientMemberId, String requestId) {
        FriendRequestTransitionPreflightService.FriendRequestSnapshot snapshot = preflightService
                .prepareForRecipient(recipientMemberId, requestId);
        if (snapshot.expired()) {
            friendRequestExpiryService.expireRequestIfNeeded(snapshot.requestId());
            return FriendRequestAcceptAttempt.stateNotAllowed();
        }
        return mutationService.acceptRequest(recipientMemberId, snapshot.requestId());
    }

    public FriendRequestTerminalAttempt declineRequest(String recipientMemberId, String requestId) {
        FriendRequestTransitionPreflightService.FriendRequestSnapshot snapshot = preflightService
                .prepareForRecipient(recipientMemberId, requestId);
        if (snapshot.expired()) {
            friendRequestExpiryService.expireRequestIfNeeded(snapshot.requestId());
            return FriendRequestTerminalAttempt.stateNotAllowed();
        }
        return mutationService.declineRequest(recipientMemberId, snapshot.requestId());
    }

    public FriendRequestTerminalAttempt cancelRequest(String requesterMemberId, String requestId) {
        FriendRequestTransitionPreflightService.FriendRequestSnapshot snapshot = preflightService
                .prepareForRequester(requesterMemberId, requestId);
        if (snapshot.expired()) {
            friendRequestExpiryService.expireRequestIfNeeded(snapshot.requestId());
            return FriendRequestTerminalAttempt.stateNotAllowed();
        }
        return mutationService.cancelRequest(requesterMemberId, snapshot.requestId());
    }

    public record FriendRequestAcceptAttempt(boolean accepted, FriendSummaryResponse friend) {
        static FriendRequestAcceptAttempt accepted(FriendSummaryResponse friend) {
            return new FriendRequestAcceptAttempt(true, friend);
        }

        static FriendRequestAcceptAttempt stateNotAllowed() {
            return new FriendRequestAcceptAttempt(false, null);
        }
    }

    public record FriendRequestTerminalAttempt(boolean completed) {
        static FriendRequestTerminalAttempt success() {
            return new FriendRequestTerminalAttempt(true);
        }

        static FriendRequestTerminalAttempt stateNotAllowed() {
            return new FriendRequestTerminalAttempt(false);
        }
    }
}
