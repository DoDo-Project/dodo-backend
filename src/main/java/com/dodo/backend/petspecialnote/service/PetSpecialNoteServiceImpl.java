package com.dodo.backend.petspecialnote.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.petspecialnote.entity.NoteType;
import com.dodo.backend.petspecialnote.entity.PetSpecialNote;
import com.dodo.backend.petspecialnote.mapper.PetSpecialNoteMapper;
import com.dodo.backend.petspecialnote.repository.PetSpecialNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * {@link PetSpecialNoteService}의 구현체입니다.
 */
@Service
@RequiredArgsConstructor
public class PetSpecialNoteServiceImpl implements PetSpecialNoteService {

    private final PetSpecialNoteRepository petSpecialNoteRepository;
    private final PetSpecialNoteMapper petSpecialNoteMapper;

    /**
     * 반려동물 특이사항을 생성하고 저장합니다.
     * <p>
     * 전달받은 반려동물 엔티티, 특이사항 내용, 특이사항 타입을 기반으로
     * {@link PetSpecialNote} 엔티티를 생성한 뒤 저장하고 생성된 식별자를 반환합니다.
     *
     * @param pet         대상 반려동물 엔티티
     * @param noteContent 특이사항 내용
     * @param noteType    특이사항 분류 타입 문자열
     * @return 생성된 특이사항 ID
     */
    @Transactional
    @Override
    public Long createPetSpecialNote(Pet pet, String noteContent, String noteType) {
        PetSpecialNote savedNote = petSpecialNoteRepository.save(PetSpecialNote.builder()
                .pet(pet)
                .noteContent(noteContent)
                .noteType(NoteType.valueOf(noteType))
                .build());
        return savedNote.getNoteId();
    }

    /**
     * 특이사항 ID로 특이사항 정보를 조회합니다.
     *
     * @param noteId 특이사항 ID
     * @return 특이사항 엔티티 Optional
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<PetSpecialNote> findPetSpecialNoteById(Long noteId) {
        return petSpecialNoteRepository.findById(noteId);
    }

    /**
     * 특이사항 정보를 동적으로 수정합니다.
     * <p>
     * MyBatis Mapper의 동적 쿼리를 사용하여 입력된 항목만 업데이트합니다.
     *
     * @param noteId      수정할 특이사항 ID
     * @param noteContent 수정할 특이사항 내용
     * @param noteType    수정할 특이사항 분류 타입 문자열
     * @return 수정된 행 수
     */
    @Transactional
    @Override
    public int updatePetSpecialNote(Long noteId, String noteContent, String noteType) {
        return petSpecialNoteMapper.updatePetSpecialNote(noteId, noteContent, noteType);
    }

    /**
     * 특이사항 정보를 삭제합니다.
     *
     * @param noteId 삭제할 특이사항 ID
     */
    @Transactional
    @Override
    public void deletePetSpecialNote(Long noteId) {
        petSpecialNoteRepository.deleteById(noteId);
    }

    /**
     * 특정 반려동물의 특이사항 목록을 조회합니다.
     *
     * @param petId 조회할 반려동물 ID
     * @return 특이사항 목록
     */
    @Transactional(readOnly = true)
    @Override
    public List<PetSpecialNote> getPetSpecialNotes(Long petId) {
        return petSpecialNoteRepository.findAllByPet_PetIdOrderByPetSpecialNotesCreatedAtDesc(petId);
    }

    /**
     * 특정 반려동물의 특이사항 목록을 페이징하여 조회합니다.
     *
     * @param petId 조회할 반려동물 ID
     * @param pageable 페이징/정렬 정보
     * @return 페이징된 특이사항 목록
     */
    @Transactional(readOnly = true)
    @Override
    public Page<PetSpecialNote> getPetSpecialNotes(Long petId, Pageable pageable) {
        return petSpecialNoteRepository.findAllByPet_PetId(petId, pageable);
    }
}
