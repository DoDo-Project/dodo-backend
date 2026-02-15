package com.dodo.backend.fence.service;

import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeRequest;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeResponse;
import com.dodo.backend.fence.entity.Fence;
import com.dodo.backend.fence.exception.FenceException;
import com.dodo.backend.fence.repository.FenceRepository;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    private final PetService petService;
    private final UserPetService userPetService;

    /**
     * {@inheritDoc}
     * <p>
     * 1. 반려동물 존재 여부를 검증합니다.
     * 2. 요청 사용자의 반려동물 접근 권한을 검증합니다.
     * 3. 기존 울타리가 있으면 갱신하고, 없으면 새로 생성합니다.
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

        Fence savedFence = fenceRepository.findByPet_PetId(request.getPetId())
                .map(existing -> Fence.builder()
                        .fenceId(existing.getFenceId())
                        .pet(existing.getPet())
                        .name(request.getFenceName())
                        .centerLatitude(request.getCenterLatitude())
                        .centerLongitude(request.getCenterLongitude())
                        .radius(request.getRadius())
                        .fenceCreatedAt(existing.getFenceCreatedAt())
                        .fenceIsActive(existing.getFenceIsActive())
                        .build())
                .orElseGet(() -> {
                    Pet pet = petService.getPetById(request.getPetId());
                    return request.toEntity(pet);
                });

        fenceRepository.save(savedFence);

        return FenceRangeResponse.toDto("울타리 설정을 완료했습니다.");
    }

}
