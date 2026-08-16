package com.skuri.skuri_backend.domain.chat.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomType type;

    @Column(length = 50)
    private String department;

    @Column(length = 500)
    private String description;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    @Column(name = "last_message_text", length = 500)
    private String lastMessageText;

    @Column(name = "last_message_sender_id", length = 36)
    private String lastMessageSenderId;

    @Column(name = "last_message_sender_name", length = 50)
    private String lastMessageSenderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_message_type", length = 20)
    private ChatMessageType lastMessageType;

    @Column(name = "last_message_timestamp")
    private LocalDateTime lastMessageTimestamp;

    private ChatRoom(
            String id,
            String name,
            ChatRoomType type,
            String department,
            String description,
            String createdBy,
            boolean isPublic,
            Integer maxMembers
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.department = department;
        this.description = description;
        this.createdBy = createdBy;
        this.isPublic = isPublic;
        this.maxMembers = maxMembers;
        this.memberCount = 0;
        this.messageCount = 0;
    }

    public static ChatRoom create(
            String id,
            String name,
            ChatRoomType type,
            String department,
            String description,
            String createdBy,
            boolean isPublic,
            Integer maxMembers
    ) {
        return new ChatRoom(id, name, type, department, description, createdBy, isPublic, maxMembers);
    }

    public static ChatRoom createPartyRoom(String partyId) {
        return new ChatRoom(
                "party:" + partyId,
                "파티 채팅방",
                ChatRoomType.PARTY,
                null,
                "택시 파티 전용 채팅방",
                null,
                false,
                null
        );
    }

    public void increaseMemberCount() {
        this.memberCount += 1;
    }

    public void decreaseMemberCount() {
        if (this.memberCount > 0) {
            this.memberCount -= 1;
        }
    }

    public void updateMemberCount(int memberCount) {
        this.memberCount = Math.max(0, memberCount);
    }

    public void applyNewMessage(ChatMessage message) {
        this.messageCount += 1;
        applyLastMessage(message);
    }

    public void refreshMessageSummary(long messageCount, ChatMessage lastMessage) {
        this.messageCount = Math.toIntExact(Math.max(0L, messageCount));
        if (lastMessage == null) {
            this.lastMessageText = null;
            this.lastMessageSenderId = null;
            this.lastMessageSenderName = null;
            this.lastMessageType = null;
            this.lastMessageTimestamp = null;
            return;
        }
        applyLastMessage(lastMessage);
    }

    private void applyLastMessage(ChatMessage message) {
        this.lastMessageText = message.getText();
        this.lastMessageSenderId = message.getSenderId();
        this.lastMessageSenderName = message.getSenderName();
        this.lastMessageType = message.getType();
        this.lastMessageTimestamp = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();
    }
}
