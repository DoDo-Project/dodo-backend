package com.dodo.backend.routepoint.socket.response;

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
 * 웹소켓 통신 관련 응답 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "웹소켓 응답 DTO 그룹")
public class WebSocketResponse {

    /**
     * 성공 응답 시 페이로드 내부에 포함될 상세 위치 정보 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "실시간 위치 상세 데이터")
    public static class RouteDataDetailResponse {

        @Schema(description = "경로 지점 식별자", example = "123")
        private Long routePointId;

        @Schema(description = "위도", example = "37.5665")
        private BigDecimal latitude;

        @Schema(description = "경도", example = "126.9780")
        private BigDecimal longitude;

        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime measuredAt;

        /**
         * 상세 위치 정보 DTO를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param routePointId 경로 지점 식별자
         * @param latitude     위도
         * @param longitude    경도
         * @param measuredAt   측정 시간
         * @return 초기화된 {@link RouteDataDetailResponse} 객체
         */
        public static RouteDataDetailResponse toDto(Long routePointId, BigDecimal latitude, BigDecimal longitude, LocalDateTime measuredAt) {
            return RouteDataDetailResponse.builder()
                    .routePointId(routePointId)
                    .latitude(latitude)
                    .longitude(longitude)
                    .measuredAt(measuredAt)
                    .build();
        }
    }

    /**
     * 데이터 처리 성공 시 반환되는 응답 페이로드 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "실시간 데이터 송신 성공 응답")
    public static class RouteDataSuccessResponse {

        @Schema(description = "상태 코드", example = "200")
        private int code;

        @Schema(description = "응답 메시지", example = "새로운 정보 수신")
        private String message;

        @Schema(description = "현재 활동 상태", example = "IN_PROGRESS")
        private String status;

        @Schema(description = "상세 데이터")
        private RouteDataDetailResponse data;

        /**
         * 성공 응답 DTO를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param code    응답 코드
         * @param message 결과 메시지
         * @param status  활동 진행 상태
         * @param data    상세 위치 정보
         * @return 초기화된 {@link RouteDataSuccessResponse} 객체
         */
        public static RouteDataSuccessResponse toDto(int code, String message, String status, RouteDataDetailResponse data) {
            return RouteDataSuccessResponse.builder()
                    .code(code)
                    .message(message)
                    .status(status)
                    .data(data)
                    .build();
        }
    }

    /**
     * 데이터 처리 에러 발생 시 반환되는 응답 페이로드 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "실시간 데이터 송신 에러 응답")
    public static class RouteDataErrorResponse {

        @Schema(description = "에러 코드", example = "404")
        private int code;

        @Schema(description = "에러 메시지", example = "활동 기록을 찾을 수 없습니다.")
        private String errorMessage;

        @Schema(description = "원본 데이터")
        private Object originalMessage;

        public static RouteDataErrorResponse toDto(int code, String errorMessage, Object originalMessage) {
            return RouteDataErrorResponse.builder()
                    .code(code)
                    .errorMessage(errorMessage)
                    .originalMessage(originalMessage)
                    .build();
        }
    }
}