package com.dodo.backend.fence.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

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

        /**
         * 울타리 범위 수정 응답 DTO를 생성합니다.
         *
         * @param message 처리 결과 메시지
         * @return 생성된 응답 DTO
         */
        public static FenceRangeUpdateResponse toDto(String message) {
            return FenceRangeUpdateResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 지도에 표시할 울타리 경계 단건 정보를 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "울타리 경계 단건 조회 응답")
    public static class FenceBoundaryResponse {

        @Schema(description = "처리 결과 메시지", example = "울타리 정보 조회를 성공했습니다.")
        private String message;

        @Schema(description = "울타리 중심점 좌표")
        private Center center;

        @Schema(description = "울타리 반경(미터)", example = "500")
        private Integer radius;

        @Schema(description = "울타리 ID", example = "1")
        private Long fenceId;

        /**
         * 울타리 중심점 좌표 DTO입니다.
         */
        @Getter
        @Builder
        @AllArgsConstructor
        @Schema(description = "울타리 중심점")
        public static class Center {

            @Schema(description = "위도", example = "37.5665")
            private BigDecimal latitude;

            @Schema(description = "경도", example = "126.9780")
            private BigDecimal longitude;
        }

        /**
         * 울타리 경계 단건 응답 DTO를 생성합니다.
         *
         * @param message   처리 결과 메시지
         * @param latitude  중심 위도
         * @param longitude 중심 경도
         * @param radius    반경(미터)
         * @param fenceId   울타리 ID
         * @return 생성된 응답 DTO
         */
        public static FenceBoundaryResponse toDto(
                String message,
                BigDecimal latitude,
                BigDecimal longitude,
                Integer radius,
                Long fenceId
        ) {
            return FenceBoundaryResponse.builder()
                    .message(message)
                    .center(Center.builder()
                            .latitude(latitude)
                            .longitude(longitude)
                            .build())
                    .radius(radius)
                    .fenceId(fenceId)
                    .build();
        }
    }

    /**
     * 울타리 경계 목록 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "울타리 경계 목록 조회 응답")
    public static class FenceBoundaryListResponse {

        @Schema(description = "처리 결과 메시지", example = "울타리 목록 조회를 성공했습니다.")
        private String message;

        @Schema(description = "울타리 경계 목록")
        private List<FenceBoundaryItem> boundaries;

        /**
         * 울타리 경계 목록 조회 응답 DTO를 생성합니다.
         *
         * @param message    처리 결과 메시지
         * @param boundaries 울타리 경계 목록
         * @return 생성된 응답 DTO
         */
        public static FenceBoundaryListResponse toDto(String message, List<FenceBoundaryItem> boundaries) {
            return FenceBoundaryListResponse.builder()
                    .message(message)
                    .boundaries(boundaries)
                    .build();
        }
    }

    /**
     * 울타리 경계 목록의 단건 항목 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "울타리 경계 목록 단건 항목")
    public static class FenceBoundaryItem {

        @Schema(description = "울타리 ID", example = "1")
        private Long fenceId;

        @Schema(description = "울타리 이름", example = "집 주변 울타리")
        private String fenceName;

        @Schema(description = "울타리 중심점 좌표")
        private FenceBoundaryResponse.Center center;

        @Schema(description = "울타리 반경(미터)", example = "500")
        private Integer radius;

        @Schema(description = "울타리 활성화 여부", example = "true")
        private Boolean isActive;

        @Schema(description = "반려동물 ID", example = "1")
        private Long petId;

        @Schema(description = "반려동물 이름", example = "도도")
        private String petName;

        @Schema(description = "반려동물 프로필 이미지 URL", example = "https://cdn.dodo.com/pets/1/profile.jpg")
        private String petImageUrl;

        /**
         * 울타리 경계 목록 단건 항목 DTO를 생성합니다.
         *
         * @param fenceId      울타리 ID
         * @param fenceName    울타리 이름
         * @param latitude     중심 위도
         * @param longitude    중심 경도
         * @param radius       반경(미터)
         * @param isActive     울타리 활성화 여부
         * @param petId        반려동물 ID
         * @param petName      반려동물 이름
         * @param petImageUrl  반려동물 프로필 이미지 URL
         * @return 생성된 응답 DTO
         */
        public static FenceBoundaryItem toDto(
                Long fenceId,
                String fenceName,
                BigDecimal latitude,
                BigDecimal longitude,
                Integer radius,
                Boolean isActive,
                Long petId,
                String petName,
                String petImageUrl
        ) {
            return FenceBoundaryItem.builder()
                    .fenceId(fenceId)
                    .fenceName(fenceName)
                    .center(FenceBoundaryResponse.Center.builder()
                            .latitude(latitude)
                            .longitude(longitude)
                            .build())
                    .radius(radius)
                    .isActive(isActive)
                    .petId(petId)
                    .petName(petName)
                    .petImageUrl(petImageUrl)
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
