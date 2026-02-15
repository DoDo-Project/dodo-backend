package com.dodo.backend.fence.service;

import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeRequest;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeResponse;

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

}
