package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class JoinRequestSseService {

    private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;
    private static final long SSE_RETRY_MILLIS = 3_000L;
    private static final int SUBSCRIBER_WARNING_THRESHOLD = 500;
    private static final int FAILURE_RATE_WINDOW_MINUTES = 5;
    private static final double FAILURE_RATE_WARNING_THRESHOLD = 5.0;

    private static final String PARTY_JOIN_REQUESTS_ENDPOINT = "/v1/sse/parties/{partyId}/join-requests";
    private static final String MY_JOIN_REQUESTS_ENDPOINT = "/v1/sse/members/me/join-requests";

    private final JoinRequestSseSnapshotService joinRequestSseSnapshotService;

    private final Map<String, PartyJoinRequestSubscriber> partyJoinRequestSubscribers = new ConcurrentHashMap<>();
    private final Map<String, MyJoinRequestSubscriber> myJoinRequestSubscribers = new ConcurrentHashMap<>();

    private final AtomicLong windowSendCount = new AtomicLong();
    private final AtomicLong windowFailureCount = new AtomicLong();
    private volatile LocalDateTime failureRateWindowStartedAt = LocalDateTime.now();

    public SseEmitter subscribePartyJoinRequests(String actorId, String partyId) {
        Map<String, Object> snapshotPayload =
                joinRequestSseSnapshotService.createPartyJoinRequestsSnapshotPayload(actorId, partyId);
        String emitterId = "party:" + partyId + ":leader:" + actorId + ":" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        partyJoinRequestSubscribers.put(emitterId, new PartyJoinRequestSubscriber(partyId, actorId, emitter));
        registerPartySubscriberLifecycle(emitterId, emitter);
        logSubscriberLifecycle(PARTY_JOIN_REQUESTS_ENDPOINT, "subscribe", emitterId, actorId, partyId, null, null);
        log.info(
                "SSE subscribe snapshot prepared: endpoint={}, emitterId={}, memberId={}, partyId={}, partySubscribers={}, mySubscribers={}, txActive={}",
                PARTY_JOIN_REQUESTS_ENDPOINT,
                emitterId,
                actorId,
                partyId,
                partyJoinRequestSubscribers.size(),
                myJoinRequestSubscribers.size(),
                TransactionSynchronizationManager.isActualTransactionActive()
        );

        sendToPartySubscriber(emitterId, "SNAPSHOT", snapshotPayload);
        return emitter;
    }

    public SseEmitter subscribeMyJoinRequests(String memberId, JoinRequestStatus status) {
        Map<String, Object> snapshotPayload =
                joinRequestSseSnapshotService.createMyJoinRequestsSnapshotPayload(memberId, status);
        String statusOrAll = status == null ? "ALL" : status.name();
        String emitterId = "my-requests:" + memberId + ":" + statusOrAll + ":" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        myJoinRequestSubscribers.put(emitterId, new MyJoinRequestSubscriber(memberId, status, emitter));
        registerMySubscriberLifecycle(emitterId, emitter);
        logSubscriberLifecycle(MY_JOIN_REQUESTS_ENDPOINT, "subscribe", emitterId, memberId, null, status, null);
        log.info(
                "SSE subscribe snapshot prepared: endpoint={}, emitterId={}, memberId={}, statusFilter={}, partySubscribers={}, mySubscribers={}, txActive={}",
                MY_JOIN_REQUESTS_ENDPOINT,
                emitterId,
                memberId,
                statusOrAll,
                partyJoinRequestSubscribers.size(),
                myJoinRequestSubscribers.size(),
                TransactionSynchronizationManager.isActualTransactionActive()
        );

        sendToMySubscriber(emitterId, "SNAPSHOT", snapshotPayload);
        return emitter;
    }

    public void publishJoinRequestCreated(JoinRequest joinRequest) {
        JoinRequestEvent event = JoinRequestEvent.from(joinRequest, null);
        publishAfterCommit(() -> {
            var payload = joinRequestSseSnapshotService.toSseItem(event.joinRequestId());
            if (payload == null) {
                return;
            }
            int leaderRecipients = broadcastToPartyLeaders(
                    event.partyId(),
                    event.leaderId(),
                    "JOIN_REQUEST_CREATED",
                    payload
            );
            int requesterRecipients = broadcastToMyJoinRequests(
                    event.requesterId(),
                    null,
                    event.currentStatus(),
                    "MY_JOIN_REQUEST_CREATED",
                    payload
            );
            logEventPublish("JOIN_REQUEST_CREATED", leaderRecipients);
            logEventPublish("MY_JOIN_REQUEST_CREATED", requesterRecipients);
        });
    }

    public void publishJoinRequestUpdated(JoinRequest joinRequest, JoinRequestStatus previousStatus) {
        JoinRequestEvent event = JoinRequestEvent.from(joinRequest, previousStatus);
        publishAfterCommit(() -> {
            var payload = joinRequestSseSnapshotService.toSseItem(event.joinRequestId());
            if (payload == null) {
                return;
            }
            int leaderRecipients = broadcastToPartyLeaders(
                    event.partyId(),
                    event.leaderId(),
                    "JOIN_REQUEST_UPDATED",
                    payload
            );
            int requesterRecipients = broadcastToMyJoinRequests(
                    event.requesterId(),
                    event.previousStatus(),
                    event.currentStatus(),
                    "MY_JOIN_REQUEST_UPDATED",
                    payload
            );
            logEventPublish("JOIN_REQUEST_UPDATED", leaderRecipients);
            logEventPublish("MY_JOIN_REQUEST_UPDATED", requesterRecipients);
        });
    }

    public void publishHeartbeat() {
        if (partyJoinRequestSubscribers.isEmpty() && myJoinRequestSubscribers.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of("timestamp", LocalDateTime.now());

        int partyRecipients = 0;
        for (String emitterId : partyJoinRequestSubscribers.keySet()) {
            if (sendToPartySubscriber(emitterId, "HEARTBEAT", payload)) {
                partyRecipients++;
            }
        }
        int myRecipients = 0;
        for (String emitterId : myJoinRequestSubscribers.keySet()) {
            if (sendToMySubscriber(emitterId, "HEARTBEAT", payload)) {
                myRecipients++;
            }
        }
        logEventPublish("HEARTBEAT", partyRecipients + myRecipients);
    }

    public void closeSubscriptionsForMember(String memberId) {
        partyJoinRequestSubscribers.forEach((emitterId, subscriber) -> {
            if (!memberId.equals(subscriber.leaderId())) {
                return;
            }
            partyJoinRequestSubscribers.remove(emitterId);
            logSubscriberLifecycle(PARTY_JOIN_REQUESTS_ENDPOINT, "close", emitterId, memberId, subscriber.partyId(), null, null);
            safeComplete(subscriber.emitter());
        });

        myJoinRequestSubscribers.forEach((emitterId, subscriber) -> {
            if (!memberId.equals(subscriber.memberId())) {
                return;
            }
            myJoinRequestSubscribers.remove(emitterId);
            logSubscriberLifecycle(
                    MY_JOIN_REQUESTS_ENDPOINT,
                    "close",
                    emitterId,
                    memberId,
                    null,
                    subscriber.statusFilter(),
                    null
            );
            safeComplete(subscriber.emitter());
        });
    }

    private void publishAfterCommit(Runnable publisher) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.run();
                }
            });
            return;
        }
        publisher.run();
    }

    private int broadcastToPartyLeaders(String partyId, String leaderId, String eventType, Object payload) {
        if (partyJoinRequestSubscribers.isEmpty()) {
            return 0;
        }

        int recipients = 0;
        for (Map.Entry<String, PartyJoinRequestSubscriber> entry : partyJoinRequestSubscribers.entrySet()) {
            PartyJoinRequestSubscriber subscriber = entry.getValue();
            if (!subscriber.partyId().equals(partyId) || !subscriber.leaderId().equals(leaderId)) {
                continue;
            }
            if (sendToPartySubscriber(entry.getKey(), eventType, payload)) {
                recipients++;
            }
        }
        return recipients;
    }

    private int broadcastToMyJoinRequests(
            String memberId,
            JoinRequestStatus previousStatus,
            JoinRequestStatus currentStatus,
            String eventType,
            Object payload
    ) {
        if (myJoinRequestSubscribers.isEmpty()) {
            return 0;
        }

        int recipients = 0;
        for (Map.Entry<String, MyJoinRequestSubscriber> entry : myJoinRequestSubscribers.entrySet()) {
            MyJoinRequestSubscriber subscriber = entry.getValue();
            if (!subscriber.memberId().equals(memberId)) {
                continue;
            }
            if (!matchesMyStatusFilter(subscriber.statusFilter(), previousStatus, currentStatus)) {
                continue;
            }
            if (sendToMySubscriber(entry.getKey(), eventType, payload)) {
                recipients++;
            }
        }
        return recipients;
    }

    private boolean matchesMyStatusFilter(
            JoinRequestStatus statusFilter,
            JoinRequestStatus previousStatus,
            JoinRequestStatus currentStatus
    ) {
        if (statusFilter == null) {
            return true;
        }
        return statusFilter == currentStatus || (previousStatus != null && statusFilter == previousStatus);
    }

    private boolean sendToPartySubscriber(String emitterId, String eventType, Object payload) {
        PartyJoinRequestSubscriber subscriber = partyJoinRequestSubscribers.get(emitterId);
        if (subscriber == null) {
            return false;
        }

        try {
            subscriber.emitter().send(
                    SseEmitter.event()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name(eventType)
                            .reconnectTime(SSE_RETRY_MILLIS)
                            .data(payload)
            );
            recordSendResult(true);
            return true;
        } catch (IOException | IllegalStateException e) {
            partyJoinRequestSubscribers.remove(emitterId);
            safeComplete(subscriber.emitter());
            recordSendResult(false);
            log.warn("JoinRequest SSE 전송 실패(파티 구독 해제): emitterId={}, eventType={}", emitterId, eventType);
            logSubscriberLifecycle(
                    PARTY_JOIN_REQUESTS_ENDPOINT,
                    "cleanup-on-send-fail",
                    emitterId,
                    subscriber.leaderId(),
                    subscriber.partyId(),
                    null,
                    null
            );
            return false;
        }
    }

    private boolean sendToMySubscriber(String emitterId, String eventType, Object payload) {
        MyJoinRequestSubscriber subscriber = myJoinRequestSubscribers.get(emitterId);
        if (subscriber == null) {
            return false;
        }

        try {
            subscriber.emitter().send(
                    SseEmitter.event()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name(eventType)
                            .reconnectTime(SSE_RETRY_MILLIS)
                            .data(payload)
            );
            recordSendResult(true);
            return true;
        } catch (IOException | IllegalStateException e) {
            myJoinRequestSubscribers.remove(emitterId);
            safeComplete(subscriber.emitter());
            recordSendResult(false);
            log.warn("JoinRequest SSE 전송 실패(내 요청 구독 해제): emitterId={}, eventType={}", emitterId, eventType);
            logSubscriberLifecycle(
                    MY_JOIN_REQUESTS_ENDPOINT,
                    "cleanup-on-send-fail",
                    emitterId,
                    subscriber.memberId(),
                    null,
                    subscriber.statusFilter(),
                    null
            );
            return false;
        }
    }

    private void registerPartySubscriberLifecycle(String emitterId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            PartyJoinRequestSubscriber subscriber = partyJoinRequestSubscribers.remove(emitterId);
            if (subscriber == null) {
                return;
            }
            logSubscriberLifecycle(
                    PARTY_JOIN_REQUESTS_ENDPOINT,
                    "complete",
                    emitterId,
                    subscriber != null ? subscriber.leaderId() : null,
                    subscriber != null ? subscriber.partyId() : null,
                    null,
                    null
            );
        });
        emitter.onTimeout(() -> {
            PartyJoinRequestSubscriber subscriber = partyJoinRequestSubscribers.remove(emitterId);
            logSubscriberLifecycle(
                    PARTY_JOIN_REQUESTS_ENDPOINT,
                    "timeout",
                    emitterId,
                    subscriber != null ? subscriber.leaderId() : null,
                    subscriber != null ? subscriber.partyId() : null,
                    null,
                    null
            );
            safeComplete(emitter);
        });
        emitter.onError(ex -> {
            PartyJoinRequestSubscriber subscriber = partyJoinRequestSubscribers.remove(emitterId);
            logSubscriberLifecycle(
                    PARTY_JOIN_REQUESTS_ENDPOINT,
                    "error",
                    emitterId,
                    subscriber != null ? subscriber.leaderId() : null,
                    subscriber != null ? subscriber.partyId() : null,
                    null,
                    ex
            );
            safeCompleteWithError(emitter, ex);
        });
    }

    private void registerMySubscriberLifecycle(String emitterId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            MyJoinRequestSubscriber subscriber = myJoinRequestSubscribers.remove(emitterId);
            if (subscriber == null) {
                return;
            }
            logSubscriberLifecycle(
                    MY_JOIN_REQUESTS_ENDPOINT,
                    "complete",
                    emitterId,
                    subscriber != null ? subscriber.memberId() : null,
                    null,
                    subscriber != null ? subscriber.statusFilter() : null,
                    null
            );
        });
        emitter.onTimeout(() -> {
            MyJoinRequestSubscriber subscriber = myJoinRequestSubscribers.remove(emitterId);
            logSubscriberLifecycle(
                    MY_JOIN_REQUESTS_ENDPOINT,
                    "timeout",
                    emitterId,
                    subscriber != null ? subscriber.memberId() : null,
                    null,
                    subscriber != null ? subscriber.statusFilter() : null,
                    null
            );
            safeComplete(emitter);
        });
        emitter.onError(ex -> {
            MyJoinRequestSubscriber subscriber = myJoinRequestSubscribers.remove(emitterId);
            logSubscriberLifecycle(
                    MY_JOIN_REQUESTS_ENDPOINT,
                    "error",
                    emitterId,
                    subscriber != null ? subscriber.memberId() : null,
                    null,
                    subscriber != null ? subscriber.statusFilter() : null,
                    ex
            );
            safeCompleteWithError(emitter, ex);
        });
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // no-op
        }
    }

    private void safeCompleteWithError(SseEmitter emitter, Throwable throwable) {
        try {
            emitter.completeWithError(throwable);
        } catch (Exception ignored) {
            // no-op
        }
    }

    private void logSubscriberLifecycle(
            String endpoint,
            String action,
            String emitterId,
            String memberId,
            String partyId,
            JoinRequestStatus statusFilter,
            Throwable throwable
    ) {
        int partySize = partyJoinRequestSubscribers.size();
        int mySize = myJoinRequestSubscribers.size();
        if (throwable == null && (partySize > SUBSCRIBER_WARNING_THRESHOLD || mySize > SUBSCRIBER_WARNING_THRESHOLD)) {
            log.warn(
                    "JoinRequest SSE subscriber threshold exceeded: endpoint={}, action={}, emitterId={}, memberId={}, partyId={}, statusFilter={}, partySubscribers={}, mySubscribers={}",
                    endpoint,
                    action,
                    emitterId,
                    memberId,
                    partyId,
                    statusFilter,
                    partySize,
                    mySize
            );
            return;
        }

        if (throwable == null) {
            log.info(
                    "JoinRequest SSE lifecycle: endpoint={}, action={}, emitterId={}, memberId={}, partyId={}, statusFilter={}, partySubscribers={}, mySubscribers={}",
                    endpoint,
                    action,
                    emitterId,
                    memberId,
                    partyId,
                    statusFilter,
                    partySize,
                    mySize
            );
            return;
        }

        log.warn(
                "JoinRequest SSE lifecycle: endpoint={}, action={}, emitterId={}, memberId={}, partyId={}, statusFilter={}, partySubscribers={}, mySubscribers={}, error={}",
                endpoint,
                action,
                emitterId,
                memberId,
                partyId,
                statusFilter,
                partySize,
                mySize,
                throwable.toString()
        );
    }

    private void logEventPublish(String eventType, int recipients) {
        if ("HEARTBEAT".equals(eventType)) {
            log.debug("JoinRequest SSE publish: eventType={}, recipients={}", eventType, recipients);
            return;
        }
        log.info(
                "JoinRequest SSE publish: eventType={}, recipients={}, partySubscribers={}, mySubscribers={}",
                eventType,
                recipients,
                partyJoinRequestSubscribers.size(),
                myJoinRequestSubscribers.size()
        );
    }

    private void recordSendResult(boolean success) {
        windowSendCount.incrementAndGet();
        if (!success) {
            windowFailureCount.incrementAndGet();
        }
        evaluateFailureRateWindow();
    }

    private synchronized void evaluateFailureRateWindow() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(failureRateWindowStartedAt.plusMinutes(FAILURE_RATE_WINDOW_MINUTES))) {
            return;
        }

        long total = windowSendCount.getAndSet(0);
        long failed = windowFailureCount.getAndSet(0);
        failureRateWindowStartedAt = now;

        if (total == 0) {
            return;
        }

        double failureRate = (failed * 100.0) / total;
        if (failureRate > FAILURE_RATE_WARNING_THRESHOLD) {
            log.warn(
                    "JoinRequest SSE send failure rate warning: window={}m, failed={}, total={}, rate={}",
                    FAILURE_RATE_WINDOW_MINUTES,
                    failed,
                    total,
                    String.format("%.2f%%", failureRate)
            );
        }
    }

    private record PartyJoinRequestSubscriber(
            String partyId,
            String leaderId,
            SseEmitter emitter
    ) {
    }

    private record MyJoinRequestSubscriber(
            String memberId,
            JoinRequestStatus statusFilter,
            SseEmitter emitter
    ) {
    }

    private record JoinRequestEvent(
            String joinRequestId,
            String partyId,
            String leaderId,
            String requesterId,
            JoinRequestStatus previousStatus,
            JoinRequestStatus currentStatus
    ) {
        private static JoinRequestEvent from(JoinRequest joinRequest, JoinRequestStatus previousStatus) {
            return new JoinRequestEvent(
                    joinRequest.getId(),
                    joinRequest.getParty().getId(),
                    joinRequest.getLeaderId(),
                    joinRequest.getRequesterId(),
                    previousStatus,
                    joinRequest.getStatus()
            );
        }
    }
}
