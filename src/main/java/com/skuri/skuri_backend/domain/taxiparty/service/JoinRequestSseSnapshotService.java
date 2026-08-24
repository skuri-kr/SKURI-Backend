package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestListItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyInvitationRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JoinRequestSseSnapshotService {

    private final PartyRepository partyRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final PartyInvitationRepository partyInvitationRepository;
    private final MemberRepository memberRepository;

    public Map<String, Object> createPartyJoinRequestsSnapshotPayload(String actorId, String partyId) {
        Party party = partyRepository.findById(partyId).orElseThrow(PartyNotFoundException::new);
        if (!party.isLeader(actorId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }

        return Map.of(
                "partyId", partyId,
                "requests", getPartyJoinRequestSnapshot(partyId)
        );
    }

    public Map<String, Object> createMyJoinRequestsSnapshotPayload(String memberId, JoinRequestStatus status) {
        return Map.of("requests", getMyJoinRequestSnapshot(memberId, status));
    }

    public JoinRequestListItemResponse toSseItem(JoinRequest joinRequest) {
        Member requester = memberRepository.findById(joinRequest.getRequesterId()).orElse(null);
        String invitationInviterId = findInvitationInviterId(joinRequest.getId());
        Member invitationInviter = invitationInviterId == null
                ? null
                : memberRepository.findById(invitationInviterId).orElse(null);
        return toSseItem(joinRequest, requester, invitationInviter);
    }

    public JoinRequestListItemResponse toSseItem(String joinRequestId) {
        return joinRequestRepository.findDetailById(joinRequestId)
                .map(this::toSseItem)
                .orElse(null);
    }

    private List<JoinRequestListItemResponse> getPartyJoinRequestSnapshot(String partyId) {
        List<JoinRequest> requests = joinRequestRepository.findByParty_IdOrderByCreatedAtDesc(partyId);
        return toSseItems(requests);
    }

    private List<JoinRequestListItemResponse> getMyJoinRequestSnapshot(String memberId, JoinRequestStatus status) {
        List<JoinRequest> requests = status == null
                ? joinRequestRepository.findByRequesterIdOrderByCreatedAtDesc(memberId)
                : joinRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(memberId, status);
        return toSseItems(requests);
    }

    private List<JoinRequestListItemResponse> toSseItems(List<JoinRequest> requests) {
        Map<String, Member> requesterMap = getMemberMap(requests.stream().map(JoinRequest::getRequesterId).toList());
        Map<String, String> invitationInviterIdsByRequestId = getInvitationInviterIdsByRequestId(requests);
        Map<String, Member> invitationInviterMap = getMemberMap(
                invitationInviterIdsByRequestId.values().stream().toList()
        );
        return requests.stream()
                .map(request -> {
                    String invitationInviterId = invitationInviterIdsByRequestId.get(request.getId());
                    Member invitationInviter = invitationInviterId == null
                            ? null
                            : invitationInviterMap.get(invitationInviterId);
                    return toSseItem(
                            request,
                            requesterMap.get(request.getRequesterId()),
                            invitationInviter
                    );
                })
                .toList();
    }

    private JoinRequestListItemResponse toSseItem(
            JoinRequest joinRequest,
            Member requester,
            Member invitationInviter
    ) {
        return new JoinRequestListItemResponse(
                joinRequest.getId(),
                joinRequest.getParty().getId(),
                joinRequest.getRequesterId(),
                requester != null ? requester.getNickname() : null,
                requester != null ? requester.getPhotoUrl() : null,
                invitationInviter != null ? invitationInviter.getNickname() : null,
                joinRequest.getStatus(),
                joinRequest.getExpiryReason(),
                joinRequest.getCreatedAt()
        );
    }

    private Map<String, String> getInvitationInviterIdsByRequestId(List<JoinRequest> requests) {
        List<String> requestIds = requests.stream().map(JoinRequest::getId).toList();
        if (requestIds.isEmpty()) {
            return Map.of();
        }

        Map<String, String> result = new HashMap<>();
        partyInvitationRepository.findAcceptedJoinRequestSources(requestIds)
                .forEach(source -> result.putIfAbsent(source.getJoinRequestId(), source.getInviterId()));
        return result;
    }

    private String findInvitationInviterId(String joinRequestId) {
        return partyInvitationRepository.findAcceptedJoinRequestSources(List.of(joinRequestId)).stream()
                .map(PartyInvitationRepository.AcceptedJoinRequestSource::getInviterId)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Member> getMemberMap(List<String> memberIds) {
        List<String> ids = memberIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Member> memberMap = new HashMap<>();
        memberRepository.findAllById(ids).forEach(member -> memberMap.put(member.getId(), member));
        return memberMap;
    }
}
