package com.dodo.backend.notification.service;

import com.dodo.backend.notification.entity.NotificationSchedule;
import com.dodo.backend.notification.entity.NotificationScheduleRepeatType;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import com.dodo.backend.notification.entity.NotificationScheduleTargetType;
import com.dodo.backend.notification.entity.NotificationType;
import com.dodo.backend.notification.repository.NotificationRepository;
import com.dodo.backend.notification.repository.NotificationScheduleRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationScheduleExecutorTest {

    @Mock
    private NotificationScheduleRepository notificationScheduleRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationScheduleExecutor notificationScheduleExecutor;

    @Test
    @DisplayName("예약 알림 선점 성공 - 토큰 기반 PROCESSING 상태로 변경")
    void claimDueSchedules_Success() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 28, 10, 0);
        NotificationSchedule schedule = createSchedule(1L, NotificationScheduleStatus.PROCESSING);

        given(notificationScheduleRepository.findDueScheduleIds(
                eq(NotificationScheduleStatus.PENDING),
                eq(now),
                any(Pageable.class)
        )).willReturn(List.of(1L));
        given(notificationScheduleRepository.claimDueSchedules(
                eq(List.of(1L)),
                eq(NotificationScheduleStatus.PENDING),
                eq(NotificationScheduleStatus.PROCESSING),
                anyString(),
                eq(now)
        )).willReturn(1);
        given(notificationScheduleRepository.findByProcessingTokenOrderByScheduledAtAsc(anyString()))
                .willReturn(List.of(schedule));

        List<ClaimedNotificationSchedule> claimedSchedules = notificationScheduleExecutor.claimDueSchedules(now);

        assertEquals(1, claimedSchedules.size());
        assertEquals(1L, claimedSchedules.get(0).scheduleId());
    }

    @Test
    @DisplayName("예약 알림 DB 처리 성공 - 알림 저장 후 스케줄 완료")
    void prepareDispatch_Success() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 28, 10, 0);
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .usersId(userId)
                .userStatus(UserStatus.ACTIVE)
                .notificationEnabled(true)
                .build();
        NotificationSchedule schedule = createSchedule(1L, NotificationScheduleStatus.PROCESSING);
        ClaimedNotificationSchedule claimedSchedule = new ClaimedNotificationSchedule(1L, "claim-token");

        given(notificationScheduleRepository.findByNotificationScheduleIdAndScheduleStatusAndProcessingToken(
                1L,
                NotificationScheduleStatus.PROCESSING,
                "claim-token"
        )).willReturn(Optional.of(schedule));
        given(userRepository.findByUserStatusAndNotificationEnabledTrue(UserStatus.ACTIVE)).willReturn(List.of(user));

        Optional<NotificationScheduleDispatch> dispatch = notificationScheduleExecutor.prepareDispatch(claimedSchedule, now);

        assertTrue(dispatch.isPresent());
        assertEquals(List.of(user), dispatch.get().targets());
        assertEquals(NotificationScheduleStatus.COMPLETED, schedule.getScheduleStatus());
        assertNull(schedule.getProcessingToken());
        assertNull(schedule.getProcessingStartedAt());
        verify(notificationRepository).saveAll(any(List.class));
    }

    private NotificationSchedule createSchedule(Long scheduleId, NotificationScheduleStatus scheduleStatus) {
        return NotificationSchedule.builder()
                .notificationScheduleId(scheduleId)
                .notificationTitle("공지 알림")
                .notificationBody("새로운 공지가 등록되었습니다.")
                .notificationType(NotificationType.SYSTEM)
                .targetType(NotificationScheduleTargetType.ALL)
                .repeatType(NotificationScheduleRepeatType.NONE)
                .scheduleStatus(scheduleStatus)
                .scheduledAt(LocalDateTime.of(2026, 6, 28, 10, 0))
                .processingToken("claim-token")
                .processingStartedAt(LocalDateTime.of(2026, 6, 28, 9, 59))
                .build();
    }
}
