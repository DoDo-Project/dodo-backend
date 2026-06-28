package com.dodo.backend.notification.repository;

import com.dodo.backend.notification.entity.Notification;
import com.dodo.backend.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

/**
 * 알림 엔티티의 영속성 처리를 담당하는 Repository입니다.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserUsersIdOrderByNotificationCreatedAtDescNotificationIdDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserUsersIdAndIsReadOrderByNotificationCreatedAtDescNotificationIdDesc(
            UUID userId,
            Boolean isRead,
            Pageable pageable
    );

    Page<Notification> findByUserUsersIdAndNotificationTypeInOrderByNotificationCreatedAtDescNotificationIdDesc(
            UUID userId,
            Collection<NotificationType> types,
            Pageable pageable
    );

    Page<Notification> findByUserUsersIdAndIsReadAndNotificationTypeInOrderByNotificationCreatedAtDescNotificationIdDesc(
            UUID userId,
            Boolean isRead,
            Collection<NotificationType> types,
            Pageable pageable
    );

    long countByUserUsersIdAndIsReadFalse(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user.usersId = :userId and n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId);

    void deleteAllByUserUsersId(UUID userId);
}
