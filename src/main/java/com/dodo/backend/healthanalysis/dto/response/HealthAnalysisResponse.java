package com.dodo.backend.healthanalysis.dto.response;

import com.dodo.backend.healthanalysis.entity.HealthAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 건강 분석 상세 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "건강 분석 상세 조회 응답")
    public static class AnalysisDetailResponse {

        @Schema(description = "응답 메시지", example = "건강 분석 상세 조회에 성공했습니다.")
        private String message;

        @Schema(description = "분석 ID", example = "1")
        private Long analysisId;

        @Schema(description = "반려동물 ID", example = "789")
        private Long petId;

        @Schema(description = "분석 제목", example = "2025년 10월 정기 건강 분석 리포트")
        private String healthAnalysisTitle;

        @Schema(description = "분석 요약", example = "활동량은 양호하나, 체중이 약간 증가하는 경향을 보입니다. 식단 조절을 권장합니다.")
        private String healthAnalysisSummary;

        @Schema(description = "상세 분석 JSON")
        private Object healthAnalysisFullContent;

        @Schema(description = "분석 일시", example = "2025-10-14T10:00:00")
        private LocalDateTime analysisDate;

        @Schema(description = "분석 타입", example = "MONTHLY")
        private String analysisType;

        @Schema(description = "분석 상태", example = "COMPLETED")
        private String analysisStatus;

        /**
         * 상세 조회 응답 DTO를 생성합니다.
         *
         * @param message 응답 메시지
         * @param analysisId 분석 ID
         * @param petId 반려동물 ID
         * @param healthAnalysisTitle 분석 제목
         * @param healthAnalysisSummary 분석 요약
         * @param healthAnalysisFullContent 상세 분석 JSON
         * @param analysisDate 분석 일시
         * @param analysisType 분석 타입 문자열
         * @param analysisStatus 분석 상태 문자열
         * @return 초기화된 상세 조회 응답 DTO
         */
        public static AnalysisDetailResponse toDto(
                String message,
                Long analysisId,
                Long petId,
                String healthAnalysisTitle,
                String healthAnalysisSummary,
                Object healthAnalysisFullContent,
                LocalDateTime analysisDate,
                String analysisType,
                String analysisStatus
        ) {
            return AnalysisDetailResponse.builder()
                    .message(message)
                    .analysisId(analysisId)
                    .petId(petId)
                    .healthAnalysisTitle(healthAnalysisTitle)
                    .healthAnalysisSummary(healthAnalysisSummary)
                    .healthAnalysisFullContent(healthAnalysisFullContent)
                    .analysisDate(analysisDate)
                    .analysisType(analysisType)
                    .analysisStatus(analysisStatus)
                    .build();
        }
    }

    /**
     * 건강 분석 수정 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "건강 분석 수정 응답")
    public static class AnalysisUpdateResponse {

        @Schema(description = "응답 메시지", example = "성공적으로 내용이 수정되었습니다.")
        private String message;

        /**
         * 수정 응답 DTO를 생성합니다.
         *
         * @param message 응답 메시지
         * @return 초기화된 수정 응답 DTO
         */
        public static AnalysisUpdateResponse toDto(String message) {
            return AnalysisUpdateResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 건강 분석 삭제 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "건강 분석 삭제 응답")
    public static class AnalysisDeleteResponse {

        @Schema(description = "응답 메시지", example = "성공적으로 삭제되었습니다.")
        private String message;

        /**
         * 삭제 응답 DTO를 생성합니다.
         *
         * @param message 응답 메시지
         * @return 초기화된 삭제 응답 DTO
         */
        public static AnalysisDeleteResponse toDto(String message) {
            return AnalysisDeleteResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 건강 분석 결과 목록 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "건강 분석 결과 목록 조회 응답")
    public static class AnalysisListResponse {

        @Schema(description = "응답 메시지", example = "건강 분석 결과 조회를 성공했습니다.")
        private String message;

        @Schema(description = "페이지 정보")
        private PageInfo pageInfo;

        @Schema(description = "건강 분석 결과 목록")
        private List<AnalysisListItem> data;

        /**
         * 목록 조회 응답 DTO를 생성합니다.
         *
         * @param message 응답 메시지
         * @param page 페이지 정보가 담긴 Page 객체
         * @param data 목록 데이터
         * @return 초기화된 목록 조회 응답 DTO
         */
        public static AnalysisListResponse toDto(String message, Page<HealthAnalysis> page, List<AnalysisListItem> data) {
            return AnalysisListResponse.builder()
                    .message(message)
                    .pageInfo(PageInfo.toDto(page))
                    .data(data)
                    .build();
        }
    }

    /**
     * 건강 분석 목록 조회의 페이지 메타 정보를 담는 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "건강 분석 목록 페이지 정보")
    public static class PageInfo {

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int page;

        @Schema(description = "페이지 크기", example = "10")
        private int size;

        @Schema(description = "전체 요소 수", example = "4")
        private long totalElements;

        @Schema(description = "전체 페이지 수", example = "1")
        private int totalPages;

        /**
         * Page 객체를 페이지 정보 DTO로 변환합니다.
         *
         * @param page Page 객체
         * @return 페이지 정보 DTO
         */
        public static PageInfo toDto(Page<HealthAnalysis> page) {
            return PageInfo.builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .build();
        }
    }

    /**
     * 건강 분석 목록의 개별 항목 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "건강 분석 목록 항목")
    public static class AnalysisListItem {

        @Schema(description = "분석 ID", example = "1")
        private Long analysisId;

        @Schema(description = "분석 제목", example = "2025년 10월 정기 건강 분석 리포트")
        private String healthAnalysisTitle;

        @Schema(description = "분석 요약", example = "활동량은 양호하나, 체중이 약간 증가하는 경향을 보입니다.")
        private String healthAnalysisSummary;

        @Schema(description = "상세 분석 JSON")
        private Object healthAnalysisFullContent;

        @Schema(description = "분석 일시", example = "2025-10-14T10:00:00")
        private LocalDateTime analysisDate;

        @Schema(description = "분석 타입", example = "MONTHLY")
        private String analysisType;

        @Schema(description = "분석 상태", example = "COMPLETED")
        private String analysisStatus;

        /**
         * 목록 항목 DTO를 생성합니다.
         *
         * @param analysisId 분석 ID
         * @param healthAnalysisTitle 분석 제목
         * @param healthAnalysisSummary 분석 요약
         * @param healthAnalysisFullContent 상세 분석 JSON
         * @param analysisDate 분석 일시
         * @param analysisType 분석 타입 문자열
         * @param analysisStatus 분석 상태 문자열
         * @return 목록 항목 DTO
         */
        public static AnalysisListItem toDto(
                Long analysisId,
                String healthAnalysisTitle,
                String healthAnalysisSummary,
                Object healthAnalysisFullContent,
                LocalDateTime analysisDate,
                String analysisType,
                String analysisStatus
        ) {
            return AnalysisListItem.builder()
                    .analysisId(analysisId)
                    .healthAnalysisTitle(healthAnalysisTitle)
                    .healthAnalysisSummary(healthAnalysisSummary)
                    .healthAnalysisFullContent(healthAnalysisFullContent)
                    .analysisDate(analysisDate)
                    .analysisType(analysisType)
                    .analysisStatus(analysisStatus)
                    .build();
        }
    }
}
