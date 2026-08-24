package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestListItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinRequestSseServiceTest {

    @Mock
    private JoinRequestSseSnapshotService joinRequestSseSnapshotService;

    @Test
    void subscribePartyJoinRequests_스냅샷준비수행() {
        JoinRequestSseService service = new JoinRequestSseService(joinRequestSseSnapshotService);
        when(joinRequestSseSnapshotService.createPartyJoinRequestsSnapshotPayload("leader-1", "party-1"))
                .thenReturn(Map.of("partyId", "party-1", "requests", List.of()));

        SseEmitter emitter = service.subscribePartyJoinRequests("leader-1", "party-1");

        assertNotNull(emitter);
        verify(joinRequestSseSnapshotService).createPartyJoinRequestsSnapshotPayload("leader-1", "party-1");
    }

    @Test
    void subscribePartyJoinRequests_리더아니면_NOT_PARTY_LEADER() {
        JoinRequestSseService service = new JoinRequestSseService(joinRequestSseSnapshotService);
        when(joinRequestSseSnapshotService.createPartyJoinRequestsSnapshotPayload("member-2", "party-1"))
                .thenThrow(new BusinessException(ErrorCode.NOT_PARTY_LEADER));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.subscribePartyJoinRequests("member-2", "party-1")
        );

        assertEquals(ErrorCode.NOT_PARTY_LEADER, exception.getErrorCode());
    }

    @Test
    void subscribeMyJoinRequests_status없음이면_전체스냅샷준비() {
        JoinRequestSseService service = new JoinRequestSseService(joinRequestSseSnapshotService);
        when(joinRequestSseSnapshotService.createMyJoinRequestsSnapshotPayload("requester-1", null))
                .thenReturn(Map.of("requests", List.of()));

        SseEmitter emitter = service.subscribeMyJoinRequests("requester-1", null);

        assertNotNull(emitter);
        verify(joinRequestSseSnapshotService).createMyJoinRequestsSnapshotPayload("requester-1", null);
    }

    @Test
    void subscribeMyJoinRequests_status지정이면_필터스냅샷준비() {
        JoinRequestSseService service = new JoinRequestSseService(joinRequestSseSnapshotService);
        when(joinRequestSseSnapshotService.createMyJoinRequestsSnapshotPayload("requester-1", JoinRequestStatus.ACCEPTED))
                .thenReturn(Map.of("requests", List.of()));

        SseEmitter emitter = service.subscribeMyJoinRequests("requester-1", JoinRequestStatus.ACCEPTED);

        assertNotNull(emitter);
        verify(joinRequestSseSnapshotService)
                .createMyJoinRequestsSnapshotPayload("requester-1", JoinRequestStatus.ACCEPTED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishJoinRequestCreated_대상구독자에게만전송() throws Exception {
        JoinRequestSseService service = new JoinRequestSseService(joinRequestSseSnapshotService);
        Party party = sampleParty("party-1", "leader-1");
        JoinRequest request = sampleJoinRequest("request-1", party, "requester-1");
        when(joinRequestSseSnapshotService.toSseItem(request)).thenReturn(sampleResponse(request));

        CountingEmitter partyMatchedEmitter = new CountingEmitter();
        CountingEmitter partyOtherEmitter = new CountingEmitter();
        CountingEmitter myMatchedEmitter = new CountingEmitter();
        CountingEmitter myFilteredOutEmitter = new CountingEmitter();
        CountingEmitter myOtherMemberEmitter = new CountingEmitter();

        Map<String, Object> partySubscribers = (Map<String, Object>) ReflectionTestUtils.getField(service, "partyJoinRequestSubscribers");
        Map<String, Object> mySubscribers = (Map<String, Object>) ReflectionTestUtils.getField(service, "myJoinRequestSubscribers");

        partySubscribers.put("party:1", createPartySubscriber("party-1", "leader-1", partyMatchedEmitter));
        partySubscribers.put("party:2", createPartySubscriber("party-2", "leader-1", partyOtherEmitter));

        mySubscribers.put("my:1", createMySubscriber("requester-1", null, myMatchedEmitter));
        mySubscribers.put("my:2", createMySubscriber("requester-1", JoinRequestStatus.ACCEPTED, myFilteredOutEmitter));
        mySubscribers.put("my:3", createMySubscriber("requester-2", null, myOtherMemberEmitter));

        service.publishJoinRequestCreated(request);

        assertEquals(1, partyMatchedEmitter.sendCount());
        assertEquals(0, partyOtherEmitter.sendCount());
        assertEquals(1, myMatchedEmitter.sendCount());
        assertEquals(0, myFilteredOutEmitter.sendCount());
        assertEquals(0, myOtherMemberEmitter.sendCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishJoinRequestUpdated_이전상태현재상태필터로전송() throws Exception {
        JoinRequestSseService service = new JoinRequestSseService(joinRequestSseSnapshotService);
        Party party = sampleParty("party-1", "leader-1");
        JoinRequest request = sampleJoinRequest("request-1", party, "requester-1");
        request.accept();

        when(joinRequestSseSnapshotService.toSseItem(request)).thenReturn(sampleResponse(request));

        CountingEmitter partyEmitter = new CountingEmitter();
        CountingEmitter myPendingEmitter = new CountingEmitter();
        CountingEmitter myAcceptedEmitter = new CountingEmitter();
        CountingEmitter myDeclinedEmitter = new CountingEmitter();

        Map<String, Object> partySubscribers = (Map<String, Object>) ReflectionTestUtils.getField(service, "partyJoinRequestSubscribers");
        Map<String, Object> mySubscribers = (Map<String, Object>) ReflectionTestUtils.getField(service, "myJoinRequestSubscribers");
        partySubscribers.put("party:1", createPartySubscriber("party-1", "leader-1", partyEmitter));
        mySubscribers.put("my:pending", createMySubscriber("requester-1", JoinRequestStatus.PENDING, myPendingEmitter));
        mySubscribers.put("my:accepted", createMySubscriber("requester-1", JoinRequestStatus.ACCEPTED, myAcceptedEmitter));
        mySubscribers.put("my:declined", createMySubscriber("requester-1", JoinRequestStatus.DECLINED, myDeclinedEmitter));

        service.publishJoinRequestUpdated(request, JoinRequestStatus.PENDING);

        assertEquals(1, partyEmitter.sendCount());
        assertEquals(1, myPendingEmitter.sendCount());
        assertEquals(1, myAcceptedEmitter.sendCount());
        assertEquals(0, myDeclinedEmitter.sendCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishHeartbeat_전송실패시_구독해제() throws Exception {
        JoinRequestSseService service = new JoinRequestSseService(joinRequestSseSnapshotService);
        Map<String, Object> partySubscribers = (Map<String, Object>) ReflectionTestUtils.getField(service, "partyJoinRequestSubscribers");
        Map<String, Object> mySubscribers = (Map<String, Object>) ReflectionTestUtils.getField(service, "myJoinRequestSubscribers");

        SseEmitter failingEmitter = new SseEmitter() {
            @Override
            public synchronized void send(SseEventBuilder builder) throws IOException {
                throw new IOException("send failed");
            }
        };
        CountingEmitter successEmitter = new CountingEmitter();

        partySubscribers.put("party:1", createPartySubscriber("party-1", "leader-1", failingEmitter));
        mySubscribers.put("my:1", createMySubscriber("requester-1", null, successEmitter));

        service.publishHeartbeat();

        assertTrue(partySubscribers.isEmpty());
        assertEquals(1, successEmitter.sendCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishHeartbeat_양채널전송동작() throws Exception {
        JoinRequestSseService service = new JoinRequestSseService(joinRequestSseSnapshotService);
        Map<String, Object> partySubscribers = (Map<String, Object>) ReflectionTestUtils.getField(service, "partyJoinRequestSubscribers");
        Map<String, Object> mySubscribers = (Map<String, Object>) ReflectionTestUtils.getField(service, "myJoinRequestSubscribers");

        CountingEmitter partyEmitter = new CountingEmitter();
        CountingEmitter myEmitter = new CountingEmitter();
        partySubscribers.put("party:1", createPartySubscriber("party-1", "leader-1", partyEmitter));
        mySubscribers.put("my:1", createMySubscriber("requester-1", null, myEmitter));

        service.publishHeartbeat();

        assertEquals(1, partyEmitter.sendCount());
        assertEquals(1, myEmitter.sendCount());
    }

    private Object createPartySubscriber(String partyId, String leaderId, SseEmitter emitter) throws Exception {
        Class<?> clazz = Class.forName("com.skuri.skuri_backend.domain.taxiparty.service.JoinRequestSseService$PartyJoinRequestSubscriber");
        Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, String.class, SseEmitter.class);
        constructor.setAccessible(true);
        return constructor.newInstance(partyId, leaderId, emitter);
    }

    private Object createMySubscriber(String memberId, JoinRequestStatus statusFilter, SseEmitter emitter) throws Exception {
        Class<?> clazz = Class.forName("com.skuri.skuri_backend.domain.taxiparty.service.JoinRequestSseService$MyJoinRequestSubscriber");
        Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, JoinRequestStatus.class, SseEmitter.class);
        constructor.setAccessible(true);
        return constructor.newInstance(memberId, statusFilter, emitter);
    }

    private Party sampleParty(String partyId, String leaderId) {
        Party party = Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                4,
                List.of("빠른출발"),
                "테스트"
        );
        ReflectionTestUtils.setField(party, "id", partyId);
        return party;
    }

    private JoinRequest sampleJoinRequest(String requestId, Party party, String requesterId) {
        JoinRequest request = JoinRequest.create(party, requesterId);
        ReflectionTestUtils.setField(request, "id", requestId);
        ReflectionTestUtils.setField(request, "createdAt", LocalDateTime.now());
        return request;
    }

    private JoinRequestListItemResponse sampleResponse(JoinRequest request) {
        return new JoinRequestListItemResponse(
                request.getId(),
                request.getParty().getId(),
                request.getRequesterId(),
                "요청자",
                "https://example.com/profile.jpg",
                null,
                request.getStatus(),
                request.getExpiryReason(),
                request.getCreatedAt()
        );
    }

    private static final class CountingEmitter extends SseEmitter {
        private int sendCount;

        @Override
        public synchronized void send(SseEventBuilder builder) {
            sendCount++;
        }

        private int sendCount() {
            return sendCount;
        }
    }
}
