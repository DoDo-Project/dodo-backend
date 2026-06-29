package com.dodo.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 인증(Auth) 도메인 관련 응답 데이터를 그룹화하여 관리하는 클래스입니다.
 * <p>
 * 소셜 로그인 성공 여부에 따른 서비스 토큰 발급 또는 신규 가입을 위한 임시 토큰 전달 등 인증 결과에 대한 응답 구조를 정의합니다.
 */
@Schema(description = "인증 관련 응답 DTO 그룹")
public class AuthResponse {

    /**
     * 소셜 로그인 성공 시(기존 회원) 클라이언트에게 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "소셜 로그인 성공 응답")
    public static class SocialLoginResponse {

        @Schema(description = "응답 메시지", example = "로그인이 완료되었습니다.")
        private String message;

        @Schema(description = "프로필 이미지 URL", example = "https://i.pravatar.cc/150?img=3")
        private String profileUrl;

        @Schema(description = "서버 접근용 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String accessToken;

        @Schema(description = "엑세스 토큰 재발급용 리프레시 토큰", example = "def50200f29184b294277418292...")
        private String refreshToken;

        @Schema(description = "액세스 토큰 만료 시간 (밀리초 단위)", example = "3600000")
        private Long accessTokenExpiresIn;

        public static SocialLoginResponse toDto(String message, String profileUrl, String accessToken, String refreshToken, Long accessTokenExpiresIn) {
            return SocialLoginResponse.builder()
                    .message(message)
                    .profileUrl(profileUrl)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(accessTokenExpiresIn)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "관리자 로그인 성공 응답")
    public static class AdminLoginResponse {

        @Schema(description = "응답 메시지", example = "관리자 로그인이 완료되었습니다.")
        private String message;

        @Schema(description = "관리자 Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String accessToken;

        @Schema(description = "관리자 Refresh Token", example = "def50200f29184b294277418292...")
        private String refreshToken;

        @Schema(description = "Access Token 만료 시간(밀리초)", example = "3600000")
        private Long accessTokenExpiresIn;

        @Schema(description = "권한", example = "ADMIN")
        private String role;

        public static AdminLoginResponse toDto(String accessToken, String refreshToken, Long accessTokenExpiresIn, String role) {
            return AdminLoginResponse.builder()
                    .message("관리자 로그인이 완료되었습니다.")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(accessTokenExpiresIn)
                    .role(role)
                    .build();
        }
    }

    /**
     * 신규 회원가입 대상자일 경우 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "신규 가입 대상자 응답")
    public static class SocialRegisterResponse {

        @Schema(description = "응답 메시지", example = "추가 정보가 필요합니다.")
        private String message;

        @Schema(description = "소셜에서 가져온 이메일 (회원가입 시 사용)", example = "user@naver.com")
        private String email;

        @Schema(description = "소셜에서 가져온 이름", example = "홍길동")
        private String name;

        @Schema(description = "회원가입 요청 시 본인 인증용 토큰", example = "def50200f29184b294277418292...")
        private String registrationToken;

        @Schema(description = "임시 토큰 만료 시간 (밀리초 단위)", example = "1800000")
        private Long tokenExpiresIn;

        public static SocialRegisterResponse toDto(String message, String email, String name, String registrationToken, Long tokenExpiresIn){
            return SocialRegisterResponse.builder()
                    .message(message)
                    .email(email)
                    .name(name)
                    .registrationToken(registrationToken)
                    .tokenExpiresIn(tokenExpiresIn)
                    .build();
        }
    }

    /**
     * 토큰 재발급 성공 시 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "토큰 재발급 응답")
    public static class TokenResponse {

        @Schema(description = "응답 메시지", example = "성공적으로 토큰이 재발급되었습니다.")
        private String message;

        @Schema(description = "새로 발급된 서버 접근용 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String accessToken;

        @Schema(description = "새로 갱신된 리프레시 토큰 (Rotation)", example = "def50200f29184b294277418292...")
        private String refreshToken;

        @Schema(description = "액세스 토큰 만료 시간 (초 단위)", example = "3600")
        private Long accessTokenExpiresIn;

        /**
         * 토큰 문자열과 만료 시간을 받아 응답 DTO를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param accessToken       새로 생성된 액세스 토큰
         * @param refreshToken      새로 생성된 리프레시 토큰
         * @param accessTokenExpiresIn 액세스 토큰의 유효 기간 (밀리초 단위 -> 초 단위 변환 필요 시 로직 확인)
         * @return 빌드된 TokenResponse 객체
         */
        public static TokenResponse toDto(String accessToken, String refreshToken, Long accessTokenExpiresIn, String message) {
            return TokenResponse.builder()
                    .message(message)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(accessTokenExpiresIn)
                    .build();
        }
    }

    /**
     * 장치 인증 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "장치 인증 결과 응답")
    public static class DeviceAuthResponse {

        @Schema(description = "응답 메시지", example = "인증이 완료되었습니다.")
        private String message;

        @Schema(description = "액세스 토큰")
        private String accessToken;

        @Schema(description = "리프레시 토큰")
        private String refreshToken;

        @Schema(description = "액세스 토큰 만료 시간(초)", example = "3600")
        private Long accessTokenExpiresIn;

        @Schema(description = "연동된 반려동물 ID", example = "1")
        private Long petId;

        /**
         * 장치 인증 성공 정보를 바탕으로 응답 DTO를 생성하는 정적 팩토리 메서드입니다.
         *
         * @param message              결과 메시지
         * @param accessToken          발급된 액세스 토큰
         * @param refreshToken         발급된 리프레시 토큰
         * @param accessTokenExpiresIn 액세스 토큰 만료 시간 (초 단위)
         * @param petId                인증된 장치와 연결된 반려동물의 ID
         * @return 초기화된 {@link DeviceAuthResponse} 객체
         */
        public static DeviceAuthResponse toDto(String message, String accessToken, String refreshToken, Long accessTokenExpiresIn, Long petId) {
            return DeviceAuthResponse.builder()
                    .message(message)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(accessTokenExpiresIn)
                    .petId(petId)
                    .build();
        }
    }
}
