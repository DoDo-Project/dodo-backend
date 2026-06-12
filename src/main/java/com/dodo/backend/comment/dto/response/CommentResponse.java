package com.dodo.backend.comment.dto.response;

import com.dodo.backend.comment.entity.Comment;
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
public class CommentResponse {

    /**
     * 댓글 작성 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommentCreateResponse {

        /**
         * 응답 메시지입니다.
         */
        private String message;

        /**
         * 생성된 댓글 ID입니다.
         */
        private Long commentId;

        /**
         * 생성된 댓글 내용입니다.
         */
        private String commentContent;

        /**
         * 댓글 작성자 ID입니다.
         */
        private String userId;

        /**
         * 댓글 작성자 닉네임입니다.
         */
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
    public static class CommentListResponse {

        /**
         * 응답 메시지입니다.
         */
        private String message;

        /**
         * 페이지 정보입니다.
         */
        private PageInfoResponse pageInfo;

        /**
         * 댓글 목록입니다.
         */
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
    public static class PageInfoResponse {

        /**
         * 현재 페이지 번호입니다.
         */
        private int page;

        /**
         * 페이지 크기입니다.
         */
        private int size;

        /**
         * 전체 요소 수입니다.
         */
        private long totalElements;

        /**
         * 전체 페이지 수입니다.
         */
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
    public static class CommentItemResponse {

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
         * 댓글 작성자 정보입니다.
         */
        private CommentAuthorResponse author;

        /**
         * 댓글 작성 일시입니다.
         */
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
    public static class CommentAuthorResponse {

        /**
         * 작성자 ID입니다.
         */
        private String userId;

        /**
         * 작성자 닉네임입니다.
         */
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
    public static class CommentSimpleResponse {

        /**
         * 응답 메시지입니다.
         */
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

    /**
     * 내가 쓴 댓글 목록 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyCommentListResponse {

        /**
         * 응답 메시지입니다.
         */
        private String message;

        /**
         * 페이지 정보입니다.
         */
        private PageInfoResponse pageInfo;

        /**
         * 내가 쓴 댓글 목록입니다.
         */
        private List<MyCommentItemResponse> data;

        /**
         * MyBatis 조회 결과를 내가 쓴 댓글 목록 조회 응답 DTO로 변환합니다.
         *
         * @param queryResponses 내가 쓴 댓글 목록 조회 결과
         * @param page           현재 페이지 번호
         * @param size           페이지 크기
         * @param totalElements  전체 댓글 수
         * @param message        응답 메시지
         * @return 내가 쓴 댓글 목록 조회 응답 DTO
         */
        public static MyCommentListResponse toDto(
                List<MyCommentListQueryResponse> queryResponses,
                int page,
                int size,
                long totalElements,
                String message
        ) {
            List<MyCommentItemResponse> data = queryResponses == null
                    ? List.of()
                    : queryResponses.stream()
                    .map(MyCommentItemResponse::toDto)
                    .toList();

            return MyCommentListResponse.builder()
                    .message(message)
                    .pageInfo(PageInfoResponse.toDto(page, size, totalElements))
                    .data(data)
                    .build();
        }
    }

    /**
     * 내가 쓴 댓글 목록 아이템 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyCommentItemResponse {

        /**
         * 댓글 ID입니다.
         */
        private Long commentId;

        /**
         * 댓글이 작성된 게시글 ID입니다.
         */
        private Long boardId;

        /**
         * 댓글이 작성된 게시글 제목입니다.
         */
        private String boardTitle;

        /**
         * 부모 댓글 ID입니다.
         */
        private Long parentCommentId;

        /**
         * 댓글 내용입니다.
         */
        private String commentContent;

        /**
         * 댓글 작성 일시입니다.
         */
        private LocalDateTime createdAt;

        /**
         * 내가 쓴 댓글 목록 조회 결과를 응답 아이템 DTO로 변환합니다.
         *
         * @param queryResponse 내가 쓴 댓글 목록 조회 결과
         * @return 내가 쓴 댓글 목록 아이템 응답 DTO
         */
        public static MyCommentItemResponse toDto(MyCommentListQueryResponse queryResponse) {
            return MyCommentItemResponse.builder()
                    .commentId(queryResponse.getCommentId())
                    .boardId(queryResponse.getBoardId())
                    .boardTitle(queryResponse.getBoardTitle())
                    .parentCommentId(queryResponse.getParentCommentId())
                    .commentContent(queryResponse.getCommentContent())
                    .createdAt(queryResponse.getCreatedAt())
                    .build();
        }
    }

    /**
     * 내가 쓴 댓글 목록 조회 MyBatis 결과 DTO입니다.
     */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyCommentListQueryResponse {

        /**
         * 댓글 ID입니다.
         */
        private Long commentId;

        /**
         * 게시글 ID입니다.
         */
        private Long boardId;

        /**
         * 게시글 제목입니다.
         */
        private String boardTitle;

        /**
         * 부모 댓글 ID입니다.
         */
        private Long parentCommentId;

        /**
         * 댓글 내용입니다.
         */
        private String commentContent;

        /**
         * 댓글 작성 일시입니다.
         */
        private LocalDateTime createdAt;
    }
}
