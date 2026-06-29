package com.dodo.backend.notification.dto.request;

import com.dodo.backend.notification.entity.NotificationScheduleRepeatType;
import com.dodo.backend.notification.entity.NotificationScheduleTargetType;
import com.dodo.backend.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 알림 API에서 사용하는 요청 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "알림 요청 DTO 그룹")
public class NotificationRequest {

    /**
     * 알림 읽음 여부 변경 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 읽음 여부 변경 요청")
    public static class NotificationReadUpdateRequest {

        @NotNull(message = "알림 읽음 여부는 필수입니다.")
        @Schema(description = "변경할 읽음 여부", example = "true")
        private Boolean isRead;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 스케줄 등록 요청")
    public static class NotificationScheduleCreateRequest {

        @NotBlank(message = "title은 필수 값입니다.")
        @Size(max = 255, message = "title은 255자 이하로 입력해주세요.")
        @Schema(description = "알림 제목", example = "공지 알림")
        private String title;

        @NotBlank(message = "body는 필수 값입니다.")
        @Schema(description = "알림 내용", example = "새로운 공지가 등록되었습니다.")
        private String body;

        @NotNull(message = "notificationType은 필수 값입니다.")
        @Schema(description = "알림 유형", example = "SYSTEM")
        private NotificationType notificationType;

        @NotNull(message = "targetType은 필수 값입니다.")
        @Schema(description = "발송 대상 유형", example = "ALL")
        private NotificationScheduleTargetType targetType;

        @Schema(description = "targetType이 USERS일 때 발송 대상 사용자 ID 목록")
        private List<UUID> targetUserIds;

        @NotNull(message = "scheduledAt은 필수 값입니다.")
        @FutureOrPresent(message = "scheduledAt은 현재 또는 미래 시간이어야 합니다.")
        @Schema(description = "예약 발송 시간", example = "2026-06-27T14:30:00")
        private LocalDateTime scheduledAt;

        @Schema(description = "반복 유형. 미입력 시 NONE", example = "NONE")
        private NotificationScheduleRepeatType repeatType;
    }
}
