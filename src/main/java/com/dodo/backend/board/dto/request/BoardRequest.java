package com.dodo.backend.board.dto.request;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 요청 DTO 그룹
 */
@Schema(description = "게시글 요청 DTO 그룹")
public class BoardRequest {

    /**
     * 게시글 생성 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "게시글 생성 요청")
    public static class BoardCreateRequest {

        @Schema(description = "게시글 제목", example = "저희 강아지 자랑합니다!")
        @NotBlank(message = "제목은 필수입니다.")
        private String boardTitle;

        @Schema(description = "게시글 내용", example = "오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?")
        @NotBlank(message = "내용은 필수입니다.")
        private String boardContent;

        @Schema(description = "게시글 이미지 URL 목록")
        private List<String> imageFileUrls;

        /**
         * DTO → Entity 변환
         */
        public Board toEntity(User user) {
            return Board.builder()
                    .user(user)
                    .boardTitle(this.boardTitle)
                    .boardContent(this.boardContent)
                    .boardStatus(BoardStatus.PUBLISHED)
                    .boardStatusUpdatedAt(LocalDateTime.now())
                    .boardType(BoardType.FREE)
                    .build();
        }
    }

    /**
     * 게시글 수정 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "게시글 수정 요청")
    public static class BoardUpdateRequest {

        @Schema(description = "수정할 게시글 제목", example = "우와 우리 애가!")
        private String boardTitle;

        @Schema(description = "수정할 게시글 내용", example = "산책을 했어요!!")
        private String boardContent;

        @Schema(description = "수정할 게시글 이미지 URL 목록", example = "[\"https://example.com/images/bori_1.jpg\", \"https://example.com/images/bori_2.jpg\"]")
        private List<String> imageFileUrls;

        /**
         * 게시글 테이블에 반영할 수정 필드가 있는지 확인합니다.
         *
         * @return 제목 또는 내용에 실제 텍스트가 있으면 true
         */
        public boolean hasBoardUpdateFields() {
            return StringUtils.hasText(boardTitle) || StringUtils.hasText(boardContent);
        }

        /**
         * 게시글 이미지에 반영할 수정 필드가 있는지 확인합니다.
         *
         * @return 이미지 URL 목록에 실제 텍스트가 하나 이상 있으면 true
         */
        public boolean hasImageUpdateFields() {
            return imageFileUrls != null && imageFileUrls.stream().anyMatch(StringUtils::hasText);
        }
    }
}
