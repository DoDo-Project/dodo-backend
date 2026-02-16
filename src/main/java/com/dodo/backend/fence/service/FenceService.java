package com.dodo.backend.fence.service;

import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeRequest;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceLocationCheckResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceStatusResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 울타리(Fence) 도메인의 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 * <p>
 * 울타리 생성, 조회, 수정, 활성화/비활성화 정책을 이 계층에서 확장합니다.
 */
public interface FenceService {

    /**
     * 반려동물의 울타리 거리 범위를 설정합니다.
     *
     * @param userId  요청한 사용자 ID
     * @param request 울타리 설정 요청 DTO
     * @return 설정 완료 응답 DTO
     */
    FenceRangeResponse setFenceRange(UUID userId, FenceRangeRequest request);

    /**
     * 반려동물의 울타리 활성화 상태를 조회합니다.
     *
     * @param petId 반려동물 ID
     * @return 울타리 활성화 상태 응답 DTO
     */
    FenceStatusResponse getFenceStatus(Long petId);

    /**
     * 디바이스 토큰 기반으로 반려동물의 울타리 활성화 상태를 조회합니다.
     *
     * @param petId          반려동물 ID
     * @param devicePrincipal 디바이스 토큰 Principal(subject)
     * @return 울타리 활성화 상태 응답 DTO
     */
    FenceStatusResponse getFenceStatusForDevice(Long petId, String devicePrincipal);

    /**
     * 반려동물 ID 기준으로 실시간 위치의 울타리 내부 여부를 판정합니다.
     *
     * @param petId      반려동물 ID
     * @param latitude   실시간 위도
     * @param longitude  실시간 경도
     * @param measuredAt 측정 시각
     * @return 울타리 판정 결과 DTO
     */
    FenceLocationCheckResponse checkFenceLocationByPet(Long petId, BigDecimal latitude, BigDecimal longitude, LocalDateTime measuredAt);

    /**
     * 실시간 위치 데이터를 기반으로 울타리 내부 여부를 판정합니다.
     *
     * @param userId     요청한 사용자 ID
     * @param petId      반려동물 ID
     * @param latitude   실시간 위도
     * @param longitude  실시간 경도
     * @param measuredAt 측정 시각
     * @return 울타리 판정 결과 DTO
     */
    FenceLocationCheckResponse checkFenceLocation(UUID userId, Long petId, BigDecimal latitude, BigDecimal longitude, LocalDateTime measuredAt);

}
