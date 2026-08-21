package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.image.service.ProfileImageStorageService;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberBankAccountRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberNotificationSettingsRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberProfileRequest;
import com.skuri.skuri_backend.domain.member.dto.response.MemberMeResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberPublicProfileResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberUpsertResult;
import com.skuri.skuri_backend.domain.member.entity.LinkedAccount;
import com.skuri.skuri_backend.domain.member.entity.LinkedAccountProvider;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.event.MemberLifecycleEvent;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.exception.WithdrawnMemberRejoinNotAllowedException;
import com.skuri.skuri_backend.domain.member.repository.LinkedAccountRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final String PROFILE_IMAGE_OWNERSHIP_MESSAGE =
            "photoUrl은 본인이 업로드한 PROFILE_IMAGE URL만 사용할 수 있습니다.";

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private LinkedAccountRepository linkedAccountRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private ProfileImageStorageService profileImageStorageService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberService memberService;

    @Test
    void createMember_신규회원_생성성공() {
        AuthenticatedMember authenticatedMember = authenticatedMember();
        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).thenReturn(null);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertTrue(result.created());
        assertEquals(authenticatedMember.uid(), result.member().id());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void createMember_저장충돌시_기존회원으로복구() {
        AuthenticatedMember authenticatedMember = authenticatedMember();
        Member existingMember = memberEntity(authenticatedMember.uid(), authenticatedMember.email());

        doThrow(new DataIntegrityViolationException("duplicate member"))
                .when(memberRepository).insert(any(Member.class));
        when(memberRepository.findById(authenticatedMember.uid())).thenReturn(Optional.of(existingMember));
        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).thenReturn(null);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertFalse(result.created());
        assertEquals(authenticatedMember.uid(), result.member().id());
    }

    @Test
    void createMember_저장충돌후_재조회실패시_충돌예외() {
        AuthenticatedMember authenticatedMember = authenticatedMember();

        doThrow(new DataIntegrityViolationException("duplicate member"))
                .when(memberRepository).insert(any(Member.class));
        when(memberRepository.findById(authenticatedMember.uid())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberService.createMember(authenticatedMember)
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(linkedAccountRepository, never()).saveAndFlush(any(LinkedAccount.class));
    }

    @Test
    void createMember_탈퇴한동일UID가있으면_재가입불가예외() {
        AuthenticatedMember authenticatedMember = authenticatedMember();
        Member withdrawnMember = memberEntity(authenticatedMember.uid(), authenticatedMember.email());
        withdrawnMember.withdraw(LocalDateTime.now());

        doThrow(new DataIntegrityViolationException("duplicate member"))
                .when(memberRepository).insert(any(Member.class));
        when(memberRepository.findById(authenticatedMember.uid())).thenReturn(Optional.of(withdrawnMember));

        assertThrows(
                WithdrawnMemberRejoinNotAllowedException.class,
                () -> memberService.createMember(authenticatedMember)
        );
        verify(linkedAccountRepository, never()).saveAndFlush(any(LinkedAccount.class));
    }

    @Test
    void createMember_linkedAccount중복충돌은_무해처리() {
        AuthenticatedMember authenticatedMember = authenticatedMember();

        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate linked account"));
        when(linkedAccountRepository.existsByMemberIdAndProvider(authenticatedMember.uid(), LinkedAccountProvider.GOOGLE))
                .thenReturn(true);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertTrue(result.created());
        verify(linkedAccountRepository).existsByMemberIdAndProvider(eq(authenticatedMember.uid()), eq(LinkedAccountProvider.GOOGLE));
    }

    @Test
    void createMember_provider이름이있어도_기본닉네임으로생성() {
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "google.com",
                "google-provider-id",
                "구글표시이름",
                "https://example.com/profile.jpg"
        );
        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).thenReturn(null);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertTrue(result.created());
        assertEquals("스쿠리 유저", result.member().nickname());
        assertEquals("구글표시이름", result.member().realname());
        assertNull(result.member().photoUrl());
        verify(linkedAccountRepository).saveAndFlush(
                argThat(linkedAccount -> "https://example.com/profile.jpg".equals(linkedAccount.getPhotoUrl()))
        );
    }

    @Test
    void createMember_비소셜로그인_password인경우_linkedAccount부가필드null로저장() {
        AuthenticatedMember authenticatedMember = new AuthenticatedMember(
                "firebase-uid",
                "admin@sungkyul.ac.kr",
                "password",
                "password-provider-id",
                "관리자",
                "https://example.com/admin.jpg"
        );
        when(linkedAccountRepository.saveAndFlush(any(LinkedAccount.class))).thenReturn(null);

        MemberUpsertResult result = memberService.createMember(authenticatedMember);

        assertTrue(result.created());
        verify(linkedAccountRepository).saveAndFlush(
                argThat(linkedAccount ->
                        linkedAccount.getProvider() == LinkedAccountProvider.PASSWORD
                                && linkedAccount.getProviderId() == null
                                && linkedAccount.getEmail() == null
                                && linkedAccount.getProviderDisplayName() == null
                                && linkedAccount.getPhotoUrl() == null
                )
        );
    }

    @Test
    void updateMyProfile_부분수정_null필드유지() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        member.updateProfile("기존닉네임", "20201234", "컴퓨터공학과", "https://example.com/old.jpg");
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        MemberMeResponse response = memberService.updateMyProfile(
                "firebase-uid",
                new UpdateMemberProfileRequest("새닉네임", null, null, "https://example.com/new.jpg")
        );

        assertEquals("새닉네임", response.nickname());
        assertEquals("20201234", response.studentId());
        assertEquals("컴퓨터공학과", response.department());
        assertEquals("기존실명", response.realname());
        assertEquals("https://example.com/new.jpg", response.photoUrl());
        verify(profileImageStorageService).validateProfilePhotoReference(
                "firebase-uid",
                "https://example.com/old.jpg",
                "https://example.com/new.jpg"
        );
        verify(chatService, never()).removeMemberFromDepartmentChatRooms("firebase-uid");
    }

    @Test
    void updateMyProfile_프로필이처음완료되면_친구프로필발급이벤트를발행한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));
        when(departmentService.normalizeSupported("컴퓨터공학과")).thenReturn("컴퓨터공학과");

        MemberMeResponse response = memberService.updateMyProfile(
                "firebase-uid",
                new UpdateMemberProfileRequest("새회원", "20261234", "컴퓨터공학과", null)
        );

        assertEquals("새회원", response.nickname());
        assertEquals("새회원", member.getNicknameKey());
        verify(memberRepository).saveAndFlush(member);
        verify(eventPublisher).publish(new MemberLifecycleEvent.MemberProfileCompleted("firebase-uid"));
    }

    @Test
    void updateMyProfile_예약닉네임이면_거부한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberService.updateMyProfile(
                        "firebase-uid",
                        new UpdateMemberProfileRequest("우리 스쿠리\u00a0유저 모임", null, null, null)
                )
        );

        assertEquals(ErrorCode.NICKNAME_RESERVED, exception.getErrorCode());
        verify(memberRepository, never()).saveAndFlush(any(Member.class));
    }

    @Test
    void updateMyProfile_ACTIVE회원과중복된닉네임이면_거부한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));
        when(memberRepository.existsActiveNicknameConflict("firebase-uid", "중복닉네임")).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberService.updateMyProfile(
                        "firebase-uid",
                        new UpdateMemberProfileRequest("중복닉네임", null, null, null)
                )
        );

        assertEquals(ErrorCode.NICKNAME_ALREADY_EXISTS, exception.getErrorCode());
        verify(memberRepository, never()).saveAndFlush(any(Member.class));
    }

    @Test
    void updateMyProfile_동시중복저장충돌도_닉네임중복오류로변환한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));
        doThrow(new DataIntegrityViolationException("duplicate nickname_key"))
                .when(memberRepository).saveAndFlush(member);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberService.updateMyProfile(
                        "firebase-uid",
                        new UpdateMemberProfileRequest("동시닉네임", null, null, null)
                )
        );

        assertEquals(ErrorCode.NICKNAME_ALREADY_EXISTS, exception.getErrorCode());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateMyProfile_photoUrlNull이면_기존사진을유지한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        member.updateProfile("기존닉네임", "20201234", "컴퓨터공학과", "https://example.com/old.jpg");
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        MemberMeResponse response = memberService.updateMyProfile(
                "firebase-uid",
                new UpdateMemberProfileRequest("새닉네임", null, null, null)
        );

        assertEquals("새닉네임", response.nickname());
        assertEquals("https://example.com/old.jpg", response.photoUrl());
        verify(profileImageStorageService).validateProfilePhotoReference(
                "firebase-uid",
                "https://example.com/old.jpg",
                null
        );
        verify(chatService, never()).removeMemberFromDepartmentChatRooms("firebase-uid");
    }

    @Test
    void deleteMyProfilePhoto_기존사진이있으면_DB를null로갱신하고_스토리지정리를위임한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        member.updateProfile(
                "기존닉네임",
                "20201234",
                "컴퓨터공학과",
                "https://cdn.skuri.app/uploads/profiles/firebase-uid/2026/04/06/photo.jpg"
        );
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        memberService.deleteMyProfilePhoto("firebase-uid");

        assertNull(member.getPhotoUrl());
        verify(profileImageStorageService).deleteOwnedManagedProfileImage(
                "firebase-uid",
                "https://cdn.skuri.app/uploads/profiles/firebase-uid/2026/04/06/photo.jpg"
        );
    }

    @Test
    void deleteMyProfilePhoto_photoUrl이이미null이어도_안전하게동작한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        memberService.deleteMyProfilePhoto("firebase-uid");

        assertNull(member.getPhotoUrl());
        verify(profileImageStorageService, never()).deleteOwnedManagedProfileImage(any(), any());
    }

    @Test
    void updateMyProfile_사진소유권검증에실패하면_예외를전파한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        member.updateProfile("기존닉네임", "20201234", "컴퓨터공학과", "https://example.com/old.jpg");
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));
        doThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, PROFILE_IMAGE_OWNERSHIP_MESSAGE))
                .when(profileImageStorageService)
                .validateProfilePhotoReference(
                        "firebase-uid",
                        "https://example.com/old.jpg",
                        "https://cdn.skuri.app/uploads/profiles/other-member/2026/04/06/photo.jpg"
                );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberService.updateMyProfile(
                        "firebase-uid",
                        new UpdateMemberProfileRequest(
                                null,
                                null,
                                null,
                                "https://cdn.skuri.app/uploads/profiles/other-member/2026/04/06/photo.jpg"
                        )
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals(PROFILE_IMAGE_OWNERSHIP_MESSAGE, exception.getMessage());
        assertEquals("https://example.com/old.jpg", member.getPhotoUrl());
        verify(chatService, never()).removeMemberFromDepartmentChatRooms("firebase-uid");
    }

    @Test
    void updateMyProfile_학과가변경되면_기존학과방멤버십을정리한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        member.updateProfile("기존닉네임", "20201234", "컴퓨터공학과", null);
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));
        when(departmentService.normalizeSupported("경영학과")).thenReturn("경영학과");

        MemberMeResponse response = memberService.updateMyProfile(
                "firebase-uid",
                new UpdateMemberProfileRequest(null, null, "경영학과", null)
        );

        assertEquals("경영학과", response.department());
        verify(chatService).removeMemberFromDepartmentChatRooms("firebase-uid");
    }

    @Test
    void updateMyProfile_레거시학과명은_정규화해서저장한다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));
        when(departmentService.normalizeSupported("소프트웨어학과")).thenReturn("미디어소프트웨어학과");

        MemberMeResponse response = memberService.updateMyProfile(
                "firebase-uid",
                new UpdateMemberProfileRequest(null, null, "소프트웨어학과", null)
        );

        assertEquals("미디어소프트웨어학과", response.department());
        verify(chatService).removeMemberFromDepartmentChatRooms("firebase-uid");
    }

    @Test
    void updateMyProfile_지원하지않는학과면_VALIDATION_ERROR() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));
        when(departmentService.normalizeSupported("없는학과")).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberService.updateMyProfile(
                        "firebase-uid",
                        new UpdateMemberProfileRequest(null, null, "없는학과", null)
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void updateMyProfile_학번을공백으로바꿔_완료프로필을미완료로만들수없다() {
        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "기존실명", LocalDateTime.now());
        member.updateProfile("기존닉네임", "20201234", "컴퓨터공학과", null);
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> memberService.updateMyProfile(
                        "firebase-uid",
                        new UpdateMemberProfileRequest(null, "   ", null, null)
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("20201234", member.getStudentId());
    }

    @Test
    void updateMyProfile_회원없음_MEMBER_NOT_FOUND() {
        when(memberRepository.findActiveById("not-found")).thenReturn(Optional.empty());

        assertThrows(
                MemberNotFoundException.class,
                () -> memberService.updateMyProfile("not-found", new UpdateMemberProfileRequest(null, null, null, null))
        );
    }

    @Test
    void updateMyBankAccount_hideNameNull이면False() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        MemberMeResponse response = memberService.updateMyBankAccount(
                "firebase-uid",
                new UpdateMemberBankAccountRequest("카카오뱅크", "3333-01-1234567", "홍길동", null)
        );

        assertNotNull(response.bankAccount());
        assertEquals("카카오뱅크", response.bankAccount().bankName());
        assertEquals("3333-01-1234567", response.bankAccount().accountNumber());
        assertEquals("홍길동", response.bankAccount().accountHolder());
        assertFalse(response.bankAccount().hideName());
    }

    @Test
    void updateMyBankAccount_회원없음_MEMBER_NOT_FOUND() {
        when(memberRepository.findActiveById("not-found")).thenReturn(Optional.empty());

        assertThrows(
                MemberNotFoundException.class,
                () -> memberService.updateMyBankAccount(
                        "not-found",
                        new UpdateMemberBankAccountRequest("카카오뱅크", "3333-01-1234567", "홍길동", false)
                )
        );
    }

    @Test
    void updateMyNotificationSettings_부분수정_지정필드만변경() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        MemberMeResponse response = memberService.updateMyNotificationSettings(
                "firebase-uid",
                new UpdateMemberNotificationSettingsRequest(
                        null,
                        false,
                        null,
                        null,
                        false,
                        true,
                        null,
                        null,
                        null,
                        null,
                        Map.of("news", false, "academy", true, "scholarship", true)
                )
        );

        assertNotNull(response.notificationSetting());
        assertTrue(response.notificationSetting().allNotifications());
        assertFalse(response.notificationSetting().partyNotifications());
        assertTrue(response.notificationSetting().noticeNotifications());
        assertTrue(response.notificationSetting().boardLikeNotifications());
        assertFalse(response.notificationSetting().commentNotifications());
        assertTrue(response.notificationSetting().bookmarkedPostCommentNotifications());
        assertTrue(response.notificationSetting().systemNotifications());
        assertTrue(response.notificationSetting().academicScheduleNotifications());
        assertTrue(response.notificationSetting().academicScheduleDayBeforeEnabled());
        assertFalse(response.notificationSetting().academicScheduleAllEventsEnabled());
        assertEquals(Map.of("news", false, "academy", true, "scholarship", true), response.notificationSetting().noticeNotificationsDetail());
    }

    @Test
    void updateMyNotificationSettings_회원없음_MEMBER_NOT_FOUND() {
        when(memberRepository.findActiveById("not-found")).thenReturn(Optional.empty());

        assertThrows(
                MemberNotFoundException.class,
                () -> memberService.updateMyNotificationSettings(
                        "not-found",
                        new UpdateMemberNotificationSettingsRequest(
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
        );
    }

    @Test
    void getMyProfile_lastLogin갱신() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        LocalDateTime oldLastLogin = LocalDateTime.now().minusDays(1);
        member.updateLastLogin(oldLastLogin);
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        LocalDateTime callStartedAt = LocalDateTime.now();
        MemberMeResponse response = memberService.getMyProfile("firebase-uid");

        assertNotNull(response.lastLogin());
        assertTrue(response.lastLogin().isAfter(oldLastLogin));
        assertTrue(!response.lastLogin().isBefore(callStartedAt));
    }

    @Test
    void getMyProfile_기존회원의학사일정알림기본값을보존한다() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        ReflectionTestUtils.setField(member.getNotificationSetting(), "academicScheduleNotifications", null);
        ReflectionTestUtils.setField(member.getNotificationSetting(), "academicScheduleDayBeforeEnabled", null);
        ReflectionTestUtils.setField(member.getNotificationSetting(), "academicScheduleAllEventsEnabled", null);
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        MemberMeResponse response = memberService.getMyProfile("firebase-uid");

        assertNotNull(response.notificationSetting());
        assertTrue(response.notificationSetting().academicScheduleNotifications());
        assertTrue(response.notificationSetting().academicScheduleDayBeforeEnabled());
        assertFalse(response.notificationSetting().academicScheduleAllEventsEnabled());
    }

    @Test
    void getMemberById_공개프로필반환() {
        Member member = memberEntity("firebase-uid", "user@sungkyul.ac.kr");
        member.updateProfile("공개닉네임", null, "컴퓨터공학과", "https://example.com/target.jpg");
        when(memberRepository.findActiveById("firebase-uid")).thenReturn(Optional.of(member));

        MemberPublicProfileResponse response = memberService.getMemberById("firebase-uid");

        assertEquals("firebase-uid", response.id());
        assertEquals("공개닉네임", response.nickname());
        assertEquals("컴퓨터공학과", response.department());
        assertEquals("https://example.com/target.jpg", response.photoUrl());
    }

    @Test
    void getMemberById_회원없음_MEMBER_NOT_FOUND() {
        when(memberRepository.findActiveById("not-found")).thenReturn(Optional.empty());

        assertThrows(
                MemberNotFoundException.class,
                () -> memberService.getMemberById("not-found")
        );
    }

    private AuthenticatedMember authenticatedMember() {
        return new AuthenticatedMember(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "google.com",
                "google-provider-id",
                "홍길동",
                "https://example.com/profile.jpg"
        );
    }

    private Member memberEntity(String id, String email) {
        return Member.create(
                id,
                email,
                null,
                LocalDateTime.now()
        );
    }
}
