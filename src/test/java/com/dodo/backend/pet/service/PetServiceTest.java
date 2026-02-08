package com.dodo.backend.pet.service;

import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.pet.dto.request.PetRequest;
import com.dodo.backend.pet.dto.response.PetResponse;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.entity.PetSex;
import com.dodo.backend.pet.entity.PetSpecies;
import com.dodo.backend.pet.exception.PetErrorCode;
import com.dodo.backend.pet.exception.PetException;
import com.dodo.backend.pet.mapper.PetMapper;
import com.dodo.backend.pet.repository.PetRepository;
import com.dodo.backend.petweight.service.PetWeightService;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.userpet.entity.RegistrationStatus;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link PetService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @InjectMocks
    private PetServiceImpl petService;

    @Mock
    private PetRepository petRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPetService userPetService;

    @Mock
    private PetMapper petMapper;

    @Mock
    private PetWeightService petWeightService;

    @Mock
    private ImageFileService imageFileService;

    /**
     * 펫 등록 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("펫 등록 성공: 정상적인 요청 시 펫이 저장되고 유저와 연결된다.")
    void registerPet_Success() {
        log.info("펫 등록 성공 케이스 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder()
                .petName("바둑이")
                .species("CANINE")
                .breed("Poodle")
                .age(3)
                .birth(LocalDateTime.now())
                .registrationNumber("1234567890")
                .sex("MALE")
                .deviceId("DEV_123")
                .build();

        Pet savedPet = request.toEntity();
        ReflectionTestUtils.setField(savedPet, "petId", 1L);

        log.info("사용자가 존재하고 등록번호가 중복되지 않는 상황을 설정합니다.");
        given(userRepository.existsById(userId)).willReturn(true);
        given(petRepository.existsByRegistrationNumber(request.getRegistrationNumber())).willReturn(false);
        given(petRepository.save(any(Pet.class))).willReturn(savedPet);

        // when
        log.info("펫 등록 서비스 로직을 호출합니다.");
        PetResponse.PetRegisterResponse response = petService.registerPet(userId, request);

        // then
        log.info("등록된 펫 ID가 반환되었는지 확인하고, 관련 서비스가 호출되었는지 검증합니다.");
        assertNotNull(response);
        assertEquals(1L, response.getPetId());

        verify(userRepository, times(1)).existsById(userId);
        verify(petRepository, times(1)).save(any(Pet.class));
        verify(userPetService, times(1)).registerUserPet(userId, savedPet, RegistrationStatus.APPROVED);
        log.info("펫 등록 성공 테스트가 통과되었습니다.");
    }

    /**
     * 존재하지 않는 유저 ID로 펫 등록을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("펫 등록 실패: 존재하지 않는 유저 ID인 경우 예외가 발생한다.")
    void registerPet_Fail_UserNotFound() {
        log.info("존재하지 않는 유저로 인한 펫 등록 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder().build();

        log.info("유저가 존재하지 않는 상황을 설정합니다.");
        given(userRepository.existsById(userId)).willReturn(false);

        // when
        log.info("펫 등록 요청 시 예외가 발생하는지 확인합니다.");
        PetException exception = assertThrows(PetException.class, () ->
                petService.registerPet(userId, request)
        );

        // then
        log.info("발생한 예외 코드가 USER_NOT_FOUND인지 검증합니다.");
        assertEquals(PetErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        log.info("유저 미발견 실패 테스트가 통과되었습니다.");
    }

    /**
     * 이미 등록된 펫 등록번호로 등록을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("펫 등록 실패: 이미 존재하는 등록번호인 경우 예외가 발생한다.")
    void registerPet_Fail_DuplicateRegistrationNumber() {
        log.info("등록번호 중복으로 인한 펫 등록 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        PetRequest.PetRegisterRequest request = PetRequest.PetRegisterRequest.builder()
                .registrationNumber("1234567890")
                .build();

        log.info("이미 존재하는 등록번호라고 가정합니다.");
        given(userRepository.existsById(userId)).willReturn(true);
        given(petRepository.existsByRegistrationNumber(request.getRegistrationNumber())).willReturn(true);

        // when
        log.info("중복된 등록번호로 요청 시 예외가 발생하는지 확인합니다.");
        PetException exception = assertThrows(PetException.class, () ->
                petService.registerPet(userId, request)
        );

        // then
        log.info("발생한 예외 코드가 REGISTRATION_NUMBER_DUPLICATED인지 검증합니다.");
        assertEquals(PetErrorCode.REGISTRATION_NUMBER_DUPLICATED, exception.getErrorCode());
        verify(petRepository, times(0)).save(any(Pet.class));
        log.info("등록번호 중복 실패 테스트가 통과되었습니다.");
    }

    /**
     * 반려동물 정보 수정 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 성공: 소유자가 요청하고 중복이 없으면 정상적으로 수정된다.")
    void updatePet_Success() {
        log.info("펫 정보 수정 성공 테스트를 시작합니다.");
        // given
        Long petId = 1L;
        UUID userId = UUID.randomUUID();

        Pet existingPet = Pet.builder()
                .petId(petId)
                .registrationNumber("OLD-123")
                .petName("초코")
                .sex(PetSex.MALE)
                .build();

        PetRequest.PetUpdateRequest request = PetRequest.PetUpdateRequest.builder()
                .petName("새로운초코")
                .registrationNumber("NEW-999")
                .sex("FEMALE")
                .age(5)
                .build();

        log.info("펫이 존재하고 요청자가 소유자이며, 새 등록번호가 중복되지 않음을 설정합니다.");
        given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
        given(userPetService.isUserPetOwner(userId, petId)).willReturn(true);
        given(petRepository.existsByRegistrationNumber("NEW-999")).willReturn(false);

        // when
        log.info("펫 수정 서비스 로직을 호출합니다.");
        PetResponse.PetUpdateResponse response = petService.updatePet(petId, request, userId);

        // then
        log.info("반환된 펫 이름이 수정된 이름과 일치하는지, 업데이트 Mapper가 호출되었는지 검증합니다.");
        assertNotNull(response);
        assertEquals("새로운초코", response.getPetName());

        verify(petMapper, times(1)).updatePetProfileInfo(request, petId);
        log.info("펫 수정 성공 테스트가 통과되었습니다.");
    }

    /**
     * 반려동물 정보 수정 실패: 권한 없음 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 실패: 소유자가 아닌 경우 권한 없음 예외가 발생한다.")
    void updatePet_Fail_PermissionDenied() {
        log.info("권한 없음으로 인한 펫 수정 실패 테스트를 시작합니다.");
        // given
        Long petId = 1L;
        UUID userId = UUID.randomUUID();
        PetRequest.PetUpdateRequest request = PetRequest.PetUpdateRequest.builder().build();

        Pet existingPet = Pet.builder().petId(petId).build();

        log.info("요청자가 해당 펫의 소유자가 아니라고 설정합니다.");
        given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
        given(userPetService.isUserPetOwner(userId, petId)).willReturn(false);

        // when
        log.info("수정 요청 시 권한 예외가 발생하는지 확인합니다.");
        PetException exception = assertThrows(PetException.class, () ->
                petService.updatePet(petId, request, userId)
        );

        // then
        log.info("발생한 예외 코드가 UPDATE_PERMISSION_DENIED인지 검증합니다.");
        assertEquals(PetErrorCode.UPDATE_PERMISSION_DENIED, exception.getErrorCode());
        verify(petMapper, times(0)).updatePetProfileInfo(any(), any());
        log.info("권한 없음 실패 테스트가 통과되었습니다.");
    }

    /**
     * 존재하지 않는 반려동물 ID로 수정을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("펫 수정 실패: 존재하지 않는 펫 ID인 경우 예외가 발생한다.")
    void updatePet_Fail_NotFound() {
        log.info("존재하지 않는 펫 수정 시도 실패 테스트를 시작합니다.");
        // given
        Long petId = 999L;
        UUID userId = UUID.randomUUID();
        PetRequest.PetUpdateRequest request = PetRequest.PetUpdateRequest.builder().build();

        log.info("해당 ID의 펫이 존재하지 않는다고 설정합니다.");
        given(petRepository.findById(petId)).willReturn(Optional.empty());

        // when
        log.info("수정 요청 시 예외가 발생하는지 확인합니다.");
        PetException exception = assertThrows(PetException.class, () ->
                petService.updatePet(petId, request, userId)
        );

        // then
        log.info("발생한 예외 코드가 PET_NOT_FOUND인지 검증합니다.");
        assertEquals(PetErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(petMapper, times(0)).updatePetProfileInfo(any(), any());
        log.info("펫 미발견 실패 테스트가 통과되었습니다.");
    }

    /**
     * 가족 초대 코드 발급 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 성공: 펫이 존재하면 UserPetService를 통해 코드가 발급된다.")
    void issueInvitationCode_Success() {
        log.info("가족 초대 코드 발급 성공 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        String expectedCode = "ABC1234";
        Long expectedExpiresIn = 900L;

        Map<String, Object> serviceResult = Map.of(
                "code", expectedCode,
                "expiresIn", expectedExpiresIn
        );

        log.info("펫이 존재하고, 초대 코드 생성 서비스가 정상 결과를 반환한다고 설정합니다.");
        given(petRepository.existsById(petId)).willReturn(true);
        given(userPetService.generateInvitationCode(userId, petId)).willReturn(serviceResult);

        // when
        log.info("초대 코드 발급 메서드를 호출합니다.");
        PetResponse.PetInvitationResponse response = petService.issueInvitationCode(userId, petId);

        // then
        log.info("반환된 코드와 만료 시간이 예상값과 일치하는지 검증합니다.");
        assertNotNull(response);
        assertEquals(expectedCode, response.getCode());
        assertEquals(expectedExpiresIn, response.getExpiresIn());

        verify(petRepository, times(1)).existsById(petId);
        verify(userPetService, times(1)).generateInvitationCode(userId, petId);
        log.info("초대 코드 발급 성공 테스트가 통과되었습니다.");
    }

    /**
     * 존재하지 않는 펫 ID로 초대 코드를 요청할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("초대 코드 발급 실패: 펫이 존재하지 않으면 예외가 발생한다.")
    void issueInvitationCode_Fail_PetNotFound() {
        log.info("존재하지 않는 펫에 대한 초대 코드 발급 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 999L;

        log.info("펫이 존재하지 않는다고 설정합니다.");
        given(petRepository.existsById(petId)).willReturn(false);

        // when
        log.info("발급 요청 시 예외가 발생하는지 확인합니다.");
        PetException exception = assertThrows(PetException.class, () ->
                petService.issueInvitationCode(userId, petId)
        );

        // then
        log.info("발생한 예외 코드가 PET_NOT_FOUND인지 검증합니다.");
        assertEquals(PetErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(userPetService, times(0)).generateInvitationCode(any(), any());
        log.info("초대 코드 발급 실패 테스트가 통과되었습니다.");
    }

    /**
     * 가족 초대 코드 입력 및 참여 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("가족 참여 신청 성공: 유효한 코드로 요청 시 펫 ID와 성공 메시지를 반환한다.")
    void applyForFamily_Success() {
        log.info("가족 참여 신청 성공 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        String invitationCode = "7X9K2P";
        Long petId = 100L;
        PetRequest.PetFamilyJoinRequest request = new PetRequest.PetFamilyJoinRequest(invitationCode);

        log.info("초대 코드가 유효하여 펫 ID를 반환한다고 설정합니다.");
        given(userPetService.registerByInvitation(userId, invitationCode)).willReturn(petId);

        // when
        log.info("가족 신청 메서드를 호출합니다.");
        PetResponse.PetFamilyJoinRequestResponse response = petService.applyForFamily(userId, request);

        // then
        log.info("응답에 펫 ID와 성공 메시지가 포함되어 있는지 검증합니다.");
        assertNotNull(response);
        assertEquals(petId, response.getPetId());
        assertEquals("가족 등록을 신청했습니다. 승인을 기다려주세요.", response.getMessage());

        verify(userPetService, times(1)).registerByInvitation(userId, invitationCode);
        log.info("가족 참여 신청 성공 테스트가 통과되었습니다.");
    }

    /**
     * 펫 삭제 성공 시나리오를 테스트합니다.
     * (가족 나가기 로직 반영: UserPet 관계 삭제)
     */
    @Test
    @DisplayName("펫 삭제 성공: 소유자가 나갈 경우 UserPet 관계가 삭제된다.")
    void deletePet_Success() {
        log.info("펫 가족 나가기 성공 테스트를 시작합니다.");
        // given
        Long petId = 1L;
        UUID userId = UUID.randomUUID();

        log.info("펫이 존재하고 요청자가 소유자이며, 다른 가족이 남아있다고 설정합니다.");
        given(petRepository.existsById(petId)).willReturn(true);
        given(userPetService.isUserPetOwner(userId, petId)).willReturn(true);
        given(userPetService.existsFamilyMember(petId)).willReturn(true);

        // when
        log.info("펫 삭제(가족 나가기) 메서드를 호출합니다.");
        petService.deletePet(userId, petId);

        // then
        log.info("관계 삭제 로직이 호출되었는지, 펫 정보 삭제는 호출되지 않았는지 검증합니다.");
        verify(userPetService, times(1)).deleteUserPetRelation(userId, petId);
        verify(petRepository, times(0)).deleteById(petId);
        log.info("가족 나가기 성공 테스트가 통과되었습니다.");
    }

    /**
     * 펫 삭제 실패: 권한 없음 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("펫 삭제 실패: 소유자가 아닌 경우 예외가 발생한다.")
    void deletePet_Fail_PermissionDenied() {
        log.info("권한 없음으로 인한 펫 삭제 실패 테스트를 시작합니다.");
        // given
        Long petId = 1L;
        UUID userId = UUID.randomUUID();

        log.info("펫은 존재하지만 요청자가 소유자가 아니라고 설정합니다.");
        given(petRepository.existsById(petId)).willReturn(true);
        given(userPetService.isUserPetOwner(userId, petId)).willReturn(false);

        // when
        log.info("삭제 요청 시 예외가 발생하는지 확인합니다.");
        PetException exception = assertThrows(PetException.class, () ->
                petService.deletePet(userId, petId)
        );

        // then
        log.info("발생한 예외 코드가 ACTION_PERMISSION_DENIED인지 검증합니다.");
        assertEquals(PetErrorCode.ACTION_PERMISSION_DENIED, exception.getErrorCode());
        verify(userPetService, times(0)).deleteUserPetRelation(any(), any());
        log.info("권한 없음 삭제 실패 테스트가 통과되었습니다.");
    }

    /**
     * 펫 삭제 실패: 존재하지 않는 펫 ID 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("펫 삭제 실패: 펫이 존재하지 않는 경우 예외가 발생한다.")
    void deletePet_Fail_NotFound() {
        log.info("존재하지 않는 펫 삭제 실패 테스트를 시작합니다.");
        // given
        Long petId = 999L;
        UUID userId = UUID.randomUUID();

        log.info("펫이 존재하지 않는다고 설정합니다.");
        given(petRepository.existsById(petId)).willReturn(false);

        // when
        log.info("삭제 요청 시 예외가 발생하는지 확인합니다.");
        PetException exception = assertThrows(PetException.class, () ->
                petService.deletePet(userId, petId)
        );

        // then
        log.info("발생한 예외 코드가 PET_NOT_FOUND인지 검증합니다.");
        assertEquals(PetErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(userPetService, times(0)).deleteUserPetRelation(any(), any());
        log.info("펫 미발견 삭제 실패 테스트가 통과되었습니다.");
    }

    /**
     * 펫 디바이스 재등록 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("디바이스 재등록 성공: 중복되지 않은 새 ID로 요청 시 정상적으로 업데이트된다.")
    void updateDevice_Success() {
        log.info("디바이스 재등록 성공 테스트를 시작합니다.");
        // given
        Long petId = 1L;
        UUID userId = UUID.randomUUID();
        String oldDeviceId = "OLD_DEV";
        String newDeviceId = "NEW_DEV";

        Pet pet = Pet.builder()
                .petId(petId)
                .petName("나비")
                .deviceId(oldDeviceId)
                .build();

        PetRequest.PetDeviceUpdateRequest request = PetRequest.PetDeviceUpdateRequest.builder()
                .deviceId(newDeviceId)
                .build();

        log.info("펫이 존재하고 소유자 권한이 있으며, 새 디바이스 ID가 중복되지 않는다고 설정합니다.");
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));
        given(userPetService.isUserPetOwner(userId, petId)).willReturn(true);
        given(petRepository.existsByDeviceId(newDeviceId)).willReturn(false);

        // when
        log.info("디바이스 업데이트 메서드를 호출합니다.");
        PetResponse.PetDeviceUpdateResponse response = petService.updateDevice(userId, petId, request);

        // then
        log.info("반환된 디바이스 정보가 새 ID와 일치하는지, 업데이트 Mapper가 호출되었는지 검증합니다.");
        assertNotNull(response);
        assertEquals(petId, response.getPetId());
        assertEquals(oldDeviceId, response.getPreviousDeviceId());
        assertEquals(newDeviceId, response.getNewDeviceId());

        verify(petMapper, times(1)).updatePetDevice(petId, newDeviceId);
        log.info("디바이스 재등록 성공 테스트가 통과되었습니다.");
    }

    /**
     * 펫 디바이스 재등록 실패: 권한 없음 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("디바이스 재등록 실패: 소유자가 아닌 경우 예외가 발생한다.")
    void updateDevice_Fail_PermissionDenied() {
        log.info("권한 없음으로 인한 디바이스 재등록 실패 테스트를 시작합니다.");
        // given
        Long petId = 1L;
        UUID userId = UUID.randomUUID();
        String newDeviceId = "NEW_DEV";

        Pet pet = Pet.builder().petId(petId).build();
        PetRequest.PetDeviceUpdateRequest request = PetRequest.PetDeviceUpdateRequest.builder()
                .deviceId(newDeviceId)
                .build();

        log.info("요청자가 해당 펫의 소유자가 아니라고 설정합니다.");
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));
        given(userPetService.isUserPetOwner(userId, petId)).willReturn(false);

        // when
        log.info("업데이트 요청 시 예외가 발생하는지 확인합니다.");
        PetException exception = assertThrows(PetException.class, () ->
                petService.updateDevice(userId, petId, request)
        );

        // then
        log.info("발생한 예외 코드가 ACTION_PERMISSION_DENIED인지 검증합니다.");
        assertEquals(PetErrorCode.ACTION_PERMISSION_DENIED, exception.getErrorCode());
        verify(petMapper, times(0)).updatePetDevice(any(), any());
        log.info("권한 없음 재등록 실패 테스트가 통과되었습니다.");
    }

    /**
     * 펫 디바이스 재등록 실패: 이미 사용 중인 ID 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("디바이스 재등록 실패: 이미 다른 펫이 사용 중인 디바이스 ID면 예외가 발생한다.")
    void updateDevice_Fail_DuplicateDeviceId() {
        log.info("디바이스 ID 중복으로 인한 재등록 실패 테스트를 시작합니다.");
        // given
        Long petId = 1L;
        UUID userId = UUID.randomUUID();
        String oldDeviceId = "OLD_DEV";
        String duplicateDeviceId = "DUPLICATE_DEV";

        Pet pet = Pet.builder()
                .petId(petId)
                .deviceId(oldDeviceId)
                .build();

        PetRequest.PetDeviceUpdateRequest request = PetRequest.PetDeviceUpdateRequest.builder()
                .deviceId(duplicateDeviceId)
                .build();

        log.info("요청한 디바이스 ID가 이미 존재한다고 설정합니다.");
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));
        given(userPetService.isUserPetOwner(userId, petId)).willReturn(true);
        given(petRepository.existsByDeviceId(duplicateDeviceId)).willReturn(true);

        // when
        log.info("업데이트 요청 시 예외가 발생하는지 확인합니다.");
        PetException exception = assertThrows(PetException.class, () ->
                petService.updateDevice(userId, petId, request)
        );

        // then
        log.info("발생한 예외 코드가 DEVICE_ID_DUPLICATED인지 검증합니다.");
        assertEquals(PetErrorCode.DEVICE_ID_DUPLICATED, exception.getErrorCode());
        verify(petMapper, times(0)).updatePetDevice(any(), any());
        log.info("ID 중복 재등록 실패 테스트가 통과되었습니다.");
    }
}