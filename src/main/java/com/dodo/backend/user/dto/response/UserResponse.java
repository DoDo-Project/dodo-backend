package com.dodo.backend.user.dto.response;

import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 유저(User) 도메인의 응답 데이터를 그룹화하여 관리하는 클래스입니다.
 * <p>
 * 내부 정적 클래스를 통해 회원가입 완료, 정보 조회 등 상황별 응답 DTO를 정의합니다.
 */
@Schema(description = "유저 관련 response 그룹")
public class UserResponse {

    /**
     * 회원가입 완료 후 클라이언트에게 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "회원가입 완료 및 토큰 발급 응답")
    public static class UserRegisterResponse {

        @Schema(description = "응답 메시지", example = "회원가입이 완료되었습니다.")
        private String message;

        @Schema(description = "프로필 이미지 URL", example = "https://i.pravatar.cc/150?img=3")
        private String profileUrl;

        @Schema(description = "서버 접근용 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String accessToken;

        @Schema(description = "엑세스 토큰 재발급용 리프레시 토큰", example = "def50200f29184b294277418292...")
        private String refreshToken;

        @Schema(description = "액세스 토큰 만료 시간 (밀리초 단위)", example = "3600000")
        private Long accessTokenExpiresIn;

        public static UserRegisterResponse toDto(User user, String message, String accessToken, String refreshToken, Long accessTokenExpiresIn) {
            return UserRegisterResponse.builder()
                    .message(message)
                    .profileUrl(user.getProfileUrl())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .accessTokenExpiresIn(accessTokenExpiresIn)
                    .build();
        }
    }

    /**
     * 유저의 상세 프로필 정보를 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "유저 상세 정보 조회 응답")
    public static class UserInfoResponse {

        @Schema(description = "응답 메시지", example = "유저 정보 조회 성공했습니다.")
        private String message;

        @Schema(description = "이메일", example = "100gusqls@naver.com")
        private String email;

        @Schema(description = "사용자 이름", example = "백현빈")
        private String name;

        @Schema(description = "사용자 닉네임", example = "고길동")
        private String nickname;

        @Schema(description = "활동 지역", example = "서울특별시 동대문구")
        private String region;

        @Schema(description = "가족 여부", example = "true")
        private Boolean hasFamily;

        @Schema(description = "프로필 이미지 URL", example = "https://i.pravatar.cc/150?img=3")
        private String profileUrl;

        @Schema(description = "알림 수신 여부", example = "true")
        private Boolean notificationEnabled;

        @Schema(description = "계정 생성일", example = "2025-09-30T14:30:00Z")
        private LocalDateTime userCreatedAt;

        public static UserInfoResponse toDto(User user, String message) {
            return UserInfoResponse.builder()
                    .message(message)
                    .email(user.getEmail())
                    .name(user.getName())
                    .nickname(user.getNickname())
                    .region(user.getRegion())
                    .hasFamily(user.getHasFamily())
                    .profileUrl(user.getProfileUrl())
                    .notificationEnabled(user.getNotificationEnabled())
                    .userCreatedAt(user.getUserCreatedAt())
                    .build();
        }
    }

    /**
     * 유저 정보 수정 성공 시 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "유저 정보 수정 완료 응답")
    public static class UserUpdateResponse {

        @Schema(description = "응답 메시지", example = "유저 정보 조회 성공했습니다.")
        private String message;

        @Schema(description = "이메일 주소", example = "100gusqls@naver.com")
        private String email;

        @Schema(description = "유저 실명", example = "백현빈")
        private String name;

        @Schema(description = "유저 닉네임", example = "김길자")
        private String nickname;

        @Schema(description = "활동 지역", example = "부산시 해운대구")
        private String region;

        @Schema(description = "가족 여부", example = "false")
        private Boolean hasFamily;

        @Schema(description = "프로필 이미지 URL", example = "https://i.pravatar.cc/150?img=3")
        private String profileUrl;

        @Schema(description = "계정 생성일", example = "2025-09-30T14:30:00Z")
        private LocalDateTime createdAt;

        /**
         * 엔티티 객체를 수정 완료 응답 DTO로 변환합니다.
         *
         * @param user 수정된 유저 엔티티
         * @return UserUpdateExResponse DTO
         */
        public static UserUpdateResponse toDto(User user, String message, String nickname, String region, Boolean hasFamily) {
            return UserUpdateResponse.builder()
                    .message(message)
                    .email(user.getEmail())
                    .name(user.getName())
                    .nickname(nickname)
                    .region(region)
                    .hasFamily(hasFamily)
                    .profileUrl(user.getProfileUrl())
                    .createdAt(user.getUserCreatedAt())
                    .build();
        }
    }

    /**
     * 닉네임 중복 확인 결과를 반환하는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "닉네임 중복 확인 응답")
    public static class NicknameCheckResponse {

        @Schema(description = "응답 메시지", example = "닉네임 중복 확인이 완료되었습니다.")
        private String message;

        @Schema(description = "확인한 닉네임", example = "도도")
        private String nickname;

        @Schema(description = "닉네임 중복 여부", example = "false")
        private Boolean duplicated;

        public static NicknameCheckResponse toDto(String nickname, Boolean duplicated) {
            return NicknameCheckResponse.builder()
                    .message("닉네임 중복 확인이 완료되었습니다.")
                    .nickname(nickname)
                    .duplicated(duplicated)
                    .build();
        }
    }
}
