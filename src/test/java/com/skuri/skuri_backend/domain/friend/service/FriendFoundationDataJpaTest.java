package com.skuri.skuri_backend.domain.friend.service;

import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodePreviewResponse;
import com.skuri.skuri_backend.domain.friend.dto.response.FriendCodeResponse;
import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.domain.friend.entity.FriendCodeStatus;
import com.skuri.skuri_backend.domain.friend.exception.FriendCodeNotFoundException;
import com.skuri.skuri_backend.domain.friend.exception.FriendCodeRegenerationCooldownException;
import com.skuri.skuri_backend.domain.friend.repository.FriendCodeRegistryRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendPreferenceRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendRequestRepository;
import com.skuri.skuri_backend.domain.friend.repository.FriendshipRepository;
import com.skuri.skuri_backend.domain.friend.repository.MemberBlockRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaAuditingConfig.class,
        FriendCodeGenerator.class,
        FriendProfileProvisioningAttemptService.class,
        FriendProfileProvisioningService.class,
        FriendCodeRegenerationAttemptService.class,
        FriendRelationshipService.class,
        FriendRelationshipQueryService.class,
        FriendCodeService.class,
        FriendPrivacyService.class
})
class FriendFoundationDataJpaTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendProfileRepository friendProfileRepository;

    @Autowired
    private FriendCodeRegistryRepository friendCodeRegistryRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendPreferenceRepository friendPreferenceRepository;

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private FriendProfileProvisioningService provisioningService;

    @Autowired
    private FriendCodeService friendCodeService;

    @Autowired
    private FriendPrivacyService friendPrivacyService;

    @Autowired
    private FriendRelationshipService friendRelationshipService;

    @AfterEach
    void tearDown() {
        memberBlockRepository.deleteAll();
        friendPreferenceRepository.deleteAll();
        friendshipRepository.deleteAll();
        friendRequestRepository.deleteAll();
        friendProfileRepository.deleteAll();
        friendCodeRegistryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void provisioning은_멱등적이며_활성코드를_한개만_발급한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");

        provisioningService.ensureForActiveMember("member-1");
        provisioningService.ensureForActiveMember("member-1");

        assertThat(friendProfileRepository.count()).isEqualTo(1);
        assertThat(friendCodeRegistryRepository.count()).isEqualTo(1);
        assertThat(friendCodeRegistryRepository.findAll().getFirst().getStatus()).isEqualTo(FriendCodeStatus.ACTIVE);
    }

    @Test
    void 재발급은_기존코드를_영구폐기하고_24시간_제한을_적용한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        FriendCodeResponse original = friendCodeService.getMyCode("member-1");

        FriendCodeResponse regenerated = friendCodeService.regenerateMyCode("member-1");

        assertThat(regenerated.friendCode()).isNotEqualTo(original.friendCode());
        assertThat(friendCodeRegistryRepository.findAll())
                .extracting(registry -> registry.getStatus())
                .containsExactlyInAnyOrder(FriendCodeStatus.ACTIVE, FriendCodeStatus.RETIRED);
        assertThatThrownBy(() -> friendCodeService.regenerateMyCode("member-1"))
                .isInstanceOf(FriendCodeRegenerationCooldownException.class);
    }

    @Test
    void 폐기된코드는_preview에서_일반적인_대상없음으로_처리한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        saveMember("member-2", "two@sungkyul.ac.kr", "회원2");
        String retiredCode = friendCodeService.getMyCode("member-1").friendCode();
        friendCodeService.regenerateMyCode("member-1");

        assertThatThrownBy(() -> friendCodeService.preview("member-2", retiredCode))
                .isInstanceOf(FriendCodeNotFoundException.class);
    }

    @Test
    void preview는_내부회원ID대신_friendPublicId와_허용된프로필만_반환한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        saveMember("member-2", "two@sungkyul.ac.kr", "회원2");
        String targetCode = friendCodeService.getMyCode("member-1").friendCode();

        FriendCodePreviewResponse response = friendCodeService.preview("member-2", targetCode.toLowerCase());

        assertThat(response.friendPublicId()).isNotEqualTo("member-1");
        assertThat(response.nickname()).isEqualTo("스쿠리 유저");
        assertThat(response.canSendFriendRequest()).isTrue();
    }

    @Test
    void privacy_변경은_서버에_저장된_최종값을_반환한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");

        assertThat(friendPrivacyService.getMyPrivacy("member-1").nicknameSearchable()).isFalse();
        assertThat(friendPrivacyService.updateMyPrivacy("member-1", true).nicknameSearchable()).isTrue();
        assertThat(friendPrivacyService.getMyPrivacy("member-1").nicknameSearchable()).isTrue();
    }

    @Test
    void preview는_기존친구와_PENDING에는_요청불가를_반영하고_차단은_대상없음으로_숨긴다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        saveMember("member-2", "two@sungkyul.ac.kr", "회원2");
        String targetCode = friendCodeService.getMyCode("member-1").friendCode();
        String targetPublicId = provisioningService.ensureForActiveMember("member-1").getPublicId();

        assertThat(friendCodeService.preview("member-2", targetCode).canSendFriendRequest()).isTrue();
        String requestId = friendRelationshipService.createRequest("member-2", targetPublicId).requestId();
        assertThat(friendCodeService.preview("member-2", targetCode).canSendFriendRequest()).isFalse();
        friendRelationshipService.acceptRequest("member-1", requestId);
        assertThat(friendCodeService.preview("member-2", targetCode).canSendFriendRequest()).isFalse();

        friendRelationshipService.blockMember("member-1", provisioningService.ensureForActiveMember("member-2").getPublicId());
        assertThatThrownBy(() -> friendCodeService.preview("member-2", targetCode))
                .isInstanceOf(FriendCodeNotFoundException.class);
    }

    @Test
    void 탈퇴정리는_프로필을_삭제하고_활성코드는_폐기한다() {
        saveMember("member-1", "one@sungkyul.ac.kr", "회원1");
        String originalCode = friendCodeService.getMyCode("member-1").friendCode();

        provisioningService.retireForWithdrawnMember("member-1", LocalDateTime.now());

        assertThat(friendProfileRepository.findById("member-1")).isEmpty();
        assertThat(friendCodeRegistryRepository.findAll())
                .allSatisfy(code -> {
                    assertThat(code.getStatus()).isEqualTo(FriendCodeStatus.RETIRED);
                    assertThat(code.getOwnerMemberId()).isNull();
                });
        assertThat(friendCodeRegistryRepository.findByNormalizedCode(
                originalCode.replace("-", "")
        )).isPresent();
    }

    private void saveMember(String id, String email, String realname) {
        memberRepository.saveAndFlush(Member.create(id, email, realname, LocalDateTime.now()));
    }
}
