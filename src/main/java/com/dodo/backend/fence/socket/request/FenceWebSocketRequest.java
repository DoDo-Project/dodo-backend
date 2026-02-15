package com.dodo.backend.fence.socket.request;

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
 * 울타리 웹소켓 요청 DTO 그룹입니다.
 */
@Schema(description = "울타리 웹소켓 요청 DTO 그룹")
public class FenceWebSocketRequest {

    /**
     * 실시간 위치 전송 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "실시간 위치 전송 요청")
    public static class FenceLocationRequest {

        @Schema(description = "위도", example = "37.5665")
        private BigDecimal latitude;

        @Schema(description = "경도", example = "126.9780")
        private BigDecimal longitude;

        @Schema(description = "측정 시각", example = "2025-10-02T10:55:00")
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime measuredAt;
    }
}
