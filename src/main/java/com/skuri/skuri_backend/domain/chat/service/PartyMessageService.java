package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.util.AccountHolderMasker;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.SendChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.entity.ChatAccountData;
import com.skuri.skuri_backend.domain.chat.entity.ChatArrivalData;
import com.skuri.skuri_backend.domain.chat.entity.ChatArrivalSettlementMemberData;
import com.skuri.skuri_backend.domain.member.entity.BankAccount;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.MemberSettlement;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementAccountSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementStatus;
import com.skuri.skuri_backend.domain.taxiparty.exception.PartyNotFoundException;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PartyMessageService {

    private static final String PARTY_CHAT_ROOM_PREFIX = "party:";

    private final PartyRepository partyRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public PartySpecialMessagePayload buildClientPayload(
            String chatRoomId,
            String senderId,
            SendChatMessageRequest request
    ) {
        requirePartyMember(chatRoomId, senderId);

        return switch (request.type()) {
            case ACCOUNT -> buildAccountPayload(senderId, request.account());
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "파티 특수 메시지 타입이 아닙니다.");
        };
    }

    @Transactional(readOnly = true)
    public PartySpecialMessagePayload buildArrivalPayload(Party party, String senderId) {
        if (!party.isLeader(senderId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }
        if (party.getStatus() != PartyStatus.ARRIVED) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "ARRIVED 상태에서만 도착 메시지를 전송할 수 있습니다.");
        }

        SettlementAccountSnapshot settlementAccount = party.getSettlementAccount();
        if (settlementAccount == null || !settlementAccount.isComplete()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "정산 계좌 정보를 모두 입력해야 합니다.");
        }

        ChatArrivalData arrivalData = buildArrivalData(party);

        Integer taxiFare = party.getTaxiFare();
        Integer perPersonAmount = party.getPerPersonAmount();
        Integer splitMemberCount = party.getSplitMemberCount();
        String text = (taxiFare == null || perPersonAmount == null || splitMemberCount == null)
                ? "택시가 목적지에 도착했어요."
                : "택시가 목적지에 도착했어요. 총 "
                + taxiFare + "원, "
                + splitMemberCount + "명 정산, 1인당 "
                + perPersonAmount + "원입니다.";
        return new PartySpecialMessagePayload(text, null, arrivalData);
    }

    @Transactional(readOnly = true)
    ChatArrivalData buildArrivalData(Party party) {
        SettlementAccountSnapshot settlementAccount = party.getSettlementAccount();
        if (settlementAccount == null || !settlementAccount.isComplete()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "정산 계좌 정보를 모두 입력해야 합니다.");
        }

        ChatAccountData accountData = toChatAccountData(
                settlementAccount.getBankName(),
                settlementAccount.getAccountNumber(),
                settlementAccount.getAccountHolder(),
                settlementAccount.getHideName()
        );

        return new ChatArrivalData(
                party.getTaxiFare(),
                party.getPerPersonAmount(),
                party.getSplitMemberCount(),
                party.getSettlementTargetMemberIds(),
                party.getSettlementItems().stream()
                        .map(this::toArrivalSettlementMemberData)
                        .toList(),
                accountData
        );
    }

    @Transactional(readOnly = true)
    public PartySpecialMessagePayload buildEndPayload(Party party, String senderId) {
        if (!party.isLeader(senderId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_LEADER);
        }
        if (party.getStatus() != PartyStatus.ENDED) {
            throw new BusinessException(ErrorCode.INVALID_PARTY_STATE_TRANSITION, "ENDED 상태에서만 종료 메시지를 전송할 수 있습니다.");
        }

        String text = switch (party.getEndReason()) {
            case CANCELLED -> "리더가 파티를 취소했어요.";
            case FORCE_ENDED -> party.getSettlementStatus() == SettlementStatus.COMPLETED
                    ? "모든 정산 확인 후 파티를 종료했어요."
                    : "리더가 파티를 종료했어요.";
            case TIMEOUT -> "파티가 자동 종료되었어요.";
            case WITHDRAWED -> "리더 탈퇴로 파티가 종료되었어요.";
            case ARRIVED -> "파티가 종료되었어요.";
            case null -> "파티가 종료되었어요.";
        };
        return new PartySpecialMessagePayload(text, null, null);
    }

    private PartySpecialMessagePayload buildAccountPayload(String senderId, SendChatMessageRequest.AccountPayload accountRequest) {
        if (accountRequest == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ACCOUNT 메시지에는 account payload가 필요합니다.");
        }

        String bankName = accountRequest.bankName().trim();
        String accountNumber = accountRequest.accountNumber().trim();
        String accountHolder = accountRequest.accountHolder().trim();
        boolean hideName = Boolean.TRUE.equals(accountRequest.hideName());

        Member member = memberRepository.findActiveByIdForUpdate(senderId).orElseThrow(MemberNotFoundException::new);
        if (Boolean.TRUE.equals(accountRequest.remember())) {
            member.updateBankAccount(BankAccount.of(bankName, accountNumber, accountHolder, hideName));
        }

        ChatAccountData accountData = toChatAccountData(bankName, accountNumber, accountHolder, hideName);
        String text = String.format("계좌 정보를 공유했어요. (%s %s)", bankName, accountNumber);
        return new PartySpecialMessagePayload(text, accountData, null);
    }

    private ChatAccountData toChatAccountData(
            String bankName,
            String accountNumber,
            String accountHolder,
            Boolean hideName
    ) {
        return new ChatAccountData(
                bankName,
                accountNumber,
                AccountHolderMasker.mask(accountHolder, hideName),
                hideName
        );
    }

    private ChatArrivalSettlementMemberData toArrivalSettlementMemberData(MemberSettlement settlement) {
        return new ChatArrivalSettlementMemberData(
                settlement.getMemberId(),
                settlement.getDisplayName(),
                settlement.isSettled(),
                settlement.getSettledAt(),
                settlement.isLeftParty(),
                settlement.getLeftAt()
        );
    }

    private Party requirePartyMember(String chatRoomId, String senderId) {
        String partyId = extractPartyId(chatRoomId);
        Party party = partyRepository.findDetailById(partyId).orElseThrow(PartyNotFoundException::new);
        if (!party.isMember(senderId)) {
            throw new BusinessException(ErrorCode.NOT_PARTY_MEMBER);
        }
        return party;
    }

    private String extractPartyId(String chatRoomId) {
        if (!StringUtils.hasText(chatRoomId) || !chatRoomId.startsWith(PARTY_CHAT_ROOM_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파티 채팅방 ID 형식이 아닙니다.");
        }
        return chatRoomId.substring(PARTY_CHAT_ROOM_PREFIX.length());
    }
}
