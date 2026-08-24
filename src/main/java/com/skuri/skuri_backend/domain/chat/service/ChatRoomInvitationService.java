package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationBatchResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationEligibleFriendsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationMutationResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationReceivedResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationSendResultResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomInvitationTargetResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendInvitationCandidateResponse;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService;
import com.skuri.skuri_backend.domain.friend.service.FriendRelationshipQueryService.InvitationCandidate;
import com.skuri.skuri_backend.domain.member.constant.DepartmentAliasNormalizer;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomInvitationService {

    private static final int EXPIRY_RECONCILIATION_BATCH_SIZE = 100;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomInvitationRepository invitationRepository;
    private final MemberRepository memberRepository;
    private final FriendRelationshipQueryService friendRelationshipQueryService;
    private final FriendMemberPairLockService pairLockService;
    private final ChatRoomInvitationSendItemService sendItemService;
    private final ChatRoomInvitationTransitionService transitionService;
    private final ChatRoomInvitationExpirationService expirationService;

    @Transactional
    public ChatRoomInvitationEligibleFriendsResponse getEligibleFriends(
            String inviterMemberId,
            String chatRoomId
    ) {
        pairLockService.requireActiveProfileCompleteMember(inviterMemberId);
        ChatRoom room = requireInvitableRoomMember(chatRoomId, inviterMemberId);
        if (isFull(room)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FULL);
        }

        List<InvitationCandidate> candidates = friendRelationshipQueryService.getInvitationCandidates(inviterMemberId);
        Set<String> candidateMemberIds = candidates.stream().map(InvitationCandidate::memberId).collect(Collectors.toSet());
        if (candidateMemberIds.isEmpty()) {
            return eligibleResponse(room, List.of(), List.of(), List.of(), 0);
        }
        Set<String> memberIds = Set.copyOf(chatRoomMemberRepository
                .findMemberIdsByChatRoomIdAndCandidateMemberIds(chatRoomId, candidateMemberIds));
        Set<String> pendingInviteeIds = Set.copyOf(
                invitationRepository.findPendingInviteeIds(
                        chatRoomId,
                        LocalDateTime.now(),
                        candidateMemberIds
                )
        );

        int notEligibleCount = 0;
        List<FriendInvitationCandidateResponse> eligible = new java.util.ArrayList<>();
        List<FriendInvitationCandidateResponse> alreadyMembers = new java.util.ArrayList<>();
        List<FriendInvitationCandidateResponse> alreadyPending = new java.util.ArrayList<>();
        for (InvitationCandidate candidate : candidates) {
            if (!isDepartmentEligible(room, candidate.response().department())) {
                notEligibleCount++;
            } else if (memberIds.contains(candidate.memberId())) {
                alreadyMembers.add(candidate.response());
            } else if (pendingInviteeIds.contains(candidate.memberId())) {
                alreadyPending.add(candidate.response());
            } else {
                eligible.add(candidate.response());
            }
        }
        return eligibleResponse(room, eligible, alreadyMembers, alreadyPending, notEligibleCount);
    }

    public ChatRoomInvitationBatchResponse send(
            String inviterMemberId,
            String chatRoomId,
            List<String> friendPublicIds
    ) {
        pairLockService.requireActiveProfileCompleteMember(inviterMemberId);
        requireInvitableRoomMember(chatRoomId, inviterMemberId);
        List<String> normalized = new java.util.ArrayList<>(new LinkedHashSet<>(friendPublicIds));
        List<ChatRoomInvitationSendResultResponse> results = normalized.stream()
                .map(friendPublicId -> sendItemService.send(inviterMemberId, chatRoomId, friendPublicId))
                .toList();
        return new ChatRoomInvitationBatchResponse(results);
    }

    public List<ChatRoomInvitationReceivedResponse> getReceived(String inviteeMemberId) {
        pairLockService.requireActiveProfileCompleteMember(inviteeMemberId);
        Member invitee = memberRepository.findActiveById(inviteeMemberId)
                .orElseThrow(MemberNotFoundException::new);
        invitationRepository.findTimedOutPendingReceivedIds(
                        inviteeMemberId,
                        LocalDateTime.now(),
                        PageRequest.of(0, EXPIRY_RECONCILIATION_BATCH_SIZE)
                )
                .forEach(expirationService::expireIfTimedOut);
        invitationRepository.findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
                        inviteeMemberId,
                        List.of(ChatRoomInvitationStatus.PENDING)
                )
                .forEach(invitation -> transitionService.reconcile(invitation.getId()));

        List<ChatRoomInvitation> invitations = invitationRepository
                .findByInviteeIdAndStatusInOrderByCreatedAtDescIdDesc(
                        inviteeMemberId,
                        List.of(ChatRoomInvitationStatus.PENDING, ChatRoomInvitationStatus.EXPIRED)
                );
        Map<String, ChatRoom> rooms = chatRoomRepository.findAllById(
                        invitations.stream().map(ChatRoomInvitation::getChatRoomId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(ChatRoom::getId, Function.identity()));
        Map<String, FriendInvitationCandidateResponse> inviters = friendRelationshipQueryService
                .findInvitationCandidatesByMemberIds(
                        inviteeMemberId,
                        invitations.stream().map(ChatRoomInvitation::getInviterId).collect(Collectors.toSet())
                );
        Set<String> joinedChatRoomIds = Set.copyOf(
                chatRoomMemberRepository.findChatRoomIdsByMemberId(inviteeMemberId)
        );
        return invitations.stream()
                .map(invitation -> toReceivedResponse(
                        invitation,
                        rooms.get(invitation.getChatRoomId()),
                        inviters.get(invitation.getInviterId()),
                        invitee.getDepartment(),
                        joinedChatRoomIds
                ))
                .toList();
    }

    public ChatRoomInvitationMutationResponse accept(String inviteeMemberId, String invitationId) {
        ChatRoomInvitationTransitionService.AcceptAttempt attempt = transitionService.accept(
                inviteeMemberId,
                invitationId
        );
        if (attempt.outcome() != ChatRoomInvitationTransitionService.AcceptOutcome.ACCEPTED) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_STATE_NOT_ALLOWED);
        }
        return new ChatRoomInvitationMutationResponse(
                invitationId,
                attempt.chatRoomId(),
                ChatRoomInvitationStatus.ACCEPTED
        );
    }

    public ChatRoomInvitationMutationResponse decline(String inviteeMemberId, String invitationId) {
        if (!transitionService.decline(inviteeMemberId, invitationId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_STATE_NOT_ALLOWED);
        }
        ChatRoomInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_NOT_FOUND));
        return new ChatRoomInvitationMutationResponse(invitationId, invitation.getChatRoomId(), invitation.getStatus());
    }

    public void cancel(String inviterMemberId, String invitationId) {
        if (!transitionService.cancel(inviterMemberId, invitationId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVITATION_STATE_NOT_ALLOWED);
        }
    }

    private ChatRoom requireInvitableRoomMember(String chatRoomId, String inviterMemberId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.isPublic() || room.getType() == ChatRoomType.PARTY) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "공개 PARTY 이외의 채팅방만 친구를 초대할 수 있습니다.");
        }
        if (!chatRoomMemberRepository.existsById_ChatRoomIdAndId_MemberId(chatRoomId, inviterMemberId)) {
            throw new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER);
        }
        return room;
    }

    private boolean isFull(ChatRoom room) {
        return room.getMaxMembers() != null && room.getMemberCount() >= room.getMaxMembers();
    }

    private boolean isDepartmentEligible(ChatRoom room, String department) {
        if (room.getType() != ChatRoomType.DEPARTMENT) {
            return true;
        }
        return Objects.equals(
                DepartmentAliasNormalizer.normalizeCandidate(room.getDepartment()),
                DepartmentAliasNormalizer.normalizeCandidate(department)
        );
    }

    private ChatRoomInvitationEligibleFriendsResponse eligibleResponse(
            ChatRoom room,
            List<FriendInvitationCandidateResponse> eligible,
            List<FriendInvitationCandidateResponse> alreadyMembers,
            List<FriendInvitationCandidateResponse> alreadyPending,
            int notEligibleCount
    ) {
        Integer remainingCapacity = room.getMaxMembers() == null
                ? null
                : Math.max(0, room.getMaxMembers() - room.getMemberCount());
        return new ChatRoomInvitationEligibleFriendsResponse(
                room.getId(),
                room.getName(),
                remainingCapacity,
                7,
                room.getType() == ChatRoomType.DEPARTMENT,
                eligible,
                alreadyMembers,
                alreadyPending,
                alreadyMembers.size(),
                alreadyPending.size(),
                notEligibleCount
        );
    }

    private ChatRoomInvitationReceivedResponse toReceivedResponse(
            ChatRoomInvitation invitation,
            ChatRoom room,
            FriendInvitationCandidateResponse inviter,
            String inviteeDepartment,
            Set<String> joinedChatRoomIds
    ) {
        ChatRoomInvitationTargetResponse target = !canExposeTarget(room, inviteeDepartment, joinedChatRoomIds)
                ? null
                : new ChatRoomInvitationTargetResponse(
                        room.getId(),
                        room.getName(),
                        room.getType(),
                        room.getMemberCount(),
                        room.getMaxMembers()
                );
        return new ChatRoomInvitationReceivedResponse(
                invitation.getId(),
                "CHAT_ROOM",
                invitation.getStatus(),
                invitation.getStatus() == ChatRoomInvitationStatus.EXPIRED ? invitation.getExpiryReason() : null,
                inviter,
                target,
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getRespondedAt()
        );
    }

    private boolean canExposeTarget(
            ChatRoom room,
            String inviteeDepartment,
            Set<String> joinedChatRoomIds
    ) {
        if (room == null || room.getType() != ChatRoomType.DEPARTMENT || joinedChatRoomIds.contains(room.getId())) {
            return room != null;
        }
        return Objects.equals(
                DepartmentAliasNormalizer.normalizeCandidate(room.getDepartment()),
                DepartmentAliasNormalizer.normalizeCandidate(inviteeDepartment)
        );
    }
}
