package com.dodo.backend.fence.repository;

import com.dodo.backend.fence.entity.Fence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 울타리(Fence) 엔티티의 영속성 처리를 담당하는 JPA 리포지토리입니다.
 */
public interface FenceRepository extends JpaRepository<Fence, Long> {

    /**
     * 반려동물 ID로 울타리 정보를 조회합니다.
     *
     * @param petId 반려동물 ID
     * @return 울타리 엔티티(Optional)
     */
    Optional<Fence> findByPet_PetId(Long petId);

}
