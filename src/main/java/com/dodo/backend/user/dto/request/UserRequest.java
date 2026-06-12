package com.dodo.backend.user.dto.request;

import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 사용자(User) 도메인 관련 요청 데이터를 그룹화하여 관리하는 클래스입니다.
 * <p>
 * 회원가입 시의 프로필 추가 정보 입력, 계정 상태 변경을 위한 본인 인증 번호 전달 등 사용자 정보 수정과 관련된 요청 구조를 정의합니다.
 */
@Schema(description = "유저 관련 요청 DTO 그룹")
public class UserRequest {

    /**
     * 회원가입(추가 정보 입력) 요청 시 클라이언트가 전송하는 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "회원가입 추가 정보 입력 요청")
    public static class UserRegisterRequest {

        @Schema(description = "유저 닉네임", example = "강력한 개발자")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9 ]*$", message = "닉네임은 한글, 영문, 숫자, 공백만 가능합니다.")
        private String nickname;

        @Schema(description = "활동 지역", example = "서울시 동대문구")
        @NotBlank(message = "지역 정보는 필수입니다.")
        @Size(max = 50, message = "지역 정보가 너무 깁니다.")
        private String region;

        @Schema(description = "가족 여부 (true: 있다, false: 없다)", example = "true")
        @NotNull(message = "가족 여부는 필수입니다.")
        private Boolean hasFamily;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/images/profile.jpg")
        private String profileUrl;

        /**
         * DTO의 데이터와 식별자(Email)를 조합하여 업데이트용 User 엔티티를 생성합니다.
         */
        public User toEntity(String email) {
            return User.builder()
                    .email(email)
                    .nickname(this.nickname)
                    .region(this.region)
                    .hasFamily(this.hasFamily)
                    .profileUrl(StringUtils.hasText(this.profileUrl) ? this.profileUrl : null)
                    .build();
        }
    }

    /**
     * 회원 탈퇴 확정을 위한 인증 번호 검증 요청 DTO입니다.
     */
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "회원 탈퇴 본인 인증 요청")
    public static class WithdrawalRequest {

        @Schema(description = "이메일로 발송된 6자리 인증 번호", example = "936514")
        @NotBlank(message = "인증 번호는 필수입니다.")
        @Size(min = 6, max = 6, message = "인증 번호는 6자리여야 합니다.")
        private String authCode;
    }

    /**
     * 기존 사용자 정보를 수정하기 위한 요청 DTO입니다.
     * <p>
     * 닉네임, 활동 지역, 가족 여부 중 변경을 원하는 필드만 선택적으로 전송할 수 있습니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "유저 정보 수정 요청")
    public static class UserUpdateRequest {

        @Schema(description = "변경할 닉네임 (선택)", example = "김길자")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]*$", message = "닉네임은 한글, 영문, 숫자만 가능합니다.")
        private String nickname;

        @Schema(description = "변경할 활동 지역 (선택) ", example = "부산시 해운대구")
        @Size(max = 50, message = "지역 정보가 너무 깁니다.")
        private String region;

        @Schema(description = "변경할 가족 여부 (선택 true: 있다, false: 없다)", example = "false")
        private Boolean hasFamily;

        @Schema(description = "변경할 프로필 사진 ", example = "https://example.com/images/profile.jpg")
        private String profileUrl;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "알림 수신 여부 변경 요청")
    public static class NotificationUpdateRequest {

        @Schema(description = "변경할 알림 수신 여부 (true: 수신, false: 거부)", example = "false")
        @NotNull(message = "알림 설정 값은 필수입니다.")
        private Boolean notificationEnabled;
    }
}
