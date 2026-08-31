package com.skuri.skuri_backend.domain.contentblock.service;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.exception.PostNotFoundException;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.contentblock.dto.request.CreateContentBlockRequest;
import com.skuri.skuri_backend.domain.contentblock.entity.ContentBlockTargetType;
import com.skuri.skuri_backend.domain.contentblock.repository.ContentBlockRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:content-block-withdrawal;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        JpaAuditingConfig.class,
        ContentBlockService.class,
        ContentBlockCreateWithdrawalConcurrencyDataJpaTest.MemberPairLockProbeConfiguration.class
})
class ContentBlockCreateWithdrawalConcurrencyDataJpaTest {

    private static final String BLOCKER_ID = "blocker";
    private static final String TARGET_ID = "target";
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 8, 31, 18, 30);

    @Autowired
    private ContentBlockService contentBlockService;

    @Autowired
    private ContentBlockRepository contentBlockRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MemberPairLockProbe memberPairLockProbe;

    private String postId;

    @BeforeEach
    void setUp() {
        memberRepository.saveAndFlush(member(BLOCKER_ID));
        memberRepository.saveAndFlush(member(TARGET_ID));
        postId = postRepository.saveAndFlush(
                Post.create("제목", "본문", TARGET_ID, "대상", null, false, PostCategory.GENERAL)
        ).getId();
    }

    @AfterEach
    void tearDown() {
        contentBlockRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void 대상탈퇴가회원잠금을선점하면_차단생성은기존POST_NOT_FOUND이고_dangling행이없다() throws Exception {
        CountDownLatch targetWithdrawnAndLocked = new CountDownLatch(1);
        CountDownLatch allowWithdrawalCommit = new CountDownLatch(1);
        CountDownLatch createMemberPairLockAttempted = new CountDownLatch(1);
        memberPairLockProbe.arm(createMemberPairLockAttempted);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> withdrawal = executor.submit(
                    () -> withdrawTargetAfterPause(targetWithdrawnAndLocked, allowWithdrawalCommit)
            );
            assertThat(targetWithdrawnAndLocked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> creation = executor.submit(() -> contentBlockService.create(
                    BLOCKER_ID,
                    new CreateContentBlockRequest(ContentBlockTargetType.POST, postId)
            ));
            assertThat(createMemberPairLockAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> creation.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            allowWithdrawalCommit.countDown();
            withdrawal.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> creation.get(5, TimeUnit.SECONDS))
                    .hasRootCauseInstanceOf(PostNotFoundException.class);
        } finally {
            allowWithdrawalCommit.countDown();
            executor.shutdownNow();
        }

        assertThat(contentBlockRepository.findByBlockerIdAndBlockedId(BLOCKER_ID, TARGET_ID)).isEmpty();
        assertThat(memberRepository.findById(TARGET_ID)).get()
                .extracting(Member::getStatus)
                .isEqualTo(MemberStatus.WITHDRAWN);
    }

    private void withdrawTargetAfterPause(CountDownLatch locked, CountDownLatch proceed) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Member target = memberRepository.findActiveByIdForUpdate(TARGET_ID).orElseThrow();
            target.withdraw(OCCURRED_AT);
            memberRepository.saveAndFlush(target);
            locked.countDown();
            await(proceed);
        });
    }

    private Member member(String id) {
        return Member.create(id, id + "@sungkyul.ac.kr", "회원", OCCURRED_AT.minusDays(1));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrency test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class MemberPairLockProbeConfiguration {

        @Bean
        MemberPairLockProbe memberPairLockProbe() {
            return new MemberPairLockProbe();
        }

        @Bean
        MemberPairLockProbeAspect memberPairLockProbeAspect(MemberPairLockProbe memberPairLockProbe) {
            return new MemberPairLockProbeAspect(memberPairLockProbe);
        }
    }

    @Aspect
    static class MemberPairLockProbeAspect {

        private final MemberPairLockProbe memberPairLockProbe;

        MemberPairLockProbeAspect(MemberPairLockProbe memberPairLockProbe) {
            this.memberPairLockProbe = memberPairLockProbe;
        }

        @Around("execution(* com.skuri.skuri_backend.domain.member.repository.MemberRepository.findAllActiveByIdInForUpdateOrdered(..))")
        Object markAttempt(ProceedingJoinPoint joinPoint) throws Throwable {
            memberPairLockProbe.markAttempt();
            return joinPoint.proceed();
        }
    }

    static class MemberPairLockProbe {

        private final AtomicBoolean armed = new AtomicBoolean(false);
        private volatile CountDownLatch attempted;

        void arm(CountDownLatch attempted) {
            this.attempted = attempted;
            armed.set(true);
        }

        void markAttempt() {
            if (armed.compareAndSet(true, false)) {
                attempted.countDown();
            }
        }
    }
}
