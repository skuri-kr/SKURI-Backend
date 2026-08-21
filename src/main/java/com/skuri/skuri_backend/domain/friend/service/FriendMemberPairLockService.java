package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FriendMemberPairLockService {

    private final MemberRepository memberRepository;

    /**
     * 대상 공개 ID를 해석하기 전 호출자 자격만 먼저 확인한다.
     *
     * <p>여기서 잠금을 획득하면 이후 ordered pair lock과 잠금 순서가 역전될 수 있으므로,
     * 실제 write 직전 {@link #lockActivePair(String, String)}에서 다시 잠금·검증한다.</p>
     */
    public void requireActiveProfileCompleteMember(String memberId) {
        Member member = memberRepository.findActiveById(memberId).orElseThrow(MemberNotFoundException::new);
        requireProfileComplete(member);
    }

    public void lockActiveMember(String memberId) {
        Member member = memberRepository.findActiveByIdForUpdate(memberId).orElseThrow(MemberNotFoundException::new);
        requireProfileComplete(member);
    }

    public FriendMemberPair lockActivePair(String firstMemberId, String secondMemberId) {
        if (firstMemberId.equals(secondMemberId)) {
            lockActiveMember(firstMemberId);
            return FriendMemberPair.of(firstMemberId, secondMemberId);
        }

        List<Member> members = memberRepository.findAllActiveByIdInForUpdateOrdered(Set.of(firstMemberId, secondMemberId));
        Member firstMember = members.stream()
                .filter(member -> member.getId().equals(firstMemberId))
                .findFirst()
                .orElse(null);
        if (firstMember == null) {
            throw new MemberNotFoundException();
        }
        requireProfileComplete(firstMember);
        Member secondMember = members.stream()
                .filter(member -> member.getId().equals(secondMemberId))
                .findFirst()
                .orElse(null);
        if (secondMember == null || !secondMember.isProfileComplete()) {
            throw new BusinessException(ErrorCode.FRIEND_TARGET_NOT_FOUND);
        }

        return FriendMemberPair.of(firstMemberId, secondMemberId);
    }

    private void requireProfileComplete(Member member) {
        if (!member.isProfileComplete()) {
            throw new BusinessException(ErrorCode.MEMBER_PROFILE_INCOMPLETE);
        }
    }

    /**
     * 만료 요청의 terminal 정리용 잠금이다. 회원 탈퇴 후에도 이미 생성된 요청은
     * EXPIRED로 전이할 수 있어야 하므로 ACTIVE 상태를 요구하지 않는다.
     */
    public void lockExistingPairForExpiry(String firstMemberId, String secondMemberId) {
        if (firstMemberId.equals(secondMemberId)) {
            memberRepository.findByIdForUpdate(firstMemberId);
            return;
        }
        memberRepository.findAllByIdInForUpdateOrdered(Set.of(firstMemberId, secondMemberId));
    }
}
