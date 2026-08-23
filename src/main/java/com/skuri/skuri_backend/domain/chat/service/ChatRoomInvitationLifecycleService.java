package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationExpiryReason;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitationStatus;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomInvitationLifecycleService {

    private final ChatRoomInvitationRepository invitationRepository;

    @Transactional
    public void expirePendingForRoom(String chatRoomId, ChatRoomInvitationExpiryReason reason) {
        LocalDateTime now = LocalDateTime.now();
        invitationRepository.findPendingByChatRoomIdForUpdate(chatRoomId)
                .forEach(invitation -> invitation.expire(reason, now));
    }

    @Transactional(readOnly = true)
    public List<String> findPendingChatRoomIdsByInviter(String inviterId) {
        return invitationRepository.findPendingChatRoomIdsByInviterId(inviterId);
    }

    @Transactional
    public void expirePendingByInviterInRoom(
            String chatRoomId,
            String inviterId,
            ChatRoomInvitationExpiryReason reason
    ) {
        LocalDateTime now = LocalDateTime.now();
        invitationRepository.findPendingByChatRoomIdAndInviterIdForUpdate(chatRoomId, inviterId)
                .forEach(invitation -> invitation.expire(reason, now));
    }

    @Transactional
    public void expirePendingForInviteeInRoom(
            String chatRoomId,
            String inviteeId,
            ChatRoomInvitationExpiryReason reason
    ) {
        invitationRepository.findByActiveTargetKeyForUpdate(
                        com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation.activeTargetKey(
                                chatRoomId,
                                inviteeId
                        )
                )
                .filter(com.skuri.skuri_backend.domain.chat.entity.ChatRoomInvitation::isPending)
                .ifPresent(invitation -> invitation.expire(reason, LocalDateTime.now()));
    }

    @Transactional
    public void expirePendingForMemberPair(
            String firstMemberId,
            String secondMemberId,
            ChatRoomInvitationExpiryReason reason
    ) {
        LocalDateTime now = LocalDateTime.now();
        invitationRepository.findPendingByMemberPairForUpdate(firstMemberId, secondMemberId)
                .forEach(invitation -> invitation.expire(reason, now));
    }

    @Transactional
    public void expirePendingDepartmentRoomInvitationsForInvitee(
            String inviteeId,
            ChatRoomInvitationExpiryReason reason
    ) {
        LocalDateTime now = LocalDateTime.now();
        invitationRepository.findPendingDepartmentRoomInvitationsByInviteeIdForUpdate(inviteeId)
                .forEach(invitation -> invitation.expire(reason, now));
    }

    @Transactional(readOnly = true)
    public long countPendingReceived(String memberId) {
        return invitationRepository.countByInviteeIdAndStatus(memberId, ChatRoomInvitationStatus.PENDING);
    }
}
