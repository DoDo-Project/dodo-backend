package com.dodo.backend.fence.service;

import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeRequest;
import com.dodo.backend.fence.dto.request.FenceRequest.FenceRangeUpdateRequest;
import com.dodo.backend.fence.dto.request.FenceRequest.FenceToggleRequest;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceRangeUpdateResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceStatusResponse;
import com.dodo.backend.fence.dto.response.FenceResponse.FenceToggleResponse;
import com.dodo.backend.fence.entity.Fence;
import com.dodo.backend.fence.exception.FenceErrorCode;
import com.dodo.backend.fence.exception.FenceException;
import com.dodo.backend.fence.mapper.FenceMapper;
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
    private FenceMapper fenceMapper;

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

    /**
     * 반려동물의 울타리 활성화 상태 조회에 성공하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 상태 조회 성공: 활성화 상태를 반환한다.")
    void getFenceStatus_Success() {
        // given
        Long petId = 1L;
        Pet pet = Pet.builder().petId(petId).build();
        Fence fence = Fence.builder()
                .fenceId(10L)
                .pet(pet)
                .name("집 주변 울타리")
                .centerLatitude(new BigDecimal("37.5665"))
                .centerLongitude(new BigDecimal("126.9780"))
                .radius(500)
                .fenceIsActive(true)
                .build();

        given(petService.existsPetById(petId)).willReturn(true);
        given(fenceRepository.findByPet_PetId(petId)).willReturn(Optional.of(fence));

        // when
        FenceStatusResponse response = fenceService.getFenceStatus(petId);

        // then
        assertEquals("울타리 상태를 조회했습니다.", response.getMessage());
        assertEquals(true, response.getIsActive());
    }

    /**
     * 반려동물이 존재하지 않으면 PET_NOT_FOUND 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 상태 조회 실패: 반려동물이 없으면 PET_NOT_FOUND 예외가 발생한다.")
    void getFenceStatus_PetNotFound() {
        // given
        Long petId = 999L;
        given(petService.existsPetById(petId)).willReturn(false);

        // when
        FenceException exception = assertThrows(FenceException.class, () -> fenceService.getFenceStatus(petId));

        // then
        assertEquals(FenceErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(fenceRepository, never()).findByPet_PetId(any());
    }

    /**
     * 울타리 정보가 존재하지 않으면 FENCE_INFO_NOT_FOUND 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 상태 조회 실패: 울타리 정보가 없으면 FENCE_INFO_NOT_FOUND 예외가 발생한다.")
    void getFenceStatus_FenceNotFound() {
        // given
        Long petId = 1L;
        given(petService.existsPetById(petId)).willReturn(true);
        given(fenceRepository.findByPet_PetId(petId)).willReturn(Optional.empty());

        // when
        FenceException exception = assertThrows(FenceException.class, () -> fenceService.getFenceStatus(petId));

        // then
        assertEquals(FenceErrorCode.FENCE_INFO_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 울타리 상태 변경 요청이 정상 처리되는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 상태 변경 성공: 권한이 있으면 mapper 업데이트를 수행한다.")
    void toggleFence_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long fenceId = 10L;
        Long petId = 1L;

        Fence fence = Fence.builder()
                .fenceId(fenceId)
                .pet(Pet.builder().petId(petId).build())
                .name("집 주변 울타리")
                .centerLatitude(new BigDecimal("37.5665"))
                .centerLongitude(new BigDecimal("126.9780"))
                .radius(500)
                .fenceIsActive(false)
                .build();

        FenceToggleRequest request = FenceToggleRequest.builder()
                .fenceIsActive(true)
                .build();

        given(fenceRepository.findById(fenceId)).willReturn(Optional.of(fence));
        given(petService.existsPetById(petId)).willReturn(true);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(fenceMapper.updateFenceIsActive(fenceId, true)).willReturn(1);

        // when
        FenceToggleResponse response = fenceService.toggleFence(userId, fenceId, request);

        // then
        assertEquals("울타리 상태를 변경하는데 성공했습니다.", response.getMessage());
        verify(fenceMapper).updateFenceIsActive(fenceId, true);
    }

    /**
     * 울타리가 없으면 PET_NOT_FOUND 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 상태 변경 실패: 울타리가 없으면 PET_NOT_FOUND 예외가 발생한다.")
    void toggleFence_FenceNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long fenceId = 999L;
        FenceToggleRequest request = FenceToggleRequest.builder()
                .fenceIsActive(true)
                .build();

        given(fenceRepository.findById(fenceId)).willReturn(Optional.empty());

        // when
        FenceException exception = assertThrows(FenceException.class,
                () -> fenceService.toggleFence(userId, fenceId, request));

        // then
        assertEquals(FenceErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(fenceMapper, never()).updateFenceIsActive(any(), any());
    }

    /**
     * 권한이 없으면 PET_PERMISSION_DENIED 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 상태 변경 실패: 권한이 없으면 PET_PERMISSION_DENIED 예외가 발생한다.")
    void toggleFence_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long fenceId = 10L;
        Long petId = 1L;

        Fence fence = Fence.builder()
                .fenceId(fenceId)
                .pet(Pet.builder().petId(petId).build())
                .name("집 주변 울타리")
                .centerLatitude(new BigDecimal("37.5665"))
                .centerLongitude(new BigDecimal("126.9780"))
                .radius(500)
                .fenceIsActive(false)
                .build();

        FenceToggleRequest request = FenceToggleRequest.builder()
                .fenceIsActive(false)
                .build();

        given(fenceRepository.findById(fenceId)).willReturn(Optional.of(fence));
        given(petService.existsPetById(petId)).willReturn(true);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        FenceException exception = assertThrows(FenceException.class,
                () -> fenceService.toggleFence(userId, fenceId, request));

        // then
        assertEquals(FenceErrorCode.PET_PERMISSION_DENIED, exception.getErrorCode());
        verify(fenceMapper, never()).updateFenceIsActive(any(), any());
    }

    /**
     * 울타리 범위 수정 요청이 정상 처리되는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 범위 수정 성공: 권한이 있으면 범위 정보를 수정한다.")
    void updateFenceRange_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long fenceId = 10L;
        Long petId = 1L;

        Fence existingFence = Fence.builder()
                .fenceId(fenceId)
                .pet(Pet.builder().petId(petId).build())
                .name("기존 이름")
                .centerLatitude(new BigDecimal("37.5000"))
                .centerLongitude(new BigDecimal("126.9000"))
                .radius(500)
                .fenceIsActive(true)
                .build();

        Fence updatedFence = Fence.builder()
                .fenceId(fenceId)
                .pet(Pet.builder().petId(petId).build())
                .name("새로운 이름")
                .centerLatitude(new BigDecimal("37.5555"))
                .centerLongitude(new BigDecimal("127.0000"))
                .radius(1000)
                .fenceIsActive(true)
                .build();

        FenceRangeUpdateRequest request = FenceRangeUpdateRequest.builder()
                .fenceName("새로운 이름")
                .centerLatitude(new BigDecimal("37.5555"))
                .centerLongitude(new BigDecimal("127.0000"))
                .radius(1000)
                .build();

        given(fenceRepository.findById(fenceId)).willReturn(Optional.of(existingFence), Optional.of(updatedFence));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(fenceMapper.updateFenceRange(
                fenceId,
                "새로운 이름",
                new BigDecimal("37.5555"),
                new BigDecimal("127.0000"),
                1000
        )).willReturn(1);

        // when
        FenceRangeUpdateResponse response = fenceService.updateFenceRange(userId, fenceId, request);

        // then
        assertEquals("울타리 정보를 수정했습니다.", response.getMessage());
        assertEquals(fenceId, response.getGeofenceId());
        assertEquals(petId, response.getPetId());
        assertEquals("새로운 이름", response.getFenceName());
        assertEquals(new BigDecimal("37.5555"), response.getCenterLatitude());
        assertEquals(new BigDecimal("127.0000"), response.getCenterLongtitude());
        assertEquals(1000, response.getRadius());
    }

    /**
     * 수정 대상 울타리가 없으면 FENCE_NOT_FOUND 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 범위 수정 실패: 울타리가 없으면 FENCE_NOT_FOUND 예외가 발생한다.")
    void updateFenceRange_FenceNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long fenceId = 999L;
        FenceRangeUpdateRequest request = FenceRangeUpdateRequest.builder()
                .fenceName("새로운 이름")
                .centerLatitude(new BigDecimal("37.5555"))
                .centerLongitude(new BigDecimal("127.0000"))
                .radius(1000)
                .build();

        given(fenceRepository.findById(fenceId)).willReturn(Optional.empty());

        // when
        FenceException exception = assertThrows(FenceException.class,
                () -> fenceService.updateFenceRange(userId, fenceId, request));

        // then
        assertEquals(FenceErrorCode.FENCE_NOT_FOUND, exception.getErrorCode());
        verify(fenceMapper, never()).updateFenceRange(any(), any(), any(), any(), any());
    }

    /**
     * 수정 권한이 없으면 FENCE_PERMISSION_DENIED 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 범위 수정 실패: 권한이 없으면 FENCE_PERMISSION_DENIED 예외가 발생한다.")
    void updateFenceRange_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long fenceId = 10L;
        Long petId = 1L;

        Fence existingFence = Fence.builder()
                .fenceId(fenceId)
                .pet(Pet.builder().petId(petId).build())
                .name("기존 이름")
                .centerLatitude(new BigDecimal("37.5000"))
                .centerLongitude(new BigDecimal("126.9000"))
                .radius(500)
                .fenceIsActive(true)
                .build();

        FenceRangeUpdateRequest request = FenceRangeUpdateRequest.builder()
                .fenceName("새로운 이름")
                .centerLatitude(new BigDecimal("37.5555"))
                .centerLongitude(new BigDecimal("127.0000"))
                .radius(1000)
                .build();

        given(fenceRepository.findById(fenceId)).willReturn(Optional.of(existingFence));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        FenceException exception = assertThrows(FenceException.class,
                () -> fenceService.updateFenceRange(userId, fenceId, request));

        // then
        assertEquals(FenceErrorCode.FENCE_PERMISSION_DENIED, exception.getErrorCode());
        verify(fenceMapper, never()).updateFenceRange(any(), any(), any(), any(), any());
    }
}
