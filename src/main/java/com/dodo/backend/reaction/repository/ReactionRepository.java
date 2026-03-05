package com.dodo.backend.reaction.repository;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.reaction.entity.Reaction;
import com.dodo.backend.reaction.entity.ReactionType;
import com.dodo.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link Reaction} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    /**
     * 특정 사용자가 특정 활동 기록에 반응을 남긴 이력이 이미 존재하는지 확인합니다.
     *
     * @param user    반응을 남긴 사용자 엔티티
     * @param history 반응 대상 활동 기록 엔티티
     * @return 반응 이력이 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByUserAndHistory(User user, ActivityHistory history);

    /**
     * 특정 사용자가 특정 게시물에 반응을 남긴 이력이 이미 존재하는지 확인합니다.
     *
     * @param user  반응을 남긴 사용자 엔티티
     * @param board 반응 대상 게시물 엔티티
     * @return 반응 이력이 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByUserAndBoard(User user, Board board);

    /**
     * 특정 사용자가 특정 활동 기록에 남긴 반응 엔티티를 조회합니다.
     *
     * @param userId    반응을 남긴 사용자 ID
     * @param historyId 반응 대상 활동 기록 ID
     * @return 반응 엔티티 Optional
     */
    Optional<Reaction> findByUser_UsersIdAndHistory_HistoryId(UUID userId, Long historyId);

    /**
     * 특정 사용자가 특정 게시물에 남긴 반응 엔티티를 조회합니다.
     *
     * @param userId  반응을 남긴 사용자 ID
     * @param boardId 반응 대상 게시물 ID
     * @return 반응 엔티티 Optional
     */
    Optional<Reaction> findByUser_UsersIdAndBoard_BoardId(UUID userId, Long boardId);

    /**
     * 특정 사용자가 지정한 활동 기록 목록에 남긴 반응을 조회합니다.
     */
    List<Reaction> findByUser_UsersIdAndHistory_HistoryIdIn(UUID userId, Collection<Long> historyIds);

    /**
     * 지정한 활동 기록 목록에 대해 특정 반응 타입 개수를 historyId별로 집계합니다.
     */
    @Query("""
            SELECT r.history.historyId AS historyId, COUNT(r) AS reactionCount
            FROM Reaction r
            WHERE r.history.historyId IN :historyIds
              AND r.reactionType = :reactionType
            GROUP BY r.history.historyId
            """)
    List<HistoryReactionCountProjection> countGroupedByHistoryIdsAndReactionType(
            @Param("historyIds") Collection<Long> historyIds,
            @Param("reactionType") ReactionType reactionType
    );

    interface HistoryReactionCountProjection {
        Long getHistoryId();
        Long getReactionCount();
    }
}
