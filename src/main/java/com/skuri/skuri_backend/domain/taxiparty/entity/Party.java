package com.skuri.skuri_backend.domain.taxiparty.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Entity
@Table(name = "parties")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Party extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "leader_id", nullable = false, length = 36)
    private String leaderId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "departure_name", nullable = false, length = 100)),
            @AttributeOverride(name = "lat", column = @Column(name = "departure_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "departure_lng"))
    })
    private Location departure;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "destination_name", nullable = false, length = 100)),
            @AttributeOverride(name = "lat", column = @Column(name = "destination_lat")),
            @AttributeOverride(name = "lng", column = @Column(name = "destination_lng"))
    })
    private Location destination;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @Column(name = "current_members", nullable = false)
    private int currentMembers;

    @Column(name = "detail", length = 500)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 20)
    private PartyEndReason endReason;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", length = 20)
    private SettlementStatus settlementStatus;

    @Column(name = "per_person_amount")
    private Integer perPersonAmount;

    @Column(name = "taxi_fare")
    private Integer taxiFare;

    @Embedded
    private SettlementAccountSnapshot settlementAccount;

    @Version
    private Long version;

    @OrderBy("joinedAt ASC")
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PartyMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PartyTag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MemberSettlement> memberSettlements = new ArrayList<>();

    private Party(
            String leaderId,
            Location departure,
            Location destination,
            LocalDateTime departureTime,
            int maxMembers,
            String detail
    ) {
        this.leaderId = leaderId;
        this.departure = departure;
        this.destination = destination;
        this.departureTime = departureTime;
        this.maxMembers = maxMembers;
        this.detail = detail;
        this.status = PartyStatus.OPEN;
        this.currentMembers = 0;
    }

    public static Party create(
            String leaderId,
            Location departure,
            Location destination,
            LocalDateTime departureTime,
            int maxMembers,
            List<String> tags,
            String detail
    ) {
        Party party = new Party(leaderId, departure, destination, departureTime, maxMembers, detail);
        party.addMemberInternal(leaderId);
        if (tags != null) {
            tags.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .forEach(value -> party.tags.add(PartyTag.of(party, value)));
        }
        return party;
    }

    public void close() {
        if (this.status != PartyStatus.OPEN) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "OPEN 상태에서만 모집 마감할 수 있습니다.");
        }
        this.status = PartyStatus.CLOSED;
    }

    public void reopen() {
        if (this.status != PartyStatus.CLOSED) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "CLOSED 상태에서만 모집 재개할 수 있습니다.");
        }
        this.status = PartyStatus.OPEN;
    }

    public void arrive(int taxiFare, List<String> settlementTargetMemberIds, SettlementAccountSnapshot settlementAccount) {
        List<SettlementTargetSnapshot> settlementTargets = settlementTargetMemberIds == null
                ? null
                : settlementTargetMemberIds.stream()
                .map(memberId -> {
                    String normalizedMemberId = normalizeSettlementMemberId(memberId);
                    return new SettlementTargetSnapshot(normalizedMemberId, normalizedMemberId);
                })
                .toList();
        arriveWithSnapshots(taxiFare, settlementTargets, settlementAccount);
    }

    public void arriveWithSnapshots(
            int taxiFare,
            List<SettlementTargetSnapshot> settlementTargetSnapshots,
            SettlementAccountSnapshot settlementAccount
    ) {
        if (this.status != PartyStatus.OPEN && this.status != PartyStatus.CLOSED) {
            throw new BusinessException(ErrorCode.PARTY_NOT_ARRIVABLE);
        }
        if (taxiFare < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "taxiFare는 1원 이상이어야 합니다.");
        }

        if (settlementAccount == null || !settlementAccount.isComplete()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "정산 계좌 정보를 모두 입력해야 합니다.");
        }

        List<SettlementTargetSnapshot> settlementTargets = normalizeSettlementTargets(settlementTargetSnapshots);
        List<String> settlementTargetMemberIds = settlementTargets.stream()
                .map(SettlementTargetSnapshot::memberId)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .toList();
        if (settlementTargetMemberIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MEMBERS_TO_SETTLE);
        }

        Set<String> expectedTargets = getNonLeaderMemberIds().stream().collect(Collectors.toSet());
        Set<String> normalizedTargets = settlementTargetMemberIds.stream().collect(Collectors.toSet());
        if (normalizedTargets.size() != settlementTargetMemberIds.size()
                || !expectedTargets.containsAll(normalizedTargets)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "settlementTargetMemberIds에는 현재 파티의 non-leader 멤버만 포함해야 합니다."
            );
        }

        this.status = PartyStatus.ARRIVED;
        this.settlementStatus = SettlementStatus.PENDING;
        this.taxiFare = taxiFare;
        this.settlementAccount = settlementAccount;
        this.perPersonAmount = taxiFare / (settlementTargetMemberIds.size() + 1);
        this.memberSettlements.clear();
        settlementTargets.forEach(target -> this.memberSettlements.add(
                MemberSettlement.create(this, target.memberId(), target.displayName())
        ));
    }

    private List<SettlementTargetSnapshot> normalizeSettlementTargets(
            List<SettlementTargetSnapshot> settlementTargetSnapshots
    ) {
        if (settlementTargetSnapshots == null) {
            return List.of();
        }
        return settlementTargetSnapshots.stream()
                .filter(Objects::nonNull)
                .map(target -> {
                    String normalizedMemberId = normalizeSettlementMemberId(target.memberId());
                    String displayName = target.displayName();
                    if (displayName == null || displayName.isBlank()) {
                        displayName = normalizedMemberId;
                    }
                    return new SettlementTargetSnapshot(normalizedMemberId, displayName);
                })
                .toList();
    }

    private String normalizeSettlementMemberId(String memberId) {
        return memberId == null ? null : memberId.trim();
    }

    public boolean confirmSettlement(String memberId) {
        if (this.status != PartyStatus.ARRIVED || this.settlementStatus == null) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "ARRIVED 상태에서만 정산 확인할 수 있습니다.");
        }

        MemberSettlement settlement = this.memberSettlements.stream()
                .filter(item -> item.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PARTY_MEMBER, "정산 대상 멤버가 아닙니다."));

        settlement.markSettled();

        boolean allSettled = this.memberSettlements.stream().allMatch(MemberSettlement::isSettled);
        if (allSettled) {
            this.settlementStatus = SettlementStatus.COMPLETED;
        }
        return allSettled;
    }

    public void forceEnd() {
        if (this.status != PartyStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "ARRIVED 상태에서만 강제 종료할 수 있습니다.");
        }
        end(PartyEndReason.FORCE_ENDED);
    }

    public void cancel() {
        if (this.status == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }
        if (this.status != PartyStatus.OPEN && this.status != PartyStatus.CLOSED) {
            throw new BusinessException(ErrorCode.PARTY_NOT_CANCELABLE);
        }
        end(PartyEndReason.CANCELLED);
    }

    public void timeoutEnd() {
        if (this.status == PartyStatus.ENDED) {
            return;
        }
        end(PartyEndReason.TIMEOUT);
    }

    public void withdrawLeader() {
        if (this.status == PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.PARTY_ENDED);
        }
        end(PartyEndReason.WITHDRAWED);
    }

    public void updateDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public void updateDetail(String detail) {
        this.detail = detail;
    }

    public void addMember(String memberId) {
        if (this.status != PartyStatus.OPEN) {
            throw new BusinessException(ErrorCode.PARTY_CLOSED);
        }
        addMemberInternalWithCapacityCheck(memberId);
    }

    public void addInvitedMember(String memberId) {
        if (this.status != PartyStatus.OPEN && this.status != PartyStatus.CLOSED) {
            throw new BusinessException(this.status == PartyStatus.ENDED ? ErrorCode.PARTY_ENDED : ErrorCode.PARTY_CLOSED);
        }
        addMemberInternalWithCapacityCheck(memberId);
    }

    private void addMemberInternalWithCapacityCheck(String memberId) {
        if (isMember(memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_PARTY);
        }
        if (this.currentMembers >= this.maxMembers) {
            throw new BusinessException(ErrorCode.PARTY_FULL);
        }

        addMemberInternal(memberId);
    }

    public void removeMember(String memberId) {
        removePartyMember(memberId);
        this.memberSettlements.removeIf(item -> item.getMemberId().equals(memberId));
    }

    public void leaveArrivedMember(String memberId) {
        removePartyMember(memberId);
        findSettlement(memberId).ifPresent(MemberSettlement::markLeftParty);
    }

    public boolean isLeader(String memberId) {
        return this.leaderId.equals(memberId);
    }

    public boolean isMember(String memberId) {
        return this.members.stream().anyMatch(member -> member.getMemberId().equals(memberId));
    }

    public List<String> getMemberIds() {
        return this.members.stream()
                .map(PartyMember::getMemberId)
                .toList();
    }

    public List<String> getNonLeaderMemberIds() {
        return this.members.stream()
                .map(PartyMember::getMemberId)
                .filter(memberId -> !this.leaderId.equals(memberId))
                .toList();
    }

    public List<String> getTagsText() {
        return this.tags.stream().map(PartyTag::getTag).toList();
    }

    public Collection<MemberSettlement> getSettlementItems() {
        return this.memberSettlements;
    }

    public List<String> getSettlementTargetMemberIds() {
        return this.memberSettlements.stream()
                .map(MemberSettlement::getMemberId)
                .toList();
    }

    public int getSplitMemberCount() {
        if (this.settlementStatus == null) {
            return 0;
        }
        return this.memberSettlements.size() + 1;
    }

    private void addMemberInternal(String memberId) {
        this.members.add(PartyMember.create(this, memberId, LocalDateTime.now()));
        this.currentMembers++;
    }

    private void removePartyMember(String memberId) {
        PartyMember member = this.members.stream()
                .filter(item -> item.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_PARTY_MEMBER));

        this.members.remove(member);
        this.currentMembers = Math.max(0, this.currentMembers - 1);
    }

    private Optional<MemberSettlement> findSettlement(String memberId) {
        return this.memberSettlements.stream()
                .filter(item -> item.getMemberId().equals(memberId))
                .findFirst();
    }

    private void end(PartyEndReason reason) {
        this.status = PartyStatus.ENDED;
        this.endReason = reason;
        this.endedAt = LocalDateTime.now();
    }
}
