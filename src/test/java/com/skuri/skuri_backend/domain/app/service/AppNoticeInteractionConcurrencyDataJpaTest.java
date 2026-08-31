package com.skuri.skuri_backend.domain.app.service;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeComment;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCommentLike;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeLike;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeReadStatus;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentLikeRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeCommentRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeLikeRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeReadStatusRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.contentblock.service.ContentBlockQueryService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberWithdrawalSanitizer;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({
        JpaAuditingConfig.class,
        AfterCommitApplicationEventPublisher.class,
        AppNoticeService.class,
        AppNoticeInteractionConcurrencyDataJpaTest.ConcurrentCommands.class,
        AppNoticeInteractionConcurrencyDataJpaTest.AppNoticeRepositoryLockAspectConfiguration.class
})
class AppNoticeInteractionConcurrencyDataJpaTest {

    @MockitoBean
    private ContentBlockQueryService contentBlockQueryService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AppNoticeRepository appNoticeRepository;

    @Autowired
    private AppNoticeCommentRepository appNoticeCommentRepository;

    @Autowired
    private AppNoticeCommentLikeRepository appNoticeCommentLikeRepository;

    @Autowired
    private AppNoticeLikeRepository appNoticeLikeRepository;

    @Autowired
    private AppNoticeReadStatusRepository appNoticeReadStatusRepository;

    @Autowired
    private AppNoticeService appNoticeService;

    @Autowired
    private ConcurrentCommands concurrentCommands;

    @Autowired
    private AppNoticeRepositoryLockBarrier appNoticeRepositoryLockBarrier;

    @Test
    void 댓글삭제가먼저회원잠금을잡으면_탈퇴는삭제상태를보존해익명화한다() throws Exception {
        Fixture fixture = fixture("delete-first", true, false);
        CountDownLatch memberLocked = new CountDownLatch(1);
        CountDownLatch continueMutation = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> deletion = executor.submit(() -> concurrentCommands.deleteCommentAfterPause(
                    fixture.memberId(), fixture.commentId(), memberLocked, continueMutation
            ));
            await(memberLocked);
            Future<?> withdrawal = executor.submit(() -> concurrentCommands.withdraw(fixture.memberId()));

            continueMutation.countDown();
            deletion.get(5, TimeUnit.SECONDS);
            withdrawal.get(5, TimeUnit.SECONDS);
        }

