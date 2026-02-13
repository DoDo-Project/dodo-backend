package com.dodo.backend.healthanalysis.service;

import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AiReportCreateRequest;
import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AnalysisUpdateRequest;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisDetailResponse;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AiReportCreateResponse;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisUpdateResponse;

import java.util.UUID;

/**
 * 건강 분석(HealthAnalysis) 도메인 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface HealthAnalysisService {

    /**
     * 반려동물의 AI 건강 분석 보고서를 생성합니다.
     *
     * @param userId  요청한 사용자 ID
     * @param petId   분석 대상 반려동물 ID
     * @param request 분석 생성 요청 정보
     * @return 생성 결과 응답 DTO
     */
    AiReportCreateResponse createAiReport(UUID userId, Long petId, AiReportCreateRequest request);

    /**
     * 건강 분석 상세 정보를 조회합니다.
     *
     * @param userId 요청한 사용자 ID
     * @param analysisId 조회할 분석 ID
     * @return 건강 분석 상세 응답 DTO
     */
    AnalysisDetailResponse getAnalysisDetail(UUID userId, Long analysisId);

    /**
     * 건강 분석 결과를 수정합니다.
     *
     * @param userId 요청한 사용자 ID
     * @param analysisId 수정할 분석 ID
     * @param request 수정 요청 데이터
     * @return 수정 결과 응답 DTO
     */
    AnalysisUpdateResponse updateAnalysis(UUID userId, Long analysisId, AnalysisUpdateRequest request);
}
