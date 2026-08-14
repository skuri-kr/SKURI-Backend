package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.domain.image.service.ProfileImageStorageService;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberBankAccountRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberNotificationSettingsRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberProfileRequest;
import com.skuri.skuri_backend.domain.member.dto.response.MemberBankAccountResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberCreateResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberMeResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberNotificationSettingResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberPublicProfileResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberUpsertResult;
import com.skuri.skuri_backend.domain.member.entity.BankAccount;
import com.skuri.skuri_backend.domain.member.entity.LinkedAccount;
import com.skuri.skuri_backend.domain.member.entity.LinkedAccountProvider;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.NotificationSetting;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.exception.WithdrawnMemberRejoinNotAllowedException;
import com.skuri.skuri_backend.domain.member.repository.LinkedAccountRepository;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final LinkedAccountRepository linkedAccountRepository;
    private final ChatService chatService;
    private final ProfileImageStorageService profileImageStorageService;
    private final DepartmentService departmentService;

    // Intentionally non-transactional: insert 충돌(DataIntegrityViolationException) 이후
    // 복구 조회를 새로운 JPA 세션/트랜잭션에서 수행해 Session 오염을 피한다.
    public MemberUpsertResult createMember(AuthenticatedMember authenticatedMember) {
        try {
            LocalDateTime now = LocalDateTime.now();
            Member createdMember = Member.create(
                    authenticatedMember.uid(),
                    authenticatedMember.email(),
                    authenticatedMember.providerDisplayName(),
                    now
            );
            memberRepository.insert(createdMember);
            createLinkedAccount(createdMember, authenticatedMember);
            return MemberUpsertResult.created(toMemberCreateResponse(createdMember));
        } catch (DataIntegrityViolationException e) {
            Member existingMember = memberRepository.findById(authenticatedMember.uid())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "회원 생성 처리 중 충돌이 발생했습니다."));
            if (existingMember.isWithdrawn()) {
                throw new WithdrawnMemberRejoinNotAllowedException();
            }
            createLinkedAccount(existingMember, authenticatedMember);
            return MemberUpsertResult.existing(toMemberCreateResponse(existingMember));
        }
    }

    @Transactional
    public MemberMeResponse getMyProfile(String memberId) {
        Member member = getMemberOrThrow(memberId);
        member.updateLastLogin(LocalDateTime.now());
        return toMemberMeResponse(member);
    }

    @Transactional
    public MemberMeResponse updateMyProfile(String memberId, UpdateMemberProfileRequest request) {
        Member member = getMemberOrThrow(memberId);
        String previousDepartment = member.getDepartment();
        String normalizedDepartment = request.department() != null
                ? departmentService.normalizeSupported(request.department())
                : null;
        if (request.department() != null && StringUtils.hasText(request.department()) && normalizedDepartment == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 department입니다.");
        }
        profileImageStorageService.validateProfilePhotoReference(memberId, member.getPhotoUrl(), request.photoUrl());
        member.updateProfile(
                request.nickname(),
                request.studentId(),
                normalizedDepartment,
                request.photoUrl()
        );
        if (request.department() != null && !normalizeNullable(previousDepartment).equals(normalizeNullable(member.getDepartment()))) {
            chatService.removeMemberFromDepartmentChatRooms(memberId);
        }
        return toMemberMeResponse(member);
    }

    @Transactional
    public void deleteMyProfilePhoto(String memberId) {
        Member member = getMemberOrThrow(memberId);
        String previousPhotoUrl = member.getPhotoUrl();
        member.removeProfilePhoto();
        cleanupProfilePhotoAfterCommit(memberId, previousPhotoUrl);
    }

    @Transactional
    public MemberMeResponse updateMyBankAccount(String memberId, UpdateMemberBankAccountRequest request) {
        Member member = getMemberOrThrow(memberId);
        member.updateBankAccount(
                BankAccount.of(
                        request.bankName(),
                        request.accountNumber(),
                        request.accountHolder(),
                        request.hideName()
                )
        );
        return toMemberMeResponse(member);
    }

    @Transactional
    public MemberMeResponse updateMyNotificationSettings(
            String memberId,
            UpdateMemberNotificationSettingsRequest request
    ) {
        Member member = getMemberOrThrow(memberId);
        member.updateNotificationSetting(
                request.allNotifications(),
                request.partyNotifications(),
                request.noticeNotifications(),
                request.boardLikeNotifications(),
                request.commentNotifications(),
                request.bookmarkedPostCommentNotifications(),
                request.systemNotifications(),
                request.academicScheduleNotifications(),
                request.academicScheduleDayBeforeEnabled(),
                request.academicScheduleAllEventsEnabled(),
                request.noticeNotificationsDetail()
        );
        return toMemberMeResponse(member);
    }

    @Transactional(readOnly = true)
    public MemberPublicProfileResponse getMemberById(String memberId) {
        Member member = getMemberOrThrow(memberId);
        return new MemberPublicProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getDepartment(),
                member.getPhotoUrl()
        );
    }

    private void createLinkedAccount(Member member, AuthenticatedMember authenticatedMember) {
        LinkedAccountProvider provider = LinkedAccountProvider.fromSignInProvider(authenticatedMember.signInProvider());
        boolean socialProvider = provider.isSocialProvider();

        try {
            linkedAccountRepository.saveAndFlush(
                LinkedAccount.of(
                        member,
                        provider,
                        socialProvider ? authenticatedMember.providerId() : null,
                        socialProvider ? authenticatedMember.email() : null,
                        socialProvider ? authenticatedMember.providerDisplayName() : null,
                        socialProvider ? authenticatedMember.photoUrl() : null
                )
            );
        } catch (DataIntegrityViolationException e) {
            boolean alreadyLinked = linkedAccountRepository.existsByMemberIdAndProvider(member.getId(), provider);
            if (!alreadyLinked) {
                throw new BusinessException(ErrorCode.CONFLICT, "연결 계정 생성 처리 중 충돌이 발생했습니다.");
            }
        }
    }

    private Member getMemberOrThrow(String memberId) {
        return memberRepository.findActiveById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private void cleanupProfilePhotoAfterCommit(String memberId, String photoUrl) {
        if (!StringUtils.hasText(photoUrl)) {
            return;
        }

        Runnable cleanupTask = () -> profileImageStorageService.deleteOwnedManagedProfileImage(memberId, photoUrl);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanupTask.run();
                }
            });
            return;
        }
        cleanupTask.run();
    }

    private MemberCreateResponse toMemberCreateResponse(Member member) {
        return new MemberCreateResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getStudentId(),
                member.getDepartment(),
                member.getPhotoUrl(),
                member.getRealname(),
                member.isAdmin(),
                toBankAccountResponse(member.getBankAccount()),
                member.getJoinedAt()
        );
    }

    private MemberMeResponse toMemberMeResponse(Member member) {
        return new MemberMeResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getStudentId(),
                member.getDepartment(),
                member.getPhotoUrl(),
                member.getRealname(),
                member.isAdmin(),
                toBankAccountResponse(member.getBankAccount()),
                toNotificationSettingResponse(member.getNotificationSetting()),
                member.getJoinedAt(),
                member.getLastLogin()
        );
    }

    private MemberBankAccountResponse toBankAccountResponse(BankAccount bankAccount) {
        if (bankAccount == null) {
            return null;
        }
        return new MemberBankAccountResponse(
                bankAccount.getBankName(),
                bankAccount.getAccountNumber(),
                bankAccount.getAccountHolder(),
                bankAccount.getHideName()
        );
    }

    private MemberNotificationSettingResponse toNotificationSettingResponse(NotificationSetting notificationSetting) {
        if (notificationSetting == null) {
            notificationSetting = NotificationSetting.defaultSetting();
        }
        Map<String, Boolean> detail = notificationSetting.getNoticeNotificationsDetail() != null
                ? new HashMap<>(notificationSetting.getNoticeNotificationsDetail())
                : Map.of();

        return new MemberNotificationSettingResponse(
                notificationSetting.isAllNotifications(),
                notificationSetting.isPartyNotifications(),
                notificationSetting.isNoticeNotifications(),
                notificationSetting.isBoardLikeNotifications(),
                notificationSetting.isCommentNotifications(),
                notificationSetting.isBookmarkedPostCommentNotifications(),
                notificationSetting.isSystemNotifications(),
                notificationSetting.isAcademicScheduleNotifications(),
                notificationSetting.isAcademicScheduleDayBeforeEnabled(),
                notificationSetting.isAcademicScheduleAllEventsEnabled(),
                detail
        );
    }
}
