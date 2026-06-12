package com.dodo.backend.board.mapper;

import com.dodo.backend.board.dto.request.BoardRequest.BoardUpdateRequest;
import com.dodo.backend.board.dto.response.BoardResponse.BoardListQueryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;
/**
 * 게시글(Board) 도메인의 동적 수정 쿼리를 담당하는 MyBatis Mapper 인터페이스입니다.
 */
@Mapper
public interface BoardMapper {

    /**
     * 공개 상태의 게시글 목록을 페이지 단위로 조회합니다.
     *
     * @param offset 조회 시작 위치
     * @param size   조회 개수
     * @return 게시글 목록 조회 결과
     */
    List<BoardListQueryResponse> findBoardList(
            @Param("offset") int offset,
            @Param("size") int size
    );

    /**
     * 공개 상태의 게시글 전체 개수를 조회합니다.
     *
     * @return 공개 게시글 전체 개수
     */
    long countPublishedBoards();

    /**
     * 특정 사용자가 작성한 게시글 목록을 페이지 단위로 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @param offset 조회 시작 위치
     * @param size   조회 개수
     * @return 내가 쓴 게시글 목록 조회 결과
     */
    List<BoardListQueryResponse> findMyBoardList(
            @Param("userId") UUID userId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /**
     * 특정 사용자가 작성한 게시글 전체 개수를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 내가 쓴 게시글 전체 개수
     */
    long countMyBoards(@Param("userId") UUID userId);

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
