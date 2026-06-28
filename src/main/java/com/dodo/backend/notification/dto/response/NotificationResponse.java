package com.dodo.backend.notification.dto.response;

import com.dodo.backend.notification.entity.Notification;
import com.dodo.backend.notification.entity.NotificationSchedule;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import com.dodo.backend.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 API에서 사용하는 응답 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "알림 응답 DTO 그룹")
public class NotificationResponse {

    /**
     * 단순 메시지 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 단순 응답")
    public static class NotificationSimpleResponse {
        private String message;

        public static NotificationSimpleResponse toDto(String message) {
            return NotificationSimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 페이지 정보 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "페이지 정보")
    public static class PageInfoResponse {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public static PageInfoResponse toDto(Page<?> page, int displayPage) {
            return PageInfoResponse.builder()
                    .page(displayPage)
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .build();
        }
    }

    /**
     * 알림 목록 아이템 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 목록 아이템")
    public static class NotificationItemResponse {
        private Long notificationId;
        private String notificationTitle;
        private String notificationBody;
        private NotificationType notificationType;
        private Long relatedId;
        private Boolean isRead;
        private LocalDateTime createdAt;

        public static NotificationItemResponse toDto(Notification notification) {
            return NotificationItemResponse.builder()
                    .notificationId(notification.getNotificationId())
                    .notificationTitle(notification.getNotificationTitle())
                    .notificationBody(notification.getNotificationBody())
                    .notificationType(notification.getNotificationType())
                    .relatedId(notification.getRelatedId())
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getNotificationCreatedAt())
                    .build();
        }
    }

    /**
     * 알림 목록 조회 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 목록 조회 응답")
    public static class NotificationListResponse {
        private PageInfoResponse pageInfo;
        private List<NotificationItemResponse> data;
    }

    /**
     * 읽지 않은 알림 개수 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "읽지 않은 알림 개수 응답")
    public static class UnreadNotificationCountResponse {
        private long unreadCount;

        public static UnreadNotificationCountResponse toDto(long unreadCount) {
            return UnreadNotificationCountResponse.builder()
                    .unreadCount(unreadCount)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "알림 스케줄 등록 응답")
    public static class NotificationScheduleCreateResponse {
        private String message;
        private Long scheduleId;
        private NotificationScheduleStatus scheduleStatus;
        private LocalDateTime scheduledAt;

        public static NotificationScheduleCreateResponse toDto(NotificationSchedule schedule) {
            return NotificationScheduleCreateResponse.builder()
                    .message("알림 스케줄이 성공적으로 등록되었습니다.")
                    .scheduleId(schedule.getNotificationScheduleId())
                    .scheduleStatus(schedule.getScheduleStatus())
                    .scheduledAt(schedule.getScheduledAt())
                    .build();
        }
    }
}
