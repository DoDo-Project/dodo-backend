package com.dodo.backend.healthanalysis.repository;

import com.dodo.backend.healthanalysis.entity.AnalysisType;
import com.dodo.backend.healthanalysis.entity.HealthAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthAnalysisRepository extends JpaRepository<HealthAnalysis, Long> {

    /**
     * 특정 반려동물의 건강 분석 결과를 분석 일시 내림차순으로 조회합니다.
     *
     * @param petId 반려동물 ID
     * @param pageable 페이징 정보
     * @return 건강 분석 결과 페이지
     */
    Page<HealthAnalysis> findAllByPet_PetIdOrderByAnalysisDateDesc(Long petId, Pageable pageable);

    /**
     * 특정 반려동물의 건강 분석 결과를 분석 타입으로 필터링하여 분석 일시 내림차순으로 조회합니다.
     *
     * @param petId 반려동물 ID
     * @param analysisType 분석 타입
     * @param pageable 페이징 정보
     * @return 건강 분석 결과 페이지
     */
    Page<HealthAnalysis> findAllByPet_PetIdAndAnalysisTypeOrderByAnalysisDateDesc(
            Long petId,
            AnalysisType analysisType,
            Pageable pageable
    );
}
