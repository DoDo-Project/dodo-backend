package com.dodo.backend.petspecialnote.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 반려동물 특이사항 도메인의 MyBatis 매퍼 인터페이스입니다.
 */
@Mapper
public interface PetSpecialNoteMapper {

    /**
     * 특이사항 내용을 동적으로 수정합니다.
     *
     * @param noteId      수정할 특이사항 ID
     * @param noteContent 수정할 특이사항 내용
     * @param noteType    수정할 특이사항 타입 문자열
     * @return 수정된 행 수
     */
    int updatePetSpecialNote(
            @Param("noteId") Long noteId,
            @Param("noteContent") String noteContent,
            @Param("noteType") String noteType
    );
}
