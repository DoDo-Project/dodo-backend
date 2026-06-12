package com.dodo.backend.comment.dto.request;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 API에서 사용하는 요청 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "댓글 요청 DTO 그룹")
public class CommentRequest {

    /**
     * 댓글 작성 요청 DTO입니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "댓글 작성 요청")
    public static class CommentCreateRequest {

        @NotNull(message = "게시글 ID는 필수입니다.")
        @Schema(description = "댓글을 작성할 게시글 ID", example = "1")
        private Long boardId;

        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Schema(description = "댓글 내용", example = "좋은 정보 감사합니다!")
        private String commentContent;

        @Schema(description = "부모 댓글 ID", example = "null")
        private Long parentCommentId;

        /**
         * 댓글 작성 요청 정보를 {@link Comment} 엔티티로 변환합니다.
         *
         * @param board         댓글이 작성될 게시글
         * @param user          댓글 작성자
         * @param parentComment 부모 댓글
         * @return 댓글 엔티티
         */
        public Comment toEntity(Board board, User user, Comment parentComment) {
            return Comment.builder()
                    .board(board)
                    .user(user)
                    .parentComment(parentComment)
                    .commentContent(this.commentContent)
                    .build();
        }
    }

    /**
     * 댓글 수정 요청 DTO입니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "댓글 수정 요청")
    public static class CommentUpdateRequest {

        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Schema(description = "수정할 댓글 내용", example = "수정입니다.")
        private String commentContent;
    }
}
