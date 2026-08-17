package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryEventResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomSummaryEventPublisherTest {

    @Mock
    private ChatRoomSummarySnapshotReader snapshotReader;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatRoomSummaryEventPublisher publisher;

    @Test
    void publishCurrent_잠금밖에서만든요약을각멤버에게전송한다() {
        ChatRoomSummaryEventResponse payload = summaryPayload("room-1");
        when(snapshotReader.readCurrent("room-1"))
                .thenReturn(List.of(new ChatRoomSummaryDelivery("member-1", payload)));

        publisher.publishCurrent("room-1");

        ArgumentCaptor<ChatRoomSummaryEventResponse> eventCaptor = ArgumentCaptor.forClass(ChatRoomSummaryEventResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), eventCaptor.capture());
        assertEquals(payload, eventCaptor.getValue());
    }

    @Test
    void publishCurrent_같은방의후속발행은이전전송뒤에시작한다() throws Exception {
        CountDownLatch firstSendStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstSend = new CountDownLatch(1);
        CountDownLatch secondReadStarted = new CountDownLatch(1);
        AtomicInteger readCount = new AtomicInteger();
        AtomicInteger sendCount = new AtomicInteger();
        ChatRoomSummaryEventResponse payload = summaryPayload("room-1");

        when(snapshotReader.readCurrent("room-1")).thenAnswer(invocation -> {
            if (readCount.incrementAndGet() == 2) {
                secondReadStarted.countDown();
            }
            return List.of(new ChatRoomSummaryDelivery("member-1", payload));
        });
        doAnswer(invocation -> {
            if (sendCount.incrementAndGet() == 1) {
                firstSendStarted.countDown();
                assertTrue(releaseFirstSend.await(1, TimeUnit.SECONDS));
            }
            return null;
        }).when(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), any());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> publisher.publishCurrent("room-1"));
            assertTrue(firstSendStarted.await(1, TimeUnit.SECONDS));

            Future<?> second = executor.submit(() -> publisher.publishCurrent("room-1"));
            assertFalse(secondReadStarted.await(150, TimeUnit.MILLISECONDS));

            releaseFirstSend.countDown();
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);

            assertTrue(secondReadStarted.await(1, TimeUnit.SECONDS));
            assertEquals(2, readCount.get());
            assertEquals(2, sendCount.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private ChatRoomSummaryEventResponse summaryPayload(String chatRoomId) {
        return new ChatRoomSummaryEventResponse(
                "CHAT_ROOM_UPSERT",
                chatRoomId,
                "시험기간 밤샘 메이트",
                3,
                7L,
                null,
                LocalDateTime.of(2026, 8, 17, 12, 0)
        );
    }
}
