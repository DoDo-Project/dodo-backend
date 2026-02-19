package com.dodo.backend.petspecialnote.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.petspecialnote.entity.PetSpecialNote;

import java.util.List;
import java.util.Optional;

/**
 * 반려동물 특이사항 도메인의 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface PetSpecialNoteService {

    /**
     * 반려동물 특이사항을 저장합니다.
     *
     * @param pet         대상 반려동물 엔티티
     * @param noteContent 특이사항 내용
     * @param noteType    특이사항 분류 타입 문자열
     * @return 생성된 특이사항 ID
     */
    Long createPetSpecialNote(Pet pet, String noteContent, String noteType);

    /**
     * 특이사항 ID로 특이사항 정보를 조회합니다.
     *
     * @param noteId 특이사항 ID
     * @return 특이사항 엔티티 Optional
     */
    Optional<PetSpecialNote> findPetSpecialNoteById(Long noteId);

    /**
     * 특이사항 정보를 수정합니다.
     *
     * @param noteId      수정할 특이사항 ID
     * @param noteContent 수정할 특이사항 내용
     * @param noteType    수정할 특이사항 분류 타입 문자열
     * @return 수정된 행 수
     */
    int updatePetSpecialNote(Long noteId, String noteContent, String noteType);

    /**
     * 특이사항 정보를 삭제합니다.
     *
     * @param noteId 삭제할 특이사항 ID
     */
    void deletePetSpecialNote(Long noteId);

    /**
     * 특정 반려동물의 특이사항 목록을 조회합니다.
     *
     * @param petId 조회할 반려동물 ID
     * @return 특이사항 목록
     */
    List<PetSpecialNote> getPetSpecialNotes(Long petId);
}
