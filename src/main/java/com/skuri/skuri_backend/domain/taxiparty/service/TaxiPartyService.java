package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.exception.MemberWithdrawalNotAllowedException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.ArrivePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.CreatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.request.UpdatePartyRequest;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestListItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestAcceptResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MemberSettlementResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MyPartyResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyCreateResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyMemberResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyParticipantSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementAccountResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementConfirmResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryRole;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistoryStatus;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.TaxiHistorySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.MemberSettlement;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyEndReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyMember;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementAccountSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementTargetSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.exception.JoinRequestNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyTagRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class TaxiPartyService {

    private static final Set<PartyStatus> ACTIVE_PARTY_STATUSES = EnumSet.of(PartyStatus.OPEN, PartyStatus.CLOSED, PartyStatus.ARRIVED);

    private final PartyRepository partyRepository;
    private final PartyTagRepository partyTagRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final MemberRepository memberRepository;
    private final ChatService chatService;
    private final PartySseService partySseService;
    private final JoinRequestSseService joinRequestSseService;
    private final PartyInvitationLifecycleService partyInvitationLifecycleService;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional
    public PartyCreateResponse createParty(String leaderId, CreatePartyRequest request) {
        lockMemberOrThrow(leaderId);
        if (partyRepository.existsActivePartyByMemberId(leaderId, ACTIVE_PARTY_STATUSES, null)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }

        Party created = partyRepository.save(
                Party.create(
                        leaderId,
                        toLocation(request.departure()),
                        toLocation(request.destination()),
                        request.departureTime(),
                        request.maxMembers(),
                        request.tags(),
                        request.detail()
                )
        );
        chatService.createPartyChatRoom(created);
        Member leader = memberRepository.findById(leaderId).orElse(null);
        partySseService.publishPartyCreated(created, leader);
        eventPublisher.publish(new NotificationDomainEvent.PartyCreated(created.getId()));

        return new PartyCreateResponse(created.getId(), "party:" + created.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<PartySummaryResponse> getParties(
            PartyStatus status,
            LocalDateTime departureTime,
            String departureName,
            String destinationName,
            Pageable pageable
    ) {
        Page<Party> page = partyRepository.search(status, departureTime, departureName, destinationName, pageable);
        Map<String, Party> detailedPartyMap = getDetailedPartyMap(page.getContent());
        Map<String, List<String>> partyTagMap = getPartyTagMap(detailedPartyMap.keySet());
        Map<String, Member> memberMap = getMemberMap(
                detailedPartyMap.values().stream()
                        .flatMap(party -> party.getMemberIds().stream())
                        .toList()
        );

        return PageResponse.from(page.map(party -> toPartySummaryResponse(
                detailedPartyMap.getOrDefault(party.getId(), party),
                memberMap,
                partyTagMap.getOrDefault(party.getId(), List.of())
        )));
    }

    @Transactional(readOnly = true)
    public PartyDetailResponse getPartyDetail(String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        Map<String, Member> memberMap = getMemberMap(getVisibleMemberIds(party));
        return toPartyDetailResponse(party, memberMap);
    }

    @Transactional
    public PartyDetailResponse updateParty(String actorId, String partyId, UpdatePartyRequest request) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);

        if (party.getStatus() != PartyStatus.OPEN && party.getStatus() != PartyStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "OPEN/CLOSED 상태에서만 수정할 수 있습니다.");
        }
        if (request.departureTime() == null && request.detail() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "departureTime 또는 detail 중 최소 하나는 입력해야 합니다.");
        }

        if (request.departureTime() != null) {
            party.updateDepartureTime(request.departureTime());
        }
        if (request.detail() != null) {
            party.updateDetail(request.detail());
        }

        savePartyWithLockHandling(party);
        Map<String, Member> memberMap = getMemberMap(getVisibleMemberIds(party));
        partySseService.publishPartyUpdated(party, memberMap.get(party.getLeaderId()));
        return toPartyDetailResponse(party, memberMap);
    }

    @Transactional(readOnly = true)
    public List<MyPartyResponse> getMyParties(String memberId) {
        List<Party> parties = partyRepository.findMyParties(memberId);
        Map<String, Member> memberMap = getMemberMap(
                parties.stream()
                        .flatMap(party -> getVisibleMemberIds(party).stream())
                        .distinct()
                        .toList()
        );

        return parties.stream()
                .map(party -> new MyPartyResponse(
                        party.getId(),
                        party.getStatus(),
                        toLocationResponse(party.getDeparture()),
                        toLocationResponse(party.getDestination()),
                        party.isLeader(memberId),
                        toSettlementSummary(party, memberMap)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaxiHistoryItemResponse> getMyTaxiHistory(String memberId) {
        return findMyTaxiHistoryParties(memberId).stream()
                .map(party -> toTaxiHistoryItemResponse(party, memberId))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxiHistorySummaryResponse getMyTaxiHistorySummary(String memberId) {
        List<Party> historyParties = findMyTaxiHistoryParties(memberId);
        int totalRideCount = historyParties.size();
        int completedRideCount = 0;
        int savedFareAmount = 0;

        for (Party party : historyParties) {
            if (toTaxiHistoryStatus(party) != TaxiHistoryStatus.COMPLETED) {
                continue;
            }
            completedRideCount++;
            savedFareAmount += calculateSavedFareAmount(party);
        }

        return new TaxiHistorySummaryResponse(totalRideCount, completedRideCount, savedFareAmount);
    }

    @Transactional
    public PartyStatusResponse closeParty(String actorId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        party.close();
        savePartyWithLockHandling(party);
        chatService.createPartySystemMessage(party, actorId, "모집이 마감되었어요.");
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        partyInvitationLifecycleService.expirePendingForParty(
                party.getId(),
                com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.TARGET_UNAVAILABLE
        );
        return toPartyStatusResponse(party);
    }

    @Transactional
    public PartyStatusResponse reopenParty(String actorId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        party.reopen();
        savePartyWithLockHandling(party);
        chatService.createPartySystemMessage(party, actorId, "모집이 재개되었어요.");
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        return toPartyStatusResponse(party);
    }

    @Transactional
    public PartyDetailResponse arriveParty(String actorId, String partyId, ArrivePartyRequest request) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        List<String> normalizedSettlementTargetMemberIds = normalizeMemberIds(request.settlementTargetMemberIds());
        Map<String, Member> settlementTargetMemberMap = getMemberMap(normalizedSettlementTargetMemberIds);
        party.arriveWithSnapshots(
                request.taxiFare(),
                toSettlementTargetSnapshots(normalizedSettlementTargetMemberIds, settlementTargetMemberMap),
                toSettlementAccountSnapshot(request.account())
        );
        savePartyWithLockHandling(party);
        chatService.createPartyArrivalMessage(party, actorId);
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        partyInvitationLifecycleService.expirePendingForParty(
                party.getId(),
                com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.TARGET_UNAVAILABLE
        );

        Map<String, Member> memberMap = getMemberMap(getVisibleMemberIds(party));
        return toPartyDetailResponse(party, memberMap);
    }

    @Transactional
    public PartyStatusResponse endParty(String actorId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        party.forceEnd();
        savePartyWithLockHandling(party);
        chatService.createPartyEndMessage(party, actorId);
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        partyInvitationLifecycleService.expirePendingForParty(
                party.getId(),
                com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.TARGET_UNAVAILABLE
        );
        return toPartyStatusResponse(party);
    }

    @Transactional
    public PartyStatusResponse cancelParty(String actorId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);
        PartyStatus beforeStatus = party.getStatus();
        party.cancel();
        savePartyWithLockHandling(party);
        chatService.createPartyEndMessage(party, actorId);
        partySseService.publishPartyDeleted(party.getId());
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        partyInvitationLifecycleService.expirePendingForParty(
                party.getId(),
                com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.TARGET_UNAVAILABLE
        );
        return toPartyStatusResponse(party);
    }

    @Transactional
    public void kickMember(String actorId, String partyId, String memberId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, actorId);

        if (party.getStatus() == PartyStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_IN_ARRIVED);
        }
        if (party.getStatus() == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }
        if (party.isLeader(memberId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_LEADER);
        }

        List<String> recipientsBeforeRemoval = party.getMemberIds();
        String removedMemberName = resolveMembershipDisplayName(memberId);
        party.removeMember(memberId);
        savePartyWithLockHandling(party);
        chatService.syncPartyChatRoomMembers(party);
        chatService.createPartyMemberLeaveSystemMessage(
                party,
                actorId,
                toMemberLeaveSystemMessage(removedMemberName)
        );
        partySseService.publishPartyMemberLeft(party, memberId, "KICKED", recipientsBeforeRemoval);
        eventPublisher.publish(new NotificationDomainEvent.PartyMemberKicked(party.getId(), memberId));
        partyInvitationLifecycleService.expirePendingByInviterInParty(
                partyId,
                memberId,
                com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.INVITER_LEFT
        );
    }

    @Transactional
    public void leaveParty(String memberId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);

        if (!party.isMember(memberId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_MEMBER);
        }
        if (party.isLeader(memberId)) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
        }
        if (party.getStatus() == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }

        Member leavingMember = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
        String leaveSystemMessage = toMemberLeaveSystemMessage(leavingMember.getNickname());

        if (party.getStatus() == PartyStatus.ARRIVED) {
            leaveArrivedParty(party, memberId, leaveSystemMessage);
            return;
        }

        leaveOpenOrClosedParty(party, memberId, leaveSystemMessage);
    }

    @Transactional
    public JoinRequestResponse createJoinRequest(String requesterId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        lockMemberOrThrow(requesterId);

        if (party.getStatus() == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }
        if (party.getStatus() != PartyStatus.OPEN) {
            throw new BusinessException(ErrorCode.PARTY_CLOSED);
        }
        if (party.isMember(requesterId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }
        if (partyRepository.existsActivePartyByMemberId(requesterId, ACTIVE_PARTY_STATUSES, null)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }
        if (joinRequestRepository.existsByParty_IdAndRequesterIdAndStatus(partyId, requesterId, JoinRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.ALREADY_REQUESTED);
        }

        JoinRequest joinRequest = joinRequestRepository.save(JoinRequest.create(party, requesterId));
        joinRequestSseService.publishJoinRequestCreated(joinRequest);
        eventPublisher.publish(new NotificationDomainEvent.PartyJoinRequestCreated(joinRequest.getId()));
        return toJoinRequestResponse(joinRequest);
    }

    @Transactional
    public JoinRequestAcceptResponse acceptJoinRequest(String leaderId, String requestId) {
        JoinRequest joinRequest = findJoinRequestOrThrow(requestId);
        String requesterId = joinRequest.getRequesterId();
        lockMemberOrThrow(requesterId);
        Party party = partyRepository.findDetailByIdForUpdate(joinRequest.getParty().getId())
                .orElseThrow(PartyNotFoundException::new);
        JoinRequestStatus previousStatus = joinRequest.getStatus();
        PartyStatus beforeStatus = party.getStatus();
        requireJoinRequestLeader(joinRequest, leaderId);

        if (party.getStatus() == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }
        if (party.getStatus() != PartyStatus.OPEN) {
            throw new BusinessException(ErrorCode.PARTY_CLOSED);
        }

        if (party.isMember(requesterId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }
        if (partyRepository.existsActivePartyByMemberId(requesterId, ACTIVE_PARTY_STATUSES, party.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }

        joinRequest.accept();
        party.addMember(requesterId);

        partyInvitationLifecycleService.expirePendingForInviteeInParty(
                party.getId(),
                requesterId,
                com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.ALREADY_JOINED
        );

        joinRequestRepository.save(joinRequest);
        savePartyWithLockHandling(party);
        chatService.syncPartyChatRoomMembers(party);
        String requesterName = resolveMembershipDisplayName(requesterId);
        chatService.createPartyMemberJoinSystemMessage(
                party,
                leaderId,
                toMemberJoinSystemMessage(requesterName)
        );
        if (beforeStatus == PartyStatus.OPEN && party.getStatus() == PartyStatus.CLOSED) {
            chatService.createPartySystemMessage(party, leaderId, "모집이 마감되었어요.");
        }
        partySseService.publishPartyMemberJoined(party, requesterId, requesterName, party.getMemberIds());
        joinRequestSseService.publishJoinRequestUpdated(joinRequest, previousStatus);
        if (beforeStatus != party.getStatus()) {
            partySseService.publishPartyStatusChanged(party);
            eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
            partyInvitationLifecycleService.expirePendingForParty(
                    party.getId(),
                    com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.CAPACITY_FULL
            );
        }
        eventPublisher.publish(new NotificationDomainEvent.PartyJoinRequestProcessed(joinRequest.getId(), joinRequest.getStatus()));
        return toJoinRequestAcceptResponse(joinRequest);
    }

    @Transactional
    public JoinRequestResponse declineJoinRequest(String leaderId, String requestId) {
        JoinRequest joinRequest = findJoinRequestOrThrow(requestId);
        JoinRequestStatus previousStatus = joinRequest.getStatus();
        requireJoinRequestLeader(joinRequest, leaderId);
        joinRequest.decline();
        joinRequestRepository.save(joinRequest);
        joinRequestSseService.publishJoinRequestUpdated(joinRequest, previousStatus);
        eventPublisher.publish(new NotificationDomainEvent.PartyJoinRequestProcessed(joinRequest.getId(), joinRequest.getStatus()));
        return toJoinRequestResponse(joinRequest);
    }

    @Transactional
    public JoinRequestResponse cancelJoinRequest(String requesterId, String requestId) {
        JoinRequest joinRequest = findJoinRequestOrThrow(requestId);
        if (!joinRequest.isRequester(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "요청자 본인만 취소할 수 있습니다.");
        }

        JoinRequestStatus previousStatus = joinRequest.getStatus();
        joinRequest.cancel();
        joinRequestRepository.save(joinRequest);
        joinRequestSseService.publishJoinRequestUpdated(joinRequest, previousStatus);
        return toJoinRequestResponse(joinRequest);
    }

    @Transactional(readOnly = true)
    public List<JoinRequestListItemResponse> getPartyJoinRequests(String leaderId, String partyId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, leaderId);

        List<JoinRequest> requests = joinRequestRepository.findByParty_IdOrderByCreatedAtDesc(partyId);
        return mapJoinRequestResponses(requests);
    }

    @Transactional(readOnly = true)
    public List<JoinRequestListItemResponse> getMyJoinRequests(String requesterId, JoinRequestStatus status) {
        List<JoinRequest> requests = status == null
                ? joinRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId)
                : joinRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(requesterId, status);
        return mapJoinRequestResponses(requests);
    }

    @Transactional
    public SettlementConfirmResponse confirmSettlement(String leaderId, String partyId, String memberId) {
        Party party = findPartyDetailOrThrow(partyId);
        requireLeader(party, leaderId);

        boolean allSettled = party.confirmSettlement(memberId);
        savePartyWithLockHandling(party);
        chatService.syncPartyArrivalMessageSnapshot(party);
        if (allSettled) {
            eventPublisher.publish(new NotificationDomainEvent.PartySettlementCompleted(party.getId()));
        }

        MemberSettlement target = party.getSettlementItems().stream()
                .filter(item -> item.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PARTY_MEMBER, "정산 대상 멤버가 아닙니다."));

        return new SettlementConfirmResponse(memberId, target.isSettled(), target.getSettledAt(), allSettled);
    }

    @Transactional(readOnly = true)
    public void validateWithdrawalAllowed(String memberId) {
        boolean joinedArrivedParty = partyRepository.findActiveDetailsByMemberId(memberId, ACTIVE_PARTY_STATUSES).stream()
                .anyMatch(party -> party.getStatus() == PartyStatus.ARRIVED && !party.isLeader(memberId));
        if (joinedArrivedParty) {
            throw new MemberWithdrawalNotAllowedException("정산이 진행 중인 ARRIVED 파티에 참여 중인 멤버는 탈퇴할 수 없습니다.");
        }
    }

    @Transactional
    public void handleMemberWithdrawal(String memberId) {
        Set<String> targetPartyIds = new TreeSet<>(
                partyRepository.findActiveIdsByMemberId(memberId, ACTIVE_PARTY_STATUSES)
        );
        targetPartyIds.addAll(partyInvitationLifecycleService.findPendingPartyIdsByInviter(memberId));
        for (String partyId : targetPartyIds) {
            Party party = partyRepository.findDetailByIdForUpdate(partyId).orElse(null);
            partyInvitationLifecycleService.expirePendingByInviterInParty(
                    partyId,
                    memberId,
                    PartyInvitationExpiryReason.MEMBER_WITHDRAWN
            );
            if (party == null || !ACTIVE_PARTY_STATUSES.contains(party.getStatus()) || !party.isMember(memberId)) {
                continue;
            }
            if (party.isLeader(memberId)) {
                withdrawLeaderFromParty(party);
                continue;
            }

            if (party.getStatus() == PartyStatus.ARRIVED) {
                throw new MemberWithdrawalNotAllowedException("정산이 진행 중인 ARRIVED 파티에 참여 중인 멤버는 탈퇴할 수 없습니다.");
            }

            party.removeMember(memberId);
            savePartyWithLockHandling(party);
            chatService.syncPartyChatRoomMembers(party);
            partySseService.publishPartyMemberLeft(party, memberId, "WITHDRAWN", party.getMemberIds());
        }

        joinRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(memberId, JoinRequestStatus.PENDING)
                .forEach(request -> {
                    JoinRequestStatus previousStatus = request.getStatus();
                    request.cancel();
                    joinRequestRepository.save(request);
                    joinRequestSseService.publishJoinRequestUpdated(request, previousStatus);
                });
    }

    private List<JoinRequestListItemResponse> mapJoinRequestResponses(List<JoinRequest> requests) {
        Map<String, Member> requesterMap = getMemberMap(requests.stream().map(JoinRequest::getRequesterId).toList());

        return requests.stream()
                .map(request -> {
                    Member requester = requesterMap.get(request.getRequesterId());
                    return new JoinRequestListItemResponse(
                            request.getId(),
                            request.getParty().getId(),
                            request.getRequesterId(),
                            requester != null ? requester.getNickname() : null,
                            requester != null ? requester.getPhotoUrl() : null,
                            request.getStatus(),
                            request.getCreatedAt()
                    );
                })
                .toList();
    }

    private Party findPartyDetailOrThrow(String partyId) {
        return partyRepository.findDetailById(partyId)
                .orElseThrow(PartyNotFoundException::new);
    }

    private JoinRequest findJoinRequestOrThrow(String requestId) {
        return joinRequestRepository.findDetailById(requestId)
                .orElseThrow(JoinRequestNotFoundException::new);
    }

    private void lockMemberOrThrow(String memberId) {
        memberRepository.findActiveByIdForUpdate(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private void withdrawLeaderFromParty(Party party) {
        PartyStatus beforeStatus = party.getStatus();
        party.withdrawLeader();
        savePartyWithLockHandling(party);
        chatService.createPartyEndMessage(party, party.getLeaderId());
        partySseService.publishPartyStatusChanged(party);
        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(party.getId(), beforeStatus, party.getStatus()));
        partyInvitationLifecycleService.expirePendingForParty(
                party.getId(),
                com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.TARGET_UNAVAILABLE
        );

        joinRequestRepository.findByParty_IdAndStatusOrderByCreatedAtDesc(party.getId(), JoinRequestStatus.PENDING)
                .forEach(request -> {
                    JoinRequestStatus previousStatus = request.getStatus();
                    request.decline();
                    joinRequestRepository.save(request);
                    joinRequestSseService.publishJoinRequestUpdated(request, previousStatus);
                });
    }

    private void requireLeader(Party party, String actorId) {
        if (!party.isLeader(actorId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }
    }

    private void requireJoinRequestLeader(JoinRequest request, String leaderId) {
        if (!request.getLeaderId().equals(leaderId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }
    }

    private void savePartyWithLockHandling(Party party) {
        try {
            partyRepository.saveAndFlush(party);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            throw new BusinessException(ErrorCode.PARTY_CONCURRENT_MODIFICATION);
        }
    }

    void acceptInvitedMemberWithLockedParty(Party party, String inviteeMemberId, String inviterMemberId) {
        if (party.getStatus() != PartyStatus.OPEN) {
            throw new BusinessException(ErrorCode.PARTY_CLOSED);
        }
        if (!party.isMember(inviterMemberId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_MEMBER);
        }
        if (party.isMember(inviteeMemberId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }
        if (partyRepository.existsActivePartyByMemberId(inviteeMemberId, ACTIVE_PARTY_STATUSES, party.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }

        PartyStatus beforeStatus = party.getStatus();
        party.addMember(inviteeMemberId);
        cancelPendingJoinRequestsForInvitee(party.getId(), inviteeMemberId);
        savePartyWithLockHandling(party);
        chatService.syncPartyChatRoomMembers(party);
        String inviteeName = resolveMembershipDisplayName(inviteeMemberId);
        chatService.createPartyMemberJoinSystemMessage(
                party,
                inviterMemberId,
                toMemberJoinSystemMessage(inviteeName)
        );
        if (beforeStatus == PartyStatus.OPEN && party.getStatus() == PartyStatus.CLOSED) {
            chatService.createPartySystemMessage(party, inviterMemberId, "모집이 마감되었어요.");
        }
        partySseService.publishPartyMemberJoined(party, inviteeMemberId, inviteeName, party.getMemberIds());
        if (beforeStatus != party.getStatus()) {
            partySseService.publishPartyStatusChanged(party);
            eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(
                    party.getId(),
                    beforeStatus,
                    party.getStatus()
            ));
            partyInvitationLifecycleService.expirePendingForParty(
                    party.getId(),
                    com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.CAPACITY_FULL
            );
        }
    }

    private void cancelPendingJoinRequestsForInvitee(String partyId, String inviteeMemberId) {
        joinRequestRepository.findPendingByPartyIdAndRequesterIdForUpdate(partyId, inviteeMemberId)
                .forEach(request -> {
                    JoinRequestStatus previousStatus = request.getStatus();
                    request.cancel();
                    joinRequestRepository.save(request);
                    joinRequestSseService.publishJoinRequestUpdated(request, previousStatus);
                });
    }

    private PartySummaryResponse toPartySummaryResponse(
            Party party,
            Map<String, Member> memberMap,
            List<String> tags
    ) {
        Member leader = memberMap.get(party.getLeaderId());
        return new PartySummaryResponse(
                party.getId(),
                party.getLeaderId(),
                leader != null ? leader.getNickname() : null,
                leader != null ? leader.getPhotoUrl() : null,
                toParticipantSummaries(party, memberMap),
                toLocationResponse(party.getDeparture()),
                toLocationResponse(party.getDestination()),
                party.getDepartureTime(),
                party.getMaxMembers(),
                party.getCurrentMembers(),
                tags,
                party.getDetail(),
                party.getStatus(),
                party.getCreatedAt()
        );
    }

    private List<PartyParticipantSummaryResponse> toParticipantSummaries(Party party, Map<String, Member> memberMap) {
        return party.getMembers().stream()
                .map(member -> {
                    Member profile = memberMap.get(member.getMemberId());
                    return new PartyParticipantSummaryResponse(
                            member.getMemberId(),
                            profile != null ? profile.getPhotoUrl() : null,
                            profile != null ? profile.getNickname() : null,
                            party.isLeader(member.getMemberId())
                    );
                })
                .toList();
    }

    private PartyDetailResponse toPartyDetailResponse(Party party, Map<String, Member> memberMap) {
        Member leader = memberMap.get(party.getLeaderId());

        List<PartyMemberResponse> members = party.getMembers().stream()
                .map(member -> {
                    Member profile = memberMap.get(member.getMemberId());
                    return new PartyMemberResponse(
                            member.getMemberId(),
                            profile != null ? profile.getNickname() : null,
                            profile != null ? profile.getPhotoUrl() : null,
                            party.isLeader(member.getMemberId()),
                            member.getJoinedAt()
                    );
                })
                .toList();

        return new PartyDetailResponse(
                party.getId(),
                party.getLeaderId(),
                leader != null ? leader.getNickname() : null,
                leader != null ? leader.getPhotoUrl() : null,
                toLocationResponse(party.getDeparture()),
                toLocationResponse(party.getDestination()),
                party.getDepartureTime(),
                party.getMaxMembers(),
                members,
                party.getTagsText(),
                party.getDetail(),
                party.getStatus(),
                toSettlementSummary(party, memberMap),
                party.getCreatedAt()
        );
    }

    private SettlementSummaryResponse toSettlementSummary(Party party, Map<String, Member> memberMap) {
        if (party.getSettlementStatus() == null) {
            return null;
        }

        List<MemberSettlementResponse> settlements = party.getSettlementItems().stream()
                .map(item -> {
                    Member profile = memberMap.get(item.getMemberId());
                    return new MemberSettlementResponse(
                            item.getMemberId(),
                            resolveSettlementDisplayName(item, profile),
                            item.isSettled(),
                            item.getSettledAt(),
                            item.isLeftParty(),
                            item.getLeftAt()
                    );
                })
                .toList();

        return new SettlementSummaryResponse(
                party.getSettlementStatus(),
                party.getTaxiFare(),
                party.getSplitMemberCount(),
                party.getPerPersonAmount(),
                party.getSettlementTargetMemberIds(),
                toSettlementAccountResponse(party.getSettlementAccount()),
                settlements
        );
    }

    private PartyStatusResponse toPartyStatusResponse(Party party) {
        return new PartyStatusResponse(party.getId(), party.getStatus(), party.getEndReason());
    }

    private JoinRequestResponse toJoinRequestResponse(JoinRequest joinRequest) {
        return new JoinRequestResponse(joinRequest.getId(), joinRequest.getStatus());
    }

    private JoinRequestAcceptResponse toJoinRequestAcceptResponse(JoinRequest joinRequest) {
        return new JoinRequestAcceptResponse(joinRequest.getId(), joinRequest.getStatus(), joinRequest.getParty().getId());
    }

    private List<Party> findMyTaxiHistoryParties(String memberId) {
        return partyRepository.findMyParties(memberId).stream()
                .filter(this::isTaxiHistoryTarget)
                .sorted(
                        Comparator.comparing(Party::getDepartureTime, Comparator.reverseOrder())
                                .thenComparing(Party::getCreatedAt, Comparator.reverseOrder())
                )
                .toList();
    }

    private boolean isTaxiHistoryTarget(Party party) {
        return party.getStatus() == PartyStatus.ARRIVED || party.getStatus() == PartyStatus.ENDED;
    }

    private TaxiHistoryItemResponse toTaxiHistoryItemResponse(Party party, String memberId) {
        return new TaxiHistoryItemResponse(
                party.getId(),
                party.getDeparture().getName(),
                party.getDestination().getName(),
                party.getDepartureTime(),
                party.getCurrentMembers(),
                calculatePaymentAmount(party),
                party.isLeader(memberId) ? TaxiHistoryRole.LEADER : TaxiHistoryRole.MEMBER,
                toTaxiHistoryStatus(party)
        );
    }

    private Integer calculatePaymentAmount(Party party) {
        if (!hasSettlementData(party)) {
            return null;
        }
        return party.getPerPersonAmount();
    }

    private int calculateSavedFareAmount(Party party) {
        if (!hasSettlementData(party)) {
            return 0;
        }
        return Math.max(party.getTaxiFare() - party.getPerPersonAmount(), 0);
    }

    private TaxiHistoryStatus toTaxiHistoryStatus(Party party) {
        if (party.getStatus() == PartyStatus.ARRIVED) {
            return TaxiHistoryStatus.COMPLETED;
        }
        if (party.getStatus() != PartyStatus.ENDED) {
            throw new IllegalArgumentException("택시 이용 내역 대상이 아닌 파티 상태입니다: " + party.getStatus());
        }

        PartyEndReason endReason = party.getEndReason();
        if (endReason == null) {
            return hasSettlementData(party) ? TaxiHistoryStatus.COMPLETED : TaxiHistoryStatus.CANCELLED;
        }

        return switch (endReason) {
            case ARRIVED, FORCE_ENDED -> TaxiHistoryStatus.COMPLETED;
            case CANCELLED, WITHDRAWED -> TaxiHistoryStatus.CANCELLED;
            case TIMEOUT -> hasSettlementData(party) ? TaxiHistoryStatus.COMPLETED : TaxiHistoryStatus.CANCELLED;
        };
    }

    private boolean hasSettlementData(Party party) {
        return party.getSettlementStatus() != null
                && party.getTaxiFare() != null
                && party.getPerPersonAmount() != null;
    }

    private Location toLocation(com.skuri.skuri_backend.domain.taxiparty.dto.request.PartyLocationRequest request) {
        return Location.of(request.name(), request.lat(), request.lng());
    }

    private PartyLocationResponse toLocationResponse(Location location) {
        return new PartyLocationResponse(location.getName(), location.getLat(), location.getLng());
    }

    private SettlementAccountSnapshot toSettlementAccountSnapshot(ArrivePartyRequest.SettlementAccountRequest request) {
        return SettlementAccountSnapshot.of(
                request.bankName().trim(),
                request.accountNumber().trim(),
                request.accountHolder().trim(),
                request.hideName()
        );
    }

    private SettlementAccountResponse toSettlementAccountResponse(SettlementAccountSnapshot settlementAccount) {
        if (settlementAccount == null) {
            return null;
        }
        return new SettlementAccountResponse(
                settlementAccount.getBankName(),
                settlementAccount.getAccountNumber(),
                settlementAccount.getDisplayAccountHolder(),
                settlementAccount.getHideName()
        );
    }

    private void leaveOpenOrClosedParty(Party party, String memberId, String leaveSystemMessage) {
        party.removeMember(memberId);
        savePartyWithLockHandling(party);
        chatService.syncPartyChatRoomMembers(party);
        chatService.createPartyMemberLeaveSystemMessage(party, memberId, leaveSystemMessage);
        partySseService.publishPartyMemberLeft(party, memberId, "LEFT", party.getMemberIds());
        partyInvitationLifecycleService.expirePendingByInviterInParty(
                party.getId(),
                memberId,
                com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.INVITER_LEFT
        );
    }

    private void leaveArrivedParty(Party party, String memberId, String leaveSystemMessage) {
        party.leaveArrivedMember(memberId);
        savePartyWithLockHandling(party);
        chatService.syncPartyChatRoomMembers(party);
        chatService.syncPartyArrivalMessageSnapshot(party);
        chatService.createPartyMemberLeaveSystemMessage(party, memberId, leaveSystemMessage);
        partySseService.publishPartyMemberLeft(party, memberId, "LEFT", party.getMemberIds());
        partyInvitationLifecycleService.expirePendingByInviterInParty(
                party.getId(),
                memberId,
                com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason.INVITER_LEFT
        );
    }

    private List<SettlementTargetSnapshot> toSettlementTargetSnapshots(
            List<String> settlementTargetMemberIds,
            Map<String, Member> memberMap
    ) {
        if (settlementTargetMemberIds == null) {
            return List.of();
        }
        return settlementTargetMemberIds.stream()
                .map(memberId -> new SettlementTargetSnapshot(
                        memberId,
                        resolveDisplayName(memberMap.get(memberId), memberId)
                ))
                .toList();
    }

    private List<String> normalizeMemberIds(List<String> memberIds) {
        if (memberIds == null) {
            return List.of();
        }
        return memberIds.stream()
                .map(this::normalizeMemberId)
                .toList();
    }

    private String resolveSettlementDisplayName(MemberSettlement settlement, Member profile) {
        if (settlement.getDisplayName() != null && !settlement.getDisplayName().isBlank()) {
            return settlement.getDisplayName();
        }
        return resolveDisplayName(profile, settlement.getMemberId());
    }

    private String resolveDisplayName(Member member, String fallback) {
        if (member != null && member.getNickname() != null && !member.getNickname().isBlank()) {
            return member.getNickname();
        }
        return fallback;
    }

    private String resolveMembershipDisplayName(String memberId) {
        return memberRepository.findById(memberId)
                .map(Member::getNickname)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private String toMemberJoinSystemMessage(String displayName) {
        if (StringUtils.hasText(displayName)) {
            return displayName + "님이 입장했어요.";
        }
        return "새 멤버가 입장했어요.";
    }

    private String toMemberLeaveSystemMessage(String displayName) {
        if (StringUtils.hasText(displayName)) {
            return displayName + "님이 나갔어요.";
        }
        return "멤버가 나갔어요.";
    }

    private String normalizeMemberId(String memberId) {
        return memberId == null ? null : memberId.trim();
    }

    private Collection<String> getVisibleMemberIds(Party party) {
        List<String> visibleMemberIds = new ArrayList<>(party.getMemberIds());
        visibleMemberIds.addAll(party.getSettlementTargetMemberIds());
        return visibleMemberIds;
    }

    private Map<String, Party> getDetailedPartyMap(Collection<Party> parties) {
        List<String> ids = parties.stream().map(Party::getId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, Party> result = new HashMap<>();
        partyRepository.findDetailsByIds(ids).forEach(party -> result.put(party.getId(), party));
        return result;
    }

    private Map<String, List<String>> getPartyTagMap(Collection<String> partyIds) {
        List<String> ids = partyIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> result = new HashMap<>();
        partyTagRepository.findTagSummariesByPartyIds(ids).forEach(tagSummary ->
                result.computeIfAbsent(tagSummary.getPartyId(), unused -> new ArrayList<>())
                        .add(tagSummary.getTag())
        );
        return result;
    }

    private Map<String, Member> getMemberMap(Collection<String> memberIds) {
        List<String> ids = memberIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, Member> result = new HashMap<>();
        memberRepository.findAllById(ids).forEach(member -> result.put(member.getId(), member));
        return result;
    }
}
