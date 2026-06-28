package com.dodo.backend.notification.repository;

import com.dodo.backend.notification.entity.NotificationSchedule;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

    List<NotificationSchedule> findTop50ByScheduleStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            NotificationScheduleStatus scheduleStatus,
            LocalDateTime scheduledAt
    );
}
