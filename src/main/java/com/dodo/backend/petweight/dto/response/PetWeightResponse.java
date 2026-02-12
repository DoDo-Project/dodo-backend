package com.dodo.backend.petweight.dto.response;

import com.dodo.backend.petweight.entity.PetWeight;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 반려동물 체중 기록의 페이징 조회 결과를 담는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 체중 기록 페이징 조회 응답")
    public static class PetWeightHistoryResponse {

        @Schema(description = "응답 메시지", example = "조회를 성공했습니다.")
        private String message;

        @Schema(description = "체중 기록 리스트")
        private List<PetWeightInfo> weights;

        @Schema(description = "전체 페이지 수", example = "1")
        private int totalPages;

        @Schema(description = "전체 데이터 수", example = "1")
        private long totalElements;

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int currentPage;

        @Schema(description = "한 페이지 크기", example = "10")
        private int pageSize;

        /**
         * Page 객체와 메시지를 조회 응답 DTO로 변환합니다.
         *
         * @param page    Spring Data JPA의 Page 객체 (PetWeight 엔티티 포함)
         * @param message 전달할 응답 메시지
         * @return 페이징 정보가 포함된 체중 기록 조회 응답 DTO
         */
        public static PetWeightHistoryResponse toDto(Page<PetWeight> page, String message) {
            List<PetWeightInfo> infoList = page.getContent().stream()
                    .map(w -> PetWeightInfo.builder()
                            .weightId(w.getWeightId())
                            .weight(w.getWeight())
                            .petWeightsMeasuredAt(w.getPetWeightsMeasuredAt())
                            .build())
                    .collect(Collectors.toList());

            return PetWeightHistoryResponse.builder()
                    .message(message)
                    .weights(infoList)
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .currentPage(page.getNumber())
                    .pageSize(page.getSize())
                    .build();
        }
    }

    /**
     * 페이징 응답 내부에 포함되는 개별 체중 기록 정보 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "개별 체중 기록 정보")
    public static class PetWeightInfo {
        @Schema(description = "체중 기록 ID", example = "12")
        private Long weightId;

        @Schema(description = "몸무게 (kg)", example = "5.4")
        private Double weight;

        @Schema(description = "측정 일자", example = "2025-10-14")
        private LocalDate petWeightsMeasuredAt;
    }

    /**
     * 체중 기록 수정 요청이 성공적으로 처리되었을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "체중 기록 수정 결과 응답")
    public static class PetWeightUpdateResponse {

        @Schema(description = "응답 메시지", example = "몸무게 기록 수정을 완료했습니다.")
        private String message;

        /**
         * 성공 메시지를 받아 응답 DTO를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param message 클라이언트에게 전달할 성공 메시지
         * @return 초기화된 {@link PetWeightUpdateResponse} 객체
         */
        public static PetWeightUpdateResponse toDto(String message) {
            return PetWeightUpdateResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 체중 기록 삭제 요청이 성공적으로 처리되었을 때 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "체중 기록 삭제 결과 응답")
    public static class PetWeightDeleteResponse {

        @Schema(description = "응답 메시지", example = "몸무게 기록 삭제를 완료했습니다.")
        private String message;

        /**
         * 성공 메시지를 받아 응답 DTO를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param message 클라이언트에게 전달할 성공 메시지
         * @return 초기화된 {@link PetWeightDeleteResponse} 객체
         */
        public static PetWeightDeleteResponse toDto(String message) {
            return PetWeightDeleteResponse.builder()
                    .message(message)
                    .build();
        }
    }
}