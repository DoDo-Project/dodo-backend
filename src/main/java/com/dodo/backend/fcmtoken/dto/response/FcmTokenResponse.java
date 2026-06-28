package com.dodo.backend.fcmtoken.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * FCM 토큰 API에서 사용하는 응답 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "FCM 토큰 응답 DTO 그룹")
public class FcmTokenResponse {

    /**
     * 단순 메시지 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "FCM 토큰 단순 응답")
    public static class FcmTokenSimpleResponse {
        private String message;

        public static FcmTokenSimpleResponse toDto(String message) {
            return FcmTokenSimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }
}
