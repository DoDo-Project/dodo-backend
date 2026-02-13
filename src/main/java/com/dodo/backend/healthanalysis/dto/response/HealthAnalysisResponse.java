package com.dodo.backend.healthanalysis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 건강 분석 도메인의 응답 DTO 그룹입니다.
 */
@Schema(description = "건강 분석 관련 응답 DTO 그룹")
public class HealthAnalysisResponse {

    /**
     * AI 건강 분석 보고서 생성 완료 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "AI 건강 분석 보고서 생성 응답")
    public static class AiReportCreateResponse {

        @Schema(description = "응답 메시지", example = "건강 분석 보고서 생성이 완료되었습니다.")
        private String message;

        @Schema(description = "생성된 분석 ID", example = "123")
        private Long analysisId;

        /**
         * 분석 ID와 메시지를 담아 응답 DTO를 생성합니다.
         *
         * @param analysisId 생성된 분석 고유 ID
         * @param message    응답 메시지
         * @return 초기화된 {@link AiReportCreateResponse} 객체
         */
        public static AiReportCreateResponse toDto(Long analysisId, String message) {
            return AiReportCreateResponse.builder()
                    .message(message)
                    .analysisId(analysisId)
                    .build();
        }
    }
}
