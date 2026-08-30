package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberTermsConsent;
import com.skuri.skuri_backend.domain.member.entity.MemberTermsConsentSource;
import com.skuri.skuri_backend.domain.member.policy.TermsConsentPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
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

    private static final LocalDateTime SIGNUP_ACCEPTED_AT =
            LocalDateTime.of(2026, 8, 30, 9, 0);
    private static final LocalDateTime BACKFILL_AT =
            LocalDateTime.of(2099, 8, 30, 12, 0);

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberTermsConsentRepository memberTermsConsentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        Member member = Member.create(
                "member-1",
                "user@sungkyul.ac.kr",
                "사용자",
                LocalDateTime.of(2026, 8, 1, 0, 0)
        );
        member.updateProfile("가입회원", "20261234", "컴퓨터공학과", null);
        memberRepository.saveAndFlush(member);
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
            assertSingleSignupConsentAt(SIGNUP_ACCEPTED_AT);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void backfill_프로필상태와회원상태에관계없이_기존전체회원에동의를기록한다() {
        Member incompleteMember = Member.create(
                "member-2",
                "incomplete@sungkyul.ac.kr",
                "미완료회원",
                LocalDateTime.of(2026, 8, 2, 0, 0)
        );
        Member withdrawnMember = Member.create(
                "member-3",
                "withdrawn@sungkyul.ac.kr",
                "탈퇴회원",
                LocalDateTime.of(2026, 8, 3, 0, 0)
        );
        withdrawnMember.withdraw(LocalDateTime.of(2026, 8, 20, 0, 0));
        memberRepository.saveAllAndFlush(java.util.List.of(incompleteMember, withdrawnMember));

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                memberTermsConsentRepository.backfillAllMemberSignupConsents(
                        TermsConsentPolicy.CURRENT_VERSION,
                        BACKFILL_AT
                )
        );

        assertThat(memberTermsConsentRepository.findAll())
                .hasSize(3)
                .allSatisfy(consent -> {
                    assertThat(consent.getSource()).isEqualTo(MemberTermsConsentSource.SIGNUP);
                    assertThat(consent.getAcceptedAt()).isEqualTo(BACKFILL_AT);
                });
    }

    @Test
    void backfill_기준시각후생성된회원은_대상에서제외한다() {
        Member laterMember = Member.create(
                "member-4",
                "later@sungkyul.ac.kr",
                "후속회원",
                BACKFILL_AT.plusSeconds(1)
        );
        memberRepository.saveAndFlush(laterMember);
        jdbcTemplate.update(
                "update members set created_at = ? where id = ?",
                Timestamp.valueOf(BACKFILL_AT.plusSeconds(1)),
                laterMember.getId()
        );

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                memberTermsConsentRepository.backfillAllMemberSignupConsents(
                        TermsConsentPolicy.CURRENT_VERSION,
                        BACKFILL_AT
                )
        );

        assertThat(memberTermsConsentRepository.count()).isEqualTo(1);
        assertThat(memberTermsConsentRepository.existsByMember_IdAndTermsVersion(
                laterMember.getId(),
                TermsConsentPolicy.CURRENT_VERSION
        )).isFalse();
    }

    @Test
    void 가입완료와백필을_별도트랜잭션에서동시저장해도_가입완료시각한건만보존한다() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> signup = executor.submit(() -> insertSignupConsent(ready, start));
            Future<?> backfill = executor.submit(() -> backfillSignupConsent(ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            signup.get(5, TimeUnit.SECONDS);
            backfill.get(5, TimeUnit.SECONDS);

            assertThat(memberTermsConsentRepository.count()).isEqualTo(1);
            assertSingleSignupConsentAt(SIGNUP_ACCEPTED_AT);
        } finally {
            executor.shutdownNow();
        }
    }

    private void insertSignupConsent(CountDownLatch ready, CountDownLatch start) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ready.countDown();
            await(start);
            memberTermsConsentRepository.upsertSignupConsent(
                    UUID.randomUUID().toString(),
                    "member-1",
                    TermsConsentPolicy.CURRENT_VERSION,
                    SIGNUP_ACCEPTED_AT
            );
        });
    }

    private void backfillSignupConsent(CountDownLatch ready, CountDownLatch start) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ready.countDown();
            await(start);
            memberTermsConsentRepository.backfillAllMemberSignupConsents(
                    TermsConsentPolicy.CURRENT_VERSION,
                    BACKFILL_AT
            );
        });
    }

    private void assertSingleSignupConsentAt(LocalDateTime acceptedAt) {
        MemberTermsConsent consent = memberTermsConsentRepository.findAll().getFirst();
        assertThat(consent.getSource()).isEqualTo(MemberTermsConsentSource.SIGNUP);
        assertThat(consent.getAcceptedAt()).isEqualTo(acceptedAt);
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
