package com.skuri.skuri_backend.domain.chat.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.chat.entity.converter.ChatAccountDataJsonConverter;
import com.skuri.skuri_backend.domain.chat.entity.converter.ChatArrivalDataJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_room_cursor", columnList = "chat_room_id, created_at, message_order, id"),
                @Index(name = "idx_chat_messages_room_visible_latest", columnList = "chat_room_id, deleted_at, created_at, message_order, id"),
                @Index(name = "uk_chat_messages_source_event_id", columnList = "source_event_id", unique = true)
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    public static final String SOURCE_MEMBER_JOIN = "MEMBER_JOIN";
    public static final String SOURCE_MEMBER_LEAVE = "MEMBER_LEAVE";
    public static final String SOURCE_ADMIN_SYSTEM = "ADMIN_SYSTEM";
    public static final String SOURCE_MINECRAFT = "minecraft";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "chat_room_id", nullable = false, length = 100)
    private String chatRoomId;

    @Column(name = "sender_id", nullable = false, length = 36)
    private String senderId;

    @Column(name = "sender_name", length = 50)
    private String senderName;

    @Column(name = "message_order", updatable = false)
    private Long messageOrder;

    @Lob
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageType type;

    @Convert(converter = ChatAccountDataJsonConverter.class)
    @Column(name = "account_data", columnDefinition = "json")
    private ChatAccountData accountData;

    @Convert(converter = ChatArrivalDataJsonConverter.class)
    @Column(name = "arrival_data", columnDefinition = "json")
    private ChatArrivalData arrivalData;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ChatMessageDirection direction;

    @Column(length = 20)
    private String source;

    @Column(name = "minecraft_uuid", length = 50)
    private String minecraftUuid;

    @Column(name = "source_event_id", unique = true, length = 36)
    private String sourceEventId;

    @Column(name = "edited_at")
    private java.time.LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    private ChatMessage(
            String chatRoomId,
            String senderId,
            String senderName,
            Long messageOrder,
            String text,
            ChatMessageType type,
            ChatAccountData accountData,
            ChatArrivalData arrivalData
    ) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.messageOrder = messageOrder;
        this.text = text;
        this.type = type;
        this.accountData = accountData;
        this.arrivalData = arrivalData;
    }

    public static ChatMessage create(
            String chatRoomId,
            String senderId,
            String senderName,
            String text,
            ChatMessageType type,
            ChatAccountData accountData,
            ChatArrivalData arrivalData
    ) {
        return create(
                chatRoomId,
                senderId,
                senderName,
                null,
                text,
                type,
                accountData,
                arrivalData
        );
    }

    public static ChatMessage create(
            String chatRoomId,
            String senderId,
            String senderName,
            Long messageOrder,
            String text,
            ChatMessageType type,
            ChatAccountData accountData,
            ChatArrivalData arrivalData
    ) {
        return new ChatMessage(chatRoomId, senderId, senderName, messageOrder, text, type, accountData, arrivalData);
    }

    public void markSource(String source) {
        this.source = source;
    }

    public void markDirection(ChatMessageDirection direction) {
        this.direction = direction;
    }

    public void markMinecraftUuid(String minecraftUuid) {
        this.minecraftUuid = minecraftUuid;
    }

    public void markMinecraftOrigin(ChatMessageDirection direction, String minecraftUuid) {
        this.direction = direction;
        this.minecraftUuid = minecraftUuid;
        this.source = SOURCE_MINECRAFT;
    }

    public void markSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public boolean hasSource(String source) {
        return this.source != null && this.source.equals(source);
    }

    public boolean isMinecraftOrigin() {
        return hasSource(SOURCE_MINECRAFT);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void editText(String text, java.time.LocalDateTime editedAt) {
        this.text = text;
        this.editedAt = editedAt;
    }

    public void delete(java.time.LocalDateTime deletedAt) {
        this.text = null;
        this.accountData = null;
        this.arrivalData = null;
        this.deletedAt = deletedAt;
    }

    public void updateArrivalData(ChatArrivalData arrivalData) {
        this.arrivalData = arrivalData;
    }
}
