package com.dodo.backend.healthanalysis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 건강 분석 요청 DTO 모음입니다.
 */
@Schema(description = "건강 분석 요청 DTO 모음")
public class HealthAnalysisRequest {

    /**
     * AI 건강 분석 리포트 생성 요청입니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 건강 분석 리포트 생성 요청")
    public static class AiReportCreateRequest {

        @Schema(description = "분석 주기(DAILY, WEEKLY, MONTHLY)", example = "DAILY")
        private String analysisType;

        @Schema(description = "특이사항 리스트", example = "[\"최근 식욕이 줄었습니다.\", \"운동량이 늘었습니다.\"]")
        private List<String> specialNotes;
    }

    /**
     * 건강 분석 리포트 수정 요청입니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "건강 분석 리포트 수정 요청")
    public static class AnalysisUpdateRequest {

        @Schema(description = "수정할 분석 제목", example = "2025년 10월 건강 분석 리포트(수정본)")
        private String healthAnalysisTitle;

        @Schema(description = "수정할 분석 요약", example = "운동량 증가가 필요합니다. 심박수는 안정적입니다.")
        private String healthAnalysisSummary;
    }
}
