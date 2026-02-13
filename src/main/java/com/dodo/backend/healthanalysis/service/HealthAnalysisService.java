package com.dodo.backend.healthanalysis.service;

import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AiReportCreateRequest;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AiReportCreateResponse;

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
}
