package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityFinishRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse;
import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.activityhistory.entity.ActivityType;
import com.dodo.backend.activityhistory.exception.ActivityHistoryErrorCode;
import com.dodo.backend.activityhistory.exception.ActivityHistoryException;
import com.dodo.backend.activityhistory.mapper.ActivityHistoryMapper;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link ActivityHistoryService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class ActivityHistoryServiceTest {

    @InjectMocks
    private ActivityHistoryServiceImpl activityHistoryService;

    @Mock
    private ActivityHistoryRepository activityHistoryRepository;

    @Mock
    private ActivityHistoryMapper activityHistoryMapper;

    @Mock
    private PetService petService;

    @Mock
    private UserPetService userPetService;

    @Mock
    private UserService userService;

    /**
     * 활동 기록 생성 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 성공: 정상적인 요청 시 상태가 BEFORE인 기록이 생성된다.")
    void createActivity_Success() {
        log.info("활동 기록 생성 성공 케이스 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType("WALKING")
                .build();

        ActivityHistory savedHistory = request.toEntity(user, pet);
        ReflectionTestUtils.setField(savedHistory, "historyId", 100L);

        log.info("유저와 펫이 존재하고, 권한이 있으며, 중복된 활동이 없는 상황을 설정합니다.");
        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(false);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)).willReturn(false);
        given(activityHistoryRepository.save(any(ActivityHistory.class))).willReturn(savedHistory);

        // when
        log.info("활동 기록 생성 서비스 로직을 호출합니다.");
        ActivityHistoryResponse.ActivityCreateResponse response = activityHistoryService.createActivity(userId, request);

        // then
        log.info("생성된 History ID가 반환되었는지 확인하고, 저장 로직이 호출되었는지 검증합니다.");
        assertNotNull(response);
        assertEquals(100L, response.getHistoryId());
        assertEquals(ActivityType.WALKING, response.getActivityType());

        verify(userService, times(1)).getUserById(userId);
        verify(petService, times(1)).getPetById(petId);
        verify(userPetService, times(1)).isApprovedPetOwner(userId, petId);
        verify(activityHistoryRepository, times(1)).save(any(ActivityHistory.class));
        log.info("활동 기록 생성 성공 테스트가 통과되었습니다.");
    }

    /**
     * 권한이 없는 사용자가 활동 기록 생성을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 실패: 펫의 소유자가 아닌 경우 권한 예외가 발생한다.")
    void createActivity_Fail_PermissionDenied() {
        log.info("권한 없음으로 인한 활동 생성 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType("WALKING")
                .build();

        log.info("유저와 펫은 존재하지만, 소유자가 아니라고 설정합니다.");
        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        log.info("생성 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );

        // then
        log.info("발생한 예외 코드가 CREATE_PERMISSION_DENIED인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.CREATE_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).save(any(ActivityHistory.class));
        log.info("권한 없음 실패 테스트가 통과되었습니다.");
    }

    /**
     * 이미 진행 중(IN_PROGRESS)인 활동이 있을 때 생성 시도를 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 실패: 이미 진행 중인 활동이 있는 경우 예외가 발생한다.")
    void createActivity_Fail_AlreadyInProgress() {
        log.info("중복 활동(진행 중)으로 인한 생성 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType("WALKING")
                .build();

        log.info("소유자 권한은 있으나, 이미 진행 중인 활동이 존재한다고 설정합니다.");
        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(true);

        // when
        log.info("생성 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );

        // then
        log.info("발생한 예외 코드가 ALREADY_IN_PROGRESS인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.ALREADY_IN_PROGRESS, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).save(any(ActivityHistory.class));
        log.info("진행 중인 활동 중복 실패 테스트가 통과되었습니다.");
    }

    /**
     * 이미 시작 대기 중(BEFORE)인 활동이 있을 때 생성 시도를 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 실패: 이미 시작 대기 중인 활동이 있는 경우 예외가 발생한다.")
    void createActivity_Fail_AlreadyExistsBefore() {
        log.info("중복 활동(시작 대기 중)으로 인한 생성 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType("WALKING")
                .build();

        log.info("진행 중인 활동은 없으나, 이미 시작 대기 중(BEFORE)인 활동이 존재한다고 설정합니다.");
        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(false);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)).willReturn(true);

        // when
        log.info("생성 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );

        // then
        log.info("발생한 예외 코드가 ALREADY_EXISTS_BEFORE인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.ALREADY_EXISTS_BEFORE, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).save(any(ActivityHistory.class));
        log.info("시작 대기 중 활동 중복 실패 테스트가 통과되었습니다.");
    }

    /**
     * 활동 시작 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 시작 성공: 정상 요청 시 상태가 IN_PROGRESS로 변경되고 Mapper가 호출된다.")
    void startActivity_Success() {
        log.info("활동 시작 성공 케이스 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityHistoryStatus(ActivityHistoryStatus.BEFORE)
                .build();

        // Mocking: 요청 객체 (위치 정보 포함)
        ActivityHistoryRequest.ActivityStartRequest request = ActivityHistoryRequest.ActivityStartRequest.builder()
                .startLatitude(BigDecimal.valueOf(37.1234))
                .startLongitude(BigDecimal.valueOf(127.1234))
                .build();

        log.info("활동 기록이 존재하고, 소유자이며, 상태가 BEFORE인 상황을 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("활동 시작 서비스 로직을 호출합니다.");
        activityHistoryService.startActivity(userId, historyId, request);

        // then
        log.info("Mapper의 startActivity 메서드가 올바른 파라미터로 호출되었는지 검증합니다.");
        verify(activityHistoryMapper, times(1)).startActivity(
                historyId,
                ActivityHistoryStatus.IN_PROGRESS.name(),
                request.getStartLatitude(),
                request.getStartLongitude()
        );
        log.info("활동 시작 성공 테스트가 통과되었습니다.");
    }

    /**
     * 권한이 없는 사용자가 활동 시작을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 시작 실패: 기록의 소유자가 아닌 경우 예외가 발생한다.")
    void startActivity_Fail_PermissionDenied() {
        log.info("권한 없음으로 인한 활동 시작 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Long historyId = 100L;

        User otherUser = User.builder().usersId(otherUserId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(otherUser)
                .activityHistoryStatus(ActivityHistoryStatus.BEFORE)
                .build();

        ActivityHistoryRequest.ActivityStartRequest request = ActivityHistoryRequest.ActivityStartRequest.builder().build();

        log.info("활동 기록의 소유자가 요청자와 다르다고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("시작 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.startActivity(userId, historyId, request)
        );

        // then
        log.info("발생한 예외 코드가 START_PERMISSION_DENIED인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.START_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).startActivity(any(), any(), any(), any());
        log.info("권한 없음 시작 실패 테스트가 통과되었습니다.");
    }

    /**
     * 이미 진행 중이거나 완료된 활동을 다시 시작하려 할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 시작 실패: 활동 상태가 BEFORE가 아닌 경우 예외가 발생한다.")
    void startActivity_Fail_InvalidStatus() {
        log.info("잘못된 상태로 인한 활동 시작 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityHistoryStatus(ActivityHistoryStatus.IN_PROGRESS)
                .build();

        ActivityHistoryRequest.ActivityStartRequest request = ActivityHistoryRequest.ActivityStartRequest.builder().build();

        log.info("활동 기록의 상태가 이미 IN_PROGRESS라고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("시작 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.startActivity(userId, historyId, request)
        );

        // then
        log.info("발생한 예외 코드가 ALREADY_IN_PROGRESS인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.ALREADY_IN_PROGRESS, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).startActivity(any(), any(), any(), any());
        log.info("잘못된 상태 시작 실패 테스트가 통과되었습니다.");
    }

    /**
     * 존재하지 않는 활동 기록 ID로 시작을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 시작 실패: 활동 기록이 존재하지 않는 경우 예외가 발생한다.")
    void startActivity_Fail_NotFound() {
        log.info("존재하지 않는 기록으로 인한 활동 시작 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;
        ActivityHistoryRequest.ActivityStartRequest request = ActivityHistoryRequest.ActivityStartRequest.builder().build();

        log.info("해당 ID의 활동 기록이 없다고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        log.info("시작 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.startActivity(userId, historyId, request)
        );

        // then
        log.info("발생한 예외 코드가 HISTORY_NOT_FOUND인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).startActivity(any(), any(), any(), any());
        log.info("미발견 시작 실패 테스트가 통과되었습니다.");
    }

    /**
     * 중단된 활동을 재개하는 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 재개 성공: 상태가 CANCELED일 때 요청 시 재개 로직(resumeActivity)이 실행된다.")
    void startActivity_Resume_Success() {
        log.info("활동 재개 성공 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityHistoryStatus(ActivityHistoryStatus.CANCELED) // 상태: 취소됨
                .build();

        ActivityHistoryRequest.ActivityStartRequest request = ActivityHistoryRequest.ActivityStartRequest.builder()
                .startLatitude(BigDecimal.valueOf(37.5))
                .startLongitude(BigDecimal.valueOf(127.5))
                .build();

        log.info("활동 기록이 CANCELED 상태라고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("활동 시작(재개) 서비스 로직을 호출합니다.");
        ActivityHistoryResponse.ActivitySimpleResponse response = activityHistoryService.startActivity(userId, historyId, request);

        // then
        log.info("Mapper의 resumeActivity가 호출되었는지 검증합니다.");
        assertNotNull(response);
        verify(activityHistoryMapper, times(1)).resumeActivity(
                historyId,
                ActivityHistoryStatus.IN_PROGRESS.name()
        );
        verify(activityHistoryMapper, times(0)).startActivity(any(), any(), any(), any());
        log.info("활동 재개 성공 테스트가 통과되었습니다.");
    }

    /**
     * 진행 중인 활동을 취소(중단)하는 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 취소 성공: 상태가 IN_PROGRESS일 때 요청 시 취소 로직(cancelActivity)이 실행된다.")
    void cancelActivity_Success() {
        log.info("활동 취소 성공 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityHistoryStatus(ActivityHistoryStatus.IN_PROGRESS)
                .build();

        log.info("활동 기록이 IN_PROGRESS 상태라고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("활동 취소 서비스 로직을 호출합니다.");
        ActivityHistoryResponse.ActivitySimpleResponse response = activityHistoryService.cancelActivity(userId, historyId);

        // then
        log.info("Mapper의 cancelActivity가 호출되었는지 검증합니다.");
        assertNotNull(response);
        assertEquals("활동 기록이 성공적으로 중단되었습니다.", response.getMessage());

        verify(activityHistoryMapper, times(1)).cancelActivity(
                historyId,
                ActivityHistoryStatus.CANCELED.name()
        );
        log.info("활동 취소 성공 테스트가 통과되었습니다.");
    }

    /**
     * 권한이 없는 사용자가 활동 취소를 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 취소 실패: 기록의 소유자가 아닌 경우 예외가 발생한다.")
    void cancelActivity_Fail_PermissionDenied() {
        log.info("권한 없음으로 인한 활동 취소 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Long historyId = 100L;

        User otherUser = User.builder().usersId(otherUserId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(otherUser)
                .activityHistoryStatus(ActivityHistoryStatus.IN_PROGRESS)
                .build();

        log.info("활동 기록의 소유자가 요청자와 다르다고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("취소 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.cancelActivity(userId, historyId)
        );

        // then
        log.info("발생한 예외 코드가 STOP_PERMISSION_DENIED인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.STOP_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).cancelActivity(any(), any());
        log.info("권한 없음 취소 실패 테스트가 통과되었습니다.");
    }

    /**
     * 진행 중이지 않은 활동(이미 종료됨 등)을 취소하려 할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 취소 실패: 활동 상태가 IN_PROGRESS가 아닌 경우 예외가 발생한다.")
    void cancelActivity_Fail_InvalidStatus() {
        log.info("잘못된 상태로 인한 활동 취소 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityHistoryStatus(ActivityHistoryStatus.BEFORE)
                .build();

        log.info("활동 기록의 상태가 IN_PROGRESS가 아니라고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("취소 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.cancelActivity(userId, historyId)
        );

        // then
        log.info("발생한 예외 코드가 ALREADY_COMPLETED인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.ALREADY_COMPLETED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).cancelActivity(any(), any());
        log.info("잘못된 상태 취소 실패 테스트가 통과되었습니다.");
    }

    /**
     * 존재하지 않는 활동 기록 ID로 취소를 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 취소 실패: 활동 기록이 존재하지 않는 경우 예외가 발생한다.")
    void cancelActivity_Fail_NotFound() {
        log.info("존재하지 않는 기록으로 인한 활동 취소 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;

        log.info("해당 ID의 활동 기록이 없다고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        log.info("취소 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.cancelActivity(userId, historyId)
        );

        // then
        log.info("발생한 예외 코드가 HISTORY_NOT_FOUND인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).cancelActivity(any(), any());
        log.info("미발견 취소 실패 테스트가 통과되었습니다.");
    }

    /**
     * 활동 종료 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 성공: 진행 중인 활동을 완료하면 상태 변경 Mapper가 호출되고 결과를 반환한다.")
    void finishActivity_Success() {
        log.info("활동 종료 성공 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;
        LocalDateTime endTime = LocalDateTime.of(2025, 10, 1, 21, 30);

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityType(ActivityType.WALKING)
                .distance(BigDecimal.valueOf(5.235))
                .activityHistoryStatus(ActivityHistoryStatus.IN_PROGRESS)
                .activityHistoryStartAt(endTime.minusHours(1))
                .build();

        ActivityFinishRequest request = ActivityFinishRequest.builder()
                .activityHistoryStatus("COMPLETED")
                .activityHistoryEndAt(endTime)
                .build();

        log.info("활동 기록이 IN_PROGRESS 상태이며 소유자가 맞다고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("활동 종료 서비스 로직을 호출합니다.");
        ActivityHistoryResponse.ActivityFinishResponse response = activityHistoryService.finishActivity(userId, historyId, request);

        // then
        log.info("Mapper의 finishActivity가 호출되고 응답 값이 올바른지 검증합니다.");
        assertNotNull(response);
        assertEquals(historyId, response.getHistoryId());
        assertEquals(ActivityType.WALKING, response.getActivityType());
        assertEquals("COMPLETED", response.getActivityHistoryStatus());
        assertEquals(endTime, response.getActivityHistoryEndAt());

        verify(activityHistoryMapper, times(1)).finishActivity(
                historyId,
                ActivityHistoryStatus.COMPLETED.name(),
                endTime
        );
        log.info("활동 종료 성공 테스트가 통과되었습니다.");
    }

    /**
     * 권한이 없는 사용자가 활동 종료를 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 실패: 기록의 소유자가 아닌 경우 권한 예외가 발생한다.")
    void finishActivity_Fail_PermissionDenied() {
        log.info("권한 없음으로 인한 활동 종료 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Long historyId = 100L;

        User otherUser = User.builder().usersId(otherUserId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(otherUser)
                .activityHistoryStatus(ActivityHistoryStatus.IN_PROGRESS)
                .build();

        ActivityFinishRequest request = ActivityFinishRequest.builder()
                .activityHistoryStatus("COMPLETED")
                .activityHistoryEndAt(LocalDateTime.now())
                .build();

        log.info("활동 기록의 소유자가 요청자와 다르다고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("종료 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId, request)
        );

        // then
        log.info("발생한 예외 코드가 STOP_PERMISSION_DENIED인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.STOP_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).finishActivity(any(), any(), any());
        log.info("권한 없음 종료 실패 테스트가 통과되었습니다.");
    }

    /**
     * 이미 종료된 활동을 다시 종료하려 할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 실패: 활동 상태가 IN_PROGRESS가 아닌 경우 예외가 발생한다.")
    void finishActivity_Fail_InvalidStatus() {
        log.info("잘못된 상태로 인한 활동 종료 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityHistoryStatus(ActivityHistoryStatus.COMPLETED)
                .build();

        ActivityFinishRequest request = ActivityFinishRequest.builder()
                .activityHistoryStatus("COMPLETED")
                .activityHistoryEndAt(LocalDateTime.now())
                .build();

        log.info("활동 기록이 이미 완료(COMPLETED) 상태라고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        log.info("종료 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId, request)
        );

        // then
        log.info("발생한 예외 코드가 ALREADY_COMPLETED인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.ALREADY_COMPLETED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).finishActivity(any(), any(), any());
        log.info("잘못된 상태 종료 실패 테스트가 통과되었습니다.");
    }

    /**
     * 존재하지 않는 활동 기록 ID로 종료를 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 실패: 활동 기록이 존재하지 않는 경우 예외가 발생한다.")
    void finishActivity_Fail_NotFound() {
        log.info("존재하지 않는 기록으로 인한 활동 종료 실패 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;
        ActivityFinishRequest request = ActivityFinishRequest.builder().build();

        log.info("해당 ID의 활동 기록이 없다고 설정합니다.");
        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        log.info("종료 요청 시 예외가 발생하는지 확인합니다.");
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId, request)
        );

        // then
        log.info("발생한 예외 코드가 HISTORY_NOT_FOUND인지 검증합니다.");
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).finishActivity(any(), any(), any());
        log.info("미발견 종료 실패 테스트가 통과되었습니다.");
    }

    /**
     * 활동 삭제 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 삭제 성공: 본인의 활동 기록을 삭제하면 JPA delete가 호출되고 성공 메시지를 반환한다.")
    void deleteActivity_Success() {
        log.info("활동 삭제 성공 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;
        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryResponse.ActivitySimpleResponse response = activityHistoryService.deleteActivity(userId, historyId);

        // then
        assertNotNull(response);
        assertEquals("활동 기록이 성공적으로 삭제되었습니다.", response.getMessage());
        verify(activityHistoryRepository, times(1)).delete(activityHistory);
        log.info("활동 삭제 성공 테스트 통과");
    }

    /**
     * 권한 없는 사용자가 삭제 시도 시 예외 발생 테스트
     */
    @Test
    @DisplayName("활동 삭제 실패: 소유자가 아닌 경우 권한 예외가 발생한다.")
    void deleteActivity_Fail_PermissionDenied() {
        log.info("활동 삭제 실패(권한 없음) 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Long historyId = 100L;
        User otherUser = User.builder().usersId(otherUserId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(otherUser)
                .build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when & then
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.deleteActivity(userId, historyId)
        );

        assertEquals(ActivityHistoryErrorCode.DELETE_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).delete(any());
        log.info("활동 삭제 실패(권한 없음) 테스트 통과");
    }

    /**
     * 존재하지 않는 활동 기록 삭제 시도 시 예외 발생 테스트
     */
    @Test
    @DisplayName("활동 삭제 실패: 기록이 존재하지 않는 경우 예외가 발생한다.")
    void deleteActivity_Fail_NotFound() {
        log.info("활동 삭제 실패(미발견) 테스트를 시작합니다.");
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when & then
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.deleteActivity(userId, historyId)
        );

        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).delete(any());
        log.info("활동 삭제 실패(미발견) 테스트 통과");
    }
}