package com.dodo.backend.board.mapper;

import com.dodo.backend.board.dto.request.BoardRequest.BoardUpdateRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 게시글(Board) 도메인의 동적 수정 쿼리를 담당하는 MyBatis Mapper 인터페이스입니다.
 */
@Mapper
public interface BoardMapper {

    /**
     * 게시글 제목과 내용을 선택적으로 수정합니다.
     *
     * @param boardId 수정할 게시글 ID
     * @param request 수정할 게시글 정보가 담긴 요청 DTO
     */
    void updateBoard(@Param("boardId") Long boardId, @Param("request") BoardUpdateRequest request);

    /**
     * 게시글 조회수를 1 증가시킵니다.
     *
     * @param boardId 조회수를 증가시킬 게시글 ID
     */
    void increaseViewCount(@Param("boardId") Long boardId);

    /**
     * 게시글 상태를 삭제 상태로 변경합니다.
     *
     * @param boardId 삭제할 게시글 ID
     * @param status  변경할 게시글 상태
     */
    void deleteBoard(@Param("boardId") Long boardId, @Param("status") String status);
}
