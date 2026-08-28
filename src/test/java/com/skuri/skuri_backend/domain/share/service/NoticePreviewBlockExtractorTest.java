package com.skuri.skuri_backend.domain.share.service;

import com.skuri.skuri_backend.domain.share.dto.response.SharePreviewBlockResponse;
import com.skuri.skuri_backend.domain.share.model.SharePreviewBlockType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoticePreviewBlockExtractorTest {

    private final NoticePreviewBlockExtractor extractor = new NoticePreviewBlockExtractor();

    @Test
    void 공지원문순서대로_텍스트_허용이미지_표를_구조화한다() {
        String html = """
                <p>첫 안내입니다.</p>
                <img src="/upload/notice.png" alt="공지 이미지" width="1600" height="900">
                <table><tr><th>구분</th><th>일정</th></tr><tr><td>수강신청</td><td>8월 28일</td></tr></table>
                <p>마지막 안내입니다.</p>
                """;

        NoticePreviewBlockExtractor.Extraction result = extractor.extract(
                html,
                "대체 본문",
                "https://www.sungkyul.ac.kr/bbs/notice/1"
        );

        assertThat(result.blocks()).extracting(SharePreviewBlockResponse::type)
                .containsExactly(SharePreviewBlockType.TEXT, SharePreviewBlockType.IMAGE, SharePreviewBlockType.TABLE, SharePreviewBlockType.TEXT);
        assertThat(result.blocks().get(1).imageUrl()).isEqualTo("https://www.sungkyul.ac.kr/upload/notice.png");
        assertThat(result.blocks().get(1).aspectRatio()).isEqualTo(1600d / 900d);
        assertThat(result.blocks().get(2).rows().getFirst().cells().getFirst().header()).isTrue();
    }

    @Test
    void 외부이미지와_실행가능HTML은_제외하고_허용이미지는_한장만_노출한다() {
        String html = """
                <script>alert('xss')</script><iframe src="https://evil.example"></iframe>
                <img src="https://evil.example/a.png">
                <img src="https://cdn.skuri.kr/first.png">
                <img src="https://www.sungkyul.ac.kr/second.png">
                """;

        NoticePreviewBlockExtractor.Extraction result = extractor.extract(html, "", "https://www.sungkyul.ac.kr/notice/1");

        assertThat(result.blocks()).hasSize(1);
        assertThat(result.blocks().getFirst().imageUrl()).isEqualTo("https://cdn.skuri.kr/first.png");
        assertThat(result.blocks().toString()).doesNotContain("alert", "evil.example", "second.png");
        assertThat(result.truncated()).isTrue();
    }

    @Test
    void 표는_4행_5열_셀80자로_제한한다() {
        String longText = "가".repeat(100);
        StringBuilder html = new StringBuilder("<table>");
        for (int row = 0; row < 6; row++) {
            html.append("<tr>");
            for (int column = 0; column < 7; column++) {
                html.append("<td>").append(longText).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");

        SharePreviewBlockResponse table = extractor.extract(html.toString(), "", null).blocks().getFirst();

        assertThat(table.rows()).hasSize(4);
        assertThat(table.rows()).allSatisfy(row -> assertThat(row.cells()).hasSize(5));
        assertThat(table.rows().getFirst().cells().getFirst().text()).hasSize(80);
        assertThat(table.truncated()).isTrue();
    }

    @Test
    void 전체텍스트와_블록수를_제한하고_원문전체를_반환하지않는다() {
        String html = "<p>" + "가".repeat(400) + "</p><p>둘</p><p>셋</p><p>넷</p><p>다섯</p>";

        NoticePreviewBlockExtractor.Extraction result = extractor.extract(html, "", null);

        int textCodePoints = result.blocks().stream()
                .map(SharePreviewBlockResponse::text)
                .filter(java.util.Objects::nonNull)
                .mapToInt(value -> value.codePointCount(0, value.length()))
                .sum();
        assertThat(result.blocks()).hasSizeLessThanOrEqualTo(4);
        assertThat(textCodePoints).isLessThanOrEqualTo(350);
        assertThat(result.truncated()).isTrue();
    }

    @Test
    void html이없으면_안전한텍스트미리보기로_대체한다() {
        NoticePreviewBlockExtractor.Extraction result = extractor.extract(null, " 공지\n본문 ", null);

        assertThat(result.blocks()).containsExactly(SharePreviewBlockResponse.text("공지 본문", false));
    }
}
