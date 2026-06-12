package com.dodo.backend.comment.dto.response;

import com.dodo.backend.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 API에서 사용하는 응답 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "댓글 응답 DTO 그룹")
public class CommentResponse {

    /**
     * 댓글 작성 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "댓글 작성 응답")
    public static class CommentCreateResponse {

        @Schema(description = "응답 메시지", example = "댓글이 성공적으로 작성되었습니다.")
        private String message;

        @Schema(description = "댓글 ID", example = "123")
        private Long commentId;

        @Schema(description = "댓글 내용", example = "좋은 정보 감사합니다!")
        private String commentContent;

        @Schema(description = "작성자 ID", example = "uuid-user-1")
        private String userId;

        @Schema(description = "작성자 닉네임", example = "멍멍이집사")
        private String nickname;

        /**
         * 댓글 엔티티를 댓글 작성 응답 DTO로 변환합니다.
         *
         * @param comment 댓글 엔티티
         * @param message 응답 메시지
         * @return 댓글 작성 응답 DTO
         */
        public static CommentCreateResponse toDto(Comment comment, String message) {
            return CommentCreateResponse.builder()
                    .message(message)
                    .commentId(comment.getCommentId())
                    .commentContent(comment.getCommentContent())
                    .userId(comment.getUser().getUsersId().toString())
                    .nickname(comment.getUser().getNickname())
                    .build();
        }
    }

    /**
     * 댓글 목록 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "댓글 목록 조회 응답")
    public static class CommentListResponse {

        @Schema(description = "응답 메시지", example = "댓글 목록을 성공적으로 조회했습니다.")
        private String message;

        @Schema(description = "페이지 정보")
        private PageInfoResponse pageInfo;

        @Schema(description = "댓글 목록")
        private List<CommentItemResponse> data;

        /**
         * MyBatis 조회 결과를 댓글 목록 조회 응답 DTO로 변환합니다.
         *
         * @param queryResponses 댓글 목록 조회 결과
         * @param page           현재 페이지 번호
         * @param size           페이지 크기
         * @param totalElements  전체 댓글 수
         * @param message        응답 메시지
         * @return 댓글 목록 조회 응답 DTO
         */
        public static CommentListResponse toDto(
                List<CommentListQueryResponse> queryResponses,
                int page,
                int size,
                long totalElements,
                String message
        ) {
            List<CommentItemResponse> data = queryResponses == null
                    ? List.of()
                    : queryResponses.stream()
                    .map(CommentItemResponse::toDto)
                    .toList();

            return CommentListResponse.builder()
                    .message(message)
                    .pageInfo(PageInfoResponse.toDto(page, size, totalElements))
                    .data(data)
                    .build();
        }
    }

    /**
     * 페이지 정보 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "페이지 정보 응답")
    public static class PageInfoResponse {

        @Schema(description = "현재 페이지 번호", example = "0")
        private int page;

        @Schema(description = "페이지 크기", example = "10")
        private int size;

        @Schema(description = "전체 요소 수", example = "48")
        private long totalElements;

        @Schema(description = "전체 페이지 수", example = "5")
        private int totalPages;

        /**
         * 페이지 정보 응답 DTO를 생성합니다.
         *
         * @param page          현재 페이지 번호
         * @param size          페이지 크기
         * @param totalElements 전체 요소 수
         * @return 페이지 정보 응답 DTO
         */
        public static PageInfoResponse toDto(int page, int size, long totalElements) {
            int totalPages = totalElements == 0
                    ? 0
                    : (int) Math.ceil((double) totalElements / size);

            return PageInfoResponse.builder()
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .build();
        }
    }

    /**
     * 댓글 목록 아이템 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "댓글 목록 아이템 응답")
    public static class CommentItemResponse {

        @Schema(description = "댓글 ID", example = "102")
        private Long commentId;

        @Schema(description = "부모 댓글 ID", example = "null")
        private Long parentCommentId;

        @Schema(description = "댓글 내용", example = "두 번째 댓글입니다.")
        private String commentContent;

        @Schema(description = "작성자 정보")
        private CommentAuthorResponse author;

        @Schema(description = "댓글 작성 일시", example = "2025-10-14T14:30:00")
        private LocalDateTime createdAt;

        /**
         * 댓글 목록 조회 결과를 댓글 목록 아이템 응답 DTO로 변환합니다.
         *
         * @param queryResponse 댓글 목록 조회 결과
         * @return 댓글 목록 아이템 응답 DTO
         */
        public static CommentItemResponse toDto(CommentListQueryResponse queryResponse) {
            return CommentItemResponse.builder()
                    .commentId(queryResponse.getCommentId())
                    .parentCommentId(queryResponse.getParentCommentId())
                    .commentContent(queryResponse.getCommentContent())
                    .author(CommentAuthorResponse.toDto(queryResponse.getUserId(), queryResponse.getNickname()))
                    .createdAt(queryResponse.getCreatedAt())
                    .build();
        }
    }

    /**
     * 댓글 작성자 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "댓글 작성자 응답")
    public static class CommentAuthorResponse {

        @Schema(description = "작성자 ID", example = "uuid-user-1")
        private String userId;

        @Schema(description = "작성자 닉네임", example = "행복한강아지")
        private String nickname;

        /**
         * 댓글 작성자 응답 DTO를 생성합니다.
         *
         * @param userId   작성자 ID
         * @param nickname 작성자 닉네임
         * @return 댓글 작성자 응답 DTO
         */
        public static CommentAuthorResponse toDto(String userId, String nickname) {
            return CommentAuthorResponse.builder()
                    .userId(userId)
                    .nickname(nickname)
                    .build();
        }
    }

    /**
     * 댓글 목록 조회 MyBatis 결과 DTO입니다.
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommentListQueryResponse {

        /**
         * 댓글 ID입니다.
         */
        private Long commentId;

        /**
         * 부모 댓글 ID입니다.
         */
        private Long parentCommentId;

        /**
         * 댓글 내용입니다.
         */
        private String commentContent;

        /**
         * 작성자 ID입니다.
         */
        private String userId;

        /**
         * 작성자 닉네임입니다.
         */
        private String nickname;

        /**
         * 댓글 작성 일시입니다.
         */
        private LocalDateTime createdAt;
    }

    /**
     * 댓글 수정, 삭제처럼 메시지만 반환하는 단순 처리 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "댓글 단순 처리 응답")
    public static class CommentSimpleResponse {

        @Schema(description = "응답 메시지", example = "댓글이 성공적으로 수정되었습니다.")
        private String message;

        /**
         * 댓글 단순 처리 응답 DTO를 생성합니다.
         *
         * @param message 응답 메시지
         * @return 댓글 단순 처리 응답 DTO
         */
        public static CommentSimpleResponse toDto(String message) {
            return CommentSimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }
}
