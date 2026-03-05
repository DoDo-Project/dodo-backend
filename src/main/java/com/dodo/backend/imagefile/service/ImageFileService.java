package com.dodo.backend.imagefile.service;

import com.dodo.backend.imagefile.dto.response.ImageFileResponse.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 이미지 파일 도메인의 비즈니스 로직을 담당하는 서비스 인터페이스입니다.
 */
public interface ImageFileService {

    /**
     * 펫 ID 목록에 해당하는 프로필 이미지 URL들을 일괄 조회합니다.
     *
     * @param petIds 이미지 URL을 조회할 펫 ID 목록
     * @return 펫 ID(Key)와 이미지 URL(Value)을 매핑한 Map 객체
     */
    Map<Long, String> getProfileUrlsByPetIds(List<Long> petIds);

    /**
     * 이미지 파일 목록을 업로드하고 URL 목록을 반환합니다.
     *
     * @param files 업로드할 이미지 파일 목록
     * @return 업로드 응답 DTO
     */
    ImageUploadResponse uploadImages(List<MultipartFile> files);
}