        AppNoticeComment comment = appNoticeCommentRepository.findById(fixture.commentId()).orElseThrow();
        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getUserId()).isEqualTo(MemberWithdrawalSanitizer.WITHDRAWN_AUTHOR_ID);
        assertThat(appNoticeRepository.findById(fixture.noticeId()).orElseThrow().getCommentCount()).isZero();
    }

    @Test
    void 탈퇴가먼저회원잠금을잡으면_댓글삭제는거부되고익명화상태를보존한다() throws Exception {
        Fixture fixture = fixture("withdraw-first-delete", true, false);
        CountDownLatch memberLocked = new CountDownLatch(1);
        CountDownLatch continueWithdrawal = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> withdrawal = executor.submit(() -> concurrentCommands.withdrawAfterPause(
                    fixture.memberId(), memberLocked, continueWithdrawal
            ));
            await(memberLocked);
            Future<?> deletion = executor.submit(() -> appNoticeService.deleteComment(
                    fixture.memberId(), fixture.commentId()
            ));

            continueWithdrawal.countDown();
            withdrawal.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> deletion.get(5, TimeUnit.SECONDS))
                    .hasRootCauseInstanceOf(BusinessException.class);
        }

        AppNoticeComment comment = appNoticeCommentRepository.findById(fixture.commentId()).orElseThrow();
        assertThat(comment.isDeleted()).isFalse();
        assertThat(comment.getUserId()).isEqualTo(MemberWithdrawalSanitizer.WITHDRAWN_AUTHOR_ID);
        assertThat(appNoticeRepository.findById(fixture.noticeId()).orElseThrow().getCommentCount()).isEqualTo(1);
    }

    @Test
    void 좋아요취소가먼저회원잠금을잡으면_탈퇴와합쳐한번만감소한다() throws Exception {
        Fixture fixture = fixture("unlike-first", false, true);
        CountDownLatch memberLocked = new CountDownLatch(1);
        CountDownLatch continueMutation = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> unlike = executor.submit(() -> concurrentCommands.unlikeNoticeAfterPause(
                    fixture.memberId(), fixture.noticeId(), memberLocked, continueMutation
            ));
            await(memberLocked);
            Future<?> withdrawal = executor.submit(() -> concurrentCommands.withdraw(fixture.memberId()));

            continueMutation.countDown();
            unlike.get(5, TimeUnit.SECONDS);
            withdrawal.get(5, TimeUnit.SECONDS);
        }

        assertThat(appNoticeLikeRepository.findById_UserId(fixture.memberId())).isEmpty();
        assertThat(appNoticeRepository.findById(fixture.noticeId()).orElseThrow().getLikeCount()).isZero();
    }

    @Test
    void 탈퇴가먼저회원잠금을잡으면_좋아요취소는거부되고카운터는한번만감소한다() throws Exception {
        Fixture fixture = fixture("withdraw-first-unlike", false, true);
        CountDownLatch memberLocked = new CountDownLatch(1);
        CountDownLatch continueWithdrawal = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> withdrawal = executor.submit(() -> concurrentCommands.withdrawAfterPause(
                    fixture.memberId(), memberLocked, continueWithdrawal
            ));
            await(memberLocked);
            Future<?> unlike = executor.submit(() -> appNoticeService.unlikeNotice(
                    fixture.memberId(), fixture.noticeId()
            ));

            continueWithdrawal.countDown();
            withdrawal.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> unlike.get(5, TimeUnit.SECONDS))
                    .hasRootCauseInstanceOf(BusinessException.class);
        }

        assertThat(appNoticeLikeRepository.findById_UserId(fixture.memberId())).isEmpty();
        assertThat(appNoticeRepository.findById(fixture.noticeId()).orElseThrow().getLikeCount()).isZero();
    }

    @Test
    void 탈퇴와관리자공지삭제가경합해도_공지잠금후댓글정리순서로완료한다() throws Exception {
        Fixture fixture = fixtureWithNoticeAndCommentLikes("withdraw-delete-notice");
        CountDownLatch noticeLocked = new CountDownLatch(1);
        CountDownLatch continueWithdrawal = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> withdrawal = executor.submit(() -> concurrentCommands.withdrawAfterNoticeLockAndPause(
                    fixture.memberId(), fixture.noticeId(), noticeLocked, continueWithdrawal
            ));
            await(noticeLocked);
            Future<?> deletion = executor.submit(() -> appNoticeService.deleteAppNotice(fixture.noticeId()));

            continueWithdrawal.countDown();
            withdrawal.get(5, TimeUnit.SECONDS);
            deletion.get(5, TimeUnit.SECONDS);
        }

        assertThat(appNoticeRepository.findById(fixture.noticeId())).isEmpty();
        assertThat(appNoticeCommentRepository.findById(fixture.commentId())).isEmpty();
        assertThat(appNoticeLikeRepository.findById_UserId(fixture.memberId())).isEmpty();
        assertThat(appNoticeCommentLikeRepository.findById_UserId(fixture.memberId())).isEmpty();
    }

    @Test
    void 관리자공지삭제가먼저공지잠금을잡으면_탈퇴는이미삭제된연관데이터를건너뛴다() throws Exception {
        Fixture fixture = fixtureWithNoticeAndCommentLikes("delete-before-withdrawal-lock");
        CountDownLatch noticeLocked = new CountDownLatch(1);
        CountDownLatch continueDeletion = new CountDownLatch(1);
        CountDownLatch withdrawalLockAttempted = new CountDownLatch(1);
        CountDownLatch continueWithdrawalLock = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> deletion = executor.submit(() -> concurrentCommands.deleteAppNoticeAfterLockAndPause(
                    fixture.noticeId(), noticeLocked, continueDeletion
            ));
            await(noticeLocked);

            appNoticeRepositoryLockBarrier.delayNextLock(
                    fixture.noticeId(), withdrawalLockAttempted, continueWithdrawalLock
            );
            Future<?> withdrawal = executor.submit(() -> concurrentCommands.withdraw(fixture.memberId()));
            await(withdrawalLockAttempted);

            continueDeletion.countDown();
            deletion.get(5, TimeUnit.SECONDS);
            continueWithdrawalLock.countDown();
            withdrawal.get(5, TimeUnit.SECONDS);
        }

        assertThat(memberRepository.findById(fixture.memberId()).orElseThrow().isWithdrawn()).isTrue();
        assertThat(appNoticeRepository.findById(fixture.noticeId())).isEmpty();
        assertThat(appNoticeCommentRepository.findById(fixture.commentId())).isEmpty();
        assertThat(appNoticeLikeRepository.findById_UserId(fixture.memberId())).isEmpty();
        assertThat(appNoticeCommentLikeRepository.findById_UserId(fixture.memberId())).isEmpty();
    }

    @Test
    void 공지삭제는읽음상태를먼저DB에서제거한다() {
        Fixture fixture = fixture("delete-read-status", false, false);
        AppNotice notice = appNoticeRepository.findById(fixture.noticeId()).orElseThrow();
        appNoticeReadStatusRepository.saveAndFlush(AppNoticeReadStatus.create(
                notice,
                fixture.memberId(),
                LocalDateTime.now()
        ));

        appNoticeService.deleteAppNotice(fixture.noticeId());

        assertThat(appNoticeRepository.findById(fixture.noticeId())).isEmpty();
        assertThat(appNoticeReadStatusRepository.findById_UserIdAndId_AppNoticeId(
                fixture.memberId(), fixture.noticeId()
        )).isEmpty();
    }

    private Fixture fixture(String suffix, boolean withComment, boolean withLike) {
        String memberId = "member-" + suffix;
        Member member = Member.create(
                memberId,
                memberId + "@sungkyul.ac.kr",
                "회원",
                LocalDateTime.now()
        );
        memberRepository.saveAndFlush(member);

        AppNotice notice = AppNotice.create(
                "앱 공지",
                "내용",
                AppNoticeCategory.GENERAL,
                AppNoticePriority.NORMAL,
                List.of(),
                null,
                LocalDateTime.now().minusMinutes(1)
        );
        if (withComment) notice.increaseCommentCount(1);
        if (withLike) notice.increaseLikeCount(1);
        notice = appNoticeRepository.saveAndFlush(notice);

        String commentId = null;
        if (withComment) {
            AppNoticeComment comment = appNoticeCommentRepository.saveAndFlush(AppNoticeComment.create(
                    notice, memberId, "회원", "댓글", false, null, null, null
            ));
            commentId = comment.getId();
        }
        if (withLike) {
            appNoticeLikeRepository.saveAndFlush(AppNoticeLike.create(notice, memberId));
        }
        return new Fixture(memberId, notice.getId(), commentId);
    }

    private Fixture fixtureWithNoticeAndCommentLikes(String suffix) {
        String memberId = "member-" + suffix;
        Member member = Member.create(
                memberId,
                memberId + "@sungkyul.ac.kr",
                "회원",
                LocalDateTime.now()
        );
        memberRepository.saveAndFlush(member);

        AppNotice notice = AppNotice.create(
                "앱 공지",
                "내용",
                AppNoticeCategory.GENERAL,
                AppNoticePriority.NORMAL,
                List.of(),
                null,
                LocalDateTime.now().minusMinutes(1)
        );
        notice.increaseCommentCount(1);
        notice.increaseLikeCount(1);
        notice = appNoticeRepository.saveAndFlush(notice);

        AppNoticeComment comment = appNoticeCommentRepository.saveAndFlush(AppNoticeComment.create(
                notice, "author-" + suffix, "작성자", "댓글", false, null, null, null
        ));
        appNoticeLikeRepository.saveAndFlush(AppNoticeLike.create(notice, memberId));
        appNoticeCommentLikeRepository.saveAndFlush(AppNoticeCommentLike.create(comment, memberId));
        return new Fixture(memberId, notice.getId(), comment.getId());
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private record Fixture(String memberId, String noticeId, String commentId) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AppNoticeRepositoryLockAspectConfiguration {

        @Bean
        AppNoticeRepositoryLockBarrier appNoticeRepositoryLockBarrier() {
            return new AppNoticeRepositoryLockBarrier();
        }

        @Bean
        AppNoticeRepositoryLockAspect appNoticeRepositoryLockAspect(
                AppNoticeRepositoryLockBarrier appNoticeRepositoryLockBarrier
        ) {
            return new AppNoticeRepositoryLockAspect(appNoticeRepositoryLockBarrier);
        }
    }

    @Aspect
    static class AppNoticeRepositoryLockAspect {

        private final AppNoticeRepositoryLockBarrier barrier;

        AppNoticeRepositoryLockAspect(AppNoticeRepositoryLockBarrier barrier) {
            this.barrier = barrier;
        }

        @Around("execution(* com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository.findByIdForUpdate(..)) && args(appNoticeId)")
        Object lock(ProceedingJoinPoint joinPoint, String appNoticeId) throws Throwable {
            barrier.awaitIfDelayed(appNoticeId);
            return joinPoint.proceed();
        }
    }

    static class AppNoticeRepositoryLockBarrier {

        private final AtomicBoolean armed = new AtomicBoolean(false);
        private volatile String appNoticeId;
        private volatile CountDownLatch attempted;
        private volatile CountDownLatch proceed;

        void delayNextLock(String appNoticeId, CountDownLatch attempted, CountDownLatch proceed) {
            this.appNoticeId = appNoticeId;
            this.attempted = attempted;
            this.proceed = proceed;
            armed.set(true);
        }

        void awaitIfDelayed(String requestedAppNoticeId) {
            if (requestedAppNoticeId.equals(appNoticeId) && armed.compareAndSet(true, false)) {
                attempted.countDown();
                await(proceed);
            }
        }
    }

    @Service
    static class ConcurrentCommands {

        private final MemberRepository memberRepository;
        private final AppNoticeRepository appNoticeRepository;
        private final AppNoticeService appNoticeService;

        ConcurrentCommands(
                MemberRepository memberRepository,
                AppNoticeRepository appNoticeRepository,
                AppNoticeService appNoticeService
        ) {
            this.memberRepository = memberRepository;
            this.appNoticeRepository = appNoticeRepository;
            this.appNoticeService = appNoticeService;
        }

        @Transactional
        public void deleteCommentAfterPause(
                String memberId,
                String commentId,
                CountDownLatch memberLocked,
                CountDownLatch continueMutation
        ) {
            lockMemberAndPause(memberId, memberLocked, continueMutation);
            appNoticeService.deleteComment(memberId, commentId);
        }

        @Transactional
        public void unlikeNoticeAfterPause(
                String memberId,
                String noticeId,
                CountDownLatch memberLocked,
                CountDownLatch continueMutation
        ) {
            lockMemberAndPause(memberId, memberLocked, continueMutation);
            appNoticeService.unlikeNotice(memberId, noticeId);
        }

        @Transactional
        public void withdraw(String memberId) {
            Member member = memberRepository.findActiveByIdForUpdate(memberId).orElseThrow();
            member.withdraw(LocalDateTime.now());
            appNoticeService.handleMemberWithdrawal(memberId);
        }

        @Transactional
        public void withdrawAfterPause(
                String memberId,
                CountDownLatch memberLocked,
                CountDownLatch continueWithdrawal
        ) {
            Member member = memberRepository.findActiveByIdForUpdate(memberId).orElseThrow();
            member.withdraw(LocalDateTime.now());
            memberLocked.countDown();
            await(continueWithdrawal);
            appNoticeService.handleMemberWithdrawal(memberId);
        }

        @Transactional
        public void withdrawAfterNoticeLockAndPause(
                String memberId,
                String noticeId,
                CountDownLatch noticeLocked,
                CountDownLatch continueWithdrawal
        ) {
            Member member = memberRepository.findActiveByIdForUpdate(memberId).orElseThrow();
            member.withdraw(LocalDateTime.now());
            appNoticeRepository.findByIdForUpdate(noticeId).orElseThrow();
            noticeLocked.countDown();
            await(continueWithdrawal);
            appNoticeService.handleMemberWithdrawal(memberId);
        }

        @Transactional
        public void deleteAppNoticeAfterLockAndPause(
                String noticeId,
                CountDownLatch noticeLocked,
                CountDownLatch continueDeletion
        ) {
            appNoticeRepository.findByIdForUpdate(noticeId).orElseThrow();
            noticeLocked.countDown();
            await(continueDeletion);
            appNoticeService.deleteAppNotice(noticeId);
        }

        private void lockMemberAndPause(
                String memberId,
                CountDownLatch memberLocked,
                CountDownLatch continueMutation
        ) {
            memberRepository.findActiveByIdForUpdate(memberId).orElseThrow();
            memberLocked.countDown();
            await(continueMutation);
        }
    }
}
