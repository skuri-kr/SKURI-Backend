package com.skuri.skuri_backend.domain.taxiparty.service;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.taxiparty.constant.AdminPartyStatusAction;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyDetailResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyLeaderResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartyJoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.AdminPartySummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.MemberSettlementResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyLocationResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyMemberResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.PartyStatusResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementAccountResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.SettlementSummaryResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.MemberSettlement;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyInvitationExpiryReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import com.skuri.skuri_backend.infra.admin.list.AdminPageRequestPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaxiPartyAdminService {

    private static final Sort ADMIN_PARTY_DEFAULT_SORT = Sort.by(
            Sort.Order.desc("departureTime"),
            Sort.Order.desc("createdAt")
    );

    private final PartyRepository partyRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatService chatService;
    private final PartyInvitationLifecycleService partyInvitationLifecycleService;
    private final PartySseService partySseService;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PageResponse<AdminPartySummaryResponse> getAdminParties(
            PartyStatus status,
            LocalDate departureDate,
            String query,
            int page,
            int size
    ) {
        Pageable pageable = resolvePageable(page, size);
        Page<Party> partyPage = partyRepository.searchAdminParties(
                status,
                departureDate,
                normalizeQuery(query),
                pageable
        );

        Map<String, Party> detailedPartyMap = getDetailedPartyMap(partyPage.getContent());
        Map<String, Member> leaderMap = getMemberMap(
                detailedPartyMap.values().stream()
                        .map(Party::getLeaderId)
                        .distinct()
                        .toList()
        );

        return PageResponse.from(partyPage.map(party -> toAdminPartySummaryResponse(
                detailedPartyMap.getOrDefault(party.getId(), party),
                leaderMap
        )));
    }

    @Transactional(readOnly = true)
    public AdminPartyDetailResponse getAdminParty(String partyId) {
        Party party = partyRepository.findDetailById(partyId)
                .orElseThrow(PartyNotFoundException::new);
        Map<String, Member> memberMap = getMemberMap(party.getMemberIds());
        long pendingJoinRequestCount = joinRequestRepository.countByParty_IdAndStatus(partyId, JoinRequestStatus.PENDING);
        return toAdminPartyDetailResponse(party, memberMap, pendingJoinRequestCount);
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getAdminPartyMessages(
            String partyId,
            LocalDateTime cursorCreatedAt,
            String cursorId,
            Integer size
    ) {
        findPartyDetailOrThrow(partyId);
        return chatService.getAdminPartyChatMessages("party:" + partyId, cursorCreatedAt, cursorId, size);
    }

    @Transactional
    public PartyStatusResponse updatePartyStatus(String partyId, AdminPartyStatusAction action) {
        Party party = partyRepository.findDetailByIdForUpdate(partyId)
                .orElseThrow(PartyNotFoundException::new);
        PartyStatus beforeStatus = party.getStatus();
        String partyChatActorId = party.getLeaderId();

        switch (action) {
            case CLOSE -> {
                party.close();
                savePartyWithLockHandling(party);
                chatService.createPartySystemMessage(party, partyChatActorId, "모집이 마감되었어요.");
                partySseService.publishPartyStatusChanged(party);
            }
            case REOPEN -> {
                party.reopen();
                savePartyWithLockHandling(party);
                chatService.createPartySystemMessage(party, partyChatActorId, "모집이 재개되었어요.");
                partySseService.publishPartyStatusChanged(party);
            }
            case CANCEL -> {
                party.cancel();
                savePartyWithLockHandling(party);
                chatService.createPartyEndMessage(party, partyChatActorId);
                partySseService.publishPartyDeleted(party.getId());
            }
            case END -> {
                party.forceEnd();
                savePartyWithLockHandling(party);
                chatService.createPartyEndMessage(party, partyChatActorId);
                partySseService.publishPartyStatusChanged(party);
            }
        }

        if (action == AdminPartyStatusAction.CANCEL || action == AdminPartyStatusAction.END) {
            partyInvitationLifecycleService.expirePendingForParty(
                    party.getId(),
                    PartyInvitationExpiryReason.TARGET_UNAVAILABLE
            );
        }

        eventPublisher.publish(new NotificationDomainEvent.PartyStatusChanged(
                party.getId(),
                beforeStatus,
                party.getStatus()
        ));
        return new PartyStatusResponse(party.getId(), party.getStatus(), party.getEndReason());
    }

    @Transactional
    public void removePartyMember(String adminActorId, String partyId, String memberId) {
        Party party = partyRepository.findDetailByIdForUpdate(partyId)
                .orElseThrow(PartyNotFoundException::new);
        Member member = memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);

        if (party.getStatus() == PartyStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_IN_ARRIVED);
        }
        if (party.getStatus() == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }
        if (party.isLeader(memberId)) {
            throw new BusinessException(ErrorCode.PARTY_LEADER_REMOVAL_NOT_ALLOWED);
        }
        if (!party.isMember(memberId)) {
            throw new BusinessException(ErrorCode.PARTY_MEMBER_NOT_FOUND);
        }

        List<String> recipientsBeforeRemoval = party.getMemberIds();
        String removedMemberName = resolveDisplayName(member);
        party.removeMember(memberId);
        savePartyWithLockHandling(party);
        partyInvitationLifecycleService.expirePendingByInviterInParty(
                party.getId(),
                memberId,
                PartyInvitationExpiryReason.INVITER_LEFT
        );
        chatService.syncPartyChatRoomMembers(party);
        chatService.createPartyMemberLeaveSystemMessage(
                party,
                adminActorId,
                toMemberLeaveSystemMessage(removedMemberName)
        );
        partySseService.publishPartyMemberLeft(party, memberId, "KICKED", recipientsBeforeRemoval);
        eventPublisher.publish(new NotificationDomainEvent.PartyMemberKicked(party.getId(), memberId));
    }

    @Transactional
    public ChatMessageResponse createPartySystemMessage(String adminActorId, String partyId, String message) {
        Party party = findPartyDetailOrThrow(partyId);
        return chatService.createPartyAdminSystemMessage(party, adminActorId, message);
    }

    @Transactional(readOnly = true)
    public List<AdminPartyJoinRequestResponse> getPartyJoinRequests(String partyId) {
        findPartyDetailOrThrow(partyId);
        List<JoinRequest> joinRequests = joinRequestRepository.findByParty_IdAndStatusOrderByCreatedAtDesc(
                partyId,
                JoinRequestStatus.PENDING
        );
        Map<String, Member> requesterMap = getMemberMap(joinRequests.stream()
                .map(JoinRequest::getRequesterId)
                .toList());

        return joinRequests.stream()
                .map(joinRequest -> toAdminPartyJoinRequestResponse(
                        joinRequest,
                        requesterMap.get(joinRequest.getRequesterId())
                ))
                .toList();
    }

    private Pageable resolvePageable(int page, int size) {
        Pageable validated = AdminPageRequestPolicy.of(page, size);
        return PageRequest.of(validated.getPageNumber(), validated.getPageSize(), ADMIN_PARTY_DEFAULT_SORT);
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim();
    }

    private Map<String, Party> getDetailedPartyMap(List<Party> parties) {
        if (parties.isEmpty()) {
            return Map.of();
        }
        return partyRepository.findDetailsByIds(
                        parties.stream()
                                .map(Party::getId)
                                .toList()
                ).stream()
                .collect(LinkedHashMap::new, (map, party) -> map.put(party.getId(), party), Map::putAll);
    }

    private Map<String, Member> getMemberMap(Collection<String> memberIds) {
        List<String> normalizedMemberIds = memberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedMemberIds.isEmpty()) {
            return Map.of();
        }
        return memberRepository.findAllById(normalizedMemberIds).stream()
                .collect(LinkedHashMap::new, (map, member) -> map.put(member.getId(), member), Map::putAll);
    }

    private Party findPartyDetailOrThrow(String partyId) {
        return partyRepository.findDetailById(partyId)
                .orElseThrow(PartyNotFoundException::new);
    }

    private AdminPartySummaryResponse toAdminPartySummaryResponse(Party party, Map<String, Member> leaderMap) {
        Member leader = leaderMap.get(party.getLeaderId());
        return new AdminPartySummaryResponse(
                party.getId(),
                party.getStatus(),
                party.getLeaderId(),
                leader != null ? leader.getNickname() : null,
                routeSummary(party),
                party.getDepartureTime(),
                party.getCurrentMembers(),
                party.getMaxMembers(),
                party.getCreatedAt()
        );
    }

    private AdminPartyJoinRequestResponse toAdminPartyJoinRequestResponse(JoinRequest joinRequest, Member requester) {
        return new AdminPartyJoinRequestResponse(
                joinRequest.getId(),
                joinRequest.getRequesterId(),
                requester != null ? requester.getNickname() : null,
                requester != null ? requester.getRealname() : null,
                requester != null ? requester.getPhotoUrl() : null,
                requester != null ? requester.getDepartment() : null,
                requester != null ? requester.getStudentId() : null,
                joinRequest.getCreatedAt()
        );
    }

    private AdminPartyDetailResponse toAdminPartyDetailResponse(
            Party party,
            Map<String, Member> memberMap,
            long pendingJoinRequestCount
    ) {
        Member leaderProfile = memberMap.get(party.getLeaderId());
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

        return new AdminPartyDetailResponse(
                party.getId(),
                party.getStatus(),
                party.getEndReason(),
                party.getLeaderId(),
                leaderProfile != null ? leaderProfile.getNickname() : null,
                leaderProfile == null
                        ? null
                        : new AdminPartyLeaderResponse(
                                leaderProfile.getId(),
                                leaderProfile.getNickname(),
                                leaderProfile.getPhotoUrl()
                        ),
                routeSummary(party),
                toLocationResponse(party.getDeparture()),
                toLocationResponse(party.getDestination()),
                party.getDepartureTime(),
                party.getCurrentMembers(),
                party.getMaxMembers(),
                members,
                party.getTagsText(),
                party.getDetail(),
                pendingJoinRequestCount,
                party.getSettlementStatus(),
                toSettlementSummary(party, memberMap),
                resolveChatRoomId(party.getId()),
                party.getCreatedAt(),
                party.getUpdatedAt(),
                party.getEndedAt()
        );
    }

    private String resolveChatRoomId(String partyId) {
        String chatRoomId = "party:" + partyId;
        return chatRoomRepository.existsById(chatRoomId) ? chatRoomId : null;
    }

    private String routeSummary(Party party) {
        return party.getDeparture().getName() + " -> " + party.getDestination().getName();
    }

    private String resolveDisplayName(Member member) {
        if (member != null && StringUtils.hasText(member.getNickname())) {
            return member.getNickname();
        }
        return null;
    }

    private String toMemberLeaveSystemMessage(String displayName) {
        if (StringUtils.hasText(displayName)) {
            return displayName + "님이 나갔어요.";
        }
        return "멤버가 나갔어요.";
    }

    private PartyLocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }
        return new PartyLocationResponse(location.getName(), location.getLat(), location.getLng());
    }

    private SettlementSummaryResponse toSettlementSummary(Party party, Map<String, Member> memberMap) {
        if (party.getSettlementStatus() == null) {
            return null;
        }

        return new SettlementSummaryResponse(
                party.getSettlementStatus(),
                party.getTaxiFare(),
                party.getSplitMemberCount(),
                party.getPerPersonAmount(),
                party.getSettlementTargetMemberIds(),
                toSettlementAccountResponse(party),
                party.getSettlementItems().stream()
                        .map(item -> toMemberSettlementResponse(item, memberMap))
                        .toList()
        );
    }

    private MemberSettlementResponse toMemberSettlementResponse(MemberSettlement settlement, Map<String, Member> memberMap) {
        Member member = memberMap.get(settlement.getMemberId());
        String displayName = settlement.getDisplayName();
        if (!StringUtils.hasText(displayName) && member != null) {
            displayName = member.getNickname();
        }
        return new MemberSettlementResponse(
                settlement.getMemberId(),
                displayName,
                settlement.isSettled(),
                settlement.getSettledAt(),
                settlement.isLeftParty(),
                settlement.getLeftAt()
        );
    }

    private SettlementAccountResponse toSettlementAccountResponse(Party party) {
        if (party.getSettlementAccount() == null) {
            return null;
        }
        return new SettlementAccountResponse(
                party.getSettlementAccount().getBankName(),
                party.getSettlementAccount().getAccountNumber(),
                party.getSettlementAccount().getAccountHolder(),
                party.getSettlementAccount().getHideName()
        );
    }

    private void savePartyWithLockHandling(Party party) {
        try {
            partyRepository.saveAndFlush(party);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ErrorCode.PARTY_CONCURRENT_MODIFICATION);
        }
    }
}
