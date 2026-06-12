package com.dodo.backend.board.repository;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * {@link Board} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    /**
     * 게시글 유형과 상태에 맞는 게시글 목록을 생성일시 최신순으로 조회합니다.
     *
     * @param boardType 조회할 게시글 유형
     * @param boardStatus 조회할 게시글 상태
     * @param pageable 조회 개수와 페이지 정보를 담은 Pageable
     * @return 조건에 맞는 게시글 목록
     */
    List<Board> findByBoardTypeAndBoardStatusOrderByBoardCreatedAtDesc(
            BoardType boardType,
            BoardStatus boardStatus,
            Pageable pageable
    );
}
