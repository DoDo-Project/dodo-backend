package com.dodo.backend.petweight.dto.request;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.petweight.entity.PetWeight;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 반려동물 체중(PetWeight) 도메인과 관련된 요청 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "반려동물 체중 관련 요청 DTO 그룹")
public class PetWeightRequest {

    /**
     * 반려동물의 새로운 체중 기록을 추가하기 위해 클라이언트로부터 전달받는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "체중 기록 추가 요청")
    public static class PetWeightRegisterRequest {

        @Schema(description = "측정된 몸무게 (kg)", example = "5.2")
        @NotNull(message = "몸무게는 필수 입력값입니다.")
        @Positive(message = "몸무게는 양수여야 합니다.")
        private Double weight;

        @Schema(description = "측정 일자 (YYYY-MM-DD)", example = "2025-10-14")
        @NotNull(message = "측정 일자는 필수 입력값입니다.")
        private LocalDate petWeightsMeasuredAt;

        /**
         * 요청 DTO의 데이터를 기반으로 새로운 {@link PetWeight} 엔티티를 생성합니다.
         *
         * @param pet 연관된 Pet 엔티티
         * @return 초기화된 PetWeight 엔티티 객체
         */
        public PetWeight toEntity(Pet pet) {
            return PetWeight.builder()
                    .pet(pet)
                    .weight(this.weight)
                    .petWeightsMeasuredAt(this.petWeightsMeasuredAt)
                    .build();
        }
    }

    /**
     * 기존 체중 기록을 수정하기 위해 클라이언트로부터 전달받는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "체중 기록 수정 요청 (변경할 필드만 전송)")
    public static class PetWeightUpdateRequest {

        @Schema(description = "변경할 몸무게 (kg)", example = "5.3")
        @Positive(message = "몸무게는 양수여야 합니다.")
        private Double weight;

        @Schema(description = "변경할 측정 일자 (YYYY-MM-DD)", example = "2025-10-14")
        private LocalDate petWeightsMeasuredAt;
    }
}