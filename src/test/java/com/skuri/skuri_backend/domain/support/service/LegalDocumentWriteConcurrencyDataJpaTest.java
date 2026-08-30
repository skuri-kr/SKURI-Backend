package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.config.JpaAuditingConfig;
import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerIconKey;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerTone;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
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

import java.util.List;
import java.util.Set;
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
        "spring.datasource.url=jdbc:h2:mem:legal-document-write;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(JpaAuditingConfig.class)
class LegalDocumentWriteConcurrencyDataJpaTest {

    private static final String DOCUMENT_KEY = "termsOfUse";
    private static final Set<String> POLICY_SECTION_IDS =
            Set.of("article-11", "article-18", "supplementary-provisions");

    @Autowired
    private LegalDocumentRepository legalDocumentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        legalDocumentRepository.saveAndFlush(document());
    }

    @AfterEach
    void tearDown() {
        legalDocumentRepository.deleteAll();
    }

    @Test
    void 관리자수정중정책마이그레이션은_행잠금해제후최신문서를기준으로갱신한다() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch adminLocked = new CountDownLatch(1);
        CountDownLatch allowAdminCommit = new CountDownLatch(1);
        CountDownLatch migrationAttempted = new CountDownLatch(1);
        CountDownLatch migrationCompleted = new CountDownLatch(1);

        try {
            Future<?> adminUpdate = executor.submit(() -> updateAsAdmin(adminLocked, allowAdminCommit));
            assertThat(adminLocked.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> policyMigration = executor.submit(
                    () -> applyPolicyMigration(migrationAttempted, migrationCompleted)
            );
            assertThat(migrationAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(migrationCompleted.await(200, TimeUnit.MILLISECONDS)).isFalse();

            allowAdminCommit.countDown();
            adminUpdate.get(5, TimeUnit.SECONDS);
            policyMigration.get(5, TimeUnit.SECONDS);

            LegalDocument result = legalDocumentRepository.findById(DOCUMENT_KEY).orElseThrow();
            assertThat(result.getTitle()).isEqualTo("관리자가 수정한 제목");
            assertThat(result.getFooterLines()).containsExactly("관리자가 수정한 하단 문구");
            assertThat(result.isActive()).isFalse();
            assertThat(result.getSections().stream().map(LegalDocumentSection::id)).containsExactly(
                    "article-01",
                    "article-11",
                    "article-18",
                    "supplementary-provisions"
            );
            assertThat(result.getSections().get(0).paragraphs()).containsExactly("관리자가 수정한 본문");
        } finally {
            allowAdminCommit.countDown();
            executor.shutdownNow();
        }
    }

    private void updateAsAdmin(CountDownLatch locked, CountDownLatch proceed) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LegalDocument document = legalDocumentRepository
                    .findByDocumentKeyForUpdate(DOCUMENT_KEY)
                    .orElseThrow();
            locked.countDown();
            await(proceed);
            document.update(
                    "관리자가 수정한 제목",
                    document.getBannerIconKey(),
                    document.getBannerTitle(),
                    document.getBannerTone(),
                    document.getBannerLines(),
                    List.of(
                            section("article-01", "관리자가 수정한 본문"),
                            section("supplementary-provisions", "기존 부칙")
                    ),
                    List.of("관리자가 수정한 하단 문구"),
                    false
            );
            legalDocumentRepository.saveAndFlush(document);
        });
    }

    private void applyPolicyMigration(CountDownLatch attempted, CountDownLatch completed) {
        attempted.countDown();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LegalDocument document = legalDocumentRepository
                    .findByDocumentKeyForUpdate(DOCUMENT_KEY)
                    .orElseThrow();
            document.update(
                    document.getTitle(),
                    document.getBannerIconKey(),
                    document.getBannerTitle(),
                    document.getBannerTone(),
                    document.getBannerLines(),
                    LegalDocumentAdMobPolicyMigration.replaceSections(
                            document.getSections(),
                            LegalDocumentSeedMigration.termsOfUseSections(),
                            POLICY_SECTION_IDS
                    ),
                    document.getFooterLines(),
                    document.isActive()
            );
            legalDocumentRepository.saveAndFlush(document);
        });
        completed.countDown();
    }

    private LegalDocument document() {
        return LegalDocument.create(
                DOCUMENT_KEY,
                "기존 제목",
                LegalDocumentBannerIconKey.DOCUMENT,
                "기존 배너",
                LegalDocumentBannerTone.GREEN,
                List.of(),
                List.of(
                        section("article-01", "기존 본문"),
                        section("supplementary-provisions", "기존 부칙")
                ),
                List.of("기존 하단 문구"),
                true
        );
    }

    private static LegalDocumentSection section(String id, String paragraph) {
        return new LegalDocumentSection(id, List.of(paragraph), id);
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
