package com.dodo.backend.notification.service;

import com.dodo.backend.notification.entity.Notification;
import com.dodo.backend.notification.entity.NotificationSchedule;
import com.dodo.backend.notification.entity.NotificationScheduleRepeatType;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import com.dodo.backend.notification.entity.NotificationScheduleTargetType;
import com.dodo.backend.notification.repository.NotificationRepository;
import com.dodo.backend.notification.repository.NotificationScheduleRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduleExecutor {

    private static final int BATCH_SIZE = 50;

    private final NotificationScheduleRepository notificationScheduleRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int releaseExpiredClaims(LocalDateTime expiredBefore) {
        return notificationScheduleRepository.releaseExpiredClaims(
                expiredBefore,
                NotificationScheduleStatus.PENDING,
                NotificationScheduleStatus.PROCESSING
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedNotificationSchedule> claimDueSchedules(LocalDateTime now) {
        String processingToken = UUID.randomUUID().toString();
        List<Long> dueScheduleIds = notificationScheduleRepository.findDueScheduleIds(
                NotificationScheduleStatus.PENDING,
                now,
                PageRequest.of(0, BATCH_SIZE)
        );
        if (dueScheduleIds.isEmpty()) {
            return List.of();
        }

        int claimedCount = notificationScheduleRepository.claimDueSchedules(
                dueScheduleIds,
                NotificationScheduleStatus.PENDING,
                NotificationScheduleStatus.PROCESSING,
                processingToken,
                now
        );
        if (claimedCount == 0) {
            return List.of();
        }

        return notificationScheduleRepository.findByProcessingTokenOrderByScheduledAtAsc(processingToken).stream()
                .map(schedule -> new ClaimedNotificationSchedule(schedule.getNotificationScheduleId(), processingToken))
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<NotificationScheduleDispatch> prepareDispatch(ClaimedNotificationSchedule claimedSchedule, LocalDateTime now) {
        NotificationSchedule schedule = notificationScheduleRepository
                .findByNotificationScheduleIdAndScheduleStatusAndProcessingToken(
                        claimedSchedule.scheduleId(),
                        NotificationScheduleStatus.PROCESSING,
                        claimedSchedule.processingToken()
                )
                .orElse(null);
        if (schedule == null) {
            return Optional.empty();
        }

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
        }

        updateScheduleAfterExecution(schedule, now);
        if (targets.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new NotificationScheduleDispatch(
                targets,
                schedule.getNotificationTitle(),
                schedule.getNotificationBody(),
                schedule.getNotificationType(),
                schedule.getNotificationScheduleId()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseClaim(ClaimedNotificationSchedule claimedSchedule) {
        notificationScheduleRepository.releaseClaim(
                claimedSchedule.scheduleId(),
                claimedSchedule.processingToken(),
                NotificationScheduleStatus.PENDING,
                NotificationScheduleStatus.PROCESSING
        );
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
