package com.dodo.backend.routepoint.socket.request;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.routepoint.entity.RoutePoint;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 웹소켓 통신(임베디드 -> 서버)에서 사용되는 요청 데이터(Payload)를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "웹소켓 요청 DTO 그룹")
public class WebSocketRequest {

    /**
     * 실시간 활동 경로 데이터(GPS, 심박수)를 전송할 때 사용하는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "실시간 경로 데이터 전송 요청")
    public static class RouteDataRequest {

        @Schema(description = "활동 기록 ID", example = "101")
        private Long historyId;

        @Schema(description = "위도", example = "37.5665")
        private BigDecimal latitude;

        @Schema(description = "경도", example = "126.9780")
        private BigDecimal longitude;

        @Schema(description = "심박수", example = "95")
        private Integer heartrate;

        @Schema(description = "측정 시간", example = "2026-02-09T12:00:00")
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime measuredAt;

        /**
         * 수신된 DTO 데이터를 기반으로 {@link RoutePoint} 엔티티를 생성합니다.
         *
         * @param activityHistory 현재 경로가 포함될 활동 기록 엔티티
         * @return DB에 저장 가능한 RoutePoint 엔티티
         */
        public RoutePoint toEntity(ActivityHistory activityHistory) {
            return RoutePoint.builder()
                    .activityHistory(activityHistory)
                    .latitude(this.latitude)
                    .longitude(this.longitude)
                    .routePointsMeasuredAt(this.measuredAt)
                    .build();
        }
    }
}