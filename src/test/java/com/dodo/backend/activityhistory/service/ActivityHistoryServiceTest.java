package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityFinishRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.ActivityHistorySummary;
import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.activityhistory.entity.ActivityType;
import com.dodo.backend.activityhistory.exception.ActivityHistoryErrorCode;
import com.dodo.backend.activityhistory.exception.ActivityHistoryException;
import com.dodo.backend.activityhistory.mapper.ActivityHistoryMapper;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private ImageFileService imageFileService;

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

        log.info("User: {}, Pet: {}, Request: {}", userId, petId, request);

        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(false);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)).willReturn(false);
        given(activityHistoryRepository.save(any(ActivityHistory.class))).willReturn(savedHistory);

        // when
        ActivityHistoryResponse.ActivityCreateResponse response = activityHistoryService.createActivity(userId, request);
        log.info("createActivity Result: {}", response);

        // then
        assertNotNull(response);
        assertEquals(100L, response.getHistoryId());
        assertEquals(ActivityType.WALKING.name(), response.getActivityType());

        verify(userService, times(1)).getUserById(userId);
        verify(petService, times(1)).getPetById(petId);
        verify(userPetService, times(1)).isApprovedPetOwner(userId, petId);
        verify(activityHistoryRepository, times(1)).save(any(ActivityHistory.class));
        log.info("Saved HistoryId: {}, Type: {}", response.getHistoryId(), response.getActivityType());
    }

    /**
     * 권한이 없는 사용자가 활동 기록 생성을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 실패: 펫의 소유자가 아닌 경우 권한 예외가 발생한다.")
    void createActivity_Fail_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType("WALKING")
                .build();

        log.info("User: {}, Pet: {} (Not Owner)", userId, petId);

        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.CREATE_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).save(any(ActivityHistory.class));
    }

    /**
     * 이미 진행 중(IN_PROGRESS)인 활동이 있을 때 생성 시도를 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 실패: 이미 진행 중인 활동이 있는 경우 예외가 발생한다.")
    void createActivity_Fail_AlreadyInProgress() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType("WALKING")
                .build();

        log.info("User: {}, Pet: {} (Already In Progress)", userId, petId);

        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(true);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.ALREADY_IN_PROGRESS, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).save(any(ActivityHistory.class));
    }

    /**
     * 이미 시작 대기 중(BEFORE)인 활동이 있을 때 생성 시도를 테스트합니다.
     */
    @Test
    @DisplayName("활동 기록 생성 실패: 이미 시작 대기 중인 활동이 있는 경우 예외가 발생한다.")
    void createActivity_Fail_AlreadyExistsBefore() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();

        ActivityCreateRequest request = ActivityCreateRequest.builder()
                .petId(petId)
                .activityType("WALKING")
                .build();

        log.info("User: {}, Pet: {} (Already Exists Before)", userId, petId);

        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(false);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)).willReturn(true);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.ALREADY_EXISTS_BEFORE, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).save(any(ActivityHistory.class));
    }

    /**
     * 활동 시작 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 시작 성공: 정상 요청 시 상태가 IN_PROGRESS로 변경되고 Mapper가 호출된다.")
    void startActivity_Success() {
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

        log.info("User: {}, HistoryId: {}, Status: BEFORE, Request: {}", userId, historyId, request);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        activityHistoryService.startActivity(userId, historyId, request);

        // then
        verify(activityHistoryMapper, times(1)).startActivity(
                historyId,
                ActivityHistoryStatus.IN_PROGRESS.name(),
                request.getStartLatitude(),
                request.getStartLongitude()
        );
        log.info("Mapper startActivity called with status: IN_PROGRESS");
    }

    /**
     * 권한이 없는 사용자가 활동 시작을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 시작 실패: 기록의 소유자가 아닌 경우 예외가 발생한다.")
    void startActivity_Fail_PermissionDenied() {
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

        log.info("User: {}, Owner: {}, HistoryId: {}", userId, otherUserId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.startActivity(userId, historyId, request)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.START_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).startActivity(any(), any(), any(), any());
    }

    /**
     * 이미 진행 중이거나 완료된 활동을 다시 시작하려 할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 시작 실패: 활동 상태가 BEFORE가 아닌 경우 예외가 발생한다.")
    void startActivity_Fail_InvalidStatus() {
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

        log.info("User: {}, HistoryId: {}, Status: IN_PROGRESS (Invalid)", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.startActivity(userId, historyId, request)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.ALREADY_IN_PROGRESS, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).startActivity(any(), any(), any(), any());
    }

    /**
     * 존재하지 않는 활동 기록 ID로 시작을 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 시작 실패: 활동 기록이 존재하지 않는 경우 예외가 발생한다.")
    void startActivity_Fail_NotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;
        ActivityHistoryRequest.ActivityStartRequest request = ActivityHistoryRequest.ActivityStartRequest.builder().build();

        log.info("User: {}, HistoryId: {} (Not Found)", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.startActivity(userId, historyId, request)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).startActivity(any(), any(), any(), any());
    }

    /**
     * 중단된 활동을 재개하는 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 재개 성공: 상태가 CANCELED일 때 요청 시 재개 로직(resumeActivity)이 실행된다.")
    void startActivity_Resume_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityHistoryStatus(ActivityHistoryStatus.CANCELED)
                .build();

        ActivityHistoryRequest.ActivityStartRequest request = ActivityHistoryRequest.ActivityStartRequest.builder()
                .startLatitude(BigDecimal.valueOf(37.5))
                .startLongitude(BigDecimal.valueOf(127.5))
                .build();

        log.info("User: {}, HistoryId: {}, Status: CANCELED", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryResponse.ActivitySimpleResponse response = activityHistoryService.startActivity(userId, historyId, request);
        log.info("resumeActivity Result: {}", response);

        // then
        verify(activityHistoryMapper, times(1)).resumeActivity(
                historyId,
                ActivityHistoryStatus.IN_PROGRESS.name()
        );
        verify(activityHistoryMapper, times(0)).startActivity(any(), any(), any(), any());
        log.info("Mapper resumeActivity called with status: IN_PROGRESS");
    }

    /**
     * 진행 중인 활동을 취소(중단)하는 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 취소 성공: 상태가 IN_PROGRESS일 때 요청 시 취소 로직(cancelActivity)이 실행된다.")
    void cancelActivity_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityHistoryStatus(ActivityHistoryStatus.IN_PROGRESS)
                .build();

        log.info("User: {}, HistoryId: {}, Status: IN_PROGRESS", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryResponse.ActivitySimpleResponse response = activityHistoryService.cancelActivity(userId, historyId);
        log.info("cancelActivity Result: {}", response.getMessage());

        // then
        assertEquals("활동 기록이 성공적으로 중단되었습니다.", response.getMessage());
        verify(activityHistoryMapper, times(1)).cancelActivity(
                historyId,
                ActivityHistoryStatus.CANCELED.name()
        );
        log.info("Mapper cancelActivity called with status: CANCELED");
    }

    /**
     * 권한이 없는 사용자가 활동 취소를 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 취소 실패: 기록의 소유자가 아닌 경우 예외가 발생한다.")
    void cancelActivity_Fail_PermissionDenied() {
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

        log.info("User: {}, Owner: {}, HistoryId: {}", userId, otherUserId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.cancelActivity(userId, historyId)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.STOP_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).cancelActivity(any(), any());
    }

    /**
     * 진행 중이지 않은 활동(이미 종료됨 등)을 취소하려 할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 취소 실패: 활동 상태가 IN_PROGRESS가 아닌 경우 예외가 발생한다.")
    void cancelActivity_Fail_InvalidStatus() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityHistoryStatus(ActivityHistoryStatus.BEFORE)
                .build();

        log.info("User: {}, HistoryId: {}, Status: BEFORE (Invalid)", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.cancelActivity(userId, historyId)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.ALREADY_COMPLETED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).cancelActivity(any(), any());
    }

    /**
     * 존재하지 않는 활동 기록 ID로 취소를 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 취소 실패: 활동 기록이 존재하지 않는 경우 예외가 발생한다.")
    void cancelActivity_Fail_NotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;

        log.info("User: {}, HistoryId: {} (Not Found)", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.cancelActivity(userId, historyId)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).cancelActivity(any(), any());
    }

    /**
     * 활동 종료 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 성공: 진행 중인 활동을 완료하면 상태 변경 Mapper가 호출되고 결과를 반환한다.")
    void finishActivity_Success() {
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

        log.info("User: {}, HistoryId: {}, EndTime: {}", userId, historyId, endTime);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryResponse.ActivityFinishResponse response = activityHistoryService.finishActivity(userId, historyId, request);
        log.info("finishActivity Result: Status={}, EndTime={}", response.getActivityHistoryStatus(), response.getActivityHistoryEndAt());

        // then
        assertNotNull(response);
        assertEquals(historyId, response.getHistoryId());
        assertEquals(ActivityType.WALKING.name(), response.getActivityType());
        assertEquals("COMPLETED", response.getActivityHistoryStatus());
        assertEquals(endTime, response.getActivityHistoryEndAt());

        verify(activityHistoryMapper, times(1)).finishActivity(
                historyId,
                ActivityHistoryStatus.COMPLETED.name(),
                endTime
        );
        log.info("Mapper finishActivity called with status: COMPLETED");
    }

    /**
     * 권한이 없는 사용자가 활동 종료를 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 실패: 기록의 소유자가 아닌 경우 권한 예외가 발생한다.")
    void finishActivity_Fail_PermissionDenied() {
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

        log.info("User: {}, Owner: {}, HistoryId: {}", userId, otherUserId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId, request)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.STOP_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).finishActivity(any(), any(), any());
    }

    /**
     * 이미 종료된 활동을 다시 종료하려 할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 실패: 활동 상태가 IN_PROGRESS가 아닌 경우 예외가 발생한다.")
    void finishActivity_Fail_InvalidStatus() {
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

        log.info("User: {}, HistoryId: {}, Status: COMPLETED (Invalid)", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId, request)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.ALREADY_COMPLETED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).finishActivity(any(), any(), any());
    }

    /**
     * 존재하지 않는 활동 기록 ID로 종료를 시도할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 실패: 활동 기록이 존재하지 않는 경우 예외가 발생한다.")
    void finishActivity_Fail_NotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;
        ActivityFinishRequest request = ActivityFinishRequest.builder().build();

        log.info("User: {}, HistoryId: {} (Not Found)", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId, request)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).finishActivity(any(), any(), any());
    }

    /**
     * 아직 시작되지 않은 활동(BEFORE)을 종료하려 할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 실패: 아직 시작하지 않은(BEFORE) 활동인 경우 예외가 발생한다.")
    void finishActivity_Fail_NotStarted() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(User.builder().usersId(userId).build())
                .activityHistoryStatus(ActivityHistoryStatus.BEFORE)
                .build();

        log.info("User: {}, HistoryId: {}, Status: BEFORE (Not Started)", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId, ActivityFinishRequest.builder().build())
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.ACTIVITY_NOT_STARTED, exception.getErrorCode());
    }

    /**
     * 활동 삭제 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 삭제 성공: 본인의 활동 기록을 삭제하면 JPA delete가 호출되고 성공 메시지를 반환한다.")
    void deleteActivity_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;
        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .build();

        log.info("User: {}, HistoryId: {}", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryResponse.ActivitySimpleResponse response = activityHistoryService.deleteActivity(userId, historyId);
        log.info("deleteActivity Result: {}", response.getMessage());

        // then
        assertNotNull(response);
        assertEquals("활동 기록이 성공적으로 삭제되었습니다.", response.getMessage());
        verify(activityHistoryRepository, times(1)).delete(activityHistory);
    }

    /**
     * 권한 없는 사용자가 삭제 시도 시 예외 발생 테스트
     */
    @Test
    @DisplayName("활동 삭제 실패: 소유자가 아닌 경우 권한 예외가 발생한다.")
    void deleteActivity_Fail_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Long historyId = 100L;
        User otherUser = User.builder().usersId(otherUserId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(otherUser)
                .build();

        log.info("User: {}, Owner: {}, HistoryId: {}", userId, otherUserId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.deleteActivity(userId, historyId)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.DELETE_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).delete(any());
    }

    /**
     * 존재하지 않는 활동 기록 삭제 시도 시 예외 발생 테스트
     */
    @Test
    @DisplayName("활동 삭제 실패: 기록이 존재하지 않는 경우 예외가 발생한다.")
    void deleteActivity_Fail_NotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;

        log.info("User: {}, HistoryId: {} (Not Found)", userId, historyId);

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.deleteActivity(userId, historyId)
        );
        log.info("Exception Code: {}", exception.getErrorCode());

        // then
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).delete(any());
    }

    /**
     * 내 활동 기록 조회 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("내 활동 기록 조회 성공: 페이징된 데이터와 펫 이미지 URL이 올바르게 매핑되어 반환된다.")
    void getMyActivityHistory_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).petName("보리").age(5).build();

        ActivityHistory history = ActivityHistory.builder()
                .historyId(100L)
                .user(user)
                .pet(pet)
                .activityType(ActivityType.WALKING)
                .activityHistoryStatus(ActivityHistoryStatus.COMPLETED)
                .distance(BigDecimal.valueOf(3.5))
                .activityHistoryStartAt(LocalDateTime.now().minusHours(1))
                .activityHistoryEndAt(LocalDateTime.now())
                .build();

        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Page<ActivityHistory> page = new org.springframework.data.domain.PageImpl<>(List.of(history), pageable, 1);
        Map<Long, String> imageMap = Map.of(petId, "http://example.com/bori.jpg");

        log.info("User: {}, Page: 0, Size: 10", userId);

        given(userService.getUserById(userId)).willReturn(user);
        given(activityHistoryRepository.findAllByUser(user, pageable)).willReturn(page);
        given(imageFileService.getProfileUrlsByPetIds(List.of(petId))).willReturn(imageMap);

        // when
        ActivityHistoryResponse.ActivityHistoryPageResponse response = activityHistoryService.getMyActivityHistory(userId, pageable);
        ActivityHistorySummary summary = response.getHistories().get(0);

        log.info("Result Count: {}, First Item Pet: {}, Image: {}",
                response.getTotalElements(), summary.getPet().getName(), summary.getPet().getProfileImageUrl());

        // then
        assertNotNull(response);
        assertEquals("활동 기록 목록을 성공적으로 조회했습니다.", response.getMessage());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());

        assertEquals(100L, summary.getHistoryId());
        assertEquals("WALKING", summary.getActivityType());
        assertEquals("COMPLETED", summary.getActivityHistoryStatus());
        assertEquals("보리", summary.getPet().getName());
        assertEquals("http://example.com/bori.jpg", summary.getPet().getProfileImageUrl());

        verify(activityHistoryRepository, times(1)).findAllByUser(user, pageable);
        verify(imageFileService, times(1)).getProfileUrlsByPetIds(any());
        log.info("getMyActivityHistory Success");
    }
}