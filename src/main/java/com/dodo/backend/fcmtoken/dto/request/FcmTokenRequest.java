package com.dodo.backend.fcmtoken.dto.request;

import com.dodo.backend.fcmtoken.entity.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 토큰 API에서 사용하는 요청 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "FCM 토큰 요청 DTO 그룹")
public class FcmTokenRequest {

    /**
     * FCM 토큰 등록 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "FCM 토큰 등록 요청")
    public static class FcmTokenRegisterRequest {

        @NotBlank(message = "푸시 토큰은 필수입니다.")
        @Size(max = 512, message = "푸시 토큰은 512자 이하여야 합니다.")
        @Schema(description = "FCM 푸시 토큰", example = "fcm_device_token_string")
        private String token;

        @NotNull(message = "장치 유형은 필수입니다.")
        @Schema(description = "장치 유형", example = "ANDROID")
        private DeviceType deviceType;

        @Size(max = 100, message = "장치 이름은 100자 이하여야 합니다.")
        @Schema(description = "장치 이름", example = "Galaxy Z Flip 5")
        private String deviceName;
    }
}
