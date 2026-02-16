package com.dodo.backend.fence.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 울타리(Fence) 도메인 응답 DTO를 정의하는 그룹 클래스입니다.
 */
@Schema(description = "울타리 응답 DTO 그룹")
public class FenceResponse {

    /**
     * 반려동물의 울타리 활성화 상태 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "울타리 활성화 상태 조회 응답")
    public static class FenceStatusResponse {

        @Schema(description = "처리 결과 메시지", example = "울타리 상태를 조회했습니다.")
        private String message;

        @Schema(description = "울타리 활성화 여부", example = "true")
        private Boolean isActive;

        /**
         * 울타리 활성화 여부 값을 기반으로 응답 DTO를 생성합니다.
         *
         * @param message 처리 결과 메시지
         * @param isActive 울타리 활성화 여부
         * @return 생성된 응답 DTO
         */
        public static FenceStatusResponse toDto(String message, Boolean isActive) {
            return FenceStatusResponse.builder()
                    .message(message)
                    .isActive(isActive)
                    .build();
        }
    }

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

    /**
     * 울타리 기능 ON/OFF 상태 변경 결과를 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "울타리 상태 변경 응답")
    public static class FenceToggleResponse {

        @Schema(description = "처리 결과 메시지", example = "울타리 상태를 변경하는데 성공했습니다.")
        private String message;

        /**
         * 처리 결과 메시지를 기반으로 응답 DTO를 생성합니다.
         *
         * @param message 클라이언트에게 전달할 메시지
         * @return 생성된 응답 DTO
         */
        public static FenceToggleResponse toDto(String message) {
            return FenceToggleResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 울타리 범위 정보 수정 결과를 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "울타리 범위 수정 응답")
    public static class FenceRangeUpdateResponse {

        @Schema(description = "처리 결과 메시지", example = "울타리 정보를 수정했습니다.")
        private String message;

        @Schema(description = "울타리 고유 ID", example = "1")
        private Long geofenceId;

        @Schema(description = "반려동물 ID", example = "1")
        private Long petId;

        @Schema(description = "울타리 이름", example = "새로운 이름")
        private String fenceName;

        @Schema(description = "울타리 중심 위도", example = "37.5555")
        private BigDecimal centerLatitude;

        @JsonProperty("centerLongtitude")
        @Schema(description = "울타리 중심 경도", example = "127.0000")
        private BigDecimal centerLongtitude;

        @Schema(description = "울타리 반경(미터)", example = "1000")
        private Integer radius;

        /**
         * 울타리 범위 수정 응답 DTO를 생성합니다.
         *
         * @param message          처리 결과 메시지
         * @param geofenceId       울타리 고유 ID
         * @param petId            반려동물 ID
         * @param fenceName        울타리 이름
         * @param centerLatitude   울타리 중심 위도
         * @param centerLongtitude 울타리 중심 경도
         * @param radius           울타리 반경(미터)
         * @return 생성된 응답 DTO
         */
        public static FenceRangeUpdateResponse toDto(
                String message,
                Long geofenceId,
                Long petId,
                String fenceName,
                BigDecimal centerLatitude,
                BigDecimal centerLongtitude,
                Integer radius
        ) {
            return FenceRangeUpdateResponse.builder()
                    .message(message)
                    .geofenceId(geofenceId)
                    .petId(petId)
                    .fenceName(fenceName)
                    .centerLatitude(centerLatitude)
                    .centerLongtitude(centerLongtitude)
                    .radius(radius)
                    .build();
        }
    }

    /**
     * 실시간 위치가 울타리 내부인지 판정한 결과 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "실시간 울타리 판정 결과")
    public static class FenceLocationCheckResponse {

        @Schema(description = "울타리 내부 여부", example = "true")
        private Boolean insideFence;

        @Schema(description = "중심점으로부터의 거리(미터)", example = "125.42")
        private BigDecimal distanceMeter;

        @Schema(description = "설정된 울타리 반경(미터)", example = "500")
        private Integer radius;

        /**
         * 실시간 울타리 판정 결과 DTO를 생성합니다.
         *
         * @param insideFence  울타리 내부 여부
         * @param distanceMeter 중심점으로부터의 거리(미터)
         * @param radius        설정된 울타리 반경(미터)
         * @return 생성된 응답 DTO
         */
        public static FenceLocationCheckResponse toDto(Boolean insideFence, BigDecimal distanceMeter, Integer radius) {
            return FenceLocationCheckResponse.builder()
                    .insideFence(insideFence)
                    .distanceMeter(distanceMeter)
                    .radius(radius)
                    .build();
        }
    }
}
