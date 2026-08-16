package com.skuri.skuri_backend.domain.image.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatImageAssetPolicyTest {

    @Test
    void 원본과썸네일은동일한이미지자산가족으로해석한다() {
        ChatImageAsset original = ChatImageAssetPolicy.resolve("chat/2026/08/17/image.png").orElseThrow();
        ChatImageAsset thumbnail = ChatImageAssetPolicy.resolve("chat/2026/08/17/image_thumb.jpg").orElseThrow();

        assertEquals("chat/2026/08/17/image", original.familyKey());
        assertEquals(original.familyKey(), thumbnail.familyKey());
        assertEquals(original.cleanupPaths(), thumbnail.cleanupPaths());
        assertEquals(
                List.of(
                        "chat/2026/08/17/image.jpg",
                        "chat/2026/08/17/image.png",
                        "chat/2026/08/17/image.webp",
                        "chat/2026/08/17/image_thumb.jpg",
                        "chat/2026/08/17/image_thumb.png",
                        "chat/2026/08/17/image_thumb.webp"
                ),
                original.cleanupPaths()
        );
        assertFalse(original.thumbnailReference());
        assertTrue(thumbnail.thumbnailReference());
    }

    @Test
    void 채팅경로밖으로정규화되는경로는관리대상이아니다() {
        assertTrue(ChatImageAssetPolicy.resolve("chat/../profiles/member.png").isEmpty());
        assertTrue(ChatImageAssetPolicy.resolve("/chat/2026/08/image.png").isEmpty());
    }
}
