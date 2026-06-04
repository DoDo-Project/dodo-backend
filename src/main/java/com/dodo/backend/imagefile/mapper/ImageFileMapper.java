package com.dodo.backend.imagefile.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImageFileMapper {

    /**
     * 펫 프로필 이미지 URL을 수정합니다.
     *
     * @param petId            수정할 펫 ID
     * @param imageFileUrl     새로운 이미지 URL
     * @param originalFilename 이미지 URL에서 추출한 파일명
     * @return 수정된 행 수
     */
    int updatePetProfileImage(
            @Param("petId") Long petId,
            @Param("imageFileUrl") String imageFileUrl,
            @Param("originalFilename") String originalFilename
    );
}
