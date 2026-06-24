package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.AdminCreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminChatRoomMemberResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminCreateChatRoomResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatAdminService {

    private static final ZoneId CHAT_TIME_ZONE = ZoneId.of("Asia/Seoul");

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final ChatService chatService;

    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getPublicChatRooms(ChatRoomType type) {
        return chatService.getAdminPublicChatRooms(type);
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getPublicChatRoomDetail(String chatRoomId) {
        return chatService.getAdminPublicChatRoomDetail(chatRoomId);
    }

    @Transactional(readOnly = true)
    public List<AdminChatRoomMemberResponse> getPublicChatRoomMembers(String chatRoomId) {
        findPublicNonPartyRoomOrThrow(chatRoomId);

        List<ChatRoomMember> memberships = chatRoomMemberRepository.findByChatRoomIdOrdered(chatRoomId);
        List<String> memberIds = memberships.stream()
                .map(ChatRoomMember::getMemberId)
                .toList();
        Map<String, Member> membersById = memberIds.isEmpty()
                ? Map.of()
                : memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        return memberships.stream()
                .map(membership -> toAdminChatRoomMemberResponse(membership, membersById.get(membership.getMemberId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getPublicChatRoomMessages(
            String chatRoomId,
            LocalDateTime cursorCreatedAt,
            String cursorId,
            Integer size
    ) {
        return chatService.getAdminPublicChatRoomMessages(chatRoomId, cursorCreatedAt, cursorId, size);
    }

    @Transactional
    public AdminCreateChatRoomResponse createPublicChatRoom(String adminId, AdminCreateChatRoomRequest request) {
        validateCreateRequest(request);

        ChatRoom room = ChatRoom.create(
                "room:" + UUID.randomUUID(),
                request.name().trim(),
                request.type(),
                null,
                request.description(),
                adminId,
                true,
                null
        );
        ChatRoom saved = chatRoomRepository.save(room);
        chatRoomMemberRepository.save(ChatRoomMember.create(saved, adminId, LocalDateTime.now()));
        saved.updateMemberCount(1);
        chatRoomRepository.save(saved);

        return new AdminCreateChatRoomResponse(saved.getId(), saved.getName(), saved.getType());
    }

    @Transactional
    public void deletePublicChatRoom(String chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (room.getType() == ChatRoomType.PARTY) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파티 채팅방은 관리자 API로 삭제할 수 없습니다.");
        }
        if (!room.isPublic()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "공개 채팅방만 관리자 API로 삭제할 수 있습니다.");
        }

        chatMessageRepository.deleteByChatRoomId(chatRoomId);
        chatRoomMemberRepository.deleteById_ChatRoomId(chatRoomId);
        chatRoomRepository.delete(room);
    }

    private void validateCreateRequest(AdminCreateChatRoomRequest request) {
        if (request.type() == ChatRoomType.PARTY) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "PARTY 타입 채팅방은 파티 생성 시 자동 생성됩니다.");
        }
        if (!Boolean.TRUE.equals(request.isPublic())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "관리자 채팅방 생성 API는 isPublic=true만 허용합니다.");
        }
    }

    private ChatRoom findPublicNonPartyRoomOrThrow(String chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (room.getType() == ChatRoomType.PARTY || !room.isPublic()) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        return room;
    }

    private AdminChatRoomMemberResponse toAdminChatRoomMemberResponse(ChatRoomMember membership, Member member) {
        return new AdminChatRoomMemberResponse(
                membership.getMemberId(),
                member != null ? member.getEmail() : null,
                member != null ? member.getNickname() : null,
                member != null ? member.getRealname() : null,
                member != null ? member.getStudentId() : null,
                member != null ? member.getDepartment() : null,
                member != null ? member.getPhotoUrl() : null,
                toApiInstant(membership.getJoinedAt()),
                toApiInstant(membership.getLastReadAt()),
                membership.isMuted(),
                member != null ? member.getStatus() : null
        );
    }

    private Instant toApiInstant(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(CHAT_TIME_ZONE).toInstant();
    }
}
