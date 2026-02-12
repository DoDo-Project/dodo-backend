package com.dodo.backend.petweight.service;

import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.exception.PetErrorCode;
import com.dodo.backend.pet.exception.PetException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.dodo.backend.petweight.exception.PetWeightErrorCode.*;

/**
 * {@link PetWeightService}의 구현체로, 체중 기록 조회 및 관리 로직을 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetWeightServiceImpl implements PetWeightService {

    private final PetWeightRepository petWeightRepository;
    private final PetRepository petRepository;
    private final UserPetService userPetService;
    private final PetWeightMapper petWeightMapper;

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>입력된 ID 리스트가 비어있으면 빈 Map을 반환하여 불필요한 DB 호출을 방지합니다.</li>
     * <li>리포지토리를 호출하여 모든 대상 펫의 최신 체중 데이터를 한 번에 조회합니다. (IN 절 활용)</li>
     * <li>조회된 결과 리스트({@code Object[]})를 펫 ID를 Key로 하는 {@code Map<Long, Double>}으로 변환하여 반환합니다.</li>
     * </ol>
     */
    @Transactional(readOnly = true)
    @Override
    public Map<Long, Double> getRecentWeights(List<Long> petIds) {

        if (petIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> results = petWeightRepository.findRecentWeightsByPetIds(petIds);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));
    }

    /**
     * 사용자의 권한을 검증한 후, 특정 반려동물의 새로운 체중 기록을 저장합니다.
     * <p>
     * 이 메서드는 다음의 절차를 따릅니다:
     * <ol>
     * <li>{@link UserPetService#isApprovedPetOwner}를 호출하여 권한을 확인합니다.</li>
     * <li>권한이 없다면 {@link PetWeightException} (PERMISSION_DENIED)을 발생시킵니다.</li>
     * <li>{@link PetRepository#findById}를 호출하여 펫 엔티티를 조회합니다.</li>
     * <li>펫이 없다면 {@link PetWeightException} (PET_NOT_FOUND)을 발생시킵니다.</li>
     * <li>요청 데이터를 기반으로 PetWeight 엔티티를 생성하여 저장합니다.</li>
     * </ol>
     * </p>
     *
     * @param userId  요청을 보낸 사용자의 UUID
     * @param petId   체중을 기록할 대상 반려동물의 ID
     * @param request 체중(weight)과 측정 일시(measuredAt)를 포함한 DTO
     * @return 저장된 체중 기록의 ID (weightId)
     * @throws PetWeightException 권한이 없거나 해당 반려동물을 찾을 수 없는 경우
     */
    @Transactional
    @Override
    public Long addWeight(UUID userId, Long petId, PetWeightRegisterRequest request) {

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new PetWeightException(PERMISSION_DENIED);
        }

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetWeightException(PET_NOT_FOUND));

        PetWeight petWeight = request.toEntity(pet);
        return petWeightRepository.save(petWeight).getWeightId();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>권한 검증: 요청한 유저가 해당 펫의 승인된 가족인지 확인합니다.</li>
     * <li>펫 존재 확인: 해당 펫 ID가 DB에 존재하는지 확인합니다.</li>
     * <li>조회: 리포지토리의 {@code findAllByPet_PetId}를 호출하여 페이징된 데이터를 가져옵니다.</li>
     * <li>변환: 조회된 Entity Page를 DTO로 변환하여 반환합니다.</li>
     * </ol>
     */
    @Transactional(readOnly = true)
    @Override
    public PetWeightHistoryResponse getWeightHistory(UUID userId, Long petId, Pageable pageable) {

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new PetWeightException(PERMISSION_DENIED);
        }

        if (!petRepository.existsById(petId)) {
            throw new PetWeightException(PET_NOT_FOUND);
        }

        Page<PetWeight> weightPage = petWeightRepository.findAllByPet_PetId(petId, pageable);

        return PetWeightHistoryResponse.toDto(weightPage, "조회를 성공했습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 권한 검증: 사용자가 해당 펫의 주인인지 확인합니다.
     * 2. 기록 조회: weightId로 기록을 찾습니다. 없으면 예외 발생.
     * 3. 무결성 검증: 조회된 체중 기록이 요청한 petId의 것인지 확인합니다.
     * 4. 수정: MyBatis Mapper를 호출하여 값이 존재하는 필드만 동적으로 업데이트합니다.
     */
    @Transactional
    @Override
    public void updateWeight(UUID userId, Long petId, Long weightId, PetWeightUpdateRequest request) {

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new PetWeightException(PERMISSION_DENIED);
        }

        PetWeight petWeight = petWeightRepository.findById(weightId)
                .orElseThrow(() -> new PetWeightException(WEIGHT_RECORD_NOT_FOUND));

        if (!Objects.equals(petWeight.getPet().getPetId(), petId)) {
            throw new PetWeightException(WEIGHT_RECORD_NOT_FOUND);
        }

        petWeightMapper.updatePetWeight(weightId, request);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 1. 권한 검증: 사용자가 해당 펫의 승인된 가족인지 확인합니다.
     * 2. 기록 조회: 삭제할 기록을 조회합니다. 없으면 예외 발생.
     * 3. 무결성 검증: 조회된 기록이 요청 경로의 펫 ID와 일치하는지 확인합니다.
     * 4. 삭제: JPA Repository를 통해 데이터를 삭제합니다.
     */
    @Transactional
    @Override
    public void deleteWeight(UUID userId, Long petId, Long weightId) {

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new PetWeightException(PERMISSION_DENIED);
        }

        PetWeight petWeight = petWeightRepository.findById(weightId)
                .orElseThrow(() -> new PetWeightException(WEIGHT_RECORD_NOT_FOUND));

        if (!Objects.equals(petWeight.getPet().getPetId(), petId)) {
            throw new PetWeightException(WEIGHT_RECORD_NOT_FOUND);
        }

        petWeightRepository.delete(petWeight);
    }
}