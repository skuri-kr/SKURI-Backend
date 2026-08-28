package com.skuri.skuri_backend.domain.share.service;

import com.skuri.skuri_backend.domain.share.dto.response.SharePreviewBlockResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class NoticePreviewBlockExtractor {

    static final int MAX_TEXT_CODE_POINTS = 350;
    static final int MAX_BLOCKS = 4;
    static final int MAX_TABLE_ROWS = 4;
    static final int MAX_TABLE_COLUMNS = 5;
    static final int MAX_TABLE_CELL_CODE_POINTS = 80;

    private static final Set<String> BLOCK_TAGS = Set.of(
            "p", "div", "section", "article", "li", "ul", "ol",
            "h1", "h2", "h3", "h4", "h5", "h6", "blockquote"
    );

    public Extraction extract(String bodyHtml, String fallbackText, String baseUrl) {
        if (!StringUtils.hasText(bodyHtml)) {
            return fallback(fallbackText);
        }

        Document document = Jsoup.parseBodyFragment(bodyHtml, StringUtils.hasText(baseUrl) ? baseUrl : "");
        document.select("script,style,iframe,object,embed,form,input,button,textarea,video,audio,svg").remove();

        Accumulator accumulator = new Accumulator(baseUrl);
        for (Node child : document.body().childNodes()) {
            walk(child, accumulator);
        }
        accumulator.flushText();

        if (accumulator.blocks.isEmpty()) {
            return fallback(StringUtils.hasText(fallbackText) ? fallbackText : document.body().text());
        }
        return new Extraction(List.copyOf(accumulator.blocks), accumulator.truncated);
    }

    private void walk(Node node, Accumulator accumulator) {
        if (node instanceof TextNode textNode) {
            accumulator.appendText(textNode.text());
            return;
        }
        if (!(node instanceof Element element)) {
            return;
        }

        String tagName = element.normalName();
        if ("table".equals(tagName)) {
            accumulator.flushText();
            accumulator.addTable(element);
            return;
        }
        if ("img".equals(tagName)) {
            accumulator.flushText();
            accumulator.addImage(element);
            return;
        }
        if ("br".equals(tagName)) {
            accumulator.flushText();
            return;
        }

        for (Node child : element.childNodes()) {
            walk(child, accumulator);
        }
        if (BLOCK_TAGS.contains(tagName)) {
            accumulator.flushText();
        }
    }

    private Extraction fallback(String fallbackText) {
        String normalized = normalizeWhitespace(fallbackText);
        if (!StringUtils.hasText(normalized)) {
            return new Extraction(List.of(), false);
        }
        TruncatedText truncatedText = truncate(normalized, MAX_TEXT_CODE_POINTS);
        return new Extraction(
                List.of(SharePreviewBlockResponse.text(truncatedText.value(), truncatedText.truncated())),
                truncatedText.truncated()
        );
    }

    private static String normalizeWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static TruncatedText truncate(String value, int maxCodePoints) {
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxCodePoints) {
            return new TruncatedText(value, false);
        }
        int endIndex = value.offsetByCodePoints(0, maxCodePoints);
        return new TruncatedText(value.substring(0, endIndex).stripTrailing(), true);
    }

    public record Extraction(List<SharePreviewBlockResponse> blocks, boolean truncated) {
    }

    private record TruncatedText(String value, boolean truncated) {
    }

    private static final class Accumulator {

        private final List<SharePreviewBlockResponse> blocks = new ArrayList<>();
        private final StringBuilder textBuffer = new StringBuilder();
        private final String baseUrl;
        private int textCodePoints;
        private boolean imageAdded;
        private boolean tableAdded;
        private boolean truncated;

        private Accumulator(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        private void appendText(String value) {
            if (!StringUtils.hasText(value)) {
                return;
            }
            if (!textBuffer.isEmpty()) {
                textBuffer.append(' ');
            }
            textBuffer.append(value);
        }

        private void flushText() {
            String normalized = normalizeWhitespace(textBuffer.toString());
            textBuffer.setLength(0);
            if (!StringUtils.hasText(normalized)) {
                return;
            }
            if (blocks.size() >= MAX_BLOCKS || textCodePoints >= MAX_TEXT_CODE_POINTS) {
                truncated = true;
                return;
            }

            int remaining = MAX_TEXT_CODE_POINTS - textCodePoints;
            TruncatedText result = truncate(normalized, remaining);
            blocks.add(SharePreviewBlockResponse.text(result.value(), result.truncated()));
            textCodePoints += result.value().codePointCount(0, result.value().length());
            truncated |= result.truncated();
        }

        private void addImage(Element image) {
            if (imageAdded || blocks.size() >= MAX_BLOCKS) {
                truncated = true;
                return;
            }
            String imageUrl = resolveAllowedImageUrl(image.attr("src"), baseUrl);
            if (imageUrl == null) {
                return;
            }
            String alt = normalizeWhitespace(image.attr("alt"));
            TruncatedText altText = truncate(alt, 120);
            blocks.add(SharePreviewBlockResponse.image(
                    imageUrl,
                    StringUtils.hasText(altText.value()) ? altText.value() : null,
                    resolveAspectRatio(image)
            ));
            imageAdded = true;
        }

        private void addTable(Element table) {
            if (tableAdded || blocks.size() >= MAX_BLOCKS) {
                truncated = true;
                return;
            }

            List<Element> sourceRows = table.select("tr");
            List<SharePreviewBlockResponse.TableRow> rows = new ArrayList<>();
            boolean tableTruncated = sourceRows.size() > MAX_TABLE_ROWS;

            for (Element sourceRow : sourceRows) {
                if (rows.size() >= MAX_TABLE_ROWS) {
                    break;
                }
                List<Element> sourceCells = sourceRow.children().stream()
                        .filter(cell -> "th".equals(cell.normalName()) || "td".equals(cell.normalName()))
                        .toList();
                if (sourceCells.isEmpty()) {
                    continue;
                }
                if (sourceCells.size() > MAX_TABLE_COLUMNS) {
                    tableTruncated = true;
                }

                List<SharePreviewBlockResponse.TableCell> cells = new ArrayList<>();
                for (Element sourceCell : sourceCells.stream().limit(MAX_TABLE_COLUMNS).toList()) {
                    TruncatedText cellText = truncate(
                            normalizeWhitespace(sourceCell.text()),
                            MAX_TABLE_CELL_CODE_POINTS
                    );
                    int sourceRowSpan = parseSpan(sourceCell.attr("rowspan"));
                    int sourceColSpan = parseSpan(sourceCell.attr("colspan"));
                    int rowSpan = Math.min(sourceRowSpan, MAX_TABLE_ROWS);
                    int colSpan = Math.min(sourceColSpan, MAX_TABLE_COLUMNS);
                    tableTruncated |= cellText.truncated()
                            || sourceRowSpan > MAX_TABLE_ROWS
                            || sourceColSpan > MAX_TABLE_COLUMNS;
                    cells.add(new SharePreviewBlockResponse.TableCell(
                            cellText.value(),
                            "th".equals(sourceCell.normalName()),
                            rowSpan,
                            colSpan
                    ));
                }
                rows.add(new SharePreviewBlockResponse.TableRow(List.copyOf(cells)));
            }

            if (rows.isEmpty()) {
                return;
            }
            blocks.add(SharePreviewBlockResponse.table(List.copyOf(rows), tableTruncated));
            tableAdded = true;
            truncated |= tableTruncated;
        }

        private static String resolveAllowedImageUrl(String rawSrc, String baseUrl) {
            if (!StringUtils.hasText(rawSrc)) {
                return null;
            }
            try {
                URI resolved = StringUtils.hasText(baseUrl)
                        ? URI.create(baseUrl).resolve(rawSrc.trim())
                        : URI.create(rawSrc.trim());
                if (!"https".equalsIgnoreCase(resolved.getScheme()) || resolved.getHost() == null) {
                    return null;
                }
                String host = resolved.getHost().toLowerCase(Locale.ROOT);
                if (!isAllowedImageHost(host)) {
                    return null;
                }
                return resolved.toString();
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        private static boolean isAllowedImageHost(String host) {
            return host.equals("sungkyul.ac.kr")
                    || host.endsWith(".sungkyul.ac.kr")
                    || host.equals("skuri.kr")
                    || host.endsWith(".skuri.kr")
                    || host.equals("skuri.app")
                    || host.endsWith(".skuri.app");
        }

        private static Double resolveAspectRatio(Element image) {
            int width = parsePositiveInt(image.attr("width"));
            int height = parsePositiveInt(image.attr("height"));
            return width > 0 && height > 0 ? (double) width / height : null;
        }

        private static int parseSpan(String raw) {
            int value = parsePositiveInt(raw);
            return value > 0 ? value : 1;
        }

        private static int parsePositiveInt(String raw) {
            try {
                int value = Integer.parseInt(raw);
                return Math.max(0, value);
            } catch (NumberFormatException exception) {
                return 0;
            }
        }
    }
}
