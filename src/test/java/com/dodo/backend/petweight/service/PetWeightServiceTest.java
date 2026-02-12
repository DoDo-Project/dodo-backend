package com.dodo.backend.petweight.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.repository.PetRepository;
import com.dodo.backend.petweight.dto.request.PetWeightRequest;
import com.dodo.backend.petweight.dto.request.PetWeightRequest.PetWeightRegisterRequest;
import com.dodo.backend.petweight.dto.request.PetWeightRequest.PetWeightUpdateRequest;
import com.dodo.backend.petweight.dto.response.PetWeightResponse;
import com.dodo.backend.petweight.dto.response.PetWeightResponse.PetWeightHistoryResponse;
import com.dodo.backend.petweight.entity.PetWeight;
import com.dodo.backend.petweight.exception.PetWeightErrorCode;
import com.dodo.backend.petweight.exception.PetWeightException;
import com.dodo.backend.petweight.mapper.PetWeightMapper;
import com.dodo.backend.petweight.repository.PetWeightRepository;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link PetWeightService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 * <p>
 * 반려동물 목록에 대한 최신 체중 일괄 조회 및 체중 기록 추가 로직을 테스트합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class PetWeightServiceTest {

    @InjectMocks
    private PetWeightServiceImpl petWeightService;

    @Mock
    private PetWeightRepository petWeightRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetWeightMapper petWeightMapper;

    @Mock
    private UserPetService userPetService;

    /**
     * 펫 ID 목록을 입력받아 최신 체중을 정상적으로 조회하는 시나리오를 테스트합니다.
     * <p>
     * Repository가 반환하는 Object[] 리스트가 Map<Long, Double>로
     * 올바르게 변환되는지 검증합니다.
     */
    @Test
    @DisplayName("최신 체중 조회 성공: Pet ID 목록에 해당하는 최신 체중을 Map 형태로 반환한다.")
    void getRecentWeights_Success() {
        log.info("테스트 시작: 최신 체중 조회 성공 시나리오");

        // given
        List<Long> petIds = List.of(1L, 2L);
        List<Object[]> repositoryResult = List.of(
                new Object[]{1L, 5.5},
                new Object[]{2L, 8.2}
        );

        given(petWeightRepository.findRecentWeightsByPetIds(petIds)).willReturn(repositoryResult);

        // when
        Map<Long, Double> result = petWeightService.getRecentWeights(petIds);

        // then
        log.info("조회 결과 Map: {}", result);

        assertEquals(2, result.size());
        assertEquals(5.5, result.get(1L));
        assertEquals(8.2, result.get(2L));

        verify(petWeightRepository).findRecentWeightsByPetIds(petIds);

        log.info("테스트 종료: 최신 체중 조회 성공 시나리오");
    }

    /**
     * 빈 리스트가 입력되었을 때 DB 조회를 하지 않고 빈 Map을 반환하는지 테스트합니다.
     */
    @Test
    @DisplayName("최신 체중 조회: 입력된 ID 리스트가 비어있으면 DB 조회 없이 빈 Map을 반환한다.")
    void getRecentWeights_EmptyInput() {
        log.info("테스트 시작: 최신 체중 조회 (빈 리스트)");

        // given
        List<Long> emptyPetIds = Collections.emptyList();

        // when
        Map<Long, Double> result = petWeightService.getRecentWeights(emptyPetIds);

        // then
        log.info("조회 결과 Map Size: {}", result.size());
        assertTrue(result.isEmpty());

        verify(petWeightRepository, never()).findRecentWeightsByPetIds(anyList());

        log.info("테스트 종료: 최신 체중 조회 (빈 리스트)");
    }

    /**
     * 정상적인 권한과 데이터를 가지고 체중 기록을 추가하는 성공 시나리오를 테스트합니다.
     * <p>
     * 1. 권한 검증 (UserPetService)
     * 2. 펫 조회 (PetRepository)
     * 3. 체중 저장 (PetWeightRepository)
     * 위 과정이 순차적으로 실행되고 저장된 ID가 반환되는지 확인합니다.
     */
    @Test
    @DisplayName("체중 기록 추가 성공: 권한이 있고 펫이 존재하면 체중을 저장하고 ID를 반환한다.")
    void addWeight_Success() {
        log.info("테스트 시작: 체중 기록 추가 성공 시나리오");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        PetWeightRegisterRequest request = new PetWeightRegisterRequest(5.2, LocalDate.now());

        Pet mockPet = Pet.builder().petId(petId).build();
        PetWeight savedPetWeight = PetWeight.builder().weightId(100L).build();

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(petRepository.findById(petId)).willReturn(Optional.of(mockPet));
        given(petWeightRepository.save(any(PetWeight.class))).willReturn(savedPetWeight);

        // when
        Long resultId = petWeightService.addWeight(userId, petId, request);

        // then
        log.info("생성된 Weight ID: {}", resultId);
        assertEquals(100L, resultId);

        verify(userPetService).isApprovedPetOwner(userId, petId);
        verify(petRepository).findById(petId);
        verify(petWeightRepository).save(any(PetWeight.class));

        log.info("테스트 종료: 체중 기록 추가 성공 시나리오");
    }

    /**
     * 해당 펫에 대한 권한이 없는 유저가 요청했을 때 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("체중 기록 추가 실패: 해당 펫에 대한 권한이 없으면 PERMISSION_DENIED 예외가 발생한다.")
    void addWeight_PermissionDenied() {
        log.info("테스트 시작: 체중 기록 추가 실패 (권한 없음)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        PetWeightRegisterRequest request = new PetWeightRegisterRequest(5.2, LocalDate.now());

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.addWeight(userId, petId, request)
        );

        log.info("발생한 예외 코드: {}", exception.getErrorCode());
        assertEquals(PetWeightErrorCode.PERMISSION_DENIED, exception.getErrorCode());

        verify(petRepository, never()).findById(any());
        verify(petWeightRepository, never()).save(any());

        log.info("테스트 종료: 체중 기록 추가 실패 (권한 없음)");
    }

    /**
     * 권한은 있으나 존재하지 않는 펫 ID로 요청했을 때 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("체중 기록 추가 실패: 존재하지 않는 펫 ID라면 PET_NOT_FOUND 예외가 발생한다.")
    void addWeight_PetNotFound() {
        log.info("테스트 시작: 체중 기록 추가 실패 (펫 없음)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 999L;
        PetWeightRegisterRequest request = new PetWeightRegisterRequest(5.2, LocalDate.now());

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(petRepository.findById(petId)).willReturn(Optional.empty());

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.addWeight(userId, petId, request)
        );

        log.info("발생한 예외 코드: {}", exception.getErrorCode());
        assertEquals(PetWeightErrorCode.PET_NOT_FOUND, exception.getErrorCode());

        verify(petWeightRepository, never()).save(any());

        log.info("테스트 종료: 체중 기록 추가 실패 (펫 없음)");
    }

    /**
     * 펫의 체중 기록을 페이징하여 조회하는 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("체중 이력 조회 성공: 권한이 있고 펫이 존재하면 페이징된 체중 기록을 반환한다.")
    void getWeightHistory_Success() {
        log.info("테스트 시작: 체중 이력 조회 성공 시나리오");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        PetWeight pw1 = PetWeight.builder().weightId(10L).weight(5.0).petWeightsMeasuredAt(LocalDate.now()).build();
        PetWeight pw2 = PetWeight.builder().weightId(11L).weight(5.2).petWeightsMeasuredAt(LocalDate.now().minusDays(1)).build();
        Page<PetWeight> mockPage = new PageImpl<>(List.of(pw1, pw2), pageable, 2);

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(petRepository.existsById(petId)).willReturn(true);
        given(petWeightRepository.findAllByPet_PetId(petId, pageable)).willReturn(mockPage);

        // when
        PetWeightHistoryResponse response = petWeightService.getWeightHistory(userId, petId, pageable);

        // then
        assertNotNull(response);
        assertEquals(2, response.getWeights().size());
        assertEquals(2, response.getTotalElements());
        assertEquals("조회를 성공했습니다.", response.getMessage());

        verify(petWeightRepository).findAllByPet_PetId(petId, pageable);
    }

    /**
     * 이력 조회 시 권한이 없는 경우 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("체중 이력 조회 실패: 권한이 없으면 PERMISSION_DENIED 예외가 발생한다.")
    void getWeightHistory_PermissionDenied() {
        log.info("테스트 시작: 체중 이력 조회 실패 (권한 없음)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.getWeightHistory(userId, petId, pageable)
        );

        assertEquals(PetWeightErrorCode.PERMISSION_DENIED, exception.getErrorCode());
        verify(petWeightRepository, never()).findAllByPet_PetId(any(), any());
    }

    /**
     * 이력 조회 시 존재하지 않는 펫인 경우 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("체중 이력 조회 실패: 존재하지 않는 펫이면 PET_NOT_FOUND 예외가 발생한다.")
    void getWeightHistory_PetNotFound() {
        log.info("테스트 시작: 체중 이력 조회 실패 (펫 없음)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(petRepository.existsById(petId)).willReturn(false);

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.getWeightHistory(userId, petId, pageable)
        );

        assertEquals(PetWeightErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(petWeightRepository, never()).findAllByPet_PetId(any(), any());
    }

    @Test
    @DisplayName("체중 수정 성공: 권한 검증 및 데이터 무결성 확인 후 Mapper를 통해 업데이트한다.")
    void updateWeight_Success() {
        log.info("테스트 시작: 체중 수정 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Long weightId = 100L;
        PetWeightUpdateRequest request = new PetWeightUpdateRequest(6.0, null);

        Pet mockPet = Pet.builder().petId(petId).build();
        PetWeight mockPetWeight = PetWeight.builder()
                .weightId(weightId)
                .pet(mockPet)
                .weight(5.0)
                .build();

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(petWeightRepository.findById(weightId)).willReturn(Optional.of(mockPetWeight));

        // when
        petWeightService.updateWeight(userId, petId, weightId, request);

        // then
        verify(petWeightMapper).updatePetWeight(weightId, request);

        log.info("테스트 종료: 체중 수정 성공");
    }

    @Test
    @DisplayName("체중 수정 실패: 권한이 없으면 PERMISSION_DENIED 예외 발생")
    void updateWeight_PermissionDenied() {
        log.info("테스트 시작: 체중 수정 실패 (권한 없음)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Long weightId = 100L;
        PetWeightUpdateRequest request = new PetWeightUpdateRequest(6.0, null);

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.updateWeight(userId, petId, weightId, request)
        );
        assertEquals(PetWeightErrorCode.PERMISSION_DENIED, exception.getErrorCode());

        verify(petWeightMapper, never()).updatePetWeight(any(), any());
    }

    @Test
    @DisplayName("체중 수정 실패: 체중 기록이 존재하지 않으면 WEIGHT_RECORD_NOT_FOUND 예외 발생")
    void updateWeight_NotFound() {
        log.info("테스트 시작: 체중 수정 실패 (기록 없음)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Long weightId = 999L;
        PetWeightUpdateRequest request = new PetWeightUpdateRequest(6.0, null);

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(petWeightRepository.findById(weightId)).willReturn(Optional.empty());

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.updateWeight(userId, petId, weightId, request)
        );
        assertEquals(PetWeightErrorCode.WEIGHT_RECORD_NOT_FOUND, exception.getErrorCode());

        verify(petWeightMapper, never()).updatePetWeight(any(), any());
    }

    @Test
    @DisplayName("체중 수정 실패: 요청한 펫 ID와 실제 기록의 펫 ID가 다르면 WEIGHT_RECORD_NOT_FOUND 예외 발생")
    void updateWeight_PetMismatch() {
        log.info("테스트 시작: 체중 수정 실패 (펫 ID 불일치)");

        // given
        UUID userId = UUID.randomUUID();
        Long requestPetId = 1L;
        Long actualPetId = 2L;
        Long weightId = 100L;
        PetWeightUpdateRequest request = new PetWeightUpdateRequest(6.0, null);

        Pet mockPet = Pet.builder().petId(actualPetId).build();
        PetWeight mockPetWeight = PetWeight.builder()
                .weightId(weightId)
                .pet(mockPet)
                .build();

        given(userPetService.isApprovedPetOwner(userId, requestPetId)).willReturn(true);
        given(petWeightRepository.findById(weightId)).willReturn(Optional.of(mockPetWeight));

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.updateWeight(userId, requestPetId, weightId, request)
        );

        assertEquals(PetWeightErrorCode.WEIGHT_RECORD_NOT_FOUND, exception.getErrorCode());

        verify(petWeightMapper, never()).updatePetWeight(any(), any());
    }

    @Test
    @DisplayName("체중 삭제 성공: 권한이 있고 기록이 존재하면 데이터를 삭제한다.")
    void deleteWeight_Success() {
        log.info("테스트 시작: 체중 삭제 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Long weightId = 100L;

        Pet mockPet = Pet.builder().petId(petId).build();
        PetWeight mockPetWeight = PetWeight.builder()
                .weightId(weightId)
                .pet(mockPet)
                .build();

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(petWeightRepository.findById(weightId)).willReturn(Optional.of(mockPetWeight));

        // when
        petWeightService.deleteWeight(userId, petId, weightId);

        // then
        verify(petWeightRepository).delete(mockPetWeight);

        log.info("테스트 종료: 체중 삭제 성공");
    }

    @Test
    @DisplayName("체중 삭제 실패: 권한이 없으면 PERMISSION_DENIED 예외 발생")
    void deleteWeight_PermissionDenied() {
        log.info("테스트 시작: 체중 삭제 실패 (권한 없음)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Long weightId = 100L;

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.deleteWeight(userId, petId, weightId)
        );
        assertEquals(PetWeightErrorCode.PERMISSION_DENIED, exception.getErrorCode());

        verify(petWeightRepository, never()).delete(any());
    }

    @Test
    @DisplayName("체중 삭제 실패: 기록이 존재하지 않으면 WEIGHT_RECORD_NOT_FOUND 예외 발생")
    void deleteWeight_NotFound() {
        log.info("테스트 시작: 체중 삭제 실패 (기록 없음)");

        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Long weightId = 999L;

        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(petWeightRepository.findById(weightId)).willReturn(Optional.empty());

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.deleteWeight(userId, petId, weightId)
        );
        assertEquals(PetWeightErrorCode.WEIGHT_RECORD_NOT_FOUND, exception.getErrorCode());

        verify(petWeightRepository, never()).delete(any());
    }

    @Test
    @DisplayName("체중 삭제 실패: 요청한 펫 ID와 실제 기록의 펫 ID가 다르면 WEIGHT_RECORD_NOT_FOUND 예외 발생")
    void deleteWeight_PetMismatch() {
        log.info("테스트 시작: 체중 삭제 실패 (펫 ID 불일치)");

        // given
        UUID userId = UUID.randomUUID();
        Long requestPetId = 1L;
        Long actualPetId = 2L;
        Long weightId = 100L;

        Pet mockPet = Pet.builder().petId(actualPetId).build();
        PetWeight mockPetWeight = PetWeight.builder()
                .weightId(weightId)
                .pet(mockPet)
                .build();

        given(userPetService.isApprovedPetOwner(userId, requestPetId)).willReturn(true);
        given(petWeightRepository.findById(weightId)).willReturn(Optional.of(mockPetWeight));

        // when & then
        PetWeightException exception = assertThrows(PetWeightException.class, () ->
                petWeightService.deleteWeight(userId, requestPetId, weightId)
        );
        assertEquals(PetWeightErrorCode.WEIGHT_RECORD_NOT_FOUND, exception.getErrorCode());

        verify(petWeightRepository, never()).delete(any());
    }
}