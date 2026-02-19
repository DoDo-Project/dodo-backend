package com.dodo.backend.petweight.service;

import com.dodo.backend.petweight.dto.request.PetWeightRequest;
import com.dodo.backend.petweight.dto.request.PetWeightRequest.PetWeightRegisterRequest;
import com.dodo.backend.petweight.dto.request.PetWeightRequest.PetWeightUpdateRequest;
import com.dodo.backend.petweight.dto.response.PetWeightResponse.PetWeightHistoryResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 반려동물 체중 도메인의 비즈니스 로직을 담당하는 서비스 인터페이스입니다.
 */
public interface PetWeightService {

    /**
     * 여러 반려동물의 현재(가장 최근) 체중을 일괄 조회합니다.
     *
     * @param petIds 체중을 조회할 반려동물들의 ID 목록
     * @return 펫 ID를 Key로, 최근 체중(kg)을 Value로 가지는 Map 객체 (기록이 없는 펫은 포함되지 않음)
     */
    Map<Long, Double> getRecentWeights(List<Long> petIds);

    /**
     * 특정 반려동물의 새로운 체중 기록을 추가합니다.
     *
     * @param userId  요청을 보낸 사용자의 고유 ID (권한 검증용)
     * @param petId   체중을 기록할 반려동물의 식별자
     * @param request 체중 및 측정 일시가 포함된 요청 DTO
     * @return 생성된 체중 기록의 고유 식별자(weightId)
     */
    Long addWeight(UUID userId, Long petId, PetWeightRegisterRequest request);

    /**
     * 특정 반려동물의 체중 기록 이력을 페이징하여 조회합니다.
     *
     * @param userId   요청한 사용자의 ID (권한 검증용)
     * @param petId    조회할 반려동물의 ID
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 페이징된 체중 기록 응답 DTO
     */
    PetWeightHistoryResponse getWeightHistory(UUID userId, Long petId, Pageable pageable);

    /**
     * 기존 체중 기록을 수정합니다.
     *
     * @param userId   요청한 사용자의 ID
     * @param petId    반려동물 ID
     * @param weightId 수정할 체중 기록 ID
     * @param request  수정할 데이터(몸무게, 날짜)가 담긴 DTO
     */
    void updateWeight(UUID userId, Long petId, Long weightId, PetWeightUpdateRequest request);

    /**
     * 특정 체중 기록을 삭제합니다.
     *
     * @param userId   요청한 사용자의 ID
     * @param petId    반려동물 ID
     * @param weightId 삭제할 체중 기록 ID
     */
    void deleteWeight(UUID userId, Long petId, Long weightId);

    /**
     * 건강 분석용 체중 데이터를 조회합니다.
     *
     * @param petId        반려동물 ID
     * @param analysisType 분석 단위 (DAILY/WEEKLY/MONTHLY)
     * @param startDate    조회 시작일(포함)
     * @param endDate      조회 종료일(미포함)
     * @return 체중 데이터 목록 (Map 형태)
     */
    List<Map<String, Object>> getWeightsForAnalysis(Long petId, String analysisType, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 반려동물의 현재 체중과 체중 추세를 조회합니다.
     *
     * @param petId 조회할 반려동물 ID
     * @return 현재 체중 및 체중 추세 정보
     */
    Map<String, Object> getWeightInfo(Long petId);
}
