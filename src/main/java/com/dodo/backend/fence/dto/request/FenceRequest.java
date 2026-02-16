package com.dodo.backend.fence.dto.request;

import com.dodo.backend.fence.entity.Fence;
import com.dodo.backend.pet.entity.Pet;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 울타리(Fence) 도메인 요청 DTO를 정의하는 그룹 클래스입니다.
 */
@Schema(description = "울타리 요청 DTO 그룹")
public class FenceRequest {

    /**
     * 울타리 활성화/비활성화 상태를 변경할 때 사용하는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "울타리 활성화 상태 변경 요청")
    public static class FenceToggleRequest {

        @Schema(description = "울타리 활성화 여부", example = "true")
        @NotNull(message = "잘못된 요청입니다.")
        private Boolean fenceIsActive;
    }

    /**
     * 울타리 거리 범위를 설정할 때 사용하는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "울타리 거리 범위 설정 요청")
    public static class FenceRangeRequest {

        @Schema(description = "반려동물 ID", example = "1")
        @NotNull(message = "잘못된 요청입니다.")
        private Long petId;

        @Schema(description = "울타리 중심 위도", example = "37.5665")
        @NotNull(message = "잘못된 요청입니다.")
        @DecimalMin(value = "-90.0", message = "잘못된 요청입니다.")
        @DecimalMax(value = "90.0", message = "잘못된 요청입니다.")
        private BigDecimal centerLatitude;

        @Schema(description = "울타리 중심 경도", example = "126.9780")
        @NotNull(message = "잘못된 요청입니다.")
        @DecimalMin(value = "-180.0", message = "잘못된 요청입니다.")
        @DecimalMax(value = "180.0", message = "잘못된 요청입니다.")
        @JsonAlias("centerLongtitude")
        private BigDecimal centerLongitude;

        @Schema(description = "울타리 이름", example = "집 주변 울타리")
        @Size(max = 255, message = "잘못된 요청입니다.")
        private String fenceName;

        @Schema(description = "울타리 반경(미터)", example = "500")
        @NotNull(message = "잘못된 요청입니다.")
        @Positive(message = "잘못된 요청입니다.")
        private Integer radius;

        /**
         * 요청 데이터를 바탕으로 새 울타리 엔티티를 생성합니다.
         *
         * @param pet 연관 반려동물 엔티티
         * @return 생성할 울타리 엔티티
         */
        public Fence toEntity(Pet pet) {
            return Fence.builder()
                    .pet(pet)
                    .name(this.fenceName)
                    .centerLatitude(this.centerLatitude)
                    .centerLongitude(this.centerLongitude)
                    .radius(this.radius)
                    .build();
        }
    }
}
