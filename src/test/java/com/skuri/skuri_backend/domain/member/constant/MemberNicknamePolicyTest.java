package com.skuri.skuri_backend.domain.member.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberNicknamePolicyTest {

    @Test
    void 저장정규화는_앞뒤공백을제거하고_NFC로변환한다() {
        assertThat(MemberNicknamePolicy.normalizeForStorage("  e\u0301  ")).isEqualTo("é");
    }

    @Test
    void 고유키는_대소문자를구분하지않는다() {
        assertThat(MemberNicknamePolicy.toUniquenessKey("  SKuri  ")).isEqualTo("skuri");
    }

    @Test
    void 예약어는_중간유니코드공백과상관없이차단한다() {
        assertThat(MemberNicknamePolicy.isReserved("우리 스쿠리\u00a0유저 모임")).isTrue();
        assertThat(MemberNicknamePolicy.isReserved("학교 운 영 자 계정")).isTrue();
    }

    @Test
    void 예약어가없는닉네임은허용한다() {
        assertThat(MemberNicknamePolicy.isReserved("스쿠리친구")).isFalse();
    }
}
