package com.dodo.backend.pet.service;

import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.pet.dto.request.PetRequest.PetDeviceUpdateRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetFamilyJoinRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetRegisterRequest;
import com.dodo.backend.pet.dto.request.PetRequest.PetUpdateRequest;
import com.dodo.backend.pet.dto.response.PetResponse.*;
import com.dodo.backend.pet.dto.response.PetResponse.PendingUserListResponse.PendingUserResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetApplicationListResponse.PetApplicationResponse;
import com.dodo.backend.pet.dto.response.PetResponse.PetListResponse.PetSummary;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.exception.PetException;
import com.dodo.backend.pet.mapper.PetMapper;
import com.dodo.backend.pet.repository.PetRepository;
import com.dodo.backend.petweight.service.PetWeightService;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.entity.UserPet;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import static com.dodo.backend.pet.exception.PetErrorCode.*;


/**
 * {@link PetService}의 구현체로, 펫 도메인의 비즈니스 로직을 수행합니다.
 * <p>
 * 펫 정보의 유효성 검사, 엔티티 생성 및 저장, 사용자-펫 관계 설정 등의
 * 트랜잭션 처리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final UserPetService userPetService;
    private final PetWeightService petWeightService;
    private final PetMapper petMapper;
    private final ImageFileService imageFileService;

    /**
     * 사용자의 요청 정보를 기반으로 반려동물을 등록하고, 소유자 관계를 설정합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService#existsUser}를 호출하여 사용자 존재 여부를 확인합니다.</li>
     * <li>존재하지 않는 경우 {@link PetException} (USER_NOT_FOUND)을 발생시킵니다.</li>
     * <li>요청된 등록번호가 이미 존재하는지 중복 여부를 확인합니다.</li>
     * <li>요청 DTO를 {@link Pet} 엔티티로 변환하여 데이터베이스에 저장합니다.</li>
     * <li>{@link UserPetService}를 호출하여 등록한 사용자를 해당 펫의 소유자(APPROVED)로 설정합니다.</li>
     * </ol>
     */
    @Transactional
    @Override
    public PetRegisterResponse registerPet(UUID userId, PetRegisterRequest request) {

        log.info("반려동물 등록 시작 - userId: {}", userId);

        if (!userPetService.existsUser(userId)) {
            throw new PetException(USER_NOT_FOUND);
        }

        if (request.getRegistrationNumber() != null && !request.getRegistrationNumber().isBlank()) {
            if (petRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
                throw new PetException(REGISTRATION_NUMBER_DUPLICATED);
            }
        }

        Pet pet = request.toEntity();
        Pet savedPet = petRepository.save(pet);

        userPetService.registerUserPet(userId, savedPet, RegistrationStatus.APPROVED);

        log.info("펫 등록 및 유저 관계 설정 완료 - User: {}, PetId: {}", userId, savedPet.getPetId());

        return PetRegisterResponse.toDto(savedPet.getPetId(), "새 반려동물을 등록 완료했습니다.");
    }

    /**
     * 기존 반려동물의 프로필 정보를 수정합니다.
     * <p>
     * <ol>
     * <li>저장소(Repository)를 통해 수정할 반려동물 엔티티를 직접 조회합니다.</li>
     * <li>등록번호 변경 요청이 있는 경우, 해당 번호의 중복 여부를 검사합니다.</li>
     * <li>MyBatis Mapper를 호출하여 값이 존재하는 필드만 동적으로 업데이트(Dynamic Update)합니다.</li>
     * <li>수정 완료된 최신 정보를 포함한 응답 객체를 반환합니다.</li>
     * </ol>
     *
     * @param petId   수정할 반려동물의 ID
     * @param request 변경할 필드(이름, 나이, 체중 등)만 포함된 수정 요청 객체
     * @return 수정된 반려동물의 상세 정보를 담은 응답 객체
     * @throws PetException 해당 ID의 반려동물이 없거나(PET_NOT_FOUND), 변경하려는 등록번호가 이미 존재하는 경우
     */
    @Transactional
    @Override
    public PetUpdateResponse updatePet(Long petId, PetUpdateRequest request, UUID userId) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PET_NOT_FOUND));

        boolean isOwner = userPetService.isUserPetOwner(userId, petId);

        if (!isOwner) {
            throw new PetException(UPDATE_PERMISSION_DENIED);
        }
        if (request.getRegistrationNumber() != null && !Objects.equals(pet.getRegistrationNumber(), request.getRegistrationNumber())) {
            if (petRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
                log.warn("등록번호 중복 발생 - PetId: {}, 번호: {}", petId, request.getRegistrationNumber());
                throw new PetException(REGISTRATION_NUMBER_DUPLICATED);
            }
        }

        petMapper.updatePetProfileInfo(request, petId);

        log.info("반려동물 프로필 수정 성공 - PetId: {}", petId);

        return PetUpdateResponse.toDto(
                petId,
                "반려동물 정보 수정을 완료했습니다.",
                request.getRegistrationNumber() != null ? request.getRegistrationNumber() : pet.getRegistrationNumber(),
                request.getSex() != null ? request.getSex() : pet.getSex().name(),
                request.getAge() != null ? request.getAge() : pet.getAge(),
                request.getPetName() != null ? request.getPetName() : pet.getPetName(),
                request.getBreed() != null ? request.getBreed() : pet.getBreed(),
                request.getReferenceHeartRate() != null ? request.getReferenceHeartRate() : pet.getReferenceHeartRate(),
                request.getDeviceId() != null ? request.getDeviceId() : pet.getDeviceId()
        );
    }

    /**
     * 가족 초대를 위한 1회용 인증 코드를 생성합니다.
     * <p>
     * <ol>
     * <li>대상 반려동물의 존재 여부를 우선 검증합니다. (엔티티 조회 없이 존재 여부만 확인하여 성능 최적화)</li>
     * <li>실제 코드 생성 및 Redis 저장 로직은 {@link UserPetService}에 위임합니다.</li>
     * <li>생성된 6자리 코드와 유효 시간을 반환받아 응답 객체로 변환합니다.</li>
     * </ol>
     *
     * @param userId 코드를 요청한 사용자의 ID (권한 검증용)
     * @param petId  초대할 반려동물의 ID
     * @return 생성된 초대 코드와 만료 시간(초 단위)
     * @throws PetException 반려동물이 존재하지 않는 경우 (PET_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    @Override
    public PetInvitationResponse issueInvitationCode(UUID userId, Long petId) {

        if (!petRepository.existsById(petId)) {
            throw new PetException(PET_NOT_FOUND);
        }

        Map<String, Object> result = userPetService.generateInvitationCode(userId, petId);

        return PetInvitationResponse.builder()
                .message("초대 코드가 생성되었습니다.")
                .code((String) result.get("code"))
                .expiresIn((Long) result.get("expiresIn"))
                .build();
    }

    /**
     * 초대 코드를 입력하여 가족 등록을 신청(승인 대기)합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService#registerByInvitation}를 호출하여 코드 검증 및 PENDING 상태 등록을 수행합니다.</li>
     * <li>등록된 펫 ID를 반환받아 신청 성공 메시지와 함께 응답 객체로 변환합니다.</li>
     * </ol>
     *
     * @param userId  요청한 사용자의 ID
     * @param request 초대 코드가 포함된 요청 DTO
     * @return 신청된 펫 ID와 처리 결과 메시지
     */
    @Transactional
    @Override
    public PetFamilyJoinRequestResponse applyForFamily(UUID userId, PetFamilyJoinRequest request) {

        Long petId = userPetService.registerByInvitation(userId, request.getCode());

        return PetFamilyJoinRequestResponse.toDto(petId, "가족 등록을 신청했습니다. 승인을 기다려주세요.");
    }

    /**
     * 사용자의 반려동물 목록을 페이징하여 조회합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService}를 통해 페이징 된 펫 목록({@code Page<UserPet>})을 조회합니다.</li>
     * <li>조회된 목록에서 펫 ID들을 추출합니다.</li>
     * <li>추출한 ID로 {@link PetWeightService}를 호출하여 최신 체중 정보를 일괄 조회합니다.</li>
     * <li>추출한 ID로 이미지 이름(예: pet_profile_{id})을 생성하고 {@link ImageFileService}를 통해 이미지 URL을 일괄 조회합니다.</li>
     * <li>Entity 목록을 순회하며 체중 및 이미지 URL을 매핑하여 {@link PetSummary} DTO로 변환합니다.</li>
     * <li>최종적으로 페이징 정보가 포함된 응답 객체를 반환합니다.</li>
     * </ol>
     *
     * @param userId   조회할 사용자의 ID
     * @param pageable 페이징 요청 정보
     * @return 페이징 처리된 반려동물 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public PetListResponse getPetList(UUID userId, Pageable pageable) {

        Map<String, Object> result = userPetService.getUserPets(userId, pageable);
        Page<UserPet> userPetPage = (Page<UserPet>) result.get("userPetPage");

        List<Long> petIds = userPetPage.getContent().stream()
                .map(userPet -> userPet.getPet().getPetId())
                .collect(Collectors.toList());

        Map<Long, Double> weightMap = petWeightService.getRecentWeights(petIds);
        Map<Long, String> imageMap = imageFileService.getProfileUrlsByPetIds(petIds);

        Page<PetSummary> summaryPage = userPetPage.map(userPet -> {
            Pet pet = userPet.getPet();

            Double recentWeight = weightMap.get(pet.getPetId());
            String imageUrl = imageMap.get(pet.getPetId());

            return PetSummary.builder()
                    .petId(pet.getPetId())
                    .petName(pet.getPetName())
                    .species(pet.getSpecies().name())
                    .breed(pet.getBreed())
                    .sex(pet.getSex().name())
                    .age(pet.getAge())
                    .birth(pet.getBirth())
                    .weight(recentWeight)
                    .registrationNumber(pet.getRegistrationNumber())
                    .imageFileUrl(imageUrl)
                    .build();
        });

        return PetListResponse.toDto(summaryPage, "조회를 성공했습니다.");
    }

    /**
     * 대기 중인 가족 등록 요청을 승인하거나 거절합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService#approveOrRejectFamilyMember}를 호출하여 실제 상태 변경 로직을 위임합니다.</li>
     * <li>처리 결과 메시지를 반환받아 DTO에 담아 응답합니다.</li>
     * </ol>
     *
     * @param userId  요청을 수행하는 관리자(기존 가족) ID
     * @param petId        반려동물 ID
     * @param targetUserId 승인/거절 대상 유저 ID
     * @param action       처리할 상태 문자열 ("APPROVED" 또는 "REJECTED")
     * @return 펫 ID와 처리 결과 메시지가 담긴 응답 DTO
     */
    @Transactional
    @Override
    public PetFamilyApprovalResponse manageFamily(UUID userId, Long petId, UUID targetUserId, String action) {

        String resultMessage = userPetService.approveOrRejectFamilyMember(userId, petId, targetUserId, action);

        return PetFamilyApprovalResponse.toDto(petId, resultMessage);
    }

    /**
     * 내가 관리하는 모든 반려동물에게 들어온 가족 신청(대기자) 목록을 페이징하여 조회합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService}를 호출하여 내 펫들에 대한 PENDING 상태인 UserPet 엔티티 목록을 받아옵니다.</li>
     * <li>목록에서 펫 ID들을 추출하여 {@link ImageFileService}를 통해 프로필 이미지 URL을 일괄 조회합니다.</li>
     * <li>엔티티 목록을 순회하며 신청자 정보와 대상 펫 정보(이미지 포함)를 {@link PendingUserResponse} DTO로 변환합니다.</li>
     * <li>최종적으로 페이징 정보가 포함된 {@link PendingUserListResponse} 객체를 반환합니다.</li>
     * </ol>
     *
     * @param userId 요청을 수행하는 관리자(기존 가족)의 UUID
     * @param pageable  페이징 요청 정보
     * @return 페이징된 승인 대기자 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public PendingUserListResponse getAllPendingUsers(UUID userId, Pageable pageable) {

        Map<String, Object> result = userPetService.getAllPendingUsers(userId, pageable);
        Page<UserPet> entityPage = (Page<UserPet>) result.get("pendingUserPage");

        List<Long> petIds = entityPage.getContent().stream()
                .map(userPet -> userPet.getPet().getPetId())
                .collect(Collectors.toList());

        Map<Long, String> imageMap = imageFileService.getProfileUrlsByPetIds(petIds);

        Page<PendingUserResponse> dtoPage = entityPage.map(userPet ->
                PendingUserResponse.toDto(
                        userPet.getUser().getUsersId(),
                        userPet.getUser().getNickname(),
                        userPet.getUser().getProfileUrl(),
                        userPet.getPet().getPetId(),
                        userPet.getPet().getPetName(),
                        imageMap.get(userPet.getPet().getPetId()),
                        userPet.getRegistrationCreatedAt()
                )
        );

        return PendingUserListResponse.toDto(dtoPage, "조회를 성공했습니다.");
    }

    /**
     * 내가 가족 신청을 보낸 후 대기 중인 반려동물 목록을 페이징하여 조회합니다.
     * <p>
     * <ol>
     * <li>{@link UserPetService}를 호출하여 내가 신청한(PENDING) UserPet 엔티티 목록을 Map 형태로 받아옵니다.</li>
     * <li>목록에서 펫 ID들을 추출하여 {@link ImageFileService}를 통해 프로필 이미지 URL을 일괄 조회합니다.</li>
     * <li>아직 가족이 아니므로 상세 정보 조회 없이, 펫의 기본 정보(ID, 이름, 이미지, 상태)만 {@link PetApplicationResponse} DTO로 변환합니다.</li>
     * <li>최종적으로 페이징 정보가 포함된 {@link PetApplicationListResponse} 객체를 반환합니다.</li>
     * </ol>
     *
     * @param userId   조회할 사용자의 UUID
     * @param pageable 페이징 요청 정보
     * @return 페이징된 신청 내역 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    @Override
    public PetApplicationListResponse getMyPendingApplications(UUID userId, Pageable pageable) {

        Map<String, Object> result = userPetService.getMyPendingPets(userId, pageable);
        Page<UserPet> entityPage = (Page<UserPet>) result.get("pendingPetPage");

        List<Long> petIds = entityPage.getContent().stream()
                .map(userPet -> userPet.getPet().getPetId())
                .collect(Collectors.toList());

        Map<Long, String> imageMap = imageFileService.getProfileUrlsByPetIds(petIds);

        Page<PetApplicationResponse> dtoPage = entityPage.map(userPet ->
                PetApplicationResponse.toDto(
                        userPet.getPet().getPetId(),
                        userPet.getPet().getPetName(),
                        imageMap.get(userPet.getPet().getPetId()),
                        userPet.getRegistrationStatus().name(),
                        userPet.getRegistrationCreatedAt()
                )
        );

        return PetApplicationListResponse.toDto(dtoPage, "조회를 성공했습니다.");
    }

    /**
     * 반려동물 가족 그룹에서 탈퇴(삭제)합니다.
     * <p>
     * <ol>
     * <li>요청한 유저가 해당 펫의 가족(소유자)인지 검증합니다.</li>
     * <li>해당 유저와 펫의 연결 관계를 삭제합니다. (가족 나가기)</li>
     * <li>삭제 후, 해당 펫에 남은 가족이 있는지 확인합니다.</li>
     * <li>만약 남은 가족이 한 명도 없다면, 펫 정보(Pet) 자체를 DB에서 완전히 삭제합니다.</li>
     * </ol>
     *
     * @param userId 요청한 사용자의 ID
     * @param petId  삭제(탈퇴)할 반려동물 ID
     */
    @Transactional
    @Override
    public void deletePet(UUID userId, Long petId) {

        if (!petRepository.existsById(petId)) {
            throw new PetException(PET_NOT_FOUND);
        }

        if (!userPetService.isUserPetOwner(userId, petId)) {
            throw new PetException(ACTION_PERMISSION_DENIED);
        }

        userPetService.deleteUserPetRelation(userId, petId);

        if (!userPetService.existsFamilyMember(petId)) {

            petRepository.deleteById(petId);
            log.info("마지막 가족 탈퇴로 펫 정보가 완전 삭제되었습니다. PetId: {}", petId);
        } else {
            log.info("가족 그룹에서 탈퇴했습니다. (다른 가족이 남아있어 펫 정보 유지) User: {}, PetId: {}", userId, petId);
        }
    }

    /**
     * 반려동물의 디바이스를 재등록(수정)합니다.
     * <p>
     * 1. 권한 검증 (소유자 확인)
     * 2. 디바이스 ID 중복 확인 (이미 다른 펫이 사용 중인지)
     * 3. MyBatis를 통한 업데이트 수행
     * 4. DTO 반환 (엔티티 직접 참조 X)
     */
    @Transactional
    @Override
    public PetDeviceUpdateResponse updateDevice(UUID userId, Long petId, PetDeviceUpdateRequest request) {

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PET_NOT_FOUND));

        if (!userPetService.isUserPetOwner(userId, petId)) {
            throw new PetException(ACTION_PERMISSION_DENIED);
        }

        String newDeviceId = request.getDeviceId();
        String oldDeviceId = pet.getDeviceId();

        if (!Objects.equals(oldDeviceId, newDeviceId)) {
            if (petRepository.existsByDeviceId(newDeviceId)) {
                throw new PetException(DEVICE_ID_DUPLICATED);
            }
        }

        petMapper.updatePetDevice(petId, newDeviceId);

        log.info("펫 디바이스 변경 완료 - PetId: {}, Old: {}, New: {}", petId, oldDeviceId, newDeviceId);

        return PetDeviceUpdateResponse.toDto(
                pet.getPetId(),
                pet.getPetName(),
                oldDeviceId,
                newDeviceId,
                "디바이스가 성공적으로 재등록되었습니다."
        );
    }

    /**
     * 디바이스 ID로 등록된 반려동물 ID를 조회합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<Long> findPetIdByDeviceId(String deviceId) {
        return petRepository.findByDeviceId(deviceId)
                .map(Pet::getPetId);
    }

    /**
     * ID로 펫 엔티티를 조회합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public Pet getPetById(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new PetException(PET_NOT_FOUND));
    }

    /**
     * 반려동물 ID 존재 여부를 확인합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public boolean existsPetById(Long petId) {
        return petRepository.existsById(petId);
    }

    /**
     * 특정 반려동물의 기준 심박수(Reference Heart Rate)를 조회합니다.
     * <p>
     * 부정맥 판별이나 건강 상태 모니터링 시 비교 기준으로 사용됩니다.
     * 설정된 값이 없는 경우 {@code null}을 반환합니다.
     *
     * @param petId 조회할 반려동물의 ID
     * @return 기준 심박수 (BPM), 설정되지 않았으면 null
     */
    @Transactional(readOnly = true)
    @Override
    public Integer getAverageHeartRate(Long petId) {
        return petRepository.findById(petId)
                .map(Pet::getReferenceHeartRate)
                .orElse(null);
    }
}
