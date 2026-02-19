package com.dodo.backend.activityhistory.repository;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * {@link ActivityHistory} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface ActivityHistoryRepository extends JpaRepository<ActivityHistory, Long> {

    /**
     * 특정 반려동물의 활동 상태가 주어진 상태(status)와 일치하는 기록이 존재하는지 확인합니다.
     * <p>
     * 주로 이미 진행 중인 활동(IN_PROGRESS)이 있는지 중복 체크할 때 사용됩니다.
     * </p>
     *
     * @param pet    확인할 반려동물 엔티티
     * @param status 확인할 활동 상태 (예: IN_PROGRESS)
     * @return 해당 상태의 활동 기록이 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByPetAndActivityHistoryStatus(Pet pet, ActivityHistoryStatus status);

    /**
     * 특정 사용자의 활동 기록을 페이징하여 조회합니다.
     *
     * @param user     조회할 사용자 엔티티
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬 등)
     * @return 페이징된 활동 기록 리스트 (Page 객체)
     */
    Page<ActivityHistory> findAllByUser(User user, Pageable pageable);

    /**
     * 특정 반려동물의 가장 최근 활동 기록을 조회합니다.
     * <p>
     * 활동 기록 ID(historyId)를 기준으로 내림차순 정렬하여 가장 마지막에 생성된 기록을 반환합니다.
     * </p>
     *
     * @param pet 조회할 반려동물 엔티티
     * @return 가장 최근의 활동 기록을 포함한 {@link Optional} 객체
     */
    Optional<ActivityHistory> findFirstByPetOrderByHistoryIdDesc(Pet pet);

    /**
     * 특정 반려동물 ID의 가장 최근 활동 기록을 조회합니다.
     *
     * @param petId 조회할 반려동물 ID
     * @return 가장 최근의 활동 기록 Optional
     */
    Optional<ActivityHistory> findFirstByPet_PetIdOrderByHistoryIdDesc(Long petId);

    /**
     * 특정 반려동물의 일간 활동 기록을 조회합니다.
     */
    List<ActivityHistory> findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualAndActivityHistoryStartAtLessThanOrderByActivityHistoryStartAtAsc(
            Long petId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    /**
     * 특정 반려동물의 주간 활동 기록을 조회합니다.
     */
    List<ActivityHistory> findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualOrderByActivityHistoryStartAtAsc(
            Long petId,
            LocalDateTime fromDateTime
    );

    /**
     * 특정 반려동물의 월간 활동 기록을 조회합니다.
     */
    List<ActivityHistory> findAllByPet_PetIdAndActivityHistoryStartAtBetweenOrderByActivityHistoryStartAtAsc(
            Long petId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
