package com.dodo.backend.board.service;

import com.dodo.backend.board.dto.request.BoardRequest;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.exception.BoardException;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.dodo.backend.board.exception.BoardErrorCode.BOARD_NOT_FOUND;

/**
 * {@link BoardService} 구현체입니다.

 * 게시글 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    /**
     * 게시글 저장 / 조회를 위한 JPA Repository
     */
    private final BoardRepository boardRepository;

    /**
     * 사용자 조회를 위한 UserService
     */
    private final UserService userService;

    /**
     * 게시글 ID로 게시글 엔티티 조회
     */
    @Transactional(readOnly = true)
    @Override
    public Board getBoardById(Long boardId) {

        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BOARD_NOT_FOUND));
    }

    /**
     * 게시글 생성
     *
     * @param userId 요청 사용자 ID
     * @param request 게시글 생성 요청 DTO
     * @return 생성된 게시글 ID
     */
    @Override
    @Transactional
    public Long createBoard(UUID userId, BoardCreateRequest request) {


        User user = userService.getUserById(userId);


        Board board = request.toEntity(user);


        Board savedBoard = boardRepository.save(board);


        return savedBoard.getBoardId();
    }
}