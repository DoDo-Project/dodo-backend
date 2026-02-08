package com.dodo.backend.activityhistory.dto.response;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 활동기록 도메인과 관련된 응답 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "활동기록 관련 응답 DTO 그룹")
public class ActivityHistoryResponse {

    /**
     * 활동 기록 생성 성공 시 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 기록 생성 성공 응답")
    public static class ActivityCreateResponse {

        @Schema(description = "응답 메시지", example = "회원가입이 완료되었습니다.")
        private String message;

        @Schema(description = "생성된 활동 기록 ID", example = "1234")
        private Long historyId;

        @Schema(description = "활동 유형", example = "WALKING")
        private ActivityType activityType;

        /**
         * {@link ActivityHistory} 엔티티를 생성 응답 DTO로 변환합니다.
         *
         * @param activityHistory 변환할 활동 기록 엔티티
         * @return 변환된 ActivityCreateResponse 객체
         */
        public static ActivityCreateResponse toDto(ActivityHistory activityHistory, String message) {
            return ActivityCreateResponse.builder()
                    .message(message)
                    .historyId(activityHistory.getHistoryId())
                    .activityType(activityHistory.getActivityType())
                    .build();
        }
    }

    /**
     * 데이터 없이 성공 메시지만 반환할 때 사용하는 간단한 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 관련 작업 성공 응답")
    public static class ActivitySimpleResponse {

        @Schema(description = "응답 메시지", example = "활동 기록이 시작(재개)되었습니다")
        private String message;

        /**
         * 전달받은 메시지를 포함하는 단순 응답 DTO를 생성합니다.
         *
         * @param message 클라이언트에게 전달할 처리 결과 메시지
         * @return 메시지가 설정된 {@link ActivitySimpleResponse} 객체
         */
        public static ActivitySimpleResponse toDto(String message) {
            return ActivitySimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 활동 종료 성공 시 반환되는 상세 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 기록 종료 성공 응답")
    public static class ActivityFinishResponse {

        @Schema(description = "응답 메시지", example = "활동 기록이 성공적으로 종료되었습니다.")
        private String message;

        @Schema(description = "활동 기록 ID", example = "101")
        private Long historyId;

        @Schema(description = "활동 유형", example = "WALKING")
        private ActivityType activityType;

        @Schema(description = "이동 거리(km)", example = "5.235")
        private BigDecimal distance;

        @Schema(description = "활동 시작 시간", example = "2025-10-01T20:30:00")
        private LocalDateTime activityHistoryStartAt;

        @Schema(description = "활동 종료 시간", example = "2025-10-01T21:30:00")
        private LocalDateTime activityHistoryEndAt;

        @Schema(description = "활동 상태", example = "COMPLETED")
        private String activityHistoryStatus;

        /**
         * 활동 종료 정보를 기반으로 클라이언트에게 전달할 응답 DTO를 생성합니다.
         * <p>
         * 엔티티 객체 전체를 노출하지 않고, 필요한 필드 값들만 개별적으로 전달받아 객체를 구성합니다.
         * </p>
         *
         * @param historyId             활동 기록의 고유 ID
         * @param activityType          활동 유형 (예: WALKING, SLEEPING)
         * @param distance              총 이동 거리 (km)
         * @param activityHistoryStartAt 활동 시작 시간
         * @param activityHistoryEndAt   활동 종료 시간
         * @param activityHistoryStatus  최종 변경된 활동 상태 (문자열)
         * @return 생성된 {@link ActivityFinishResponse} 객체
         */
        public static ActivityFinishResponse toDto(Long historyId,
                                                   ActivityType activityType,
                                                   BigDecimal distance,
                                                   LocalDateTime activityHistoryStartAt,
                                                   LocalDateTime activityHistoryEndAt,
                                                   String activityHistoryStatus,
                                                   String message) {
            return ActivityFinishResponse.builder()
                    .message(message)
                    .historyId(historyId)
                    .activityType(activityType)
                    .distance(distance)
                    .activityHistoryStartAt(activityHistoryStartAt)
                    .activityHistoryEndAt(activityHistoryEndAt)
                    .activityHistoryStatus(activityHistoryStatus)
                    .build();
        }
    }
}