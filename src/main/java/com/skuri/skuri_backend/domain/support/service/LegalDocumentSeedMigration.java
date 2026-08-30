package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.seed.repository.SeedMigrationRepository;
import com.skuri.skuri_backend.domain.support.entity.LegalDocument;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerIconKey;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerLineTone;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerTone;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentKey;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBannerLine;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class LegalDocumentSeedMigration {

    static final String MIGRATION_KEY = "support-legal-documents-initial-seed-20260328";

    private static final Pattern SECTION_HEADING_PATTERN = Pattern.compile("^제\\d+조(?:의\\d+)?\\(.*\\)$");
    private static final Pattern ARTICLE_ID_PATTERN = Pattern.compile("^제(\\d+)조(?:의(\\d+))?.*$");

    private final LegalDocumentRepository legalDocumentRepository;
    private final SeedMigrationRepository seedMigrationRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!acquireMigrationMarker()) {
            return;
        }

        int createdCount = 0;
        createdCount += seedIfAbsent(
                LegalDocumentKey.TERMS_OF_USE,
                "이용약관",
                LegalDocumentBannerIconKey.DOCUMENT,
                "SKURI 이용약관",
                LegalDocumentBannerTone.GREEN,
                List.of(new LegalDocumentBannerLine(
                        "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일",
                        LegalDocumentBannerLineTone.PRIMARY
                )),
                parseSections(TERMS_OF_USE_RAW_TEXT),
                List.of(
                        "본 약관에 대한 문의는",
                        "앱 내 문의하기를 이용해 주세요."
                )
        );
        createdCount += seedIfAbsent(
                LegalDocumentKey.PRIVACY_POLICY,
                "개인정보 처리방침",
                LegalDocumentBannerIconKey.SHIELD,
                "SKURI 개인정보 처리방침",
                LegalDocumentBannerTone.BLUE,
                List.of(
                        new LegalDocumentBannerLine(
                                "SKURI는 이용자의 개인정보를 소중히 보호합니다.",
                                LegalDocumentBannerLineTone.PRIMARY
                        ),
                        new LegalDocumentBannerLine(
                                "시행일: 2026년 8월 30일 · 최종 수정: 2026년 8월 30일",
                                LegalDocumentBannerLineTone.SECONDARY
                        )
                ),
                parseSections(PRIVACY_POLICY_RAW_TEXT),
                List.of(
                        "개인정보 관련 문의는",
                        "앱 내 문의하기를 이용해 주세요."
                )
        );

        if (createdCount > 0) {
            log.info("법적 문서 초기 seed migration 완료: {}건 생성", createdCount);
        } else {
            log.info("법적 문서 초기 seed migration 완료: 기존 데이터 유지");
        }
    }

    private boolean acquireMigrationMarker() {
        boolean acquired = seedMigrationRepository.insertIfAbsent(
                MIGRATION_KEY,
                LocalDateTime.now()
        ) == 1;
        if (!acquired) {
            log.info("법적 문서 초기 seed migration 건너뜀: 이미 다른 인스턴스에서 적용됨");
        }
        return acquired;
    }

    private int seedIfAbsent(
            LegalDocumentKey documentKey,
            String title,
            LegalDocumentBannerIconKey bannerIconKey,
            String bannerTitle,
            LegalDocumentBannerTone bannerTone,
            List<LegalDocumentBannerLine> bannerLines,
            List<LegalDocumentSection> sections,
            List<String> footerLines
    ) {
        if (legalDocumentRepository.existsById(documentKey.value())) {
            return 0;
        }
        legalDocumentRepository.save(LegalDocument.create(
                documentKey.value(),
                title,
                bannerIconKey,
                bannerTitle,
                bannerTone,
                bannerLines,
                sections,
                footerLines,
                true
        ));
        return 1;
    }

    static List<LegalDocumentSection> termsOfUseSections() {
        return parseSections(TERMS_OF_USE_RAW_TEXT);
    }

    static List<LegalDocumentSection> privacyPolicySections() {
        return parseSections(PRIVACY_POLICY_RAW_TEXT);
    }

    private static List<LegalDocumentSection> parseSections(String rawText) {
        List<LegalDocumentSection> sections = new ArrayList<>();
        String currentTitle = null;
        List<String> currentParagraphs = new ArrayList<>();
        int sectionIndex = 0;

        for (String line : rawText.lines().map(String::trim).toList()) {
            if (line.isBlank()) {
                continue;
            }
            if (isSectionHeading(line)) {
                if (currentTitle != null) {
                    sectionIndex++;
                    sections.add(new LegalDocumentSection(sectionId(currentTitle, sectionIndex), List.copyOf(currentParagraphs), currentTitle));
                }
                currentTitle = line;
                currentParagraphs = new ArrayList<>();
                continue;
            }
            currentParagraphs.add(line);
        }

        if (currentTitle != null) {
            sectionIndex++;
            sections.add(new LegalDocumentSection(sectionId(currentTitle, sectionIndex), List.copyOf(currentParagraphs), currentTitle));
        }

        return sections;
    }

    private static boolean isSectionHeading(String line) {
        return "부칙".equals(line) || SECTION_HEADING_PATTERN.matcher(line).matches();
    }

    private static String sectionId(String title, int sectionIndex) {
        if ("부칙".equals(title)) {
            return "supplementary-provisions";
        }
        Matcher matcher = ARTICLE_ID_PATTERN.matcher(title);
        if (!matcher.matches()) {
            return "section-%02d".formatted(sectionIndex);
        }
        String articleNumber = "%02d".formatted(Integer.parseInt(matcher.group(1)));
        String subArticleNumber = matcher.group(2);
        return subArticleNumber == null
                ? "article-" + articleNumber
                : "article-" + articleNumber + "-" + subArticleNumber;
    }

    private static final String TERMS_OF_USE_RAW_TEXT = """
            제1조(목적)

            이 약관은 스쿠리 (이하 '회사' 라고 합니다)가 제공하는 제반 서비스의 이용과 관련하여 회사와 회원과의 권리, 의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.

            이 서비스는 만 14세 이상 이용자만을 대상으로 하며, 만 18세 미만의 이용자는 보호자의 동의 하에만 회원가입이 가능합니다. 회사는 청소년 보호를 위해 유해 정보 차단 등 필요한 조치를 취합니다.

            본 서비스는 개인 개발자가 비영리 목적으로 운영하는 모바일 커뮤니티 앱이며, '회사'라는 용어는 편의상 사용된 것으로, 사업자등록이 없는 개인 운영자를 의미합니다. 회사는 택시 운송의 주선, 알선, 배차 또는 결제 기능을 제공하지 않습니다.

            제2조(정의)

            이 약관에서 사용하는 주요 용어의 정의는 다음과 같습니다.

            '서비스'라 함은 구현되는 단말기(PC, TV, 휴대형단말기 등의 각종 유무선 장치를 포함)와 상관없이 '이용자'가 이용할 수 있는 회사가 제공하는 제반 서비스를 의미합니다.
            '이용자'란 이 약관에 따라 회사가 제공하는 서비스를 받는 '개인회원', '기업회원' 및 '비회원'을 말합니다.
            '개인회원'은 회사에 개인정보를 제공하여 회원등록을 한 사람으로, 회사로부터 지속적으로 정보를 제공받고 '회사'가 제공하는 서비스를 계속적으로 이용할 수 있는 자를 말합니다.
            '기업회원'은 회사에 기업정보 및 개인정보를 제공하여 회원등록을 한 사람으로, 회사로부터 지속적으로 정보를 제공받고 회사가 제공하는 서비스를 계속적으로 이용할 수 있는 자를 말합니다.
            '비회원'은 회원가입 없이 회사가 제공하는 서비스를 이용하는 자를 말합니다.
            '아이디(ID)'라 함은 회원의 식별과 서비스이용을 위하여 회원이 정하고 회사가 승인하는 문자 또는 문자와 숫자의 조합을 의미합니다.
            '비밀번호'라 함은 회원이 부여받은 아이디와 일치되는 회원임을 확인하고 비밀의 보호를 위해 회원 자신이 정한 문자(특수문자 포함)와 숫자의 조합을 의미합니다.

            제3조(약관 외 준칙)

            이 약관에서 정하지 아니한 사항에 대해서는 법령 또는 회사가 정한 서비스의 개별약관, 운영정책 및 칙 등(이하 세부지침)의 규정에 따릅니다. 또한 본 약관과 세부지침이 충돌할 경우에는 세부지침에 따릅니다.

            제4조(약관의 효력과 변경)

            이 약관은 스쿠리(이)가 제공하는 모든 인터넷서비스에 게시하여 공시합니다. '회사'는 관계법령에 위배되지 않는 범위 내에서 이 약관을 변경할 수 있습니다. 회사는 약관이 변경되는 경우 변경된 약관의 내용과 시행일을 정하여, 그 시행일로부터 최소 7일(이용자에게 불리하거나 중대한 변경은 30일) 이전부터 공지합니다. 기존 이용자에게는 별도의 전자적 수단으로 통지할 수 있습니다. 변경된 약관은 공지 또는 통지한 시행일로부터 효력이 발생합니다.

            제5조(이용자에 대한 통지)

            회사는 이 약관에 별도 규정이 없는 한 전자적 수단을 이용하여 통지할 수 있습니다. 전체 통지는 게시판 공지로 갈음할 수 있습니다.

            제6조(이용계약의 체결)

            이용계약은 회원가입 동의 및 승낙 시 체결됩니다. 일부 무료 서비스는 동의 절차 진행 시 체결될 수 있습니다.

            제7조(회원가입에 대한 승낙)

            회사는 원칙적으로 이용계약 요청을 승낙합니다. 다만 법령·운영상 제한 사유가 있는 경우 보류 또는 거절할 수 있습니다.

            제8조(회원정보의 변경)

            회원은 개인정보관리 화면에서 정보를 열람·수정할 수 있습니다. 변경사항 미통지로 인한 불이익은 회원 책임입니다.

            제9조(회원정보의 관리 및 보호)

            아이디/비밀번호 관리책임은 회원에게 있으며, 도용 또는 제3자 사용 인지 시 즉시 통지해야 합니다.

            제10조(회사의 의무)

            회사는 안정적 서비스 제공을 위해 노력하며, 불가피한 중지 시 사후 공지합니다.

            제11조(개인정보보호)

            회사는 관련 법규 및 개인정보처리방침을 적용합니다. 외부 링크 페이지에는 회사의 방침이 적용되지 않습니다.

            회사는 서비스 제공을 위하여 이메일, 이름, 학교명, 기기식별정보 등을 처리할 수 있습니다. 위치정보는 이용자의 단말기 내에서 가까운 파티 정렬 등 기능에만 일시적으로 활용되며 회사 서버에 저장되거나 다른 이용자에게 제공되지 않습니다.

            회사는 서비스 운영을 위해 Google LLC 및 그 계열사가 제공하는 Google AdMob을 이용합니다. 회원은 가입 과정에서 본 약관에 동의함으로써 다음 광고 관련 개인정보 처리와 국외이전에 동의합니다.

            가. 이전 항목: IP 주소로부터 추정되는 일반적 위치, 앱 정보, 광고 식별자(ADID/IDFA) 및 그 밖의 기기 식별자, 앱·광고 상호작용과 광고 노출 정보, 오류·진단·성능 정보
            나. 이전받는 자: Google LLC 및 Google 계열사(문의: https://support.google.com/policies/contact/general_privacy_form)
            다. 이전 국가: 미국, 싱가포르, 일본, 대만, 칠레, 우루과이, 아일랜드, 네덜란드, 벨기에, 덴마크, 핀란드, 독일 등 Google이 데이터센터를 운영하는 국가
            라. 이전 시점 및 방법: Google Mobile Ads SDK가 초기화되거나 광고를 요청·표시하는 때 암호화된 네트워크 통신으로 이전
            마. 이용 목적: 광고 제공 및 개인 최적화, 광고 효과 측정·분석, 서비스 개선, 보안과 부정 이용 방지
            바. 보유·이용기간: Google 개인정보처리방침 및 데이터 보관 정책에 따르며, 광고 서버 로그의 IP 주소 일부는 9개월 후, 쿠키 정보는 18개월 후 익명화될 수 있습니다. 법적 의무, 보안 및 부정 이용 방지를 위해 필요한 정보는 더 오래 보관될 수 있습니다.

            실제 처리 범위는 이용자의 기기 설정, 지역별 동의 선택, Google 정책 및 SDK 버전에 따라 달라질 수 있습니다. iOS 광고 추적 허용 여부는 운영체제의 개인정보 보호 및 보안 설정에서 별도로 관리됩니다.

            제12조(이용자의 의무)

            이용자는 사실에 근거하여 가입하고, 회사의 규정과 공지사항을 준수해야 합니다.

            이용자는 회사의 명시적 동의 없이 서비스 이용 권한 등을 처분할 수 없습니다.

            이용자는 택시 동승 파티 이용 시 다음 사항을 준수해야 합니다.

            가. 다른 이용자에 대한 존중과 배려를 바탕으로 한 원만한 소통
            나. 동승 약속의 성실한 이행 및 사전 통지 없는 불참 방지
            다. 요금 분담에 대한 명확한 합의 및 성실한 정산
            라. 안전한 동승을 위한 기본적인 주의 의무 준수

            제13조(서비스의 제공)

            회사의 서비스는 원칙적으로 24시간 제공됩니다. 개별 서비스 안내는 화면에서 확인합니다.

            서비스 내용:

            가. 택시 동승자 찾기 서비스
            나. 학교 공지 알림
            다. 시간표/학사일정 관리
            라. 커뮤니티

            제13조의2(택시 동승 서비스의 성격 및 책임의 한계)

            본 앱이 제공하는 택시 동승자 찾기 서비스는 택시 운송의 주선, 알선, 중개 또는 배차 서비스가 아닙니다. 본 서비스는 이용자들이 자율적으로 동행자를 찾을 수 있도록 정보를 제공하는 커뮤니티 서비스입니다.

            이용자는 본 서비스를 이용함에 있어 다음 사항을 이해하고 동의합니다.

            가. 본 서비스는 택시 운송 주선·알선 서비스가 아니며, 회사는 택시 기사와 이용자를 연결하거나 배차하는 기능을 제공하지 않습니다.
            나. 택시 호출, 차량 배차, 요금 결제 및 정산은 이용자가 직접 외부 앱(카카오T, 티머니GO, 토스 등)을 통해 진행하며, 회사는 이에 대한 책임을 부담하지 않습니다.
            다. 택시 동승 파티 내에서 발생하는 모든 법적 문제(사고, 분쟁, 계약 불이행, 손해배상 등)에 대하여 회사는 어떠한 책임도 부담하지 않으며, 이는 이용자 간 또는 이용자와 제3자(택시 기사, 택시 회사 등) 간의 문제입니다.
            라. 이용자는 본 서비스가 운송 주선·알선 서비스가 아님을 이해하고, 동승에 따른 모든 위험과 책임을 본인이 부담함에 동의합니다.
            마. 회사는 이용자 간의 동승 계약, 요금 분담, 안전, 사고 등에 대한 중재, 보증 또는 책임을 지지 않습니다.
            바. 이용자는 동승 파티 참여 시 본인의 안전에 대한 책임을 지며, 다른 이용자와의 동승 계약은 이용자 간 자율적으로 체결됩니다.
            사. 회사는 동승 파티의 성사 여부, 동승의 안전성, 요금 분담의 공정성 등에 대하여 보증하거나 책임지지 않습니다.

            제14조(서비스의 제한 등)

            부득이한 사유로 서비스 제한 또는 중지될 수 있으며, 사전 또는 사후 공지합니다.

            제15조(서비스의 해제·해지 및 탈퇴 절차)

            언제든지 탈퇴 신청이 가능하며, 일부 제한이 있을 수 있습니다.

            이용자가 택시 동승 파티에 참여 중인 경우, 파티 종료 후 탈퇴를 권장합니다. 파티 진행 중 탈퇴로 인해 발생하는 문제에 대하여 회사는 책임을 지지 않습니다.

            제16조(손해배상)

            상대방의 귀책으로 손해 발생 시 손해배상 청구가 가능합니다. 다만 공 중단 등 일정 사유는 면책됩니다.

            제17조(면책사항)

            불가항력, 이용자 귀책, 기대이익 미실현 등에 대하여 회사는 책임지지 않습니다.

            이용자 게시물 내용의 신뢰도·정확성 등에 대한 책임은 게시자에게 있으며, 회사는 분쟁에 개입하지 않습니다.

            회사는 택시 동승 파티 내에서 발생하는 모든 사고, 분쟁, 계약 불이행, 요금 분담 문제, 안전 사고 등에 대하여 어떠한 책임도 부담하지 않습니다. 택시 호출, 차량 배차, 요금 결제 및 정산은 이용자가 직접 외부 앱을 통해 진행하며, 이 과정에서 발생하는 모든 문제에 대한 책임은 이용자 본인에게 있습니다.

            회사는 외부 앱(카카오T, 티머니GO, 토스 등)의 서비스 품질, 안전성, 정확성 등에 대하여 책임을 지지 않으며, 외부 앱 이용으로 인한 손해에 대하여도 책임을 지지 않습니다.

            제18조(정보의 제공 및 광고의 게재)

            회사는 서비스 운영을 위해 Google AdMob 등 제3자 광고 네트워크를 통한 배너 광고를 게재할 수 있습니다. 본 조의 광고 게재는 전자우편·푸시 알림 등 광고성 정보의 전송과 구분됩니다.

            EEA·영국·스위스 및 관련 미국 주 등 별도 개인정보 선택권이 필요한 지역에서는 Google의 개인정보 메시지를 제공하며, 해당 지역 이용자는 앱 설정에 표시되는 광고 개인정보 설정에서 선택을 다시 확인하거나 변경할 수 있습니다. 회사는 광고 목적의 정밀 위치정보를 직접 수집하거나 저장하지 않습니다.

            제19조(권리의 귀속)

            서비스 관련 지식재산권은 회사에 귀속됩니다. 이용자에게는 사용 권한만 부여됩니다.

            제20조(콘텐츠의 관리)

            불법·유해 콘텐츠는 관련 법에 따라 게시중단·삭제될 수 있습니다. 신고 시 24시간 내 조치합니다.

            제21조(콘텐츠의 저작권)

            게시물의 저작권은 저작자에게 귀속되며, 회사는 서비스 운영 범위 내 사용이 가능합니다.

            제22조(관할법원 및 준거법)

            분쟁의 관할법원은 회사 소재지 관할법원이며, 준거법은 대한민국 법령입니다.

            택시 동승 파티 관련 분쟁은 이용자 간 자율적으로 해결하여야 하며, 회사는 분쟁 조정에 개입하지 않습니다.

            제23조(최종 사용자 라이선스 동의)

            본 약관은 각 앱마켓의 표준 EULA를 포함하며, 명시되지 않은 사항은 해당 플랫폼 EULA를 따릅니다.

            부칙

            제1조(시행일) 본 약관은 2026.08.30.부터 시행됩니다.
            """;

    private static final String PRIVACY_POLICY_RAW_TEXT = """
            제1조(목적)

            스쿠리(이하 '회사'라고 함)는 회사가 제공하고자 하는 서비스(이하 '회사 서비스')를 이용하는 개인(이하 '이용자' 또는 '개인')의 정보(이하 '개인정보')를 보호하기 위해, 개인정보보호법, 정보통신망 이용촉진 및 정보보호 등에 관한 법률(이하 '정보통신망법') 등 관련 법령을 준수하고, 서비스 이용자의 개인정보 보호 관련한 고충을 신속하고 원활하게 처리할 수 있도록 하기 위하여 다음과 같이 개인정보처리방침(이하 '본 방침')을 수립합니다.

            제2조(개인정보 처리의 원칙)

            개인정보 관련 법령 및 본 방침에 따라 회사는 이용자의 개인정보를 수집할 수 있으며 수집된 개인정보는 개인의 동의가 있는 경우에 한해 제3자에게 제공될 수 있습니다. 단, 법령의 규정 등에 의해 적법하게 강제되는 경우 회사는 수집한 이용자의 개인정보를 사전에 개인의 동의 없이 제3자에게 제공할 수도 있습니다.

            제3조(본 방침의 공개)

            회사는 이용자가 언제든지 쉽게 본 방침을 확인할 수 있도록 회사 홈페이지 첫 화면 또는 첫 화면과의 연결 화면을 통해 본 방침을 공개하고 있습니다.

            회사는 제1항에 따라 본 방침을 공개하는 경우 글자 크기, 색상 등을 활용하여 이용자가 본 방침을 쉽게 확인할 수 있도록 합니다.

            제4조(본 방침의 변경)

            본 방침은 개인정보 관련 법령, 지침, 고시 또는 정부나 회사 서비스의 정책이나 내용의 변경에 따라 개정될 수 있습니다.

            회사는 제1항에 따라 본 방침을 개정하는 경우 다음 각 호 하나 이상의 방법으로 공지합니다.

            가. 회사가 운영하는 인터넷 홈페이지의 첫 화면의 공지사항란 또는 별도의 창을 통하여 공지하는 방법
            나. 서면·모사전송·전자우편 또는 이와 비슷한 방법으로 이용자에게 공지하는 방법
            회사는 제2항의 공지는 본 방침 개정의 시행일로부터 최소 7일 이전에 공지합니다. 다만, 이용자 권리의 중요한 변경이 있을 경우에는 최소 30일 전에 공지합니다.

            제5조(회원 가입을 위한 정보)

            회사는 이용자의 회사 서비스에 대한 회원가입을 위하여 다음과 같은 정보를 수집합니다.

            필수 수집 정보: 이메일 주소, 닉네임, 학번

            선택 수집 정보: 이름

            제6조(본인 인증을 위한 정보)

            회사는 이용자의 본인인증을 위하여 다음과 같은 정보를 수집합니다.

            필수 수집 정보: 이메일 주소, 이름 및 본인확인값(CI,DI), 학번

            제7조(회사 서비스 제공을 위한 정보)

            회사는 이용자에게 회사의 서비스를 제공하기 위하여 다음과 같은 정보를 수집합니다.

            필수 수집 정보: 이메일 주소, 학번, 닉네임

            선택 수집 정보: 위치 정보, 계좌번호, 계좌 예금주명, 계좌 은행명

            다만, 위치 정보는 단말기 내에서 주변 파티 추천 및 지도 편의 기능을 위해 일시적으로만 활용되며, 회사 서버에 저장되지 않습니다. 회사는 개인위치정보를 제3자(다른 이용자 포함)에게 제공하지 않습니다.

            제8조(서비스 이용 및 부정 이용 확인을 위한 정보)

            회사는 이용자의 서비스 이용에 따른 통계 분석 및 부정이용의 확인 분석을 위하여 다음과 같은 정보를 수집합니다. (부정이용이란 회원탈퇴 후 재가입, 상품구매 후 구매취소 등을 반복적으로 행하는 등 회사가 제공하는 할인쿠폰, 이벤트 혜택 등의 경제상 이익을 불·편법적으로 수취하는 행위, 이용약관 등에서 금지하고 있는 행위, 명의도용 등의 불·편법행위 등을 말합니다.)

            필수 수집 정보: 서비스 이용기록 및 기기정보

            제9조(개인정보 수집 방법)

            회사는 다음과 같은 방법으로 이용자의 개인정보를 수집합니다.

            이용자가 회사의 홈페이지에 자신의 개인정보를 입력하는 방식
            어플리케이션 등 회사가 제공하는 홈페이지 외의 서비스를 통해 이용자가 자신의 개인정보를 입력하는 방식

            제10조(개인정보의 이용)

            회사는 개인정보를 다음 각 호의 경우에 이용합니다.

            공지사항의 전달 등 회사운영에 필요한 경우
            이용문의에 대한 회신, 불만의 처리 등 이용자에 대한 서비스 개선을 위한 경우
            회사의 서비스를 제공하기 위한 경우
            법령 및 회사 약관을 위반하는 회원에 대한 이용 제한 조치, 부정 이용 행위를 포함하여 서비스의 원활한 운영에 지장을 주는 행위에 대한 방지 및 제재를 위한 경우
            신규 서비스 개발을 위한 경우
            이벤트 및 행사 안내 등 마케팅을 위한 경우
            인구통계학적 분석, 서비스 방문 및 이용기록의 분석을 위한 경우
            개인정보 및 관심에 기반한 이용자간 관계의 형성을 위한 경우

            제11조(개인정보의 보유 및 이용기간)

            회사는 이용자의 개인정보에 대해 개인정보의 수집·이용 목적 달성을 위한 기간 동안 개인정보를 보유 및 이용합니다.

            전항에도 불구하고 회사는 내부 방침에 의해 서비스 부정이용기록은 부정 가입 및 이용 방지를 위하여 회원 탈퇴 시점으로부터 최대 1년간 보관합니다.

            제12조(법령에 따른 개인정보의 보유 및 이용기간)

            회사는 관계법령에 따라 다음과 같이 개인정보를 보유 및 이용합니다.

            전자상거래 등에서의 소비자보호에 관한 법률에 따른 보유정보 및 보유기간
            가. 계약 또는 청약철회 등에 관한 기록 : 5년
            나. 대금결제 및 재화 등의 공급에 관한 기록 : 5년
            다. 소비자의 불만 또는 분쟁처리에 관한 기록 : 3년
            라. 표시·광고에 관한 기록 : 6개월
            통신비밀보호법에 따른 보유정보 및 보유기간
            가. 웹사이트 로그 기록 자료 : 3개월
            전자금융거래법에 따른 보유정보 및 보유기간
            가. 전자금융거래에 관한 기록 : 5년

            제13조(개인정보의 파기원칙)

            회사는 원칙적으로 이용자의 개인정보 처리 목적의 달성, 보유·이용기간의 경과 등 개인정보가 필요하지 않을 경우에는 해당 정보를 지체 없이 파기합니다.

            제14조(개인정보파기절차)

            이용자가 회원가입 등을 위해 입력한 개인정보 처리 목적이 달성된 후 별도의 DB로 옮겨져(종이의 경우 별도의 서류함) 내부 방침 및 기타 관련 법령에 의한 정보보호 사유에 따라(보유 및 이용기간 참조) 일정 기간 저장된 후 파기되어집니다.

            회사는 파기 사유가 발생한 개인정보를 개인정보보호 책임자의 승인절차를 거쳐 파기합니다.

            제15조(개인정보파기방법)

            회사는 전자적 파일형태로 저장된 개인정보는 기록을 재생할 수 없는 기술적 방법을 사용하여 삭제하며, 종이로 출력된 개인정보는 분쇄기로 분쇄하거나 소각 등을 통하여 파기합니다.

            제16조(광고성 정보의 전송 조치)

            회사는 전자적 전송매체를 이용하여 영리목적의 광고성 정보를 전송하는 경우 이용자의 명시적인 사전동의를 받습니다. 다만, 다음 각호 어느 하나에 해당하는 경우에는 사전 동의를 받지 않습니다.

            가. 회사가 재화 등의 거래관계를 통하여 수신자로부터 직접 연락처를 수집한 경우, 거래가 종료된 날로부터 6개월 이내에 회사가 처리하고 수신자와 거래한 것과 동종의 재화 등에 대한 영리목적의 광고성 정보를 전송하려는 경우
            나. 「방문판매 등에 관한 법률」에 따른 전화권유판매자가 육성으로 수신자에게 개인정보의 수집출처를 고지하고 전화권유를 하는 경우
            회사는 전항에도 불구하고 수신자가 수신거부의사를 표시하거나 사전 동의를 철회한 경우에는 영리목적의 광고성 정보를 전송하지 않으며 수신거부 및 수신동의 철회에 대한 처리 결과를 알립니다.

            회사는 오후 9시부터 그다음 날 오전 8시까지의 시간에 전자적 전송매체를 이용하여 영리목적의 광고성 정보를 전송하는 경우에는 제1항에도 불구하고 그 수신자로부터 별도의 사전 동의를 받습니다.

            회사는 전자적 전송매체를 이용하여 영리목적의 광고성 정보를 전송하는 경우 다음의 사항 등을 광고성 정보에 구체적으로 밝힙니다.

            가. 회사명 및 연락처
            나. 수신 거부 또는 수신 동의의 철회 의사표시에 관한 사항의 표시
            회사는 전자적 전송매체를 이용하여 영리목적의 광고성 정보를 전송하는 경우 다음 각 호의 어느 하나에 해당하는 조치를 하지 않습니다.

            가. 광고성 정보 수신자의 수신거부 또는 수신동의의 철회를 회피·방해하는 조치
            나. 숫자·부호 또는 문자를 조합하여 전화번호·전자우편주소 등 수신자의 연락처를 자동으로 만들어 내는 조치
            다. 영리목적의 광고성 정보를 전송할 목적으로 전화번호 또는 전자우편주소를 자동으로 등록하는 조치
            라. 광고성 정보 전송자의 신원이나 광고 전송 출처를 감추기 위한 각종 조치
            마. 영리목적의 광고성 정보를 전송할 목적으로 수신자를 기망하여 회신을 유도하는 각종 조치

            제17조(아동의 개인정보보호)

            회사는 만 14세 미만 아동의 개인정보 보호를 위하여 만 14세 이상의 이용자에 한하여 회원가입을 허용합니다.

            제1항에도 불구하고 회사는 이용자가 만 14세 미만의 아동일 경우에는, 그 아동의 법정대리인으로부터 그 아동의 개인정보 수집, 이용, 제공 등의 동의를 그 아동의 법정대리인으로부터 받습니다.

            제2항의 경우 회사는 그 법정대리인의 이름, 생년월일, 성별, 중복가입확인정보(ID), 휴대폰 번호 등을 추가로 수집합니다.

            제18조(이용자의 의무)

            이용자는 자신의 개인정보를 최신의 상태로 유지해야 하며, 이용자의 부정확한 정보 입력으로 발생하는 문제의 책임은 이용자 자신에게 있습니다.

            타인의 개인정보를 도용한 회원가입의 경우 이용자 자격을 상실하거나 관련 개인정보보호 법령에 의해 처벌받을 수 있습니다.

            이용자는 전자우편주소, 비밀번호 등에 대한 보안을 유지할 책임이 있으며 제3자에게 이를 양도하거나 대여할 수 없습니다.

            제19조(개인정보 자동 수집 장치의 설치·운영 및 거부에 관한 사항)

            회사는 이용자에게 개별적인 맞춤서비스를 제공하기 위해 이용 정보를 저장하고 수시로 불러오는 개인정보 자동 수집장치(이하 '쿠키')를 사용합니다. 쿠키는 웹사이트를 운영하는데 이용되는 서버(http)가 이용자의 웹브라우저(PC 및 모바일을 포함)에게 보내는 소량의 정보이며 이용자의 저장공간에 저장되기도 합니다.

            이용자는 쿠키 설치에 대한 선택권을 가지고 있습니다. 따라서 이용자는 웹브라우저에서 옵션을 설정함으로써 쿠키를 허용하거나, 쿠키가 저장될 확인을 거치거나, 아니면 모든 쿠키의 거부할 수도 있습니다.

            다만, 쿠키의 저장을 거부할 경우에는 로그인이 필요한 회사의 일부 서비스는 이용에 어려움이 있을 수 있습니다.

            제20조(쿠키 설치 허용 지정 방법)

            웹브라우저 옵션 설정을 통해 쿠키 허용, 쿠키 차단 등의 설정을 할 수 있습니다.

            Edge: 웹브라우저 우측 상단의 설정 메뉴 > 쿠키 및 사이트 권한 > 쿠키 및 사이트 데이터 관리 및 삭제
            Chrome: 웹브라우저 우측 상단의 설정 메뉴 > 개인정보 및 보안 > 쿠키 및 기타 사이트 데이터
            Whale: 웹브라우저 우측 상단의 설정 메뉴 > 개인정보 보호 > 쿠키 및 기타 사이트 데이터

            제21조(회사의 개인정보 보호 책임자 지정)

            회사는 이용자의 개인정보를 보호하고 개인정보와 관련한 불만을 처리하기 위하여 아래와 같이 관련 부서 및 개인정보 보호 책임자를 지정하고 있습니다.

            개인정보 보호 책임자
            1) 성명: Louis Chun
            2) 직책: CTO
            3) 이메일: skuri2025@gmail.com

            제22조(권익침해에 대한 구제방법)

            정보주체는 개인정보침해로 인한 구제를 받기 위하여 개인정보분쟁조정위원회, 한국인터넷진흥원 개인정보침해신고센터 등에 분쟁해결이나 상담 등을 신청할 수 있습니다. 이 밖에 기타 개인정보침해의 신고, 상담에 대하여는 아래의 기관에 문의하시기 바랍니다.

            가. 개인정보분쟁조정위원회 : (국번없이) 1833-6972 (www.kopico.go.kr)
            나. 개인정보침해신고센터 : (국번없이) 118 (privacy.kisa.or.kr)
            다. 대검찰청 : (국번없이) 1301 (www.spo.go.kr)
            라. 경찰청 : (국번없이) 182 (ecrm.cyber.go.kr)
            회사는 정보주체의 개인정보자기결정권을 보장하고, 개인정보침해로 인한 상담 및 피해 구제를 위해 노력하고 있으며, 신고나 상담이 필요한 경우 제1항의 담당부서로 연락해주시기 바랍니다.

            개인정보 보호법 제35조(개인정보의 열람), 제36조(개인정보의 정정·삭제), 제37조(개인정보의 처리정지 등)의 규정에 의한 요구에 대하여 공공기관의 장이 행한 처분 또는 부작위로 인하여 권리 또는 이익의 침해를 받은 자는 행정심판법이 정하는 바에 따라 행정심판을 청구할 수 있습니다.

            가. 중앙행정심판위원회 : (국번없이) 110 (www.simpan.go.kr)

            제23조(Google AdMob 광고 관련 개인정보 처리 및 국외이전)

            회사는 서비스 운영을 위해 Google LLC 및 그 계열사가 제공하는 Google AdMob을 이용하여 배너 광고를 게재합니다.

            가. 처리·이전 항목: IP 주소로부터 추정되는 일반적 위치, 앱 정보, 광고 식별자(ADID/IDFA) 및 그 밖의 기기 식별자, 앱·광고 상호작용과 광고 노출 정보, 오류·진단·성능 정보
            나. 개인정보를 이전받는 자: Google LLC 및 Google 계열사(문의: https://support.google.com/policies/contact/general_privacy_form)
            다. 이전 국가: 미국, 싱가포르, 일본, 대만, 칠레, 우루과이, 아일랜드, 네덜란드, 벨기에, 덴마크, 핀란드, 독일 등 Google이 데이터센터를 운영하는 국가
            라. 이전 시점 및 방법: Google Mobile Ads SDK가 초기화되거나 광고를 요청·표시하는 때 암호화된 네트워크 통신으로 이전
            마. 처리·이전 목적: 광고 제공 및 개인 최적화, 광고 효과 측정·분석, 서비스 개선, 보안과 부정 이용 방지
            바. 보유·이용기간: Google 개인정보처리방침 및 데이터 보관 정책에 따르며, 광고 서버 로그의 IP 주소 일부는 9개월 후, 쿠키 정보는 18개월 후 익명화될 수 있습니다. 법적 의무, 보안 및 부정 이용 방지를 위해 필요한 정보는 더 오래 보관될 수 있습니다.

            회원은 광고 관련 국외이전 내용을 포함한 이용약관에 동의하는 방식으로 위 처리에 동의합니다. 동의하지 않으면 회원가입을 완료할 수 없습니다. 실제 처리 범위는 이용자의 기기 설정, 지역별 동의 선택, Google 정책 및 SDK 버전에 따라 달라질 수 있습니다.

            EEA·영국·스위스 및 관련 미국 주 등 별도 개인정보 선택권이 필요한 지역에서는 Google의 개인정보 메시지를 제공합니다. 해당 지역에서 광고 개인정보 설정이 필요한 경우 앱 설정 메뉴에 동의 철회 및 선택 변경 진입점이 표시됩니다. iOS 광고 추적 허용 여부는 운영체제의 개인정보 보호 및 보안 설정에서도 변경할 수 있습니다.

            Google의 구체적인 개인정보 처리 방식과 국외이전은 Google 개인정보처리방침(https://policies.google.com/privacy), Google 서비스 사용 앱 정보 처리 안내(https://policies.google.com/technologies/partner-sites), 광고 데이터 국외이전 안내(https://business.safety.google/adsdatatransfers/)에서 확인할 수 있습니다.

            부칙

            제1조 본 방침은 2026.08.30.부터 시행됩니다.
            """;
}
