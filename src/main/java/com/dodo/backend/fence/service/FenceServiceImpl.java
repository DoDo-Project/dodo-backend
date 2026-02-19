package com.dodo.backend.fence.service;

import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeRequest;
import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeUpdateRequest;
import com.dodo.backend.fence.dto.request.FenceRequest.FenceToggleRequest;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceBoundaryItem;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceBoundaryListResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceLocationCheckResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeUpdateResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceBoundaryResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceStatusResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceToggleResponse;
import com.dodo.backend.fence.entity.Fence;
import com.dodo.backend.fence.exception.FenceException;
import com.dodo.backend.fence.mapper.FenceMapper;
import com.dodo.backend.fence.repository.FenceRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.fence.exception.FenceErrorCode.FENCE_INFO_NOT_FOUND;
import static com.dodo.backend.fence.exception.FenceErrorCode.FENCE_NOT_FOUND;
import static com.dodo.backend.fence.exception.FenceErrorCode.FENCE_ALREADY_EXISTS;
import static com.dodo.backend.fence.exception.FenceErrorCode.FENCE_ACCESS_DENIED;
import static com.dodo.backend.fence.exception.FenceErrorCode.FENCE_PERMISSION_DENIED;
import static com.dodo.backend.fence.exception.FenceErrorCode.INVALID_REQUEST;
import static com.dodo.backend.fence.exception.FenceErrorCode.PET_NOT_FOUND;
import static com.dodo.backend.fence.exception.FenceErrorCode.PET_PERMISSION_DENIED;

/**
 * {@link FenceService}의 구현체입니다.
 * <p>
 * 울타리 도메인에 대한 실제 비즈니스 흐름을 이 클래스에서 구현합니다.
 */
@Service
@RequiredArgsConstructor
public class FenceServiceImpl implements FenceService {

    private final FenceRepository fenceRepository;
    private final FenceMapper fenceMapper;
    private final PetService petService;
    private final UserPetService userPetService;
    private final ImageFileService imageFileService;

