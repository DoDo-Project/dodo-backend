package com.dodo.backend.imagefile.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dodo.backend.imagefile.dto.response.ImageFileResponse.ImageUploadResponse;
import com.dodo.backend.imagefile.entity.ImageFile;
import com.dodo.backend.imagefile.repository.ImageFileRepository;
import com.dodo.backend.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dodo.backend.user.exception.UserErrorCode.INVALID_REQUEST;

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
    private final Cloudinary cloudinary;
    private final Tika tika = new Tika();

    /**
     * {@inheritDoc}
     * <p>
     * <b>처리 과정:</b>
     * <ol>
     * <li>입력된 펫 ID 목록이 비어있으면 빈 Map을 반환합니다.</li>
     * <li>리포지토리의 {@code findAllByPet_PetIdIn}을 호출하여 펫 ID 목록에 해당하는 이미지 파일들을 조회합니다.</li>
     * <li>조회된 엔티티에서 펫 ID와 이미지 URL을 추출하여 Map으로 변환합니다.</li>
     * </ol>
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
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public ImageUploadResponse uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new UserException(INVALID_REQUEST);
        }

        List<String> imageUrls = files.stream()
                .map(this::uploadSingleImage)
                .toList();

        return ImageUploadResponse.toDto("이미지 업로드에 성공하였습니다.", imageUrls);
    }

    private String uploadSingleImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UserException(INVALID_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new UserException(INVALID_REQUEST);
        }

        String detectedMimeType = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
            throw new UserException(INVALID_REQUEST);
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "images",
                            "resource_type", "image"
                    )
            );
            Object url = uploadResult.get("secure_url");
            if (url == null) {
                throw new IllegalStateException("Cloudinary 응답에 secure_url이 없습니다.");
            }
            return Objects.toString(url);
        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    private String detectMimeType(MultipartFile file) {
        try {
            return tika.detect(file.getBytes());
        } catch (Exception e) {
            throw new UserException(INVALID_REQUEST);
        }
    }
}
