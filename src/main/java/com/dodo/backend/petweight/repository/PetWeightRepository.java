package com.dodo.backend.petweight.repository;

import com.dodo.backend.petweight.entity.PetWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {@link PetWeight} 엔티티의 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface PetWeightRepository extends JpaRepository<PetWeight, Long> {

    /**
     * 여러 반려동물 ID를 입력받아, 각 펫의 가장 최근 체중 기록을 조회합니다.
     * <p>
     * 서브쿼리를 사용하여 그룹별(Pet ID) 가장 늦은 측정 일시(MAX date)를 찾고,
     * 해당 일시와 펫 ID에 매칭되는 체중 정보를 가져옵니다.
     *
     * @param petIds 조회할 반려동물들의 ID 목록
     * @return {@code [PetId(Long), Weight(Double)]} 형태의 Object 배열 리스트
     */
    @Query("SELECT pw.pet.petId, pw.weight " +
            "FROM PetWeight pw " +
            "WHERE pw.petWeightsMeasuredAt IN (" +
            "    SELECT MAX(sub.petWeightsMeasuredAt) " +
            "    FROM PetWeight sub " +
            "    WHERE sub.pet.petId IN :petIds " +
            "    GROUP BY sub.pet.petId" +
            ") " +
            "AND pw.pet.petId IN :petIds")
    List<Object[]> findRecentWeightsByPetIds(@Param("petIds") List<Long> petIds);
}