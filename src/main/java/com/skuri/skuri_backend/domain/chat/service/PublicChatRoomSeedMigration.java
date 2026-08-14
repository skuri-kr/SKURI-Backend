package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class PublicChatRoomSeedMigration {

    private static final String UNIVERSITY_ROOM_ID = "public:university";
    private static final String MINECRAFT_ROOM_ID = "public:game:minecraft";

    private final ChatRoomRepository chatRoomRepository;
    private final DepartmentService departmentService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        int createdCount = 0;
        createdCount += seedIfAbsent(
                UNIVERSITY_ROOM_ID,
                "성결대학교 전체 채팅방",
                ChatRoomType.UNIVERSITY,
                null,
                "성결대학교 전체 채팅방입니다."
        );
        createdCount += seedIfAbsent(
                MINECRAFT_ROOM_ID,
                "마인크래프트 채팅방",
                ChatRoomType.GAME,
                null,
                "스쿠리 서버 채팅방입니다."
        );

        for (String department : departmentService.getActiveDepartmentNames()) {
            createdCount += seedIfAbsent(
                    departmentRoomId(department),
                    department + " 채팅방",
                    ChatRoomType.DEPARTMENT,
                    department,
                    department + " 채팅방입니다."
            );
        }

        if (createdCount > 0) {
            log.info("공개 채팅방 seed migration 완료: {}건 생성", createdCount);
        }
    }

    private int seedIfAbsent(
            String roomId,
            String name,
            ChatRoomType type,
            String department,
            String description
    ) {
        return chatRoomRepository.insertPublicSeedRoomIfAbsent(
                roomId,
                name,
                type.name(),
                department,
                description
        );
    }

    private String departmentRoomId(String department) {
        return "public:department:" + sha256(department).substring(0, 16);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
