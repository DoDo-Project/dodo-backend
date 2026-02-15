package com.dodo.backend.fence.service;

import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeRequest;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeResponse;
import com.dodo.backend.fence.entity.Fence;
import com.dodo.backend.fence.exception.FenceErrorCode;
import com.dodo.backend.fence.exception.FenceException;
import com.dodo.backend.fence.repository.FenceRepository;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.userpet.service.UserPetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link FenceService}의 울타리 거리 범위 설정 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class FenceServiceTest {

    @InjectMocks
    private FenceServiceImpl fenceService;

    @Mock
    private FenceRepository fenceRepository;

    @Mock
    private PetService petService;

    @Mock
    private UserPetService userPetService;

    /**
     * 기존 울타리가 없을 때 새 울타리를 생성하고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 설정 성공: 기존 울타리가 없으면 새 울타리를 저장한다.")
    void setFenceRange_CreateSuccess() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        FenceRangeRequest request = FenceRangeRequest.builder()
                .petId(petId)
                .centerLatitude(new BigDecimal("37.5665"))
                .centerLongitude(new BigDecimal("126.9780"))
                .fenceName("집 주변 울타리")
                .radius(500)
                .build();

        Pet pet = Pet.builder().petId(petId).build();

        given(petService.existsPetById(petId)).willReturn(true);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(fenceRepository.findByPet_PetId(petId)).willReturn(Optional.empty());
        given(petService.getPetById(petId)).willReturn(pet);
        given(fenceRepository.save(any(Fence.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        FenceRangeResponse response = fenceService.setFenceRange(userId, request);

        // then
        assertEquals("울타리 설정을 완료했습니다.", response.getMessage());
        verify(fenceRepository).save(any(Fence.class));
    }

    /**
     * 기존 울타리가 있을 때 동일 울타리를 갱신 저장하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 설정 성공: 기존 울타리가 있으면 해당 울타리를 갱신 저장한다.")
    void setFenceRange_UpdateSuccess() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Pet pet = Pet.builder().petId(petId).build();
        Fence existing = Fence.builder()
                .fenceId(10L)
                .pet(pet)
                .name("기존 울타리")
                .centerLatitude(new BigDecimal("37.5000"))
                .centerLongitude(new BigDecimal("126.9000"))
                .radius(300)
                .fenceCreatedAt(LocalDateTime.now().minusDays(1))
                .fenceIsActive(true)
                .build();

        FenceRangeRequest request = FenceRangeRequest.builder()
                .petId(petId)
                .centerLatitude(new BigDecimal("37.5665"))
                .centerLongitude(new BigDecimal("126.9780"))
                .fenceName("집 주변 울타리")
                .radius(500)
                .build();

        given(petService.existsPetById(petId)).willReturn(true);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(fenceRepository.findByPet_PetId(petId)).willReturn(Optional.of(existing));
        given(fenceRepository.save(any(Fence.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        FenceRangeResponse response = fenceService.setFenceRange(userId, request);

        // then
        assertEquals("울타리 설정을 완료했습니다.", response.getMessage());
        verify(fenceRepository).save(any(Fence.class));
        verify(petService, never()).getPetById(any());
    }

    /**
     * 반려동물이 존재하지 않을 때 PET_NOT_FOUND 예외를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 설정 실패: 반려동물이 없으면 PET_NOT_FOUND 예외가 발생한다.")
    void setFenceRange_PetNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 999L;
        FenceRangeRequest request = FenceRangeRequest.builder()
                .petId(petId)
                .centerLatitude(new BigDecimal("37.5665"))
                .centerLongitude(new BigDecimal("126.9780"))
                .fenceName("집 주변 울타리")
                .radius(500)
                .build();

        given(petService.existsPetById(petId)).willReturn(false);

        // when
        FenceException exception = assertThrows(FenceException.class, () -> fenceService.setFenceRange(userId, request));

        // then
        assertEquals(FenceErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(userPetService, never()).isApprovedPetOwner(any(), any());
        verify(fenceRepository, never()).save(any(Fence.class));
    }

    /**
     * 반려동물 권한이 없을 때 PET_PERMISSION_DENIED 예외를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 설정 실패: 권한이 없으면 PET_PERMISSION_DENIED 예외가 발생한다.")
    void setFenceRange_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        FenceRangeRequest request = FenceRangeRequest.builder()
                .petId(petId)
                .centerLatitude(new BigDecimal("37.5665"))
                .centerLongitude(new BigDecimal("126.9780"))
                .fenceName("집 주변 울타리")
                .radius(500)
                .build();

        given(petService.existsPetById(petId)).willReturn(true);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        FenceException exception = assertThrows(FenceException.class, () -> fenceService.setFenceRange(userId, request));

        // then
        assertEquals(FenceErrorCode.PET_PERMISSION_DENIED, exception.getErrorCode());
        verify(fenceRepository, never()).save(any(Fence.class));
    }
}
