package com.dodo.backend.notification.service;

import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationReadUpdateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.UnreadNotificationCountResponse;
import com.dodo.backend.notification.entity.Notification;
import com.dodo.backend.notification.entity.NotificationType;
import com.dodo.backend.notification.repository.NotificationRepository;
import com.dodo.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 알림 서비스 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    /**
     * 알림 목록 조회 시 페이지 정보와 알림 데이터가 반환되는지 검증합니다.
     */
    @Test
    @DisplayName("알림 목록 조회 성공")
    void getNotifications_Success() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        Notification notification = createNotification(1L, user, false, NotificationType.COMMENT);

        given(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(notification)));

        NotificationListResponse response = notificationService.getNotifications(userId, 0, 20, null, null);

        assertEquals(0, response.getPageInfo().getPage());
        assertEquals(1, response.getData().size());
        assertEquals(NotificationType.COMMENT, response.getData().get(0).getNotificationType());
    }

    /**
     * 알림 유형 필터가 소문자나 혼합 대소문자로 전달되어도 조회가 실패하지 않는지 검증합니다.
     */
    @Test
    @DisplayName("알림 목록 조회 성공 - 유형 필터 대소문자 완화")
    void getNotifications_CaseInsensitiveTypeFilter() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        Notification notification = createNotification(1L, user, false, NotificationType.COMMENT);

        given(notificationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(notification)));

        NotificationListResponse response = notificationService.getNotifications(userId, 0, 20, null, "comment,Board");

        assertEquals(1, response.getData().size());
        assertEquals(NotificationType.COMMENT, response.getData().get(0).getNotificationType());
    }

    /**
     * 알림 읽음 처리 시 엔티티의 읽음 상태가 변경되는지 검증합니다.
     */
    @Test
    @DisplayName("알림 읽음 처리 성공")
    void updateReadStatus_Success() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        Notification notification = createNotification(1L, user, false, NotificationType.BOARD);

        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        notificationService.updateReadStatus(userId, 1L, new NotificationReadUpdateRequest(true));

        assertTrue(notification.getIsRead());
    }

    /**
     * 읽지 않은 알림 개수 조회 결과를 검증합니다.
     */
    @Test
    @DisplayName("읽지 않은 알림 개수 조회 성공")
    void getUnreadCount_Success() {
        UUID userId = UUID.randomUUID();
        given(notificationRepository.countByUserUsersIdAndIsReadFalse(userId)).willReturn(3L);

        UnreadNotificationCountResponse response = notificationService.getUnreadCount(userId);

        assertEquals(3L, response.getUnreadCount());
    }

    /**
     * 모든 알림 삭제 시 Repository 삭제 메서드가 호출되는지 검증합니다.
     */
    @Test
    @DisplayName("모든 알림 삭제 성공")
    void deleteAll_Success() {
        UUID userId = UUID.randomUUID();
        notificationService.deleteAll(userId);

        verify(notificationRepository).deleteAllByUserUsersId(userId);
    }

    /**
     * 모든 알림 읽음 처리 시 읽지 않은 알림이 읽음 상태로 변경되는지 검증합니다.
     */
    @Test
    @DisplayName("모든 알림 읽음 처리 성공")
    void readAll_Success() {
        UUID userId = UUID.randomUUID();
        notificationService.readAll(userId);

        verify(notificationRepository).markAllAsRead(userId);
    }

    private User createUser(UUID userId) {
        return User.builder()
                .usersId(userId)
                .nickname("테스터")
                .build();
    }

    private Notification createNotification(Long notificationId, User user, boolean isRead, NotificationType type) {
        return Notification.builder()
                .notificationId(notificationId)
                .user(user)
                .notificationTitle("알림 제목")
                .notificationBody("알림 내용")
                .notificationType(type)
                .relatedId(10L)
                .isRead(isRead)
                .notificationCreatedAt(LocalDateTime.of(2025, 10, 6, 12, 0))
                .build();
    }
}
