package com.dodo.backend.board.service;

import com.dodo.backend.board.dto.response.BoardResponse;
import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.dto.request.BoardRequest;

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

}