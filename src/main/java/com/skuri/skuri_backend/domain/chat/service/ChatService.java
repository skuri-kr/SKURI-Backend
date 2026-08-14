package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.CreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.request.SendChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatAccountDataResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatArrivalDataResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatArrivalSettlementMemberResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageCursorResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatReadUpdateResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomLastMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSettingsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryEventResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatAccountData;
import com.skuri.skuri_backend.domain.chat.entity.ChatArrivalData;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageDirection;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.constant.DepartmentAliasNormalizer;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftAvatarService;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftBridgeOutboxService;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 50;
    private static final int MAX_MESSAGE_PAGE_SIZE = 100;
    private static final ZoneId CHAT_TIME_ZONE = ZoneId.of("Asia/Seoul");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final PartyMessageService partyMessageService;
    private final ChatMessageOrderGenerator chatMessageOrderGenerator;
    private final SimpMessagingTemplate messagingTemplate;
    private final AfterCommitApplicationEventPublisher eventPublisher;
    private final MinecraftAvatarService minecraftAvatarService;
    private final MinecraftBridgeOutboxService minecraftBridgeOutboxService;
    private final MinecraftBridgeProperties minecraftBridgeProperties;

    @Transactional
    public void createPartyChatRoom(Party party) {
        syncPartyChatRoomMembers(party);
    }

    @Transactional
    public void syncPartyChatRoomMembers(Party party) {
        String chatRoomId = "party:" + party.getId();
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.createPartyRoom(party.getId())));
        Map<String, ChatRoomMember> currentMembers = chatRoomMemberRepository.findById_ChatRoomId(chatRoomId).stream()
                .collect(Collectors.toMap(ChatRoomMember::getMemberId, Function.identity()));
        Set<String> expectedMembers = new LinkedHashSet<>(party.getMemberIds());

        for (ChatRoomMember member : currentMembers.values()) {
            if (!expectedMembers.contains(member.getMemberId())) {
                chatRoomMemberRepository.delete(member);
            }
        }

        LocalDateTime joinedAt = LocalDateTime.now();
        for (String memberId : expectedMembers) {
            if (!currentMembers.containsKey(memberId)) {
                chatRoomMemberRepository.save(ChatRoomMember.create(room, memberId, joinedAt));
            }
        }

        room.updateMemberCount(expectedMembers.size());
        chatRoomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getChatRooms(String memberId, ChatRoomType type, Boolean joinedOnly) {
        String currentDepartment = findCurrentDepartment(memberId);
        List<ChatRoom> rooms = type == null ? chatRoomRepository.findAll() : chatRoomRepository.findByType(type);
        Map<String, ChatRoomMember> membershipMap = chatRoomMemberRepository.findById_MemberId(memberId).stream()
                .collect(Collectors.toMap(ChatRoomMember::getChatRoomId, Function.identity()));

        return rooms.stream()
                .filter(room -> isVisibleToMember(room, membershipMap.get(room.getId()), currentDepartment))
                .filter(room -> !Boolean.TRUE.equals(joinedOnly) || membershipMap.containsKey(room.getId()))
                .sorted(chatRoomComparator())
                .map(room -> toSummaryResponse(room, membershipMap.get(room.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getAdminPublicChatRooms(ChatRoomType type) {
        List<ChatRoom> rooms = type == null ? chatRoomRepository.findAll() : chatRoomRepository.findByType(type);
        return rooms.stream()
                .filter(this::isAdminPublicRoom)
                .sorted(chatRoomComparator())
                .map(room -> toSummaryResponse(room, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getChatRoomDetail(String memberId, String chatRoomId) {
        ChatRoomAccess access = findAccessibleRoom(memberId, chatRoomId);
        return toDetailResponse(access.room(), access.member());
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getAdminPublicChatRoomDetail(String chatRoomId) {
        return toDetailResponse(findAdminPublicRoomOrThrow(chatRoomId), null);
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getMessages(
            String memberId,
            String chatRoomId,
            LocalDateTime cursorCreatedAt,
            String cursorId,
            Integer size
    ) {
        ChatRoomAccess access = findAccessibleRoom(memberId, chatRoomId);
        ChatRoom room = access.room();
        requireChatRoomMember(access.member());
        return getMessagePage(room, cursorCreatedAt, cursorId, size);
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getAdminPublicChatRoomMessages(
            String chatRoomId,
            LocalDateTime cursorCreatedAt,
            String cursorId,
            Integer size
    ) {
        return getMessagePage(findAdminPublicRoomOrThrow(chatRoomId), cursorCreatedAt, cursorId, size);
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getAdminPartyChatMessages(
            String chatRoomId,
            LocalDateTime cursorCreatedAt,
            String cursorId,
            Integer size
    ) {
        return getMessagePage(findAdminPartyRoomOrThrow(chatRoomId), cursorCreatedAt, cursorId, size);
    }

    @Transactional
    public ChatReadUpdateResponse markAsRead(String memberId, String chatRoomId, Instant lastReadAt) {
        ChatRoomAccess access = findAccessibleRoom(memberId, chatRoomId);
        ChatRoom room = access.room();
        ChatRoomMember member = requireChatRoomMember(access.member());
        boolean updated = member.advanceLastReadAt(clampLastReadAt(room, lastReadAt));
        if (updated) {
            chatRoomMemberRepository.save(member);
        }
        return new ChatReadUpdateResponse(chatRoomId, toApiInstant(member.getLastReadAt()), updated);
    }

    @Transactional
    public ChatRoomSettingsResponse updateSettings(String memberId, String chatRoomId, boolean muted) {
        ChatRoomAccess access = findAccessibleRoom(memberId, chatRoomId);
        ChatRoomMember member = requireChatRoomMember(access.member());
        member.updateMuted(muted);
        chatRoomMemberRepository.save(member);
        return new ChatRoomSettingsResponse(chatRoomId, member.isMuted());
    }

    @Transactional
    public ChatRoomDetailResponse createChatRoom(String memberId, CreateChatRoomRequest request) {
        requireActiveMember(memberId);
        ChatRoom room = ChatRoom.create(
                "room:" + UUID.randomUUID(),
                request.name().trim(),
                ChatRoomType.CUSTOM,
                null,
                normalizeNullable(request.description()),
                memberId,
                true,
                null
        );
        ChatRoom saved = chatRoomRepository.save(room);
        ChatRoomMember member = ChatRoomMember.create(saved, memberId, LocalDateTime.now());
        chatRoomMemberRepository.save(member);
        saved.increaseMemberCount();
        chatRoomRepository.save(saved);
        publishAfterCommit(() -> publishChatRoomSummaryEvent(saved));
        return toDetailResponse(saved, member);
    }

    @Transactional
    public ChatRoomDetailResponse joinChatRoom(String memberId, String chatRoomId) {
        Member memberProfile = requireActiveMember(memberId);
        ChatRoomAccess access = findAccessibleRoom(memberId, chatRoomId);
        ChatRoom room = access.room();
        validatePublicRoomMembershipAction(room, "참여");

        if (access.member() != null) {
            throw new BusinessException(ErrorCode.ALREADY_CHAT_ROOM_MEMBER);
        }
        if (room.getMaxMembers() != null && room.getMemberCount() >= room.getMaxMembers()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FULL);
        }

        ChatRoomMember member = ChatRoomMember.create(room, memberId, LocalDateTime.now());
        member.advanceLastReadAt(initialLastReadAt(room));
        chatRoomMemberRepository.save(member);
        room.increaseMemberCount();
        String displayName = resolveMembershipDisplayName(memberProfile);
        createMembershipSystemMessage(
                room,
                memberId,
                displayName,
                memberProfile.getPhotoUrl(),
                displayName != null ? displayName + "님이 입장했어요." : "새 멤버가 입장했어요.",
                ChatMessage.SOURCE_MEMBER_JOIN
        );
        member.advanceLastReadAt(room.getLastMessageTimestamp());
        return toDetailResponse(room, member);
    }

    @Transactional
    public ChatRoomDetailResponse leaveChatRoom(String memberId, String chatRoomId) {
        Member memberProfile = requireActiveMember(memberId);
        ChatRoomAccess access = findAccessibleRoom(memberId, chatRoomId);
        ChatRoom room = access.room();
        validatePublicRoomMembershipAction(room, "나가기");

        ChatRoomMember member = requireChatRoomMember(access.member());
        removeMembership(member, true, false);
        String displayName = resolveMembershipDisplayName(memberProfile);
        createMembershipSystemMessage(
                room,
                memberId,
                displayName,
                memberProfile.getPhotoUrl(),
                displayName != null ? displayName + "님이 나갔어요." : "멤버가 나갔어요.",
                ChatMessage.SOURCE_MEMBER_LEAVE
        );
        return toDetailResponse(room, null);
    }

    @Transactional
    public ChatMessageResponse sendMessage(String chatRoomId, String senderId, SendChatMessageRequest request) {
        ChatRoom room = findRoomOrThrow(chatRoomId);
        requireChatRoomMember(chatRoomId, senderId);
        Member sender = memberRepository.findById(senderId).orElseThrow(MemberNotFoundException::new);

        ChatMessageType type = request.type();
        if (type == ChatMessageType.SYSTEM || type == ChatMessageType.ARRIVED || type == ChatMessageType.END) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, type + " 메시지는 서버에서 생성됩니다.");
        }

        String text;
        ChatAccountData accountData = null;
        ChatArrivalData arrivalData = null;

        if (type == ChatMessageType.TEXT) {
            text = requireText(request.text());
        } else if (type == ChatMessageType.IMAGE) {
            text = requireImageUrl(request.imageUrl());
        } else {
            if (room.getType() != ChatRoomType.PARTY) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "파티 채팅방에서만 특수 메시지를 전송할 수 있습니다.");
            }
            PartySpecialMessagePayload payload = partyMessageService.buildClientPayload(
                    chatRoomId,
                    senderId,
                    request
            );
            text = payload.text();
            accountData = payload.accountData();
            arrivalData = payload.arrivalData();
        }

        return saveAndPublishMessage(
                room,
                senderId,
                sender.getNickname(),
                sender.getPhotoUrl(),
                text,
                type,
                accountData,
                arrivalData,
                null
        );
    }

    @Transactional
    public ChatMessageResponse createPartySystemMessage(Party party, String senderId, String text) {
        Member sender = memberRepository.findById(senderId).orElseThrow(MemberNotFoundException::new);
        ChatRoom room = findRoomOrThrow("party:" + party.getId());
        return saveAndPublishMessage(
                room,
                senderId,
                sender.getNickname(),
                sender.getPhotoUrl(),
                text,
                ChatMessageType.SYSTEM,
                null,
                null,
                null
        );
    }

    @Transactional
    public ChatMessageResponse createPartyAdminSystemMessage(Party party, String adminActorId, String text) {
        ChatRoom room = findRoomOrThrow("party:" + party.getId());
        return saveAndPublishMessage(
                room,
                adminActorId,
                "관리자",
                null,
                text,
                ChatMessageType.SYSTEM,
                null,
                null,
                ChatMessage.SOURCE_ADMIN_SYSTEM
        );
    }

    @Transactional
    public ChatMessageResponse createMinecraftInboundMessage(
            String senderId,
            String senderName,
            String senderPhotoUrl,
            String text,
            ChatMessageType type,
            ChatMessageDirection direction,
            String minecraftUuid,
            String sourceEventId
    ) {
        if (StringUtils.hasText(sourceEventId)) {
            ChatMessage duplicatedMessage = chatMessageRepository.findBySourceEventId(sourceEventId).orElse(null);
            if (duplicatedMessage != null) {
                return toMessageResponse(duplicatedMessage, senderPhotoUrl);
            }
        }

        ChatRoom room = findRoomOrThrow(minecraftBridgeProperties.normalizedRoomId());
        return saveAndPublishMessage(
                room,
                senderId,
                senderName,
                senderPhotoUrl,
                text,
                type,
                null,
                null,
                ChatMessage.SOURCE_MINECRAFT,
                direction,
                minecraftUuid,
                sourceEventId
        );
    }

    @Transactional
    public ChatMessageResponse createPartyMemberJoinSystemMessage(Party party, String senderId, String text) {
        Member sender = memberRepository.findById(senderId).orElseThrow(MemberNotFoundException::new);
        ChatRoom room = findRoomOrThrow("party:" + party.getId());
        return createMembershipSystemMessage(
                room,
                senderId,
                sender.getNickname(),
                sender.getPhotoUrl(),
                text,
                ChatMessage.SOURCE_MEMBER_JOIN
        );
    }

    @Transactional
    public ChatMessageResponse createPartyMemberLeaveSystemMessage(Party party, String senderId, String text) {
        Member sender = memberRepository.findById(senderId).orElseThrow(MemberNotFoundException::new);
        ChatRoom room = findRoomOrThrow("party:" + party.getId());
        return createMembershipSystemMessage(
                room,
                senderId,
                sender.getNickname(),
                sender.getPhotoUrl(),
                text,
                ChatMessage.SOURCE_MEMBER_LEAVE
        );
    }

    @Transactional
    public ChatMessageResponse createPartyArrivalMessage(Party party, String senderId) {
        Member sender = memberRepository.findById(senderId).orElseThrow(MemberNotFoundException::new);
        PartySpecialMessagePayload payload = partyMessageService.buildArrivalPayload(party, senderId);
        ChatRoom room = findRoomOrThrow("party:" + party.getId());
        return saveAndPublishMessage(
                room,
                senderId,
                sender.getNickname(),
                sender.getPhotoUrl(),
                payload.text(),
                ChatMessageType.ARRIVED,
                payload.accountData(),
                payload.arrivalData(),
                null
        );
    }

    @Transactional
    public void syncPartyArrivalMessageSnapshot(Party party) {
        if (party.getStatus() != PartyStatus.ARRIVED) {
            return;
        }

        chatMessageRepository.findTopByChatRoomIdAndTypeOrderByCreatedAtDescMessageOrderDescIdDesc(
                        "party:" + party.getId(),
                        ChatMessageType.ARRIVED
                )
                .ifPresent(message -> message.updateArrivalData(partyMessageService.buildArrivalData(party)));
    }

    @Transactional
    public ChatMessageResponse createPartyEndMessage(Party party, String senderId) {
        Member sender = memberRepository.findById(senderId).orElseThrow(MemberNotFoundException::new);
        PartySpecialMessagePayload payload = partyMessageService.buildEndPayload(party, senderId);
        ChatRoom room = findRoomOrThrow("party:" + party.getId());
        return saveAndPublishMessage(
                room,
                senderId,
                sender.getNickname(),
                sender.getPhotoUrl(),
                payload.text(),
                ChatMessageType.END,
                payload.accountData(),
                payload.arrivalData(),
                null
        );
    }

    @Transactional
    public void removeMemberFromAllChatRooms(String memberId) {
        List<ChatRoomMember> memberships = chatRoomMemberRepository.findById_MemberId(memberId);
        for (ChatRoomMember membership : memberships) {
            removeMembership(membership, false, true);
        }
    }

    @Transactional
    public void removeMemberFromDepartmentChatRooms(String memberId) {
        Member memberProfile = requireActiveMember(memberId);
        String displayName = resolveMembershipDisplayName(memberProfile);
        List<ChatRoomMember> memberships = chatRoomMemberRepository.findById_MemberId(memberId).stream()
                .filter(membership -> membership.getChatRoom().getType() == ChatRoomType.DEPARTMENT)
                .toList();

        for (ChatRoomMember membership : memberships) {
            removeMembership(membership, true, false);
            createMembershipSystemMessage(
                    membership.getChatRoom(),
                    memberId,
                    displayName,
                    memberProfile.getPhotoUrl(),
                    displayName != null ? displayName + "님이 나갔어요." : "멤버가 나갔어요.",
                    ChatMessage.SOURCE_MEMBER_LEAVE
            );
        }
    }

    private void publishChatRoomSummaryEvent(ChatRoom room) {
        List<ChatRoomMember> members = chatRoomMemberRepository.findById_ChatRoomId(room.getId());
        for (ChatRoomMember member : members) {
            long unreadCount = calculateUnreadCount(room, member);
            ChatRoomSummaryEventResponse payload = new ChatRoomSummaryEventResponse(
                    "CHAT_ROOM_UPSERT",
                    room.getId(),
                    room.getName(),
                    room.getMemberCount(),
                    unreadCount,
                    toLastMessage(room),
                    LocalDateTime.now()
            );
            messagingTemplate.convertAndSendToUser(member.getMemberId(), "/queue/chat-rooms", payload);
        }
    }

    private void publishChatRoomRemovedEvent(ChatRoom room, String memberId) {
        ChatRoomSummaryEventResponse payload = new ChatRoomSummaryEventResponse(
                "CHAT_ROOM_REMOVED",
                room.getId(),
                room.getName(),
                room.getMemberCount(),
                0L,
                toLastMessage(room),
                LocalDateTime.now()
        );
        messagingTemplate.convertAndSendToUser(memberId, "/queue/chat-rooms", payload);
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

    private ChatRoomMember requireChatRoomMember(String chatRoomId, String memberId) {
        return requireChatRoomMember(findMembership(chatRoomId, memberId));
    }

    private ChatRoomMember requireChatRoomMember(ChatRoomMember member) {
        if (member == null) {
            throw new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER);
        }
        return member;
    }

    private ChatRoom findRoomOrThrow(String chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoom findAdminPublicRoomOrThrow(String chatRoomId) {
        ChatRoom room = findRoomOrThrow(chatRoomId);
        if (!isAdminPublicRoom(room)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        return room;
    }

    private ChatRoom findAdminPartyRoomOrThrow(String chatRoomId) {
        ChatRoom room = findRoomOrThrow(chatRoomId);
        if (room.getType() != ChatRoomType.PARTY) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        return room;
    }

    private boolean isAdminPublicRoom(ChatRoom room) {
        return room.isPublic() && room.getType() != ChatRoomType.PARTY;
    }

    private long calculateUnreadCount(ChatRoom room, ChatRoomMember member) {
        if (member == null) {
            return 0L;
        }
        LocalDateTime lastReadAt = member.getLastReadAt();
        if (lastReadAt == null) {
            return room.getMessageCount();
        }
        return chatMessageRepository.countByChatRoomIdAndCreatedAtAfter(room.getId(), lastReadAt);
    }

    private LocalDateTime clampLastReadAt(ChatRoom room, Instant requestedLastReadAt) {
        LocalDateTime normalizedRequestedLastReadAt = toChatLocalDateTime(requestedLastReadAt);
        if (normalizedRequestedLastReadAt == null) {
            return null;
        }

        LocalDateTime upperBound = LocalDateTime.now(CHAT_TIME_ZONE);
        if (room.getLastMessageTimestamp() != null && room.getLastMessageTimestamp().isBefore(upperBound)) {
            upperBound = room.getLastMessageTimestamp();
        }
        return normalizedRequestedLastReadAt.isAfter(upperBound) ? upperBound : normalizedRequestedLastReadAt;
    }

    private ChatMessagePageResponse getMessagePage(
            ChatRoom room,
            LocalDateTime cursorCreatedAt,
            String cursorId,
            Integer size
    ) {
        validateCursor(cursorCreatedAt, cursorId);

        int pageSize = size == null ? DEFAULT_MESSAGE_PAGE_SIZE : size;
        if (pageSize < 1 || pageSize > MAX_MESSAGE_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1 이상 100 이하여야 합니다.");
        }

        List<ChatMessage> fetched = chatMessageRepository.findByCursor(
                room.getId(),
                cursorCreatedAt,
                cursorId,
                resolveCursorMessageOrder(room.getId(), cursorId),
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = fetched.size() > pageSize;
        List<ChatMessage> page = hasNext ? fetched.subList(0, pageSize) : fetched;
        Map<String, String> senderPhotoUrls = resolveSenderPhotoUrls(page);
        List<ChatMessageResponse> messages = page.stream()
                .map(message -> toMessageResponse(message, senderPhotoUrls.get(message.getSenderId())))
                .toList();

        ChatMessageCursorResponse nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            ChatMessage last = page.get(page.size() - 1);
            nextCursor = new ChatMessageCursorResponse(last.getCreatedAt(), last.getId());
        }

        return new ChatMessagePageResponse(messages, hasNext, nextCursor);
    }

    private Comparator<ChatRoom> chatRoomComparator() {
        Comparator<LocalDateTime> descNullsLast = Comparator.nullsLast(Comparator.reverseOrder());
        return Comparator
                .comparing(ChatRoom::getLastMessageTimestamp, descNullsLast)
                .thenComparing(ChatRoom::getCreatedAt, descNullsLast)
                .thenComparing(ChatRoom::getId, Comparator.reverseOrder());
    }

    private ChatRoomSummaryResponse toSummaryResponse(ChatRoom room, ChatRoomMember member) {
        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getType(),
                room.getName(),
                room.getDescription(),
                room.isPublic(),
                room.getMemberCount(),
                member != null,
                calculateUnreadCount(room, member),
                toLastMessage(room),
                room.getLastMessageTimestamp(),
                member != null && member.isMuted()
        );
    }

    private ChatRoomDetailResponse toDetailResponse(ChatRoom room, ChatRoomMember member) {
        return new ChatRoomDetailResponse(
                room.getId(),
                room.getType(),
                room.getName(),
                room.getDescription(),
                room.isPublic(),
                room.getMemberCount(),
                member != null,
                calculateUnreadCount(room, member),
                toLastMessage(room),
                room.getLastMessageTimestamp(),
                member != null && member.isMuted(),
                member != null ? toApiInstant(member.getLastReadAt()) : null
        );
    }

    private ChatRoomLastMessageResponse toLastMessage(ChatRoom room) {
        if (room.getLastMessageTimestamp() == null) {
            return null;
        }
        return new ChatRoomLastMessageResponse(
                room.getLastMessageType() != null ? room.getLastMessageType().name() : ChatMessageType.SYSTEM.name(),
                room.getLastMessageText(),
                room.getLastMessageSenderName(),
                room.getLastMessageTimestamp()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message, String senderPhotoUrl) {
        ChatAccountDataResponse accountDataResponse = null;
        if (message.getAccountData() != null) {
            accountDataResponse = new ChatAccountDataResponse(
                    message.getAccountData().getBankName(),
                    message.getAccountData().getAccountNumber(),
                    message.getAccountData().getAccountHolder(),
                    message.getAccountData().getHideName()
            );
        }

        ChatArrivalDataResponse arrivalDataResponse = null;
        if (message.getArrivalData() != null) {
            arrivalDataResponse = new ChatArrivalDataResponse(
                    message.getArrivalData().getTaxiFare(),
                    message.getArrivalData().getSplitMemberCount(),
                    message.getArrivalData().getPerPersonAmount(),
                    message.getArrivalData().getSettlementTargetMemberIds(),
                    message.getArrivalData().getMemberSettlements() != null
                            ? message.getArrivalData().getMemberSettlements().stream()
                            .map(item -> new ChatArrivalSettlementMemberResponse(
                                    item.getMemberId(),
                                    item.getDisplayName(),
                                    item.isSettled(),
                                    item.getSettledAt(),
                                    item.isLeftParty(),
                                    item.getLeftAt()
                            ))
                            .toList()
                            : null,
                    message.getArrivalData().getAccountData() != null
                            ? new ChatAccountDataResponse(
                            message.getArrivalData().getAccountData().getBankName(),
                            message.getArrivalData().getAccountData().getAccountNumber(),
                            message.getArrivalData().getAccountData().getAccountHolder(),
                            message.getArrivalData().getAccountData().getHideName()
                    )
                            : null
            );
        }

        String imageUrl = message.getType() == ChatMessageType.IMAGE ? message.getText() : null;
        String text = message.getType() == ChatMessageType.IMAGE ? null : message.getText();
        String resolvedSenderPhotoUrl = message.hasSource(ChatMessage.SOURCE_ADMIN_SYSTEM) ? null : senderPhotoUrl;

        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                message.getSenderName(),
                resolvedSenderPhotoUrl,
                message.getType(),
                text,
                imageUrl,
                accountDataResponse,
                arrivalDataResponse,
                message.getCreatedAt()
        );
    }

    private Map<String, String> resolveSenderPhotoUrls(List<ChatMessage> messages) {
        List<String> senderIds = messages.stream()
                .filter(message -> !message.isMinecraftOrigin())
                .map(ChatMessage::getSenderId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, String> photoUrls = senderIds.isEmpty()
                ? new java.util.LinkedHashMap<>()
                : memberRepository.findAllById(senderIds).stream()
                .collect(
                        java.util.LinkedHashMap::new,
                        (resolvedPhotoUrls, member) -> resolvedPhotoUrls.put(member.getId(), member.getPhotoUrl()),
                        Map::putAll
                );

        for (ChatMessage message : messages) {
            if (!message.isMinecraftOrigin() || !StringUtils.hasText(message.getMinecraftUuid())) {
                continue;
            }
            photoUrls.put(message.getSenderId(), minecraftAvatarService.resolveAvatarUrl(message.getMinecraftUuid()));
        }
        return photoUrls;
    }

    private ChatMessageResponse saveAndPublishMessage(
            ChatRoom room,
            String senderId,
            String senderName,
            String senderPhotoUrl,
            String text,
            ChatMessageType type,
            ChatAccountData accountData,
            ChatArrivalData arrivalData,
            String source
    ) {
        return saveAndPublishMessage(
                room,
                senderId,
                senderName,
                senderPhotoUrl,
                text,
                type,
                accountData,
                arrivalData,
                source,
                null,
                null,
                null
        );
    }

    private ChatMessageResponse saveAndPublishMessage(
            ChatRoom room,
            String senderId,
            String senderName,
            String senderPhotoUrl,
            String text,
            ChatMessageType type,
            ChatAccountData accountData,
            ChatArrivalData arrivalData,
            String source,
            ChatMessageDirection direction,
            String minecraftUuid,
            String sourceEventId
    ) {
        String chatRoomId = room.getId();
        ChatMessage message = ChatMessage.create(
                chatRoomId,
                senderId,
                senderName,
                chatMessageOrderGenerator.nextOrder(),
                text,
                type,
                accountData,
                arrivalData
        );
        message.markSource(source);
        if (direction != null) {
            message.markDirection(direction);
        }
        if (StringUtils.hasText(minecraftUuid)) {
            message.markMinecraftUuid(minecraftUuid);
        }
        if (StringUtils.hasText(sourceEventId)) {
            message.markSourceEventId(sourceEventId);
        }

        ChatMessage saved;
        try {
            saved = StringUtils.hasText(sourceEventId)
                    ? chatMessageRepository.saveAndFlush(message)
                    : chatMessageRepository.save(message);
        } catch (DataIntegrityViolationException e) {
            if (StringUtils.hasText(sourceEventId)) {
                ChatMessage duplicatedMessage = chatMessageRepository.findBySourceEventId(sourceEventId).orElse(null);
                if (duplicatedMessage != null) {
                    return toMessageResponse(duplicatedMessage, senderPhotoUrl);
                }
            }
            throw e;
        }

        room.applyNewMessage(saved);
        chatRoomRepository.save(room);

        ChatMessageResponse response = toMessageResponse(saved, senderPhotoUrl);
        publishAfterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, response);
            publishChatRoomSummaryEvent(room);
        });
        eventPublisher.publish(new NotificationDomainEvent.ChatMessageCreated(chatRoomId, saved.getId()));
        if (minecraftBridgeProperties.normalizedRoomId().equals(chatRoomId) && !saved.isMinecraftOrigin()) {
            minecraftBridgeOutboxService.publishChatFromApp(saved);
        }

        return response;
    }

    private ChatMessageResponse createMembershipSystemMessage(
            ChatRoom room,
            String senderId,
            String senderName,
            String senderPhotoUrl,
            String text,
            String source
    ) {
        return saveAndPublishMessage(
                room,
                senderId,
                senderName,
                senderPhotoUrl,
                text,
                ChatMessageType.SYSTEM,
                null,
                null,
                source
        );
    }

    private String requireText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "TEXT 메시지에는 text가 필요합니다.");
        }
        return text;
    }

    private String requireImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "IMAGE 메시지에는 imageUrl이 필요합니다.");
        }
        return imageUrl;
    }

    private void validateCursor(LocalDateTime cursorCreatedAt, String cursorId) {
        boolean createdAtProvided = cursorCreatedAt != null;
        boolean idProvided = StringUtils.hasText(cursorId);
        if (createdAtProvided != idProvided) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "cursorCreatedAt와 cursorId는 함께 전달해야 합니다.");
        }
    }

    private Long resolveCursorMessageOrder(String chatRoomId, String cursorId) {
        if (!StringUtils.hasText(cursorId)) {
            return null;
        }
        return chatMessageRepository.findById(cursorId)
                .filter(message -> chatRoomId.equals(message.getChatRoomId()))
                .map(ChatMessage::getMessageOrder)
                .orElse(null);
    }

    private ChatRoomAccess findAccessibleRoom(String memberId, String chatRoomId) {
        ChatRoom room = findRoomOrThrow(chatRoomId);
        ChatRoomMember member = findMembership(chatRoomId, memberId);
        if (member != null) {
            return new ChatRoomAccess(room, member);
        }
        if (!room.isPublic()) {
            throw new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER);
        }
        if (room.getType() == ChatRoomType.DEPARTMENT && !matchesDepartment(room.getDepartment(), findCurrentDepartment(memberId))) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        return new ChatRoomAccess(room, null);
    }

    private ChatRoomMember findMembership(String chatRoomId, String memberId) {
        return chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId(chatRoomId, memberId)
                .orElse(null);
    }

    private boolean isVisibleToMember(ChatRoom room, ChatRoomMember member, String currentDepartment) {
        if (member != null) {
            return true;
        }
        if (!room.isPublic()) {
            return false;
        }
        return room.getType() != ChatRoomType.DEPARTMENT || matchesDepartment(room.getDepartment(), currentDepartment);
    }

    private boolean matchesDepartment(String roomDepartment, String currentDepartment) {
        return Objects.equals(normalizeDepartment(roomDepartment), normalizeDepartment(currentDepartment));
    }

    private String findCurrentDepartment(String memberId) {
        return memberRepository.findActiveById(memberId)
                .map(Member::getDepartment)
                .map(this::normalizeDepartment)
                .orElse(null);
    }

    private Member requireActiveMember(String memberId) {
        return memberRepository.findActiveById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private String resolveMembershipDisplayName(Member member) {
        if (StringUtils.hasText(member.getNickname())) {
            return member.getNickname();
        }
        if (StringUtils.hasText(member.getRealname())) {
            return member.getRealname();
        }
        return null;
    }

    private void validatePublicRoomMembershipAction(ChatRoom room, String actionName) {
        if (room.getType() == ChatRoomType.PARTY) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파티 채팅방 " + actionName + "는 택시 파티 API로 처리해야 합니다.");
        }
        if (!room.isPublic()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "공개 채팅방만 이 API로 처리할 수 있습니다.");
        }
    }

    private LocalDateTime initialLastReadAt(ChatRoom room) {
        return room.getLastMessageTimestamp() != null ? room.getLastMessageTimestamp() : LocalDateTime.now(CHAT_TIME_ZONE);
    }

    private LocalDateTime toChatLocalDateTime(Instant value) {
        if (value == null) {
            return null;
        }
        return LocalDateTime.ofInstant(value, CHAT_TIME_ZONE);
    }

    private Instant toApiInstant(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(CHAT_TIME_ZONE).toInstant();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeDepartment(String value) {
        return DepartmentAliasNormalizer.normalizeCandidate(value);
    }

    private void removeMembership(ChatRoomMember membership, boolean notifyRemovedMember, boolean publishSummaryEvent) {
        ChatRoom room = membership.getChatRoom();
        String memberId = membership.getMemberId();
        chatRoomMemberRepository.delete(membership);
        room.decreaseMemberCount();
        chatRoomRepository.save(room);
        publishAfterCommit(() -> {
            if (publishSummaryEvent) {
                publishChatRoomSummaryEvent(room);
            }
            if (notifyRemovedMember) {
                publishChatRoomRemovedEvent(room, memberId);
            }
        });
    }

    private record ChatRoomAccess(ChatRoom room, ChatRoomMember member) {
    }
}