    /**
     * {@inheritDoc}
     * <p>
     * 1. 반려동물 존재 여부를 검증합니다.
     * 2. 요청 사용자의 반려동물 접근 권한을 검증합니다.
     * 3. 동일 반려동물의 기존 울타리 존재 여부를 검증합니다.
     * 4. 기존 울타리가 없을 때만 새 울타리를 생성합니다.
     */
    @Transactional
    @Override
    public FenceRangeResponse setFenceRange(UUID userId, FenceRangeRequest request) {

        if (!petService.existsPetById(request.getPetId())) {
            throw new FenceException(PET_NOT_FOUND);
        }

        if (!userPetService.isApprovedPetOwner(userId, request.getPetId())) {
            throw new FenceException(PET_PERMISSION_DENIED);
        }

        if (fenceRepository.findByPet_PetId(request.getPetId()).isPresent()) {
            throw new FenceException(FENCE_ALREADY_EXISTS);
        }

        Pet pet = petService.getPetById(request.getPetId());
        Fence savedFence = request.toEntity(pet);
        fenceRepository.save(savedFence);

        return FenceRangeResponse.toDto("울타리 설정을 완료했습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 울타리 존재 여부를 확인하여 대상 반려동물 ID를 조회합니다.
     * 2. 반려동물 존재 여부를 검증합니다.
     * 3. 요청 사용자의 반려동물 접근 권한을 검증합니다.
     * 4. MyBatis Mapper를 통해 울타리 활성화 상태를 변경합니다.
     */
    @Transactional
    @Override
    public FenceToggleResponse toggleFence(UUID userId, Long fenceId, FenceToggleRequest request) {
        Fence fence = fenceRepository.findById(fenceId)
                .orElseThrow(() -> new FenceException(PET_NOT_FOUND));

        Long petId = fence.getPet().getPetId();

        if (!petService.existsPetById(petId)) {
            throw new FenceException(PET_NOT_FOUND);
        }

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new FenceException(PET_PERMISSION_DENIED);
        }

        fenceMapper.updateFenceIsActive(fenceId, request.getFenceIsActive());

        return FenceToggleResponse.toDto("울타리 상태를 변경하는데 성공했습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 수정 대상 울타리 존재 여부를 확인합니다.
     * 2. 요청 사용자의 울타리 접근 권한을 검증합니다.
     * 3. MyBatis Mapper를 통해 울타리 범위 정보를 수정합니다.
     * 4. 수정 완료 메시지를 반환합니다.
     */
    @Transactional
    @Override
    public FenceRangeUpdateResponse updateFenceRange(UUID userId, Long fenceId, FenceRangeUpdateRequest request) {
        Fence fence = fenceRepository.findById(fenceId)
                .orElseThrow(() -> new FenceException(FENCE_NOT_FOUND));

        Long petId = fence.getPet().getPetId();

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new FenceException(FENCE_PERMISSION_DENIED);
        }

        if (request.getFenceName() == null
                && request.getCenterLatitude() == null
                && request.getCenterLongitude() == null
                && request.getRadius() == null) {
            throw new FenceException(INVALID_REQUEST);
        }

        fenceMapper.updateFenceRange(
                fenceId,
                request.getFenceName(),
                request.getCenterLatitude(),
                request.getCenterLongitude(),
                request.getRadius()
        );
        return FenceRangeUpdateResponse.toDto("울타리 정보를 수정했습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 조회 대상 울타리 존재 여부를 확인합니다.
     * 2. 요청 사용자의 울타리 접근 권한을 검증합니다.
     * 3. 울타리 중심 좌표/반경/ID를 응답으로 반환합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public FenceBoundaryResponse getFenceBoundary(UUID userId, Long fenceId) {
        Fence fence = fenceRepository.findById(fenceId)
                .orElseThrow(() -> new FenceException(FENCE_INFO_NOT_FOUND));

        Long petId = fence.getPet().getPetId();
        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new FenceException(FENCE_ACCESS_DENIED);
        }

        return FenceBoundaryResponse.toDto(
                "울타리 정보 조회를 성공했습니다.",
                fence.getCenterLatitude(),
                fence.getCenterLongitude(),
                fence.getRadius(),
                fence.getFenceId()
        );
    }

    /**
     * 사용자가 접근 가능한 울타리 경계 목록을 조회합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService}를 통해 요청 사용자의 승인된(APPROVED) 반려동물 ID 목록을 조회합니다.</li>
     * <li>승인된 반려동물이 하나도 없으면 {@link FenceException}(FENCE_ACCESS_DENIED)을 발생시킵니다.</li>
     * <li>조회된 반려동물 ID 목록으로 울타리 목록을 조회합니다.</li>
     * <li>울타리 데이터가 없으면 {@link FenceException}(FENCE_INFO_NOT_FOUND)을 발생시킵니다.</li>
     * <li>{@link ImageFileService}에서 반려동물 프로필 이미지 URL 맵을 조회합니다.</li>
     * <li>울타리 엔티티를 목록 응답 DTO로 매핑하여 반환합니다.</li>
     * </ol>
     *
     * @param userId 요청한 사용자 ID
     * @return 울타리 경계 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public FenceBoundaryListResponse getFenceBoundaries(UUID userId) {
        List<Long> approvedPetIds = userPetService.getApprovedPetIds(userId);
        if (approvedPetIds.isEmpty()) {
            throw new FenceException(FENCE_ACCESS_DENIED);
        }

        List<Fence> fences = fenceRepository.findAllByPet_PetIdIn(approvedPetIds);
        if (fences.isEmpty()) {
            throw new FenceException(FENCE_INFO_NOT_FOUND);
        }

        Map<Long, String> petImageUrlMap = imageFileService.getProfileUrlsByPetIds(approvedPetIds);

        List<FenceBoundaryItem> items = fences.stream()
                .map(fence -> {
                    Long petId = fence.getPet().getPetId();
                    return FenceBoundaryItem.toDto(
                            fence.getFenceId(),
                            fence.getName(),
                            fence.getCenterLatitude(),
                            fence.getCenterLongitude(),
                            fence.getRadius(),
                            Boolean.TRUE.equals(fence.getFenceIsActive()),
                            petId,
                            fence.getPet().getPetName(),
                            petImageUrlMap.get(petId)
                    );
                })
                .toList();

        return FenceBoundaryListResponse.toDto("울타리 목록 조회를 성공했습니다.", items);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 반려동물 존재 여부를 검증합니다.
     * 2. 해당 반려동물의 울타리 정보를 조회합니다.
     * 3. 울타리 활성화 여부를 반환합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public FenceStatusResponse getFenceStatus(Long petId) {
        if (!petService.existsPetById(petId)) {
            throw new FenceException(PET_NOT_FOUND);
        }

        Fence fence = fenceRepository.findByPet_PetId(petId)
                .orElseThrow(() -> new FenceException(FENCE_INFO_NOT_FOUND));

        return FenceStatusResponse.toDto("울타리 상태를 조회했습니다.", Boolean.TRUE.equals(fence.getFenceIsActive()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 반려동물 존재 여부를 검증합니다.
     * 2. 디바이스 토큰과 반려동물 ID 매핑 일치 여부를 검증합니다.
     * 3. 해당 반려동물의 울타리 정보를 조회합니다.
     * 4. 울타리 활성화 여부를 반환합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public FenceStatusResponse getFenceStatusForDevice(Long petId, String devicePrincipal) {
        if (!petService.existsPetById(petId)) {
            throw new FenceException(PET_NOT_FOUND);
        }

        if (!petService.isDevicePrincipalMatchedPet(devicePrincipal, petId)) {
            throw new FenceException(PET_PERMISSION_DENIED);
        }

        Fence fence = fenceRepository.findByPet_PetId(petId)
                .orElseThrow(() -> new FenceException(FENCE_INFO_NOT_FOUND));

        return FenceStatusResponse.toDto("울타리 상태를 조회했습니다.", Boolean.TRUE.equals(fence.getFenceIsActive()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 반려동물 존재 여부를 검증합니다.
     * 2. 요청 사용자의 반려동물 접근 권한을 검증합니다.
     * 3. 반려동물의 울타리 설정 정보를 조회합니다.
     * 4. 실시간 좌표와 울타리 중심 좌표의 거리를 계산하여 내부 여부를 판정합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public FenceLocationCheckResponse checkFenceLocation(
            UUID userId,
            Long petId,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDateTime measuredAt
    ) {
        if (!petService.existsPetById(petId)) {
            throw new FenceException(PET_NOT_FOUND);
        }

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new FenceException(PET_PERMISSION_DENIED);
        }

        Fence fence = fenceRepository.findByPet_PetId(petId)
                .orElseThrow(() -> new FenceException(FENCE_INFO_NOT_FOUND));

        double distance = haversine(
                latitude.doubleValue(),
                longitude.doubleValue(),
                fence.getCenterLatitude().doubleValue(),
                fence.getCenterLongitude().doubleValue()
        );

        BigDecimal distanceMeter = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
        boolean insideFence = distanceMeter.compareTo(BigDecimal.valueOf(fence.getRadius())) <= 0;

        return FenceLocationCheckResponse.toDto(insideFence, distanceMeter, fence.getRadius());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 디바이스에서 전달된 반려동물 ID 기준으로 울타리 내부 여부를 판정합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public FenceLocationCheckResponse checkFenceLocationByPet(
            Long petId,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDateTime measuredAt
    ) {
        if (!petService.existsPetById(petId)) {
            throw new FenceException(PET_NOT_FOUND);
        }

        Fence fence = fenceRepository.findByPet_PetId(petId)
                .orElseThrow(() -> new FenceException(FENCE_INFO_NOT_FOUND));

        double distance = haversine(
                latitude.doubleValue(),
                longitude.doubleValue(),
                fence.getCenterLatitude().doubleValue(),
                fence.getCenterLongitude().doubleValue()
        );

        BigDecimal distanceMeter = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
        boolean insideFence = distanceMeter.compareTo(BigDecimal.valueOf(fence.getRadius())) <= 0;

        return FenceLocationCheckResponse.toDto(insideFence, distanceMeter, fence.getRadius());
    }

    /**
     * 하버사인 공식을 사용해 두 좌표 간의 거리(미터)를 계산합니다.
     *
     * @param lat1 시작 지점 위도
     * @param lon1 시작 지점 경도
     * @param lat2 도착 지점 위도
     * @param lon2 도착 지점 경도
     * @return 두 좌표 간의 거리(미터)
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadiusKm = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c * 1000;
    }
}
