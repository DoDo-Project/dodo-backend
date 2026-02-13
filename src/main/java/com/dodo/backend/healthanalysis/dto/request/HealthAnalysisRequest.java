package com.dodo.backend.healthanalysis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 건강 분석 도메인의 요청 DTO 그룹입니다.
 */
@Schema(description = "건강 분석 관련 요청 DTO 그룹")
public class HealthAnalysisRequest {

    /**
     * AI 건강 분석 보고서 생성 요청 DTO입니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 건강 분석 보고서 생성 요청")
    public static class AiReportCreateRequest {

        @Schema(description = "분석 타입 (DAILY, WEEKLY, MONTHLY)", example = "DAILY")
        private String analysisType;
    }
}
