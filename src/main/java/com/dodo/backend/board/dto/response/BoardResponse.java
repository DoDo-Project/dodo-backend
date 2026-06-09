package com.dodo.backend.board.dto.response;

import com.dodo.backend.board.entity.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 게시글 상세 조회 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 상세 조회 응답")
    public static class BoardDetailResponse {

        @Schema(description = "응답 메시지", example = "게시글 상세 조회에 성공했습니다.")
        private String message;

        @Schema(description = "게시글 ID", example = "123")
        private Long boardId;

        @Schema(description = "게시글 제목", example = "저희 강아지 자랑합니다!")
        private String boardTitle;

        @Schema(description = "게시글 내용", example = "오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?")
        private String boardContent;

        @Schema(description = "게시글 이미지 URL 목록", example = "[\"https://example.com/images/bori_1.jpg\", \"https://example.com/images/bori_2.jpg\"]")
        private List<String> imageFileUrls;

        @Schema(description = "작성자 닉네임", example = "자유로운영혼")
        private String nickname;

        @Schema(description = "조회수", example = "51")
        private Integer viewCount;

        @Schema(description = "게시글 생성 일시", example = "2025-10-06T10:00:00")
        private LocalDateTime boardCreatedAt;

        @Schema(description = "게시글 수정 일시", example = "2025-10-06T10:30:00")
        private LocalDateTime modifiedAt;

        /**
         * 게시글 엔티티와 이미지 URL을 상세 조회 응답 DTO로 변환합니다.
         *
         * @param board        게시글 엔티티
         * @param imageFileUrls 게시글 이미지 URL 목록
         * @param message      응답 메시지
         * @return 게시글 상세 조회 응답 DTO
         */
        public static BoardDetailResponse toDto(Board board, List<String> imageFileUrls, String message) {
            return toDto(board, imageFileUrls, message, board.getViewCount());
        }

        /**
         * 게시글 엔티티와 이미지 URL을 상세 조회 응답 DTO로 변환합니다.
         *
         * @param board         게시글 엔티티
         * @param imageFileUrls 게시글 이미지 URL 목록
         * @param message       응답 메시지
         * @param viewCount     응답에 표시할 조회수
         * @return 게시글 상세 조회 응답 DTO
         */
        public static BoardDetailResponse toDto(Board board, List<String> imageFileUrls, String message, Integer viewCount) {
            return BoardDetailResponse.builder()
                    .message(message)
                    .boardId(board.getBoardId())
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .imageFileUrls(imageFileUrls)
                    .nickname(board.getUser().getNickname())
                    .viewCount(viewCount)
                    .boardCreatedAt(board.getBoardCreatedAt())
                    .modifiedAt(board.getModifiedAt())
                    .build();
        }
    }

    /**
     * 게시글 단순 처리 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 단순 처리 응답")
    public static class BoardSimpleResponse {

        @Schema(description = "응답 메시지", example = "게시글이 성공적으로 수정되었습니다.")
        private String message;

        /**
         * 게시글 단순 처리 응답 DTO를 생성합니다.
         *
         * @param message 응답 메시지
         * @return 게시글 단순 처리 응답 DTO
         */
        public static BoardSimpleResponse toDto(String message) {
            return BoardSimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }
}
