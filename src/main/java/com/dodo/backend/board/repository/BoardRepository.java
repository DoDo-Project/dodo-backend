package com.dodo.backend.board.repository;

import com.dodo.backend.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * {@link Board} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
}
