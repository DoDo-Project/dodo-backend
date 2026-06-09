package com.dodo.backend.board.service;

import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardUpdateRequest;
import com.dodo.backend.board.dto.response.BoardResponse.BoardDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardSimpleResponse;
import com.dodo.backend.board.entity.Board;

import java.util.UUID;
/**
 * 게시물(Board) 도메인의 비즈니스 로직을 처리하는 서비스 인터페이스입니다.
 */
public interface BoardService {

    /**
     * 게시물 ID로 게시물 엔티티를 조회합니다.
     *
     * @param boardId 게시물 ID
     * @return 게시물 엔티티
     */
    Board getBoardById(Long boardId);

    /**
     * 게시글 생성
     */
    Long createBoard(UUID userId, BoardCreateRequest request);

    /**
     * 특정 게시글의 상세 정보를 조회합니다.
     *
     * @param boardId 조회할 게시글 ID
     * @return 게시글 상세 조회 응답 DTO
     */
    BoardDetailResponse getBoardDetail(UUID userId, Long boardId);

    /**
     * 특정 게시글을 수정합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 수정할 게시글 ID
     * @param request 게시글 수정 요청 DTO
     * @return 게시글 수정 응답 DTO
     */
    BoardSimpleResponse updateBoard(UUID userId, Long boardId, BoardUpdateRequest request);

    /**
     * 특정 게시글을 삭제합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 삭제할 게시글 ID
     * @return 게시글 삭제 응답 DTO
     */
    BoardSimpleResponse deleteBoard(UUID userId, Long boardId);
}
