package com.skuri.skuri_backend.domain.chat.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatRoomTest {

    @Test
    void 최신메시지요약은원문을보존하면서목록미리보기만500codePoint로자른다() {
        ChatRoom room = ChatRoom.create(
                "room-1",
                "공개 채팅방",
                ChatRoomType.CUSTOM,
                null,
                null,
                null,
                true,
                null
        );
        String longText = "가".repeat(499) + "🙂" + "끝";
        ChatMessage message = ChatMessage.create(
                "room-1",
                "member-1",
                "홍길동",
                longText,
                ChatMessageType.TEXT,
                null,
                null
        );

        room.refreshMessageSummary(1L, message);

        assertEquals(500, room.getLastMessageText().codePointCount(0, room.getLastMessageText().length()));
        assertEquals("가".repeat(499) + "🙂", room.getLastMessageText());
        assertEquals(longText, message.getText());
    }
}
