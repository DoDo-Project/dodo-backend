package com.dodo.backend.imagefile.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.imagefile.dto.response.ImageFileResponse.ImageUploadResponse;
import com.dodo.backend.imagefile.entity.ImageFile;
import com.dodo.backend.imagefile.exception.ImageFileException;
import com.dodo.backend.imagefile.mapper.ImageFileMapper;
import com.dodo.backend.imagefile.repository.ImageFileRepository;
import com.dodo.backend.pet.entity.Pet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link ImageFileService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 * <p>
 * 펫 프로필 이미지 일괄 조회 및 URL 매핑 로직을 테스트합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class ImageFileServiceTest {

    @InjectMocks
    private ImageFileServiceImpl imageFileService;

    @Mock
    private ImageFileRepository imageFileRepository;

    @Mock
    private ImageFileMapper imageFileMapper;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    /**
     * 펫 ID 목록을 입력받아 이미지 URL을 정상적으로 조회하는 시나리오를 테스트합니다.
     * <p>
     * 조회된 ImageFile 엔티티들이 Map<PetId, ImageUrl> 형태로
     * 올바르게 변환되는지 검증합니다.
     */
    @Test
    @DisplayName("프로필 이미지 조회 성공: Pet ID 목록에 해당하는 이미지 URL을 Map 형태로 반환한다.")
    void getProfileUrlsByPetIds_Success() {
        log.info("테스트 시작: 프로필 이미지 조회 성공 시나리오");

        // given
        List<Long> petIds = List.of(10L, 20L);
        log.info("요청 Pet IDs: {}", petIds);

        Pet pet1 = Pet.builder().build();
        ReflectionTestUtils.setField(pet1, "petId", 10L);

        ImageFile image1 = ImageFile.builder()
                .pet(pet1)
                .imageFileUrl("https://example.com/pet1.jpg")
                .build();

        Pet pet2 = Pet.builder().build();
        ReflectionTestUtils.setField(pet2, "petId", 20L);

        ImageFile image2 = ImageFile.builder()
                .pet(pet2)
                .imageFileUrl("https://example.com/pet2.jpg")
                .build();

        given(imageFileRepository.findAllByPet_PetIdIn(petIds)).willReturn(List.of(image1, image2));

        // when
        Map<Long, String> result = imageFileService.getProfileUrlsByPetIds(petIds);

        // then
        log.info("조회 결과 Map: {}", result);

        assertEquals(2, result.size());
        assertEquals("https://example.com/pet1.jpg", result.get(10L));
        assertEquals("https://example.com/pet2.jpg", result.get(20L));

        verify(imageFileRepository).findAllByPet_PetIdIn(petIds);

        log.info("테스트 종료: 프로필 이미지 조회 성공 시나리오");
    }

    /**
     * 입력 리스트가 null이거나 비어있을 때 빈 Map을 반환하는지 테스트합니다.
     */
    @Test
    @DisplayName("프로필 이미지 조회: 입력된 ID 리스트가 비어있으면 DB 조회 없이 빈 Map을 반환한다.")
    void getProfileUrlsByPetIds_EmptyInput() {
        log.info("테스트 시작: 프로필 이미지 조회 (빈 리스트)");

        // given
        List<Long> emptyIds = Collections.emptyList();

        // when
        Map<Long, String> result = imageFileService.getProfileUrlsByPetIds(emptyIds);

        // then
        log.info("조회 결과 Map Size: {}", result.size());
        assertTrue(result.isEmpty());

        verify(imageFileRepository, never()).findAllByPet_PetIdIn(anyList());

        log.info("테스트 종료: 프로필 이미지 조회 (빈 리스트)");
    }

    @Test
    @DisplayName("펫 프로필 이미지 저장 성공: 이미지 URL을 ImageFile 엔티티로 저장한다.")
    void savePetProfileImage_Success() {
        // given
        Pet pet = Pet.builder().build();
        String imageFileUrl = "https://example.com/images/bori.jpg";

        // when
        imageFileService.savePetProfileImage(pet, imageFileUrl);

        // then
        verify(imageFileRepository).save(any(ImageFile.class));
    }

    @Test
    @DisplayName("펫 프로필 이미지 수정 성공: 기존 이미지 URL을 mapper로 수정한다.")
    void updatePetProfileImage_Success() {
        // given
        Pet pet = Pet.builder().petId(1L).build();
        String imageFileUrl = "https://example.com/images/bori.jpg";
        given(imageFileMapper.updatePetProfileImage(1L, imageFileUrl, "bori.jpg")).willReturn(1);

        // when
        imageFileService.updatePetProfileImage(pet, imageFileUrl);

        // then
        verify(imageFileMapper).updatePetProfileImage(1L, imageFileUrl, "bori.jpg");
        verify(imageFileRepository, never()).save(any(ImageFile.class));
    }

    @Test
    @DisplayName("게시글 이미지 저장 성공: 이미지 URL 목록을 ImageFile 엔티티 목록으로 저장한다.")
    void saveBoardImages_Success() {
        // given
        Board board = Board.builder().boardId(123L).build();
        List<String> imageFileUrls = List.of(
                "https://example.com/images/bori_1.jpg",
                "https://example.com/images/bori_2.jpg",
                " "
        );

        // when
        imageFileService.saveBoardImages(board, imageFileUrls);

        // then
        ArgumentCaptor<List<ImageFile>> imageFilesCaptor = ArgumentCaptor.forClass(List.class);
        verify(imageFileRepository).saveAll(imageFilesCaptor.capture());

        List<ImageFile> savedImages = imageFilesCaptor.getValue();
        assertEquals(2, savedImages.size());
        assertEquals(board, savedImages.get(0).getBoard());
        assertEquals("https://example.com/images/bori_1.jpg", savedImages.get(0).getImageFileUrl());
        assertEquals("bori_1.jpg", savedImages.get(0).getOriginalFilename());
        assertEquals(0L, savedImages.get(0).getSize());
        assertEquals("https://example.com/images/bori_2.jpg", savedImages.get(1).getImageFileUrl());
    }

    @Test
    @DisplayName("게시글 이미지 조회 성공: 이미지 URL 목록을 반환한다.")
    void getBoardImageUrls_Success() {
        // given
        Long boardId = 123L;
        ImageFile firstImage = ImageFile.builder()
                .imageFileUrl("https://example.com/images/bori_1.jpg")
                .build();
        ImageFile secondImage = ImageFile.builder()
                .imageFileUrl("https://example.com/images/bori_2.jpg")
                .build();

        given(imageFileRepository.findAllByBoard_BoardIdOrderByImageFileIdAsc(boardId))
                .willReturn(List.of(firstImage, secondImage));

        // when
        List<String> imageFileUrls = imageFileService.getBoardImageUrls(boardId);

        // then
        assertEquals(2, imageFileUrls.size());
        assertEquals("https://example.com/images/bori_1.jpg", imageFileUrls.get(0));
        assertEquals("https://example.com/images/bori_2.jpg", imageFileUrls.get(1));
        verify(imageFileRepository).findAllByBoard_BoardIdOrderByImageFileIdAsc(boardId);
    }

    @Test
    @DisplayName("게시글 이미지 조회: 게시글 ID가 null이면 빈 목록을 반환한다.")
    void getBoardImageUrls_NullBoardId() {
        // when
        List<String> imageFileUrls = imageFileService.getBoardImageUrls(null);

        // then
        assertTrue(imageFileUrls.isEmpty());
        verify(imageFileRepository, never()).findAllByBoard_BoardIdOrderByImageFileIdAsc(any());
    }

    @Test
    @DisplayName("게시글 이미지 교체 성공: 기존 이미지를 삭제하고 새 이미지를 저장한다.")
    void replaceBoardImages_Success() {
        // given
        Board board = Board.builder().boardId(123L).build();
        List<String> imageFileUrls = List.of(
                "https://example.com/images/bori_1.jpg",
                "https://example.com/images/bori_2.jpg"
        );

        // when
        imageFileService.replaceBoardImages(board, imageFileUrls);

        // then
        verify(imageFileRepository).deleteAllByBoard_BoardId(123L);
        verify(imageFileRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("게시글 이미지 교체 성공: 빈 리스트가 전달되면 기존 이미지만 삭제한다.")
    void replaceBoardImages_Success_EmptyListDeletesImages() {
        // given
        Board board = Board.builder().boardId(123L).build();

        // when
        imageFileService.replaceBoardImages(board, Collections.emptyList());

        // then
        verify(imageFileRepository).deleteAllByBoard_BoardId(123L);
        verify(imageFileRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("게시글 이미지 삭제 성공: 게시글 ID 기준으로 이미지를 삭제한다.")
    void deleteBoardImages_Success() {
        // given
        Long boardId = 123L;

        // when
        imageFileService.deleteBoardImages(boardId);

        // then
        verify(imageFileRepository).deleteAllByBoard_BoardId(boardId);
    }

    @Test
    @DisplayName("이미지 업로드 성공: 파일 목록 업로드 후 URL 목록을 반환한다.")
    void uploadImages_Success() throws Exception {
        // given
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00};
        byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file1 = new MockMultipartFile("files", "a.jpg", "image/jpeg", jpegBytes);
        MockMultipartFile file2 = new MockMultipartFile("files", "b.png", "image/png", pngBytes);

        given(cloudinary.uploader()).willReturn(uploader);
        given(uploader.upload(any(byte[].class), any(Map.class)))
                .willReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/images/a.jpg"))
                .willReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/images/b.png"));

        // when
        ImageUploadResponse response = imageFileService.uploadImages(List.of(file1, file2));

        // then
        assertNotNull(response);
        assertEquals("이미지 업로드에 성공하였습니다.", response.getMessage());
        assertEquals(2, response.getImageUrls().size());
    }

    @Test
    @DisplayName("이미지 업로드 실패: 이미지가 아닌 파일이면 예외가 발생한다.")
    void uploadImages_Fail_InvalidFileType() {
        // given
        MockMultipartFile file = new MockMultipartFile("files", "a.txt", "text/plain", "text".getBytes());

        // when
        ImageFileException exception = assertThrows(ImageFileException.class, () ->
                imageFileService.uploadImages(List.of(file))
        );

        // then
        assertNotNull(exception.getErrorCode());
    }
}
