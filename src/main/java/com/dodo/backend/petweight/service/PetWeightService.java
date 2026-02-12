package com.dodo.backend.petweight.service;

import com.dodo.backend.petweight.dto.request.PetWeightRequest;
import com.dodo.backend.petweight.dto.request.PetWeightRequest.PetWeightRegisterRequest;

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
}