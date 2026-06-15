package com.dodo.backend.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신고 API에서 사용하는 응답 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "신고 응답 DTO 그룹")
public class ReportResponse {

    /**
     * 신고 처리 결과 메시지를 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "신고 단순 처리 응답")
    public static class ReportSimpleResponse {

        @Schema(description = "응답 메시지", example = "신고가 성공적으로 접수되었습니다.")
        private String message;

        /**
         * 신고 단순 처리 응답 DTO를 생성합니다.
         *
         * @param message 응답 메시지
         * @return 신고 단순 처리 응답 DTO
         */
        public static ReportSimpleResponse toDto(String message) {
            return ReportSimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }
}
