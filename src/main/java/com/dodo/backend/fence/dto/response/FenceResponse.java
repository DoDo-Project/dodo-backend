package com.dodo.backend.fence.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 울타리(Fence) 도메인 응답 DTO를 정의하는 그룹 클래스입니다.
 */
@Schema(description = "울타리 응답 DTO 그룹")
public class FenceResponse {

    /**
     * 울타리 거리 범위 설정 완료 시 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "울타리 거리 범위 설정 응답")
    public static class FenceRangeResponse {

        @Schema(description = "처리 결과 메시지", example = "울타리 설정을 완료했습니다.")
        private String message;

        /**
         * 메시지를 기반으로 응답 DTO를 생성합니다.
         *
         * @param message 클라이언트에게 전달할 메시지
         * @return 생성된 응답 DTO
         */
        public static FenceRangeResponse toDto(String message) {
            return FenceRangeResponse.builder()
                    .message(message)
                    .build();
        }
    }
}
