package com.dodo.backend.board.dto.response;

import com.dodo.backend.board.entity.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 도메인 응답 DTO 그룹
 */
@Schema(description = "게시글 응답 DTO 그룹")
public class BoardResponse {

    /**
     * 게시글 생성 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "게시글 생성 응답")
    public static class BoardCreateResponse {

        @Schema(description = "응답 메시지", example = "게시글이 성공적으로 작성되었습니다.")
        private String message;

        @Schema(description = "생성된 게시글 ID", example = "1")
        private Long boardId;

        public static BoardCreateResponse toDto(Long boardId, String message) {
            return BoardCreateResponse.builder()
                    .message(message)
                    .boardId(boardId)
                    .build();
        }
    }
}