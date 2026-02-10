package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
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
import com.dodo.backend.routepoint.service.RoutePointService;
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
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private RoutePointService routePointService;

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

        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(false);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)).willReturn(false);
        given(activityHistoryRepository.save(any(ActivityHistory.class))).willReturn(savedHistory);

        // when
        ActivityHistoryResponse.ActivityCreateResponse response = activityHistoryService.createActivity(userId, request);

        // then
        assertNotNull(response);
        assertEquals(100L, response.getHistoryId());
        assertEquals(ActivityType.WALKING.name(), response.getActivityType());

        verify(userService, times(1)).getUserById(userId);
        verify(petService, times(1)).getPetById(petId);
        verify(userPetService, times(1)).isApprovedPetOwner(userId, petId);
        verify(activityHistoryRepository, times(1)).save(any(ActivityHistory.class));
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

        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );

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

        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(true);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );

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

        given(userService.getUserById(userId)).willReturn(user);
        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)).willReturn(false);
        given(activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)).willReturn(true);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.createActivity(userId, request)
        );

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

        ActivityStartRequest request = ActivityStartRequest.builder()
                .startLatitude(BigDecimal.valueOf(37.1234))
                .startLongitude(BigDecimal.valueOf(127.1234))
                .build();

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

        ActivityStartRequest request = ActivityStartRequest.builder().build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.startActivity(userId, historyId, request)
        );

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

        ActivityStartRequest request = ActivityStartRequest.builder().build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.startActivity(userId, historyId, request)
        );

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
        ActivityStartRequest request = ActivityStartRequest.builder().build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.startActivity(userId, historyId, request)
        );

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

        ActivityStartRequest request = ActivityStartRequest.builder()
                .startLatitude(BigDecimal.valueOf(37.5))
                .startLongitude(BigDecimal.valueOf(127.5))
                .build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        activityHistoryService.startActivity(userId, historyId, request);

        // then
        verify(activityHistoryMapper, times(1)).resumeActivity(
                historyId,
                ActivityHistoryStatus.IN_PROGRESS.name()
        );
        verify(activityHistoryMapper, times(0)).startActivity(any(), any(), any(), any());
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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryResponse.ActivitySimpleResponse response = activityHistoryService.cancelActivity(userId, historyId);

        // then
        assertEquals("활동 기록이 성공적으로 중단되었습니다.", response.getMessage());
        verify(activityHistoryMapper, times(1)).cancelActivity(
                historyId,
                ActivityHistoryStatus.CANCELED.name()
        );
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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.cancelActivity(userId, historyId)
        );

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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.cancelActivity(userId, historyId)
        );

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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.cancelActivity(userId, historyId)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).cancelActivity(any(), any());
    }

    /**
     * 활동 종료 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 성공: 진행 중인 활동을 완료하면 거리 계산 후 상태 변경 Mapper가 호출되고 결과를 반환한다.")
    void finishActivity_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        BigDecimal calculatedDistance = BigDecimal.valueOf(5.235);

        User user = User.builder().usersId(userId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .activityType(ActivityType.WALKING)
                .distance(BigDecimal.ZERO)
                .activityHistoryStatus(ActivityHistoryStatus.IN_PROGRESS)
                .activityHistoryStartAt(startTime)
                .build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));
        given(routePointService.calculateTotalDistance(historyId)).willReturn(calculatedDistance);

        // when
        ActivityHistoryResponse.ActivityFinishResponse response = activityHistoryService.finishActivity(userId, historyId);

        // then
        assertNotNull(response);
        assertEquals(historyId, response.getHistoryId());
        assertNotNull(response.getActivityHistoryEndAt());
        assertEquals(calculatedDistance, response.getDistance());

        verify(activityHistoryMapper, times(1)).finishActivity(
                eq(historyId),
                eq("COMPLETED"),
                any(LocalDateTime.class),
                eq(calculatedDistance)
        );
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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.STOP_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).finishActivity(any(), any(), any(), any());
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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.ALREADY_COMPLETED, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).finishActivity(any(), any(), any(), any());
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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(activityHistoryMapper, times(0)).finishActivity(any(), any(), any(), any());
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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.finishActivity(userId, historyId)
        );

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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryResponse.ActivitySimpleResponse response = activityHistoryService.deleteActivity(userId, historyId);

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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.deleteActivity(userId, historyId)
        );

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

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.deleteActivity(userId, historyId)
        );

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

        given(userService.getUserById(userId)).willReturn(user);
        given(activityHistoryRepository.findAllByUser(user, pageable)).willReturn(page);
        given(imageFileService.getProfileUrlsByPetIds(List.of(petId))).willReturn(imageMap);

        // when
        ActivityHistoryResponse.ActivityHistoryPageResponse response = activityHistoryService.getMyActivityHistory(userId, pageable);
        ActivityHistorySummary summary = response.getHistories().get(0);

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
    }

    /**
     * 특정 활동 기록의 상세 정보를 성공적으로 조회하는 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 상세 조회 성공: 완료된 활동이며 권한이 있는 경우 상세 정보를 반환한다.")
    void getActivityHistoryDetail_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 101L;
        Long petId = 12L;

        Pet pet = Pet.builder().petId(petId).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .pet(pet)
                .activityType(ActivityType.WALKING)
                .activityHistoryStatus(ActivityHistoryStatus.COMPLETED)
                .distance(BigDecimal.valueOf(5.25))
                .activityHistoryStartAt(LocalDateTime.of(2025, 9, 30, 14, 0))
                .activityHistoryEndAt(LocalDateTime.of(2025, 9, 30, 14, 35))
                .startLatitude(BigDecimal.valueOf(37.4979))
                .startLongitude(BigDecimal.valueOf(127.0276))
                .build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(history));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);

        // when
        ActivityHistoryResponse.ActivityHistoryDetailResponse response = activityHistoryService.getActivityHistoryDetail(userId, historyId);

        // then
        assertNotNull(response);
        assertEquals("해당 활동 정보를 성공적으로 조회했습니다.", response.getMessage());
        assertEquals(historyId, response.getHistoryId());
        assertEquals(petId, response.getPetId());
        assertEquals(BigDecimal.valueOf(5.25), response.getDistance());
        assertEquals(0, response.getReactionCount());
        assertFalse(response.getIsLikedByMe());

        verify(activityHistoryRepository, times(1)).findById(historyId);
        verify(userPetService, times(1)).isApprovedPetOwner(userId, petId);
    }

    /**
     * 완료되지 않은 활동 기록 조회 시 실패하는 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 상세 조회 실패: 활동이 진행 중(IN_PROGRESS)인 경우 잘못된 요청 예외가 발생한다.")
    void getActivityHistoryDetail_Fail_NotCompleted() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 101L;
        Long petId = 12L;

        Pet pet = Pet.builder().petId(petId).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .pet(pet)
                .activityHistoryStatus(ActivityHistoryStatus.IN_PROGRESS)
                .build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(history));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.getActivityHistoryDetail(userId, historyId)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 권한이 없는 사용자가 활동 상세 정보를 조회하려 할 때 실패하는 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("활동 상세 조회 실패: 반려동물의 가족 구성원이 아닌 경우 권한 예외가 발생한다.")
    void getActivityHistoryDetail_Fail_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 101L;
        Long petId = 12L;

        Pet pet = Pet.builder().petId(petId).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .pet(pet)
                .build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(history));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.getActivityHistoryDetail(userId, historyId)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.VIEW_PERMISSION_DENIED, exception.getErrorCode());
    }

    /**
     * 반려동물의 활동 상태 조회 성공 시나리오를 테스트합니다. (진행 중인 활동이 있는 경우)
     */
    @Test
    @DisplayName("반려동물 활동 상태 조회 성공: 활동이 진행 중인 경우 기록 ID를 반환한다.")
    void getPetActivityStatus_Success_InProgress() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Long historyId = 100L;

        Pet pet = Pet.builder().petId(petId).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .activityHistoryStatus(ActivityHistoryStatus.IN_PROGRESS)
                .build();

        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.findFirstByPetOrderByHistoryIdDesc(pet)).willReturn(Optional.of(history));

        // when
        ActivityHistoryResponse.ActivityStatusResponse response = activityHistoryService.getPetActivityStatus(userId, petId);

        // then
        assertNotNull(response);
        assertEquals("활동 기록중인 애완동물입니다.", response.getMessage());
        assertEquals(historyId, response.getHistoryId());

        verify(petService, times(1)).getPetById(petId);
        verify(userPetService, times(1)).isApprovedPetOwner(userId, petId);
        verify(activityHistoryRepository, times(1)).findFirstByPetOrderByHistoryIdDesc(pet);
    }

    /**
     * 반려동물의 활동 상태 조회 성공 시나리오를 테스트합니다. (시작 전 또는 중단된 활동이 있는 경우)
     */
    @Test
    @DisplayName("반려동물 활동 상태 조회 성공: 활동이 시작 전인 경우 기록 ID를 반환한다.")
    void getPetActivityStatus_Success_Before() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Long historyId = 101L;

        Pet pet = Pet.builder().petId(petId).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(historyId)
                .activityHistoryStatus(ActivityHistoryStatus.BEFORE)
                .build();

        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.findFirstByPetOrderByHistoryIdDesc(pet)).willReturn(Optional.of(history));

        // when
        ActivityHistoryResponse.ActivityStatusResponse response = activityHistoryService.getPetActivityStatus(userId, petId);

        // then
        assertNotNull(response);
        assertEquals("활동 시작 전 상태입니다.", response.getMessage());
        assertEquals(historyId, response.getHistoryId());
    }

    /**
     * 반려동물의 활동 상태 조회 성공 시나리오를 테스트합니다. (진행 중인 활동이 없는 경우)
     */
    @Test
    @DisplayName("반려동물 활동 상태 조회 성공: 진행 중인 활동이 없는 경우 ID를 null로 반환한다.")
    void getPetActivityStatus_Success_NoCurrentActivity() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;

        Pet pet = Pet.builder().petId(petId).build();
        ActivityHistory history = ActivityHistory.builder()
                .historyId(99L)
                .activityHistoryStatus(ActivityHistoryStatus.COMPLETED)
                .build();

        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.findFirstByPetOrderByHistoryIdDesc(pet)).willReturn(Optional.of(history));

        // when
        ActivityHistoryResponse.ActivityStatusResponse response = activityHistoryService.getPetActivityStatus(userId, petId);

        // then
        assertNotNull(response);
        assertEquals("현재 진행 중인 활동이 없습니다.", response.getMessage());
        assertNull(response.getHistoryId());
    }

    /**
     * 반려동물의 활동 상태 조회 성공 시나리오를 테스트합니다. (기록이 아예 없는 경우)
     */
    @Test
    @DisplayName("반려동물 활동 상태 조회 성공: 활동 기록이 전혀 없는 경우 전용 메시지를 반환한다.")
    void getPetActivityStatus_Success_EmptyHistory() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Pet pet = Pet.builder().petId(petId).build();

        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(activityHistoryRepository.findFirstByPetOrderByHistoryIdDesc(pet)).willReturn(Optional.empty());

        // when
        ActivityHistoryResponse.ActivityStatusResponse response = activityHistoryService.getPetActivityStatus(userId, petId);

        // then
        assertNotNull(response);
        assertEquals("활동 기록이 없습니다.", response.getMessage());
        assertNull(response.getHistoryId());
    }

    /**
     * 반려동물의 활동 상태 조회 실패 시나리오를 테스트합니다. (권한 없음)
     */
    @Test
    @DisplayName("반려동물 활동 상태 조회 실패: 해당 반려동물의 가족이 아닌 경우 권한 예외가 발생한다.")
    void getPetActivityStatus_Fail_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 1L;
        Pet pet = Pet.builder().petId(petId).build();

        given(petService.getPetById(petId)).willReturn(pet);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.getPetActivityStatus(userId, petId)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.VIEW_PERMISSION_DENIED, exception.getErrorCode());
        verify(activityHistoryRepository, times(0)).findFirstByPetOrderByHistoryIdDesc(any());
    }

    /**
     * 특정 활동 기록의 상세 경로 조회 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("상세 경로 조회 성공: 권한이 있는 사용자가 요청 시 경로 데이터를 DTO로 변환하여 반환한다.")
    void getActivityRoute_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;
        Long petId = 1L;

        User user = User.builder().usersId(userId).build();
        Pet pet = Pet.builder().petId(petId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .user(user)
                .pet(pet)
                .activityType(ActivityType.WALKING)
                .activityHistoryStatus(ActivityHistoryStatus.COMPLETED)
                .distance(BigDecimal.valueOf(3.5))
                .build();

        // Mock RoutePoint data (Map List)
        List<Map<String, Object>> routePointMaps = List.of(
                Map.of(
                        "routePointId", 1L,
                        "latitude", BigDecimal.valueOf(37.1),
                        "longitude", BigDecimal.valueOf(127.1),
                        "measuredAt", LocalDateTime.now()
                ),
                Map.of(
                        "routePointId", 2L,
                        "latitude", BigDecimal.valueOf(37.2),
                        "longitude", BigDecimal.valueOf(127.2),
                        "measuredAt", LocalDateTime.now().plusMinutes(1)
                )
        );

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(routePointService.getRoutePoints(historyId)).willReturn(routePointMaps);

        // when
        ActivityHistoryResponse.ActivityRouteResponse response = activityHistoryService.getActivityRoute(userId, historyId);

        // then
        assertNotNull(response);
        assertEquals(historyId, response.getHistoryId());
        assertEquals("상세 경로를 가져오는데 성공했습니다.", response.getMessage());
        assertEquals(2, response.getRoutePoints().size());

        // 첫 번째 좌표 검증
        assertEquals(1L, response.getRoutePoints().get(0).getRoutePointId());
        assertEquals(BigDecimal.valueOf(37.1), response.getRoutePoints().get(0).getLatitude());

        verify(activityHistoryRepository, times(1)).findById(historyId);
        verify(userPetService, times(1)).isApprovedPetOwner(userId, petId);
        verify(routePointService, times(1)).getRoutePoints(historyId);
    }

    /**
     * 존재하지 않는 활동 기록의 상세 경로 조회 시 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("상세 경로 조회 실패: 활동 기록이 존재하지 않는 경우 예외가 발생한다.")
    void getActivityRoute_Fail_NotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 999L;

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.getActivityRoute(userId, historyId)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.HISTORY_NOT_FOUND, exception.getErrorCode());
        verify(routePointService, times(0)).getRoutePoints(any());
    }

    /**
     * 권한이 없는 사용자가 상세 경로를 조회하려 할 때 예외 발생을 테스트합니다.
     */
    @Test
    @DisplayName("상세 경로 조회 실패: 반려동물의 보호자가 아닌 경우 권한 예외가 발생한다.")
    void getActivityRoute_Fail_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long historyId = 100L;
        Long petId = 1L;

        Pet pet = Pet.builder().petId(petId).build();
        ActivityHistory activityHistory = ActivityHistory.builder()
                .historyId(historyId)
                .pet(pet)
                .build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(activityHistory));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        ActivityHistoryException exception = assertThrows(ActivityHistoryException.class, () ->
                activityHistoryService.getActivityRoute(userId, historyId)
        );

        // then
        assertEquals(ActivityHistoryErrorCode.VIEW_PERMISSION_DENIED, exception.getErrorCode());
        verify(routePointService, times(0)).getRoutePoints(any());
    }
}