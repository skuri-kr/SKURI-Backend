package com.skuri.skuri_backend.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 채팅방 변경 커밋 뒤 현재 상태만으로 요약 이벤트를 발행한다.
 *
 * <p>단일 인스턴스 운영에서 같은 방의 발행 순서를 지키되, 브로커 전송 전에 DB 행 잠금은 해제한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatRoomSummaryEventPublisher {

    private static final int PUBLICATION_LOCK_STRIPES = 128;

    private final ChatRoomSummarySnapshotReader snapshotReader;
    private final SimpMessagingTemplate messagingTemplate;
    private final ReentrantLock[] publicationLocks = createPublicationLocks();

    public void publishCurrent(String chatRoomId) {
        ReentrantLock publicationLock = publicationLocks[Math.floorMod(chatRoomId.hashCode(), PUBLICATION_LOCK_STRIPES)];
        publicationLock.lock();
        try {
            snapshotReader.readCurrent(chatRoomId).forEach(delivery ->
                    messagingTemplate.convertAndSendToUser(
                            delivery.memberId(),
                            "/queue/chat-rooms",
                            delivery.payload()
                    )
            );
        } finally {
            publicationLock.unlock();
        }
    }

    private ReentrantLock[] createPublicationLocks() {
        ReentrantLock[] locks = new ReentrantLock[PUBLICATION_LOCK_STRIPES];
        for (int index = 0; index < PUBLICATION_LOCK_STRIPES; index++) {
            locks[index] = new ReentrantLock();
        }
        return locks;
    }
}
