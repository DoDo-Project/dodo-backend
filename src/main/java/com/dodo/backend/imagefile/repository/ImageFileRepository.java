package com.dodo.backend.imagefile.repository;

import com.dodo.backend.imagefile.entity.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {@link ImageFile} 엔티티의 데이터베이스 접근을 담당하는 리포지토리입니다.
 */
@Repository
public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {

    /**
     * 특정 펫 ID에 해당하는 이미지 파일을 조회합니다.
     *
     * @param petId 조회할 펫의 ID
     * @return 이미지 파일 엔티티 (Optional)
     */
    Optional<ImageFile> findByPet_PetId(Long petId);

    /**
     * 여러 펫 ID들에 해당하는 이미지 파일들을 일괄 조회합니다.
     * <p>
     * 펫 목록 조회 시 N+1 문제를 방지하기 위해 사용됩니다.
     *
     * @param petIds 조회할 펫 ID 목록
     * @return 이미지 파일 엔티티 리스트
     */
    List<ImageFile> findAllByPet_PetIdIn(List<Long> petIds);

    /**
     * 특정 게시글 ID에 연결된 이미지 파일 목록을 이미지 파일 ID 오름차순으로 조회합니다.
     *
     * @param boardId 조회할 게시글 ID
     * @return 게시글 이미지 파일 목록
     */
    List<ImageFile> findAllByBoard_BoardIdOrderByImageFileIdAsc(Long boardId);

    /**
     * 특정 게시글 ID에 연결된 이미지 파일들을 삭제합니다.
     *
     * @param boardId 삭제할 게시글 ID
     */
    void deleteAllByBoard_BoardId(Long boardId);
}
