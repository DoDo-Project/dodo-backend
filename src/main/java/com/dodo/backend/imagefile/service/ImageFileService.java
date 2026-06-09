package com.dodo.backend.imagefile.service;

import com.dodo.backend.imagefile.dto.response.ImageFileResponse.ImageUploadResponse;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.pet.entity.Pet;
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

    /**
     * 펫 프로필 이미지 URL을 저장합니다.
     *
     * @param pet          이미지와 연결할 펫
     * @param imageFileUrl 저장할 이미지 URL
     */
    void savePetProfileImage(Pet pet, String imageFileUrl);

    /**
     * 펫 프로필 이미지 URL을 수정합니다.
     * 기존 이미지 정보가 없으면 새로 저장합니다.
     *
     * @param pet          이미지와 연결할 펫
     * @param imageFileUrl 수정할 이미지 URL
     */
    void updatePetProfileImage(Pet pet, String imageFileUrl);

    /**
     * 게시글 이미지 URL 목록을 저장합니다.
     *
     * @param board         이미지와 연결할 게시글
     * @param imageFileUrls 저장할 이미지 URL 목록
     */
    void saveBoardImages(Board board, List<String> imageFileUrls);

    /**
     * 게시글 이미지 URL 목록을 조회합니다.
     *
     * @param boardId 조회할 게시글 ID
     * @return 게시글 이미지 URL 목록
     */
    List<String> getBoardImageUrls(Long boardId);

    /**
     * 게시글 이미지를 URL 목록으로 교체합니다.
     *
     * @param board         이미지와 연결할 게시글
     * @param imageFileUrls 새 이미지 URL 목록
     */
    void replaceBoardImages(Board board, List<String> imageFileUrls);

    /**
     * 게시글에 연결된 모든 이미지를 삭제합니다.
     *
     * @param boardId 삭제할 게시글 ID
     */
    void deleteBoardImages(Long boardId);
}
