package com.dodo.backend.comment.mapper;

import com.dodo.backend.comment.dto.response.CommentResponse.CommentListQueryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 댓글(Comment) 도메인의 목록 조회 및 동적 수정 쿼리를 담당하는 MyBatis Mapper 인터페이스입니다.
 */
@Mapper
public interface CommentMapper {

    /**
     * 특정 게시글의 댓글 목록을 페이지 단위로 조회합니다.
     *
     * @param boardId 댓글을 조회할 게시글 ID
     * @param offset  조회 시작 위치
     * @param size    조회 개수
     * @return 댓글 목록 조회 결과
     */
    List<CommentListQueryResponse> findCommentsByBoardId(
            @Param("boardId") Long boardId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /**
     * 특정 게시글의 댓글 전체 개수를 조회합니다.
     *
     * @param boardId 댓글 수를 조회할 게시글 ID
     * @return 댓글 전체 개수
     */
    long countCommentsByBoardId(@Param("boardId") Long boardId);

    /**
     * 댓글 내용을 수정합니다.
     *
     * @param commentId      수정할 댓글 ID
     * @param commentContent 수정할 댓글 내용
     */
    void updateComment(
            @Param("commentId") Long commentId,
            @Param("commentContent") String commentContent
    );
}
