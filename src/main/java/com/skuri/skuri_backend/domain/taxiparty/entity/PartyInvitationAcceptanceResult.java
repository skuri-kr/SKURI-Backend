package com.skuri.skuri_backend.domain.taxiparty.entity;

/**
 * 택시파티 초대 수락이 최초로 확정됐을 때의 결과입니다.
 *
 * <p>초대 수락 재시도는 현재 파티나 동승 요청 상태를 재해석하지 않고 이 값을 그대로 반환합니다.</p>
 */
public enum PartyInvitationAcceptanceResult {
    JOINED,
    LEADER_APPROVAL_PENDING
}
