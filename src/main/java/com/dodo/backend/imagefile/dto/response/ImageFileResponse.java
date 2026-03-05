package com.dodo.backend.imagefile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 이미지 파일 업로드 응답 DTO 모음입니다.
 */
@Schema(description = "이미지 파일 응답 DTO 모음")
public class ImageFileResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "이미지 업로드 응답 DTO")
    public static class ImageUploadResponse {

        @Schema(description = "응답 메시지", example = "이미지 업로드에 성공하였습니다.")
        private String message;

        @Schema(description = "업로드된 이미지 URL 목록")
        private List<String> imageUrls;

        public static ImageUploadResponse toDto(String message, List<String> imageUrls) {
            return ImageUploadResponse.builder()
                    .message(message)
                    .imageUrls(imageUrls)
                    .build();
        }
    }
}
