package com.dodo.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증(Auth) 도메인 관련 요청 데이터를 그룹화하여 관리하는 클래스입니다.
 * <p>
 * 소셜 로그인 인가 코드 전달, 토큰 재발급 요청 등 인증 프로세스 전반에서 클라이언트가 서버로 전송하는 데이터를 정의합니다.
 */
@Schema(description = "인증 관련 요청 DTO 그룹")
public class AuthRequest {

    /**
     * 소셜 로그인 인증을 위해 클라이언트가 전달하는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "소셜 로그인 및 토큰 발급 요청")
    public static class SocialLoginRequest {

        @Schema(description = "소셜 제공자 타입 (GOOGLE, NAVER)", example = "GOOGLE")
        @NotBlank(message = "provider는 필수 값입니다.")
        private String provider;

        @Schema(description = "OAuth 인가 코드 (Authorization Code)", example = "4/0AdQtV5...")
        @NotBlank(message = "code는 필수 값입니다.")
        private String code;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "관리자 전용 로그인 요청")
    public static class AdminLoginRequest {

        @Schema(description = "관리자 이메일", example = "admin@dodo.com")
        @NotBlank(message = "email은 필수 값입니다.")
        private String email;

        @Schema(description = "관리자 로그인 비밀번호", example = "admin-password")
        @NotBlank(message = "password는 필수 값입니다.")
        private String password;
    }

    /**
     * 로그아웃 요청 시 리프레시 토큰을 전달받는 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "로그아웃 요청")
    public static class LogoutRequest {

        @Schema(description = "삭제할 리프레시 토큰", example = "def50200f29184b294277418292...")
        @NotBlank(message = "리프레시 토큰은 필수 값입니다.")
        private String refreshToken;
    }

    /**
     * 액세스 토큰 재발급을 위한 요청 DTO입니다.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "토큰 재발급 요청")
    public static class ReissueRequest {

        @Schema(description = "만료된 액세스 토큰을 갱신하기 위한 리프레시 토큰", example = "abc12300f29184b...")
        @NotBlank(message = "리프레시 토큰은 필수 값입니다.")
        private String refreshToken;
    }

    /**
     * 장치 인증(로그인) 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "장치 인증 요청")
    public static class DeviceAuthRequest {
        @NotBlank(message = "디바이스 ID는 필수입니다.")
        @Schema(description = "디바이스 고유 ID", example = "ABC123XYZ")
        private String deviceId;
    }
}
