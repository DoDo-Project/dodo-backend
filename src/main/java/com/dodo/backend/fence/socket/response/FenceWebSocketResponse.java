package com.dodo.backend.fence.socket.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 울타리 웹소켓 응답 DTO 그룹입니다.
 */
@Schema(description = "울타리 웹소켓 응답 DTO 그룹")
public class FenceWebSocketResponse {

    /**
     * 브로드캐스트용 실시간 위치 데이터 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "브로드캐스트 실시간 위치 데이터")
    public static class FenceLocationBroadcastResponse {

        @Schema(description = "반려동물 ID", example = "1")
        private Long petId;

        @Schema(description = "위도", example = "37.5665")
        private BigDecimal latitude;

        @Schema(description = "경도", example = "126.9780")
        private BigDecimal longitude;

        @Schema(description = "측정 시각", example = "2025-10-02T10:55:00")
        private LocalDateTime measuredAt;

        @Schema(description = "울타리 내부 여부", example = "true")
        private Boolean insideFence;

        @Schema(description = "중심점으로부터의 거리(미터)", example = "125.42")
        private BigDecimal distanceMeter;

        @Schema(description = "설정된 울타리 반경(미터)", example = "500")
        private Integer radius;

        @Schema(description = "울타리 판정 메시지", example = "설정 반경 500m 이내입니다.")
        private String message;
    }

    /**
     * 개인 응답 성공 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "실시간 위치 수신 성공 응답")
    public static class FenceLocationSuccessResponse {

        @Schema(description = "응답 코드", example = "200")
        private int code;

        @Schema(description = "응답 메시지", example = "위치 데이터 수신 및 처리 완료")
        private String message;
    }

    /**
     * 개인 응답 에러 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "실시간 위치 수신 실패 응답")
    public static class FenceLocationErrorResponse {

        @Schema(description = "에러 코드", example = "404")
        private int code;

        @Schema(description = "에러 메시지", example = "애완동물을 찾을 수 없습니다.")
        private String message;

        @Schema(description = "원본 메시지")
        private Object originalMessage;
    }
}
