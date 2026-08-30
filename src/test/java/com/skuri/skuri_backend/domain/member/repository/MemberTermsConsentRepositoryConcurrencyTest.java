package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.policy.TermsConsentPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:member-terms-consent;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(JpaAuditingConfig.class)
class MemberTermsConsentRepositoryConcurrencyTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberTermsConsentRepository memberTermsConsentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        memberRepository.saveAndFlush(Member.create(
                "member-1",
                "user@sungkyul.ac.kr",
                "사용자",
                TermsConsentPolicy.EMAIL_CONSENT_MEMBER_JOINED_AT_CUTOFF.minusDays(1)
        ));
    }

    @AfterEach
    void tearDown() {
        memberTermsConsentRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void 동일회원의약관동의를_별도트랜잭션에서동시저장해도_한건만저장한다() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> first = executor.submit(() -> insertSignupConsent(ready, start));
            Future<?> second = executor.submit(() -> insertSignupConsent(ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);

            assertThat(memberTermsConsentRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void 가입동의와이메일백필을_별도트랜잭션에서동시저장해도_한건만저장한다() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> signup = executor.submit(() -> insertSignupConsent(ready, start));
            Future<?> backfill = executor.submit(() -> backfillEmailConsent(ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            signup.get(5, TimeUnit.SECONDS);
            backfill.get(5, TimeUnit.SECONDS);

            assertThat(memberTermsConsentRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private void insertSignupConsent(CountDownLatch ready, CountDownLatch start) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ready.countDown();
            await(start);
            memberTermsConsentRepository.insertSignupConsentIfAbsent(
                    UUID.randomUUID().toString(),
                    "member-1",
                    TermsConsentPolicy.CURRENT_VERSION,
                    LocalDateTime.now()
            );
        });
    }

    private void backfillEmailConsent(CountDownLatch ready, CountDownLatch start) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ready.countDown();
            await(start);
            memberTermsConsentRepository.backfillEmailConsents(
                    TermsConsentPolicy.CURRENT_VERSION,
                    TermsConsentPolicy.EMAIL_CONSENT_MEMBER_JOINED_AT_CUTOFF
            );
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
