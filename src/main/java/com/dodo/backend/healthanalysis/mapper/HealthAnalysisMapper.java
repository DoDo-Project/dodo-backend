package com.dodo.backend.healthanalysis.mapper;

import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AnalysisUpdateRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HealthAnalysisMapper {

    /**
     * 건강 분석 제목/요약을 선택적으로 수정합니다.
     *
     * @param analysisId 수정할 분석 ID
     * @param request 수정 요청 DTO
     */
    void updateHealthAnalysis(@Param("analysisId") Long analysisId, @Param("request") AnalysisUpdateRequest request);
}
