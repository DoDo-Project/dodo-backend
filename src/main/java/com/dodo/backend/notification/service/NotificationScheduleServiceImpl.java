package com.dodo.backend.notification.service;

import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationScheduleCreateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleCreateResponse;
import com.dodo.backend.notification.entity.Notification;
import com.dodo.backend.notification.entity.NotificationSchedule;
import com.dodo.backend.notification.entity.NotificationScheduleRepeatType;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import com.dodo.backend.notification.entity.NotificationScheduleTargetType;
import com.dodo.backend.notification.exception.NotificationException;
import com.dodo.backend.notification.repository.NotificationRepository;
import com.dodo.backend.notification.repository.NotificationScheduleRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.dodo.backend.notification.exception.NotificationErrorCode.INVALID_REQUEST;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduleServiceImpl implements NotificationScheduleService {

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FcmNotificationSender fcmNotificationSender;

    @Transactional
    @Override
    public NotificationScheduleCreateResponse createSchedule(UUID adminId, NotificationScheduleCreateRequest request) {
        validateCreateRequest(adminId, request);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotificationException(INVALID_REQUEST));

        NotificationSchedule schedule = NotificationSchedule.builder()
                .createdBy(admin)
                .notificationTitle(request.getTitle())
                .notificationBody(request.getBody())
                .notificationType(request.getNotificationType())
                .targetType(request.getTargetType())
                .targetUserIds(toTargetUserIds(request))
                .repeatType(resolveRepeatType(request.getRepeatType()))
                .scheduleStatus(NotificationScheduleStatus.PENDING)
                .scheduledAt(request.getScheduledAt())
                .build();

        NotificationSchedule savedSchedule = notificationScheduleRepository.save(schedule);
        return NotificationScheduleCreateResponse.toDto(savedSchedule);
    }

    @Scheduled(fixedDelayString = "${notification.scheduler.fixed-delay:60000}")
    @Transactional
    public void executeDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        List<NotificationSchedule> dueSchedules =
                notificationScheduleRepository.findTop50ByScheduleStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        NotificationScheduleStatus.PENDING,
                        now
                );

        dueSchedules.forEach(schedule -> executeSchedule(schedule, now));
    }

    private void executeSchedule(NotificationSchedule schedule, LocalDateTime now) {
        List<User> targets = findTargets(schedule);
        if (!targets.isEmpty()) {
            notificationRepository.saveAll(targets.stream()
                    .map(user -> Notification.builder()
                            .user(user)
                            .notificationTitle(schedule.getNotificationTitle())
                            .notificationBody(schedule.getNotificationBody())
                            .notificationType(schedule.getNotificationType())
                            .relatedId(schedule.getNotificationScheduleId())
                            .isRead(false)
                            .build())
                    .toList());

            fcmNotificationSender.sendToUsers(
                    targets,
                    schedule.getNotificationTitle(),
                    schedule.getNotificationBody(),
                    schedule.getNotificationType(),
                    schedule.getNotificationScheduleId()
            );
        }

        updateScheduleAfterExecution(schedule, now);
    }

    private List<User> findTargets(NotificationSchedule schedule) {
        if (schedule.getTargetType() == NotificationScheduleTargetType.ALL) {
            return userRepository.findByUserStatusAndNotificationEnabledTrue(UserStatus.ACTIVE);
        }

        List<UUID> targetUserIds = parseTargetUserIds(schedule.getTargetUserIds());
        if (targetUserIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findByUsersIdInAndUserStatusAndNotificationEnabledTrue(targetUserIds, UserStatus.ACTIVE);
    }

    private void updateScheduleAfterExecution(NotificationSchedule schedule, LocalDateTime now) {
        if (schedule.getRepeatType() == NotificationScheduleRepeatType.DAILY) {
            schedule.reschedule(nextDailyScheduleAt(schedule.getScheduledAt(), now), now);
            return;
        }
        if (schedule.getRepeatType() == NotificationScheduleRepeatType.WEEKLY) {
            schedule.reschedule(nextWeeklyScheduleAt(schedule.getScheduledAt(), now), now);
            return;
        }
        schedule.complete(now);
    }

    private LocalDateTime nextDailyScheduleAt(LocalDateTime scheduledAt, LocalDateTime now) {
        LocalDateTime next = scheduledAt;
        do {
            next = next.plusDays(1);
        } while (!next.isAfter(now));
        return next;
    }

    private LocalDateTime nextWeeklyScheduleAt(LocalDateTime scheduledAt, LocalDateTime now) {
        LocalDateTime next = scheduledAt;
        do {
            next = next.plusWeeks(1);
        } while (!next.isAfter(now));
        return next;
    }

    private NotificationScheduleRepeatType resolveRepeatType(NotificationScheduleRepeatType repeatType) {
        return repeatType == null ? NotificationScheduleRepeatType.NONE : repeatType;
    }

    private void validateCreateRequest(UUID adminId, NotificationScheduleCreateRequest request) {
        if (adminId == null || request == null || request.getTargetType() == null) {
            throw new NotificationException(INVALID_REQUEST);
        }
        if (request.getTargetType() == NotificationScheduleTargetType.USERS
                && (request.getTargetUserIds() == null || request.getTargetUserIds().isEmpty())) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

    private String toTargetUserIds(NotificationScheduleCreateRequest request) {
        if (request.getTargetType() != NotificationScheduleTargetType.USERS) {
            return null;
        }
        return request.getTargetUserIds().stream()
                .distinct()
                .map(UUID::toString)
                .reduce((left, right) -> left + "," + right)
                .orElseThrow(() -> new NotificationException(INVALID_REQUEST));
    }

    private List<UUID> parseTargetUserIds(String targetUserIds) {
        if (targetUserIds == null || targetUserIds.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(targetUserIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(UUID::fromString)
                    .toList();
        } catch (IllegalArgumentException e) {
            log.warn("알림 스케줄 대상 사용자 ID 파싱 실패 - targetUserIds: {}", targetUserIds);
            return List.of();
        }
    }
}
