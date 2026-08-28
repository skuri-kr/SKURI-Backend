package com.skuri.skuri_backend.domain.share.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skuri.skuri_backend.domain.share.model.SharePreviewBlockType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공개 콘텐츠 미리보기 블록")
public record SharePreviewBlockResponse(
        @Schema(description = "블록 유형", example = "TEXT")
        SharePreviewBlockType type,

        @Schema(description = "TEXT 블록 본문", nullable = true)
        String text,

        @Schema(description = "IMAGE 블록 HTTPS URL", nullable = true)
        String imageUrl,

        @Schema(description = "이미지 대체 텍스트", nullable = true)
        String alt,

        @Schema(description = "이미지 가로/세로 비율", nullable = true, example = "1.7778")
        Double aspectRatio,

        @Schema(description = "TABLE 블록 행", nullable = true)
        List<TableRow> rows,

        @Schema(description = "이 블록이 원문보다 잘렸는지 여부")
        boolean truncated
) {

    public static SharePreviewBlockResponse text(String text, boolean truncated) {
        return new SharePreviewBlockResponse(SharePreviewBlockType.TEXT, text, null, null, null, null, truncated);
    }

    public static SharePreviewBlockResponse image(String imageUrl, String alt, Double aspectRatio) {
        return new SharePreviewBlockResponse(SharePreviewBlockType.IMAGE, null, imageUrl, alt, aspectRatio, null, false);
    }

    public static SharePreviewBlockResponse table(List<TableRow> rows, boolean truncated) {
        return new SharePreviewBlockResponse(SharePreviewBlockType.TABLE, null, null, null, null, rows, truncated);
    }

    @Schema(description = "미리보기 표 행")
    public record TableRow(List<TableCell> cells) {
    }

    @Schema(description = "미리보기 표 셀")
    public record TableCell(
            String text,
            boolean header,
            int rowSpan,
            int colSpan
    ) {
    }
}
