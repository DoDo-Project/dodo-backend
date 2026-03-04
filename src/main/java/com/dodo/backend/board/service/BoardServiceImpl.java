package com.dodo.backend.board.service;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.reaction.exception.ReactionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.dodo.backend.reaction.exception.ReactionErrorCode.BOARD_NOT_FOUND;

/**
 * {@link BoardService} 구현체입니다.
 */
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;

    /**
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public Board getBoardById(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ReactionException(BOARD_NOT_FOUND));
    }
}
