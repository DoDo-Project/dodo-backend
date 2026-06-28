package com.dodo.backend.notification.service;

import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationReadUpdateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationItemResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.PageInfoResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.UnreadNotificationCountResponse;
import com.dodo.backend.notification.entity.Notification;
import com.dodo.backend.notification.entity.NotificationType;
import com.dodo.backend.notification.exception.NotificationErrorCode;
import com.dodo.backend.notification.exception.NotificationException;
import com.dodo.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.dodo.backend.notification.exception.NotificationErrorCode.INVALID_REQUEST;
import static com.dodo.backend.notification.exception.NotificationErrorCode.NOTIFICATION_DELETE_FORBIDDEN;
import static com.dodo.backend.notification.exception.NotificationErrorCode.NOTIFICATION_NOT_FOUND;
import static com.dodo.backend.notification.exception.NotificationErrorCode.NOTIFICATION_UPDATE_FORBIDDEN;

/**
 * {@link NotificationService} 구현체입니다.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String READ_SUCCESS_MESSAGE = "알림이 성공적으로 읽음 처리되었습니다.";
    private static final String READ_ALL_SUCCESS_MESSAGE = "모든 알림이 성공적으로 읽음 처리되었습니다.";

    private final NotificationRepository notificationRepository;

    /**
     * 알림 목록을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @param page 조회할 페이지 번호
     * @param size 페이지당 알림 수
     * @param isRead 읽음 여부 필터
     * @param type 알림 유형 필터
     * @return 알림 목록 조회 결과
     */
    @Transactional(readOnly = true)
    @Override
    public NotificationListResponse getNotifications(UUID userId, int page, int size, Boolean isRead, String type) {
        validatePageRequest(userId, page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        List<NotificationType> types = parseTypes(type);

        Page<Notification> notifications = findNotifications(userId, isRead, types, pageable);
        return NotificationListResponse.builder()
                .pageInfo(PageInfoResponse.toDto(notifications, page))
                .data(notifications.getContent().stream().map(NotificationItemResponse::toDto).toList())
                .build();
    }

    /**
     * 특정 알림의 읽음 여부를 변경합니다.
     *
     * @param userId 요청 사용자 ID
     * @param notificationId 읽음 여부를 변경할 알림 ID
     * @param request 읽음 여부 변경 요청
     * @return 읽음 처리 성공 메시지
     */
    @Transactional
    @Override
    public NotificationSimpleResponse updateReadStatus(UUID userId, Long notificationId, NotificationReadUpdateRequest request) {
        if (request == null || request.getIsRead() == null) {
            throw new NotificationException(INVALID_REQUEST);
        }
        Notification notification = findOwnedNotification(userId, notificationId, NOTIFICATION_UPDATE_FORBIDDEN);
        notification.updateReadStatus(request.getIsRead());
        return NotificationSimpleResponse.toDto(READ_SUCCESS_MESSAGE);
    }

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param userId 요청 사용자 ID
     * @param notificationId 삭제할 알림 ID
     */
    @Transactional
    @Override
    public void deleteNotification(UUID userId, Long notificationId) {
        Notification notification = findOwnedNotification(userId, notificationId, NOTIFICATION_DELETE_FORBIDDEN);
        notificationRepository.delete(notification);
    }

    /**
     * 읽지 않은 알림 개수를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    @Override
    public UnreadNotificationCountResponse getUnreadCount(UUID userId) {
        validateUserId(userId);
        return UnreadNotificationCountResponse.toDto(notificationRepository.countByUserUsersIdAndIsReadFalse(userId));
    }

    /**
     * 모든 알림을 읽음 처리합니다.
     *
     * @param userId 요청 사용자 ID
     * @return 전체 읽음 처리 성공 메시지
     */
    @Transactional
    @Override
    public NotificationSimpleResponse readAll(UUID userId) {
        validateUserId(userId);
        notificationRepository.markAllAsRead(userId);
        return NotificationSimpleResponse.toDto(READ_ALL_SUCCESS_MESSAGE);
    }

    /**
     * 모든 알림을 삭제합니다.
     *
     * @param userId 요청 사용자 ID
     */
    @Transactional
    @Override
    public void deleteAll(UUID userId) {
        validateUserId(userId);
        notificationRepository.deleteAllByUserUsersId(userId);
    }

    private Page<Notification> findNotifications(UUID userId, Boolean isRead, List<NotificationType> types, Pageable pageable) {
        if (isRead != null && !types.isEmpty()) {
            return notificationRepository.findByUserUsersIdAndIsReadAndNotificationTypeInOrderByNotificationCreatedAtDescNotificationIdDesc(
                    userId,
                    isRead,
                    types,
                    pageable
            );
        }
        if (isRead != null) {
            return notificationRepository.findByUserUsersIdAndIsReadOrderByNotificationCreatedAtDescNotificationIdDesc(userId, isRead, pageable);
        }
        if (!types.isEmpty()) {
            return notificationRepository.findByUserUsersIdAndNotificationTypeInOrderByNotificationCreatedAtDescNotificationIdDesc(userId, types, pageable);
        }
        return notificationRepository.findByUserUsersIdOrderByNotificationCreatedAtDescNotificationIdDesc(userId, pageable);
    }

    private Notification findOwnedNotification(UUID userId, Long notificationId, NotificationErrorCode forbiddenErrorCode) {
        validateUserId(userId);
        validateId(notificationId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NOTIFICATION_NOT_FOUND));
        if (notification.getUser() == null || !userId.equals(notification.getUser().getUsersId())) {
            throw new NotificationException(forbiddenErrorCode);
        }
        return notification;
    }

    private List<NotificationType> parseTypes(String type) {
        if (type == null || type.isBlank()) {
            return List.of();
        }
        try {
            List<NotificationType> types = Arrays.stream(type.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(NotificationType::valueOf)
                    .distinct()
                    .toList();

            if (types.isEmpty()) {
                throw new NotificationException(INVALID_REQUEST);
            }
            return types;
        } catch (IllegalArgumentException e) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

    private void validatePageRequest(UUID userId, int page, int size) {
        if (userId == null || page <= 0 || size <= 0 || size > MAX_PAGE_SIZE) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new NotificationException(INVALID_REQUEST);
        }
    }
}
