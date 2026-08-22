package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.constant.AdminMemberSortField;
import com.skuri.skuri_backend.domain.member.constant.MemberNicknamePolicy;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.friend.entity.FriendProfile;
import com.skuri.skuri_backend.domain.friend.repository.FriendProfileRepository;
import com.skuri.skuri_backend.domain.notification.entity.FcmToken;
import com.skuri.skuri_backend.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class MemberRepositoryDataJpaTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendProfileRepository friendProfileRepository;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Test
    void searchAdminMembers_realname오름차순을지원한다() {
        saveMember("member-b", "박하나", "b@sungkyul.ac.kr", "경영학과", "20230002",
                LocalDateTime.of(2025, 3, 2, 9, 0), LocalDateTime.of(2026, 3, 2, 10, 0), false);
        saveMember("member-a", "김하나", "a@sungkyul.ac.kr", "컴퓨터공학과", "20230001",
                LocalDateTime.of(2025, 3, 1, 9, 0), LocalDateTime.of(2026, 3, 1, 10, 0), false);

        Page<AdminMemberSummaryProjection> result = memberRepository.searchAdminMembers(
                null,
                null,
                null,
                null,
                AdminMemberSortField.REALNAME,
                Sort.Direction.ASC,
                PageRequest.of(0, 10)
        );

        assertEquals(List.of("member-a", "member-b"), result.getContent().stream().map(AdminMemberSummaryProjection::id).toList());
        assertEquals(List.of("김하나", "박하나"), result.getContent().stream().map(AdminMemberSummaryProjection::realname).toList());
    }

    @Test
    void searchAdminMembers_lastLoginOs내림차순과최신토큰매핑을지원한다() {
        saveMember("member-android", "안드로이드", "android@sungkyul.ac.kr", "컴퓨터공학과", "20230011",
                LocalDateTime.of(2025, 3, 11, 9, 0), LocalDateTime.of(2026, 3, 11, 10, 0), false);
        saveMember("member-ios", "아이오에스", "ios@sungkyul.ac.kr", "컴퓨터공학과", "20230012",
                LocalDateTime.of(2025, 3, 12, 9, 0), LocalDateTime.of(2026, 3, 12, 10, 0), false);
        saveMember("member-none", "토큰없음", "none@sungkyul.ac.kr", "컴퓨터공학과", "20230013",
                LocalDateTime.of(2025, 3, 13, 9, 0), LocalDateTime.of(2026, 3, 13, 10, 0), false);

        saveFcmToken("member-android", "token-old", "ios", "1.3.0", LocalDateTime.of(2026, 3, 20, 10, 0));
        saveFcmToken("member-android", "token-new", "android", "1.4.2", LocalDateTime.of(2026, 3, 21, 10, 0));
        saveFcmToken("member-ios", "token-ios", "ios", "1.5.0", LocalDateTime.of(2026, 3, 22, 10, 0));

        Page<AdminMemberSummaryProjection> result = memberRepository.searchAdminMembers(
                null,
                null,
                null,
                null,
                AdminMemberSortField.LAST_LOGIN_OS,
                Sort.Direction.DESC,
                PageRequest.of(0, 10)
        );

        assertEquals(
                List.of("member-ios", "member-android", "member-none"),
                result.getContent().stream().map(AdminMemberSummaryProjection::id).toList()
        );
        assertEquals(
                Arrays.asList("ios", "android", null),
                result.getContent().stream().map(AdminMemberSummaryProjection::lastLoginOs).toList()
        );
        assertEquals(
                Arrays.asList("1.5.0", "1.4.2", null),
                result.getContent().stream().map(AdminMemberSummaryProjection::currentAppVersion).toList()
        );
    }

    @Test
    void searchAdminMembers_currentAppVersion정렬을지원한다() {
        saveMember("member-v142", "앱버전142", "v142@sungkyul.ac.kr", "컴퓨터공학과", "20230021",
                LocalDateTime.of(2025, 3, 21, 9, 0), LocalDateTime.of(2026, 3, 21, 10, 0), false);
        saveMember("member-v150", "앱버전150", "v150@sungkyul.ac.kr", "컴퓨터공학과", "20230022",
                LocalDateTime.of(2025, 3, 22, 9, 0), LocalDateTime.of(2026, 3, 22, 10, 0), false);
        saveMember("member-v131", "앱버전131", "v131@sungkyul.ac.kr", "컴퓨터공학과", "20230023",
                LocalDateTime.of(2025, 3, 23, 9, 0), LocalDateTime.of(2026, 3, 23, 10, 0), false);

        saveFcmToken("member-v142", "token-v142", "ios", "1.4.2", LocalDateTime.of(2026, 3, 21, 10, 0));
        saveFcmToken("member-v150", "token-v150", "android", "1.5.0", LocalDateTime.of(2026, 3, 22, 10, 0));
        saveFcmToken("member-v131", "token-v131", "ios", "1.3.1", LocalDateTime.of(2026, 3, 23, 10, 0));

        Page<AdminMemberSummaryProjection> result = memberRepository.searchAdminMembers(
                null,
                null,
                null,
                null,
                AdminMemberSortField.CURRENT_APP_VERSION,
                Sort.Direction.DESC,
                PageRequest.of(0, 10)
        );

        assertEquals(
                List.of("member-v150", "member-v142", "member-v131"),
                result.getContent().stream().map(AdminMemberSummaryProjection::id).toList()
        );
    }

    @Test
    void searchAdminMembers_currentAppVersion정렬에서도_null값은항상마지막으로정렬한다() {
        saveMember("member-null", "앱버전없음", "null@sungkyul.ac.kr", "컴퓨터공학과", "20230031",
                LocalDateTime.of(2025, 3, 31, 9, 0), LocalDateTime.of(2026, 3, 31, 10, 0), false);
        saveMember("member-v142", "앱버전142", "v142@sungkyul.ac.kr", "컴퓨터공학과", "20230032",
                LocalDateTime.of(2025, 3, 30, 9, 0), LocalDateTime.of(2026, 3, 30, 10, 0), false);
        saveMember("member-v131", "앱버전131", "v131@sungkyul.ac.kr", "컴퓨터공학과", "20230033",
                LocalDateTime.of(2025, 3, 29, 9, 0), LocalDateTime.of(2026, 3, 29, 10, 0), false);

        saveFcmToken("member-v142", "token-v142-2", "ios", "1.4.2", LocalDateTime.of(2026, 3, 30, 10, 0));
        saveFcmToken("member-v131", "token-v131-2", "android", "1.3.1", LocalDateTime.of(2026, 3, 29, 10, 0));

        Page<AdminMemberSummaryProjection> result = memberRepository.searchAdminMembers(
                null,
                null,
                null,
                null,
                AdminMemberSortField.CURRENT_APP_VERSION,
                Sort.Direction.DESC,
                PageRequest.of(0, 10)
        );

        assertEquals(
                List.of("member-v142", "member-v131", "member-null"),
                result.getContent().stream().map(AdminMemberSummaryProjection::id).toList()
        );
        assertEquals(
                Arrays.asList("1.4.2", "1.3.1", null),
                result.getContent().stream().map(AdminMemberSummaryProjection::currentAppVersion).toList()
        );
    }

    @Test
    void searchAdminMembers_기존필터를유지한다() {
        saveMember("member-match", "홍길동", "hong@sungkyul.ac.kr", "컴퓨터공학과", "2023112233",
                LocalDateTime.of(2025, 3, 1, 9, 0), LocalDateTime.of(2026, 3, 29, 10, 0), false);
        saveMember("member-admin", "홍길동", "admin@sungkyul.ac.kr", "컴퓨터공학과", "2023112234",
                LocalDateTime.of(2025, 3, 2, 9, 0), LocalDateTime.of(2026, 3, 29, 11, 0), true);
        saveMember("member-department", "홍길동", "dept@sungkyul.ac.kr", "경영학과", "2023112235",
                LocalDateTime.of(2025, 3, 3, 9, 0), LocalDateTime.of(2026, 3, 29, 12, 0), false);
        Member withdrawn = saveMember("member-withdrawn", "홍길동", "withdrawn@sungkyul.ac.kr", "컴퓨터공학과", "2023112236",
                LocalDateTime.of(2025, 3, 4, 9, 0), LocalDateTime.of(2026, 3, 29, 13, 0), false);
        withdrawn.withdraw(LocalDateTime.of(2026, 3, 29, 14, 0));
        memberRepository.saveAndFlush(withdrawn);

        Page<AdminMemberSummaryProjection> result = memberRepository.searchAdminMembers(
                "홍길동",
                MemberStatus.ACTIVE,
                false,
                "컴퓨터공학과",
                AdminMemberSortField.JOINED_AT,
                Sort.Direction.DESC,
                PageRequest.of(0, 10)
        );

        assertEquals(List.of("member-match"), result.getContent().stream().map(AdminMemberSummaryProjection::id).toList());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void existsActiveNicknameConflict_레거시닉네임과닉네임키를모두검사한다() {
        Member legacy = Member.create("legacy", "legacy@sungkyul.ac.kr", null, LocalDateTime.now());
        legacy.updateProfile("LegacyName", "20260001", "컴퓨터공학과", null);
        memberRepository.saveAndFlush(legacy);

        Member keyed = Member.create("keyed", "keyed@sungkyul.ac.kr", null, LocalDateTime.now());
        keyed.updateProfile("새닉네임", "새닉네임", "20260002", "컴퓨터공학과", null);
        memberRepository.saveAndFlush(keyed);

        assertTrue(memberRepository.existsActiveNicknameConflict("requester", "legacyname"));
        assertTrue(memberRepository.existsActiveNicknameConflict("requester", "새닉네임"));
        assertFalse(memberRepository.existsActiveNicknameConflict("requester", "사용가능"));
        assertFalse(memberRepository.existsActiveNicknameConflict("legacy", "legacyname"));
    }

    @Test
    void existsActiveNicknameConflict_탈퇴회원닉네임은재사용할수있다() {
        Member withdrawn = Member.create("withdrawn", "withdrawn@sungkyul.ac.kr", null, LocalDateTime.now());
        withdrawn.updateProfile("재사용닉네임", "재사용닉네임", "20260003", "컴퓨터공학과", null);
        withdrawn.withdraw(LocalDateTime.now());
        memberRepository.saveAndFlush(withdrawn);

        assertFalse(memberRepository.existsActiveNicknameConflict("requester", "재사용닉네임"));
    }

    @Test
    void 프로필완료후보조회는_기존예약어닉네임도학번과학과가있으면포함한다() {
        Member reserved = Member.create("reserved", "reserved@sungkyul.ac.kr", null, LocalDateTime.now());
        reserved.updateProfile("스쿠리\u00a0유저", "스쿠리\u00a0유저", "20260004", "컴퓨터공학과", null);
        memberRepository.saveAndFlush(reserved);
        Member reservedSearchable = Member.create("reserved-search", "reserved-search@sungkyul.ac.kr", null, LocalDateTime.now());
        reservedSearchable.updateProfile("운\t영자", "운\t영자", "20260005", "컴퓨터공학과", null);
        memberRepository.saveAndFlush(reservedSearchable);
        friendProfileRepository.saveAndFlush(
                FriendProfile.create("reserved-search", "reserved-search-public", "reserved-search-code")
        );

        assertTrue(reserved.isProfileComplete());
        assertTrue(reservedSearchable.isProfileComplete());
        assertEquals(
                List.of("reserved"),
                memberRepository.findProfileCompleteActiveMemberIdsWithoutFriendProfile(PageRequest.of(0, 10))
        );
        assertEquals(2, memberRepository.countProfileCompleteActiveMembers());
        assertEquals(
                List.of("reserved-search-public"),
                friendProfileRepository.findNicknameSearchResults(
                                "requester", "", null, null, PageRequest.of(0, 10)
                        ).stream()
                        .map(projection -> projection.getFriendPublicId())
                        .toList()
        );
        assertEquals(1, friendProfileRepository.countForProfileCompleteActiveMembers());
    }

    @Test
    void 프로필완료후보조회는_StringIsBlank_기준의공백만있는필드를제외한다() {
        Member blankNickname = Member.create("blank-nickname", "blank-nickname@sungkyul.ac.kr", null, LocalDateTime.now());
        blankNickname.updateProfile("\t", "\t", "20260007", "컴퓨터공학과", null);
        Member blankStudentId = Member.create("blank-student-id", "blank-student-id@sungkyul.ac.kr", null, LocalDateTime.now());
        blankStudentId.updateProfile("학번공백", "학번공백", "\t", "컴퓨터공학과", null);
        Member blankDepartment = Member.create("blank-department", "blank-department@sungkyul.ac.kr", null, LocalDateTime.now());
        blankDepartment.updateProfile("학과공백", "학과공백", "20260008", "\t", null);
        memberRepository.saveAllAndFlush(List.of(blankNickname, blankStudentId, blankDepartment));
        friendProfileRepository.saveAndFlush(
                FriendProfile.create("blank-nickname", "blank-nickname-public", "blank-nickname-code")
        );

        assertFalse(blankNickname.isProfileComplete());
        assertFalse(blankStudentId.isProfileComplete());
        assertFalse(blankDepartment.isProfileComplete());
        assertTrue(
                memberRepository.findProfileCompleteActiveMemberIdsWithoutFriendProfile(PageRequest.of(0, 10)).isEmpty()
        );
        assertEquals(0, memberRepository.countProfileCompleteActiveMembers());
        assertTrue(
                friendProfileRepository.findNicknameSearchResults(
                        "requester", "", null, null, PageRequest.of(0, 10)
                ).isEmpty()
        );
        assertEquals(0, friendProfileRepository.countForProfileCompleteActiveMembers());
    }

    @Test
    void 소문자화시길이가늘어난_50자닉네임의고유키를저장한다() {
        String nickname = "İ".repeat(50);
        Member member = Member.create("expanding-key", "expanding-key@sungkyul.ac.kr", null, LocalDateTime.now());
        member.updateProfile(
                nickname,
                MemberNicknamePolicy.toUniquenessKey(nickname),
                "20260006",
                "컴퓨터공학과",
                null
        );

        memberRepository.saveAndFlush(member);

        assertEquals(100, memberRepository.findById("expanding-key").orElseThrow().getNicknameKey().length());
    }

    private Member saveMember(
            String id,
            String realname,
            String email,
            String department,
            String studentId,
            LocalDateTime joinedAt,
            LocalDateTime lastLogin,
            boolean isAdmin
    ) {
        Member member = Member.create(id, email, realname, joinedAt);
        member.updateProfile("닉네임-" + id, studentId, department, null);
        ReflectionTestUtils.setField(member, "joinedAt", joinedAt);
        ReflectionTestUtils.setField(member, "lastLogin", lastLogin);
        ReflectionTestUtils.setField(member, "isAdmin", isAdmin);
        return memberRepository.saveAndFlush(member);
    }

    private void saveFcmToken(String userId, String token, String platform, String appVersion, LocalDateTime lastUsedAt) {
        FcmToken fcmToken = FcmToken.create(userId, token, platform, appVersion);
        ReflectionTestUtils.setField(fcmToken, "createdAt", lastUsedAt.minusMinutes(5));
        ReflectionTestUtils.setField(fcmToken, "lastUsedAt", lastUsedAt);
        fcmTokenRepository.saveAndFlush(fcmToken);
    }
}
