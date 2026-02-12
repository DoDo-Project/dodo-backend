package com.dodo.backend.petweight.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 반려동물 체중(PetWeight) 도메인과 관련된 응답 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "반려동물 체중 관련 응답 DTO 그룹")
public class PetWeightResponse {

    /**
     * 체중 기록 추가 요청이 성공적으로 처리되었을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "체중 기록 추가 결과 응답")
    public static class PetWeightRegisterResponse {

        @Schema(description = "처리 결과 메시지", example = "반려동물 몸무게 기록 추가를 완료했습니다.")
        private String message;

        @Schema(description = "생성된 체중 기록의 고유 ID", example = "1")
        private Long weightId;

        /**
         * 생성된 체중 기록 ID와 메시지를 받아 응답 DTO를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param weightId 생성된 체중 기록의 고유 식별자
         * @param message  클라이언트에게 전달할 성공 메시지
         * @return 초기화된 {@link PetWeightRegisterResponse} 객체
         */
        public static PetWeightRegisterResponse toDto(Long weightId, String message) {
            return PetWeightRegisterResponse.builder()
                    .weightId(weightId)
                    .message(message)
                    .build();
        }
    }
}