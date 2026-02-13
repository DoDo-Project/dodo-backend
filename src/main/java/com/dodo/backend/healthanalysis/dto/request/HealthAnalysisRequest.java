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

    /**
     * 건강 분석 결과 수정 요청 DTO입니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "건강 분석 결과 수정 요청")
    public static class AnalysisUpdateRequest {

        @Schema(description = "수정할 분석 제목", example = "2025년 10월 정기 건강 분석 리포트 (수정본)")
        private String healthAnalysisTitle;

        @Schema(description = "수정할 분석 요약", example = "활동량은 양호하나 체중이 약간 증가하는 경향을 보입니다. 식단 조절 및 산책 시간 증가를 강력히 권장합니다.")
        private String healthAnalysisSummary;
    }
}
