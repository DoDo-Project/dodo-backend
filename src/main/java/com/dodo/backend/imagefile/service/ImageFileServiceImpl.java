package com.dodo.backend.imagefile.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dodo.backend.imagefile.dto.response.ImageFileResponse.ImageUploadResponse;
import com.dodo.backend.imagefile.entity.ImageFile;
import com.dodo.backend.imagefile.exception.ImageFileException;
import com.dodo.backend.imagefile.mapper.ImageFileMapper;
import com.dodo.backend.imagefile.repository.ImageFileRepository;
import com.dodo.backend.pet.entity.Pet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.function.ThrowingFunction;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dodo.backend.imagefile.exception.ImageFileErrorCode.IMAGE_UPLOAD_FAILED;
import static com.dodo.backend.imagefile.exception.ImageFileErrorCode.INVALID_REQUEST;

/**
 * {@link ImageFileService}의 구현체입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageFileServiceImpl implements ImageFileService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final ImageFileRepository imageFileRepository;
    private final ImageFileMapper imageFileMapper;
    private final Cloudinary cloudinary;
    private final Tika tika = new Tika();

    /**
     * 펫 ID 목록에 해당하는 프로필 이미지 URL을 일괄 조회합니다.
     * <p>
     * 입력된 펫 ID 목록이 비어 있으면 DB를 조회하지 않고 빈 Map을 반환합니다.
     * 조회된 이미지 파일은 펫 ID를 key, 이미지 URL을 value로 변환합니다.
     *
     * @param petIds 이미지 URL을 조회할 펫 ID 목록
     * @return 펫 ID와 이미지 URL을 매핑한 Map
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> getProfileUrlsByPetIds(List<Long> petIds) {

        if (petIds == null || petIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ImageFile> images = imageFileRepository.findAllByPet_PetIdIn(petIds);

        return images.stream()
                .collect(Collectors.toMap(
                        img -> img.getPet().getPetId(),
                        ImageFile::getImageFileUrl
                ));
    }

    /**
     * 이미지 파일 목록을 Cloudinary에 업로드하고 업로드된 이미지 URL 목록을 반환합니다.
     * <p>
     * 요청 파일이 비어 있거나 허용되지 않은 MIME 타입이면 잘못된 요청 예외를 발생시키고,
     * 정상 파일은 개별 업로드 후 secure URL만 응답 DTO에 담습니다.
     *
     * @param files 업로드할 이미지 파일 목록
     * @return 업로드 성공 메시지와 이미지 URL 목록
     */
    @Transactional(readOnly = true)
    @Override
    public ImageUploadResponse uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ImageFileException(INVALID_REQUEST);
        }

        List<String> imageUrls = files.stream()
                .map(ThrowingFunction.of(
                        this::uploadSingleImage,
                        (message, exception) -> new ImageFileException(IMAGE_UPLOAD_FAILED)
                ))
                .toList();

        return ImageUploadResponse.toDto("이미지 업로드에 성공하였습니다.", imageUrls);
    }

    /**
     * 펫 프로필 이미지 URL을 {@code image_file} 테이블에 신규 저장합니다.
     * <p>
     * 이미지 URL이 비어 있으면 저장하지 않습니다. 파일 크기는 외부 URL만 전달받는
     * 현재 요청 구조상 알 수 없으므로 기본값 {@code 0L}로 저장합니다.
     *
     * @param pet          이미지와 연결할 펫 엔티티
     * @param imageFileUrl 저장할 이미지 URL
     */
    @Transactional
    @Override
    public void savePetProfileImage(Pet pet, String imageFileUrl) {
        if (pet == null || isBlank(imageFileUrl)) {
            return;
        }

        ImageFile imageFile = ImageFile.builder()
                .pet(pet)
                .imageFileUrl(imageFileUrl)
                .size(0L)
                .originalFilename(extractOriginalFilename(imageFileUrl))
                .build();

        imageFileRepository.save(imageFile);
    }

    /**
     * 펫 프로필 이미지 URL을 수정합니다.
     * <p>
     * 기존 이미지 행이 있으면 MyBatis mapper로 URL과 원본 파일명을 갱신하고,
     * 기존 행이 없으면 신규 이미지 정보로 저장합니다.
     *
     * @param pet          이미지와 연결된 펫 엔티티
     * @param imageFileUrl 수정할 이미지 URL
     */
    @Transactional
    @Override
    public void updatePetProfileImage(Pet pet, String imageFileUrl) {
        if (pet == null || pet.getPetId() == null || isBlank(imageFileUrl)) {
            return;
        }

        int updatedRows = imageFileMapper.updatePetProfileImage(
                pet.getPetId(),
                imageFileUrl,
                extractOriginalFilename(imageFileUrl)
        );

        if (updatedRows == 0) {
            savePetProfileImage(pet, imageFileUrl);
        }
    }

    /**
     * 단일 이미지 파일을 검증한 뒤 Cloudinary에 업로드합니다.
     * <p>
     * 파일이 비어 있거나 허용되지 않은 MIME 타입이면 이미지 파일 도메인 예외를 발생시키고,
     * 업로드 결과에 secure URL이 없으면 업로드 실패 예외를 발생시킵니다.
     *
     * @param file 업로드할 이미지 파일
     * @return Cloudinary에서 반환한 secure URL
     * @throws Exception 파일 바이트 조회 또는 Cloudinary 업로드 중 checked exception이 발생한 경우
     */
    private String uploadSingleImage(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new ImageFileException(INVALID_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new ImageFileException(INVALID_REQUEST);
        }

        String detectedMimeType = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
            throw new ImageFileException(INVALID_REQUEST);
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "images",
                        "resource_type", "image"
                )
        );
        Object url = uploadResult.get("secure_url");
        if (url == null) {
            throw new ImageFileException(IMAGE_UPLOAD_FAILED);
        }
        return Objects.toString(url);
    }

    /**
     * 파일 바이트를 기반으로 실제 MIME 타입을 감지합니다.
     *
     * @param file MIME 타입을 확인할 이미지 파일
     * @return 감지된 MIME 타입
     * @throws Exception 파일 바이트 조회 또는 MIME 타입 감지 중 checked exception이 발생한 경우
     */
    private String detectMimeType(MultipartFile file) throws Exception {
        return tika.detect(file.getBytes());
    }

    /**
     * 문자열이 null이거나 공백 문자열인지 확인합니다.
     *
     * @param value 확인할 문자열
     * @return null 또는 공백 문자열이면 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 이미지 URL에서 원본 파일명을 추출합니다.
     * <p>
     * 쿼리 파라미터는 제거하고 마지막 경로 구간을 파일명으로 사용합니다.
     * 추출할 수 없으면 기본 파일명을 반환합니다.
     *
     * @param imageFileUrl 파일명을 추출할 이미지 URL
     * @return 추출된 파일명 또는 기본 파일명
     */
    private String extractOriginalFilename(String imageFileUrl) {
        int queryStart = imageFileUrl.indexOf('?');
        String urlWithoutQuery = queryStart >= 0 ? imageFileUrl.substring(0, queryStart) : imageFileUrl;
        int slashIndex = urlWithoutQuery.lastIndexOf('/');

        if (slashIndex < 0 || slashIndex == urlWithoutQuery.length() - 1) {
            return "pet_profile_image";
        }

        return urlWithoutQuery.substring(slashIndex + 1);
    }
}
