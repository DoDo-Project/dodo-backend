package com.dodo.backend.reaction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 반응(Reaction) 도메인과 관련된 응답 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "반응 도메인 응답 DTO 그룹")
public class ReactionResponse {

    /**
     * 메시지만 반환하는 단순 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반응 처리 단순 응답 DTO")
    public static class ReactionSimpleResponse {

        @Schema(description = "처리 결과 메시지", example = "반응이 성공적으로 추가되었습니다.")
        private String message;

        /**
         * 메시지를 기반으로 단순 응답 DTO를 생성합니다.
         *
         * @param message 클라이언트에 전달할 처리 결과 메시지
         * @return 메시지를 포함한 응답 DTO
         */
        public static ReactionSimpleResponse toDto(String message) {
            return ReactionSimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }
}
