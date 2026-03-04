package com.dodo.backend.reaction.repository;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.reaction.entity.Reaction;
import com.dodo.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
