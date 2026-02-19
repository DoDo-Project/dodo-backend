package com.dodo.backend.petspecialnote.repository;

import com.dodo.backend.petspecialnote.entity.PetSpecialNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 반려동물 특이사항 엔티티의 영속성 처리를 담당하는 JPA 리포지토리입니다.
 */
@Repository
public interface PetSpecialNoteRepository extends JpaRepository<PetSpecialNote, Long> {

    /**
     * 특정 반려동물의 특이사항 목록을 생성일 내림차순으로 조회합니다.
     *
     * @param petId 조회할 반려동물 ID
     * @return 특이사항 목록
     */
    List<PetSpecialNote> findAllByPet_PetIdOrderByPetSpecialNotesCreatedAtDesc(Long petId);

    /**
     * 특정 반려동물의 특이사항 목록을 페이징하여 조회합니다.
     *
     * @param petId     조회할 반려동물 ID
     * @param pageable  페이징/정렬 정보
     * @return 페이징된 특이사항 목록
     */
    Page<PetSpecialNote> findAllByPet_PetId(Long petId, Pageable pageable);
}
