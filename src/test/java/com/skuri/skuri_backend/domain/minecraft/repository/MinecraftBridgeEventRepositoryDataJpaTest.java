package com.skuri.skuri_backend.domain.minecraft.repository;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftBridgeEvent;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftBridgeEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class MinecraftBridgeEventRepositoryDataJpaTest {

    @Autowired
    private MinecraftBridgeEventRepository minecraftBridgeEventRepository;

    @Test
    void findReplayEventsAfter_같은createdAt에서도_anchor뒤이벤트만조회한다() {
        MinecraftBridgeEvent before = minecraftBridgeEventRepository.saveAndFlush(createEvent("before"));
        MinecraftBridgeEvent anchor = minecraftBridgeEventRepository.saveAndFlush(createEvent("anchor"));
        MinecraftBridgeEvent after = minecraftBridgeEventRepository.saveAndFlush(createEvent("after"));
        MinecraftBridgeEvent later = minecraftBridgeEventRepository.saveAndFlush(createEvent("later"));

        LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 3, 30, 13, 20);
        ReflectionTestUtils.setField(before, "createdAt", sameCreatedAt);
        ReflectionTestUtils.setField(anchor, "createdAt", sameCreatedAt);
        ReflectionTestUtils.setField(after, "createdAt", sameCreatedAt);
        ReflectionTestUtils.setField(later, "createdAt", sameCreatedAt.plusSeconds(1));
        minecraftBridgeEventRepository.saveAllAndFlush(List.of(before, anchor, after, later));

        List<MinecraftBridgeEvent> replayEvents = minecraftBridgeEventRepository.findReplayEventsAfter(
                sameCreatedAt,
                anchor.getId()
        );

        assertThat(replayEvents)
                .extracting(MinecraftBridgeEvent::getPayload)
                .containsExactly("after", "later");
    }

    private MinecraftBridgeEvent createEvent(String payload) {
        return MinecraftBridgeEvent.create(
                MinecraftBridgeEventType.CHAT_FROM_APP,
                payload,
                Instant.parse("2026-03-31T00:00:00Z")
        );
    }
}
