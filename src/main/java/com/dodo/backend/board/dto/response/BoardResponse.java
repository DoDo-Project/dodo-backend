package com.dodo.backend.board.dto.response;

import com.dodo.backend.board.entity.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 API에서 사용하는 응답 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "게시글 응답 DTO 그룹")
public class BoardResponse {

    /**
     * 게시글 생성 응답 DTO입니다.
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

        /**
         * 게시글 생성 응답 DTO를 생성합니다.
         *
         * @param boardId 생성된 게시글 ID
         * @param message 응답 메시지
         * @return 게시글 생성 응답 DTO
         */
        public static BoardCreateResponse toDto(Long boardId, String message) {
            return BoardCreateResponse.builder()
                    .message(message)
                    .boardId(boardId)
                    .build();
        }
    }

    /**
     * 게시글 목록 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 목록 조회 응답")
    public static class BoardListResponse {

        @Schema(description = "응답 메시지", example = "게시글 목록 조회를 성공했습니다.")
        private String message;

        @Schema(description = "게시글 목록")
        private List<BoardListItemResponse> boards;

        @Schema(description = "전체 페이지 수", example = "1")
        private int totalPages;

        @Schema(description = "전체 게시글 수", example = "1")
        private long totalElements;

        @Schema(description = "현재 페이지 번호", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        /**
         * MyBatis 조회 결과를 게시글 목록 조회 응답 DTO로 변환합니다.
         *
         * @param queryResponses 게시글 목록 조회 결과
         * @param totalElements  전체 게시글 수
         * @param currentPage    현재 페이지 번호
         * @param pageSize       페이지 크기
         * @param message        응답 메시지
         * @return 게시글 목록 조회 응답 DTO
         */
        public static BoardListResponse toDto(
                List<BoardListQueryResponse> queryResponses,
                long totalElements,
                int currentPage,
                int pageSize,
                String message
        ) {
            List<BoardListItemResponse> boards = queryResponses == null
                    ? List.of()
                    : queryResponses.stream()
                    .map(BoardListItemResponse::toDto)
                    .toList();

            int totalPages = totalElements == 0
                    ? 0
                    : (int) Math.ceil((double) totalElements / pageSize);

            return BoardListResponse.builder()
                    .message(message)
                    .boards(boards)
                    .totalPages(totalPages)
                    .totalElements(totalElements)
                    .currentPage(currentPage)
                    .pageSize(pageSize)
                    .build();
        }
    }

    /**
     * 게시글 목록 아이템 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 목록 아이템 응답")
    public static class BoardListItemResponse {

        private static final int CONTENT_PREVIEW_LENGTH = 20;

        @Schema(description = "게시글 ID", example = "1")
        private Long boardId;

        @Schema(description = "게시글 제목", example = "우리 강아지 자랑합니다")
        private String boardTitle;

        @Schema(description = "게시글 본문 20자 미리보기", example = "오늘 산책하다가 찍은 사진이에요")
        private String boardContentPreview;

        @Schema(description = "대표 이미지 URL", example = "https://example.com/images/bori_1.jpg")
        private String thumbnailImageUrl;

        @Schema(description = "작성자 닉네임", example = "자유로운산책")
        private String nickname;

        @Schema(description = "조회수", example = "51")
        private Integer viewCount;

        @Schema(description = "댓글 수", example = "3")
        private Long commentCount;

        @Schema(description = "좋아요 수", example = "12")
        private Long likeCount;

        @Schema(description = "싫어요 수", example = "1")
        private Long dislikeCount;

        @Schema(description = "게시글 생성 일시", example = "2026-01-31T13:52:32.68613")
        private LocalDateTime createdAt;

        @Schema(description = "게시글 수정 일시", example = "2026-01-31T14:10:12.12345")
        private LocalDateTime modifiedAt;

        /**
         * 게시글 목록 조회 결과를 게시글 목록 아이템 응답 DTO로 변환합니다.
         *
         * @param queryResponse 게시글 목록 조회 결과
         * @return 게시글 목록 아이템 응답 DTO
         */
        public static BoardListItemResponse toDto(BoardListQueryResponse queryResponse) {
            return BoardListItemResponse.builder()
                    .boardId(queryResponse.getBoardId())
                    .boardTitle(queryResponse.getBoardTitle())
                    .boardContentPreview(createContentPreview(queryResponse.getBoardContent()))
                    .thumbnailImageUrl(queryResponse.getThumbnailImageUrl())
                    .nickname(queryResponse.getNickname())
                    .viewCount(queryResponse.getViewCount())
                    .commentCount(defaultZero(queryResponse.getCommentCount()))
                    .likeCount(defaultZero(queryResponse.getLikeCount()))
                    .dislikeCount(defaultZero(queryResponse.getDislikeCount()))
                    .createdAt(queryResponse.getCreatedAt())
                    .modifiedAt(queryResponse.getModifiedAt())
                    .build();
        }

        /**
         * 게시글 본문을 20자 미리보기 문자열로 변환합니다.
         *
         * @param boardContent 게시글 본문
         * @return 게시글 본문 20자 미리보기
         */
        private static String createContentPreview(String boardContent) {
            if (boardContent == null) {
                return "";
            }

            if (boardContent.length() <= CONTENT_PREVIEW_LENGTH) {
                return boardContent;
            }

            return boardContent.substring(0, CONTENT_PREVIEW_LENGTH);
        }

        /**
         * 숫자 값이 null이면 0을 반환합니다.
         *
         * @param value 변환할 숫자 값
         * @return null이 아닌 숫자 값
         */
        private static Long defaultZero(Long value) {
            return value == null ? 0L : value;
        }
    }

    /**
     * 게시글 목록 조회 MyBatis 결과 DTO입니다.
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BoardListQueryResponse {

        @Schema(description = "게시글 ID", example = "1")
        private Long boardId;

        @Schema(description = "게시글 제목", example = "우리 강아지 자랑합니다!")
        private String boardTitle;

        @Schema(description = "게시글 본문", example = "오늘 산책하다 찍은 사진이에요.")
        private String boardContent;

        @Schema(description = "대표 이미지 URL", example = "https://example.com/images/bori_1.jpg")
        private String thumbnailImageUrl;

        @Schema(description = "작성자 닉네임", example = "자유로운산책")
        private String nickname;

        @Schema(description = "조회수", example = "51")
        private Integer viewCount;

        @Schema(description = "댓글 수", example = "3")
        private Long commentCount;

        @Schema(description = "좋아요 수", example = "12")
        private Long likeCount;

        @Schema(description = "싫어요 수", example = "1")
        private Long dislikeCount;

        @Schema(description = "게시글 생성 일시", example = "2026-01-31T13:52:32.68613")
        private LocalDateTime createdAt;

        @Schema(description = "게시글 수정 일시", example = "2026-01-31T14:10:12.12345")
        private LocalDateTime modifiedAt;
    }

    /**
     * 게시글 상세 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 상세 조회 응답")
    public static class BoardDetailResponse {

        @Schema(description = "작성자 프로필 URL", example = "https://example.com/profiles/kim.jpg")
        private String profileUrl;

        @Schema(description = "응답 메시지", example = "게시글 상세 조회에 성공했습니다.")
        private String message;

        @Schema(description = "게시글 ID", example = "123")
        private Long boardId;

        @Schema(description = "게시글 제목", example = "우리 강아지 자랑합니다")
        private String boardTitle;

        @Schema(description = "게시글 내용", example = "오늘 산책하다 찍은 사진이에요.")
        private String boardContent;

        @Schema(description = "게시글 이미지 URL 목록", example = "[\"https://example.com/images/bori_1.jpg\", \"https://example.com/images/bori_2.jpg\"]")
        private List<String> imageFileUrls;

        @Schema(description = "작성자 닉네임", example = "자유로운산책")
        private String nickname;

        @Schema(description = "조회수", example = "51")
        private Integer viewCount;

        @Schema(description = "좋아요 수", example = "12")
        private Long likeCount;

        @Schema(description = "싫어요 수", example = "1")
        private Long dislikeCount;

        @Schema(description = "게시글 생성 일시", example = "2025-10-06T10:00:00")
        private LocalDateTime boardCreatedAt;

        @Schema(description = "게시글 수정 일시", example = "2025-10-06T10:30:00")
        private LocalDateTime modifiedAt;

        /**
         * 게시글 엔티티와 응답에 표시할 조회수를 상세 조회 응답 DTO로 변환합니다.
         *
         * @param board         게시글 엔티티
         * @param imageFileUrls 게시글 이미지 URL 목록
         * @param message       응답 메시지
         * @param viewCount     응답에 표시할 조회수
         * @param likeCount     좋아요 수
         * @param dislikeCount  싫어요 수
         * @return 게시글 상세 조회 응답 DTO
         */
        public static BoardDetailResponse toDto(
                Board board,
                List<String> imageFileUrls,
                String message,
                Integer viewCount,
                Long likeCount,
                Long dislikeCount
        ) {
            return BoardDetailResponse.builder()
                    .message(message)
                    .boardId(board.getBoardId())
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .imageFileUrls(imageFileUrls)
                    .nickname(board.getUser().getNickname())
                    .profileUrl(board.getUser().getProfileUrl())
                    .viewCount(viewCount)
                    .likeCount(defaultZero(likeCount))
                    .dislikeCount(defaultZero(dislikeCount))
                    .boardCreatedAt(board.getBoardCreatedAt())
                    .modifiedAt(board.getModifiedAt())
                    .build();
        }

        private static Long defaultZero(Long value) {
            return value == null ? 0L : value;
        }
    }

    /**
     * 게시글 수정, 삭제처럼 메시지만 반환하는 단순 처리 응답 DTO입니다.
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

    /**
     * 게시글 임시 저장 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "게시글 임시 저장 응답")
    public static class BoardTempSaveResponse {

        @Schema(description = "응답 메시지", example = "게시글이 성공적으로 임시 저장되었습니다.")
        private String message;

        @Schema(description = "Redis에 저장된 임시 데이터의 고유 키", example = "b1a2c3d4-e5f6-7g8h-i9j0-k1l2m3n4o5p6")
        private String sessionKey;

        /**
         * 게시글 임시 저장 응답 DTO를 생성합니다.
         *
         * @param sessionKey Redis에 저장된 임시 데이터의 고유 키
         * @param message    응답 메시지
         * @return 게시글 임시 저장 응답 DTO
         */
        public static BoardTempSaveResponse toDto(String sessionKey, String message) {
            return BoardTempSaveResponse.builder()
                    .message(message)
                    .sessionKey(sessionKey)
                    .build();
        }
    }

    /**
     * Redis에 임시 저장된 게시글 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "임시 저장 게시글 조회 응답")
    public static class BoardTempSaveDetailResponse {

        @Schema(description = "응답 메시지", example = "임시 저장된 게시글을 성공적으로 불러왔습니다.")
        private String message;

        @Schema(description = "임시 저장 게시글 제목", example = "임시 저장 제목")
        private String boardTitle;

        @Schema(description = "임시 저장 게시글 내용", example = "임시 저장 내용")
        private String boardContent;

        @Schema(description = "임시 저장 게시글 이미지 URL 목록", example = "[\"https://example.com/images/bori_1.jpg\", \"https://example.com/images/bori_2.jpg\"]")
        private List<String> imageFileUrls;
    }
}
