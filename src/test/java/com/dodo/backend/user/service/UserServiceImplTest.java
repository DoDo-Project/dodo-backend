package com.dodo.backend.user.service;

import com.dodo.backend.auth.exception.AuthErrorCode;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.user.dto.request.UserRequest.UserRegisterRequest;
import com.dodo.backend.user.dto.request.UserRequest.UserUpdateRequest;
import com.dodo.backend.user.dto.response.UserResponse;
import com.dodo.backend.user.dto.response.UserResponse.UserRegisterResponse;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.exception.UserErrorCode;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 유저 서비스 레이어의 비즈니스 로직을 검증하는 통합 테스트 클래스입니다.
 */
@SpringBootTest
@Transactional
@Slf4j
@TestPropertySource(properties = {
        "naver.client_id=test_naver_id",
        "naver.client_secret=test_naver_secret",
        "naver.redirect_uri=http://localhost:8080/login/oauth2/code/naver",
        "google.client_id=test_google_id",
        "google.client_secret=test_google_secret",
        "google.redirect_uri=http://localhost:8080/login/oauth2/code/google",
        "jwt.secret=test_jwt_secret_key_must_be_very_long_to_pass_validation_check"
})
class UserServiceImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private WebClient webClient;

    /**
     * 소셜 로그인 유저 조회 및 최초 저장 기능을 검증하는 내부 테스트 클래스입니다.
     */
    @Nested
    @DisplayName("소셜 유저 조회 및 저장 테스트")
    class FindOrSaveSocialUserTest {

        /**
         * DB에 존재하지 않는 신규 소셜 유저가 REGISTER 상태로 저장되는지 검증합니다.
         * <p>
         * 1. Given: 신규 이메일과 이름을 준비합니다.
         * 2. When: 소셜 유저 조회 및 저장 로직을 호출합니다.
         * 3. Then: 신규 회원 여부가 true이고, 저장된 유저 상태가 REGISTER인지 확인합니다.
         */
        @Test
        @DisplayName("신규 유저는 REGISTER 상태로 저장")
        void newMemberRegistrationTest() {
            //given
            String email = "new_user@test.com";
            String name = "신규유저";
            log.info("신규 유저 저장 테스트 시작 - 이메일: {}", email);

            //when
            Map<String, Object> result = userService.findOrSaveSocialUser(email, name, "");

            //then
            assertThat(result.get("isNewMember")).isEqualTo(true);
            User savedUser = userRepository.findByEmail(email).orElseThrow();
            assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.REGISTER);
            log.info("신규 유저 가입 대기 상태 저장 확인 완료");
        }

        /**
         * 이미 정지 상태인 유저가 소셜 로그인으로 조회될 때 현재 상태가 그대로 반환되는지 검증합니다.
         * <p>
         * 1. Given: SUSPENDED 상태의 테스트 유저를 저장합니다.
         * 2. When: 같은 이메일로 소셜 유저 조회 및 저장 로직을 호출합니다.
         * 3. Then: 신규 회원 여부가 false이고, 상태가 SUSPENDED로 반환되는지 확인합니다.
         */
        @Test
        @DisplayName("정지된 계정은 접근 제한 예외 발생")
        void suspendedUserTest() {
            //given
            String email = "suspended@test.com";
            User suspendedUser = createTestUser(email, UserStatus.SUSPENDED);
            userRepository.save(suspendedUser);
            log.info("정지 계정 로그인 차단 테스트 시작 - 이메일: {}", email);

            //when
            Map<String, Object> result = userService.findOrSaveSocialUser(email, "정지유저", "");

            //then
            assertThat(result.get("isNewMember")).isEqualTo(false);
            assertThat(result.get("status")).isEqualTo(UserStatus.SUSPENDED);
            log.info("정지 계정 상태 반환 확인 완료");
        }
    }

    /**
     * 회원가입 마지막 단계의 추가 정보 입력 및 계정 활성화 기능을 검증하는 내부 테스트 클래스입니다.
     */
    @Nested
    @DisplayName("추가 정보 입력 및 가입 완료 테스트")
    class RegisterAdditionalInfoTest {

        /**
         * REGISTER 상태의 유저가 추가 정보를 입력하면 ACTIVE 상태로 변경되고 프로필 URL이 반영되는지 검증합니다.
         * <p>
         * 1. Given: REGISTER 상태의 테스트 유저와 프로필 URL이 포함된 요청을 준비합니다.
         * 2. When: 추가 정보 등록 로직을 호출합니다.
         * 3. Then: 유저 상태가 ACTIVE로 변경되고, DB와 응답에 프로필 URL이 반영되었는지 확인합니다.
         */
        @Test
        @DisplayName("정보 입력 완료 시 ACTIVE 상태로 변경")
        void completeRegistrationSuccessTest() {
            //given
            String email = "register@test.com";
            userRepository.save(createTestUser(email, UserStatus.REGISTER));
            em.flush();
            em.clear();
            log.info("추가 정보 입력 테스트 시작 - 이메일: {}", email);

            UserRegisterRequest request = UserRegisterRequest.builder()
                    .nickname("멋진닉네임")
                    .region("서울")
                    .hasFamily(true)
                    .profileUrl("https://example.com/images/profile.jpg")
                    .build();

            //when
            UserRegisterResponse response = userService.registerAdditionalInfo(request, email);

            //then
            em.clear();
            User updatedUser = userRepository.findByEmail(email).orElseThrow();
            assertThat(updatedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(updatedUser.getProfileUrl()).isEqualTo("https://example.com/images/profile.jpg");
            assertThat(response.getProfileUrl()).isEqualTo("https://example.com/images/profile.jpg");
            log.info("유저 상태 활성화 및 데이터 반영 성공 확인");
        }

        /**
         * 프로필 URL을 전달하지 않은 회원가입 완료 요청에서 기존 소셜 프로필 이미지가 유지되는지 검증합니다.
         * <p>
         * 1. Given: 기존 프로필 URL을 가진 REGISTER 상태 유저를 저장합니다.
         * 2. When: 프로필 URL 없이 추가 정보 등록 로직을 호출합니다.
         * 3. Then: DB와 응답의 프로필 URL이 기존 값으로 유지되는지 확인합니다.
         */
        @Test
        @DisplayName("프로필 URL 미전달 시 기존 소셜 프로필 이미지 유지")
        void completeRegistrationKeepExistingProfileUrlTest() {
            //given
            String email = "register_keep@test.com";
            String existingProfileUrl = "https://example.com/images/original.jpg";
            User existingUser = User.builder()
                    .email(email)
                    .name("테스트유저")
                    .nickname("")
                    .profileUrl(existingProfileUrl)
                    .region("서울")
                    .notificationEnabled(true)
                    .role(UserRole.USER)
                    .userStatus(UserStatus.REGISTER)
                    .userCreatedAt(LocalDateTime.now())
                    .hasFamily(false)
                    .build();
            userRepository.save(existingUser);
            em.flush();
            em.clear();

            UserRegisterRequest request = UserRegisterRequest.builder()
                    .nickname("유지닉네임")
                    .region("서울")
                    .hasFamily(true)
                    .build();

            //when
            UserRegisterResponse response = userService.registerAdditionalInfo(request, email);

            //then
            em.clear();
            User updatedUser = userRepository.findByEmail(email).orElseThrow();
            assertThat(updatedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(updatedUser.getProfileUrl()).isEqualTo(existingProfileUrl);
            assertThat(response.getProfileUrl()).isEqualTo(existingProfileUrl);
            log.info("프로필 URL 미전달 시 기존 소셜 프로필 이미지 유지 확인");
        }

        /**
         * 공백 문자열 프로필 URL이 전달되더라도 기존 소셜 프로필 이미지가 덮어써지지 않는지 검증합니다.
         * <p>
         * 1. Given: 기존 프로필 URL을 가진 REGISTER 상태 유저를 저장합니다.
         * 2. When: 공백 문자열 프로필 URL로 추가 정보 등록 로직을 호출합니다.
         * 3. Then: DB와 응답의 프로필 URL이 기존 값으로 유지되는지 확인합니다.
         */
        @Test
        @DisplayName("프로필 URL 공백 전달 시 기존 소셜 프로필 이미지 유지")
        void completeRegistrationKeepExistingProfileUrlWhenBlankTest() {
            //given
            String email = "register_blank@test.com";
            String existingProfileUrl = "https://example.com/images/original-blank.jpg";
            User existingUser = User.builder()
                    .email(email)
                    .name("테스트유저")
                    .nickname("")
                    .profileUrl(existingProfileUrl)
                    .region("서울")
                    .notificationEnabled(true)
                    .role(UserRole.USER)
                    .userStatus(UserStatus.REGISTER)
                    .userCreatedAt(LocalDateTime.now())
                    .hasFamily(false)
                    .build();
            userRepository.save(existingUser);
            em.flush();
            em.clear();

            UserRegisterRequest request = UserRegisterRequest.builder()
                    .nickname("공백닉네임")
                    .region("서울")
                    .hasFamily(true)
                    .profileUrl(" ")
                    .build();

            //when
            UserRegisterResponse response = userService.registerAdditionalInfo(request, email);

            //then
            em.clear();
            User updatedUser = userRepository.findByEmail(email).orElseThrow();
            assertThat(updatedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(updatedUser.getProfileUrl()).isEqualTo(existingProfileUrl);
            assertThat(response.getProfileUrl()).isEqualTo(existingProfileUrl);
            log.info("프로필 URL 공백 전달 시 기존 소셜 프로필 이미지 유지 확인");
        }

        /**
         * 이미 사용 중인 닉네임으로 회원가입 완료를 시도하면 중복 예외가 발생하는지 검증합니다.
         * <p>
         * 1. Given: 기존 ACTIVE 유저와 같은 닉네임을 사용하는 REGISTER 유저를 준비합니다.
         * 2. When: 중복 닉네임으로 추가 정보 등록 로직을 호출합니다.
         * 3. Then: {@link UserErrorCode#NICKNAME_DUPLICATED} 에러 코드가 반환되는지 확인합니다.
         */
        @Test
        @DisplayName("중복 닉네임 사용 시 예외 발생")
        void duplicatedNicknameTest() {
            //given
            String existingNick = "중복닉네임";
            User existingUser = User.builder()
                    .email("existing@test.com")
                    .name("기존유저")
                    .nickname(existingNick)
                    .region("서울") // region 필수값 추가
                    .userStatus(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .notificationEnabled(true)
                    .profileUrl("")
                    .userCreatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(existingUser);

            String newEmail = "new@test.com";
            userRepository.save(createTestUser(newEmail, UserStatus.REGISTER));
            log.info("닉네임 중복 가입 차단 테스트 시작 - 닉네임: {}", existingNick);

            UserRegisterRequest request = UserRegisterRequest.builder()
                    .nickname(existingNick)
                    .region("서울")
                    .hasFamily(false)
                    .build();

            //when
            UserException exception = assertThrows(UserException.class, () -> {
                userService.registerAdditionalInfo(request, newEmail);
            });

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.NICKNAME_DUPLICATED);
            log.info("닉네임 중복 가입 차단 확인 완료");
        }
    }

    /**
     * 유저 프로필 정보 수정 기능을 검증하는 내부 테스트 클래스입니다.
     */
    @Nested
    @DisplayName("유저 정보 수정 테스트")
    class UpdateUserInfoTest {

        /**
         * 선택적으로 전달된 프로필 필드만 수정되고 전달되지 않은 값은 기존 값으로 유지되는지 검증합니다.
         * <p>
         * 1. Given: ACTIVE 상태의 테스트 유저와 일부 필드만 포함된 수정 요청을 준비합니다.
         * 2. When: 유저 정보 수정 로직을 호출합니다.
         * 3. Then: 요청한 필드는 변경되고, 요청하지 않은 가족 여부는 기존 값으로 유지되는지 확인합니다.
         */
        @Test
        @DisplayName("선택적 필드 수정 시 요청 데이터만 변경되고 응답에 반영됨")
        void updateUserInfoPartialSuccessTest() {
            //given
            String email = "update@test.com";
            User user = User.builder()
                    .email(email)
                    .name("백현빈")
                    .nickname("초기닉네임")
                    .region("서울")
                    .hasFamily(true)
                    .userStatus(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .notificationEnabled(true)
                    .profileUrl("url")
                    .userCreatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
            em.flush();
            em.clear();
            log.info("유저 정보 선택적 수정 테스트 시작");

            UserUpdateRequest request = UserUpdateRequest.builder()
                    .nickname("변경닉네임")
                    .region("부산")
                    .hasFamily(null)
                    .build();

            //when
            UserResponse.UserUpdateResponse response = userService.updateUserInfo(user.getUsersId(), request);

            //then
            assertThat(response.getNickname()).isEqualTo("변경닉네임");
            assertThat(response.getRegion()).isEqualTo("부산");
            assertThat(response.getHasFamily()).isTrue();

            em.flush();
            em.clear();
            User updatedUser = userRepository.findById(user.getUsersId()).orElseThrow();
            assertThat(updatedUser.getNickname()).isEqualTo("변경닉네임");
            log.info("필드 선택적 수정 및 기존 값 유지 확인 완료");
        }

        /**
         * 다른 유저가 이미 사용 중인 닉네임으로 수정하려 할 때 중복 예외가 발생하는지 검증합니다.
         * <p>
         * 1. Given: 중복 대상 닉네임을 가진 유저와 수정 대상 유저를 저장합니다.
         * 2. When: 수정 대상 유저가 중복 닉네임으로 변경을 요청합니다.
         * 3. Then: {@link UserErrorCode#NICKNAME_DUPLICATED} 에러 코드가 반환되는지 확인합니다.
         */
        @Test
        @DisplayName("이미 존재하는 닉네임으로 수정 시도 시 UserException 발생")
        void updateUserInfoDuplicateNicknameTest() {
            //given
            String takenNickname = "이미있는닉네임";
            User otherUser = User.builder()
                    .email("other@test.com")
                    .name("기존유저")
                    .nickname(takenNickname)
                    .region("서울") // region 필수값 추가
                    .userStatus(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .notificationEnabled(true)
                    .profileUrl("")
                    .userCreatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(otherUser);

            User targetUser = createTestUser("target@test.com", UserStatus.ACTIVE);
            userRepository.save(targetUser);
            em.flush();
            em.clear();
            log.info("수정 시 닉네임 중복 차단 테스트 시작");

            UserUpdateRequest request = UserUpdateRequest.builder()
                    .nickname(takenNickname)
                    .build();

            //when
            UserException exception = assertThrows(UserException.class, () -> {
                userService.updateUserInfo(targetUser.getUsersId(), request);
            });

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.NICKNAME_DUPLICATED);
            log.info("수정 시 닉네임 중복 차단 확인 완료");
        }

        /**
         * 본인이 현재 사용 중인 닉네임을 그대로 전달하면 중복 예외 없이 수정되는지 검증합니다.
         * <p>
         * 1. Given: ACTIVE 상태의 테스트 유저를 저장합니다.
         * 2. When: 기존 닉네임과 변경할 지역을 포함한 수정 요청을 보냅니다.
         * 3. Then: 닉네임은 유지되고 지역 값이 응답에 반영되는지 확인합니다.
         */
        @Test
        @DisplayName("본인의 현재 닉네임 유지 시 예외 없이 수정 성공")
        void updateUserInfoSameNicknameSuccessTest() {
            //given
            String myNickname = "나의닉네임";
            User user = User.builder()
                    .email("me@test.com")
                    .name("본인")
                    .nickname(myNickname)
                    .region("서울")
                    .userStatus(UserStatus.ACTIVE)
                    .role(UserRole.USER)
                    .notificationEnabled(true)
                    .profileUrl("")
                    .userCreatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(user);
            em.flush();
            em.clear();
            log.info("본인 닉네임 유지 수정 테스트 시작");

            UserUpdateRequest request = UserUpdateRequest.builder()
                    .nickname(myNickname)
                    .region("인천")
                    .build();

            //when
            UserResponse.UserUpdateResponse response = userService.updateUserInfo(user.getUsersId(), request);

            //then
            assertThat(response.getNickname()).isEqualTo(myNickname);
            assertThat(response.getRegion()).isEqualTo("인천");
            log.info("본인 닉네임 유지 수정 성공 확인");
        }
    }

    /**
     * 유저의 알림 수신 설정 변경 기능({@link UserService#updateNotification})을 검증하는 내부 테스트 클래스입니다.
     */
    @Nested
    @DisplayName("알림 수신 설정 변경 테스트")
    class UpdateNotificationTest {

        /**
         * 정상적인 유저의 알림 수신 여부 값을 변경(ON -> OFF)하고, DB에 올바르게 반영되는지 검증합니다.
         * <p>
         * 1. Given: 알림 설정이 켜져있는(ON) 상태의 테스트 유저를 생성 및 저장합니다.
         * 2. When: 해당 유저의 식별자로 알림 끄기(OFF) 요청을 보냅니다.
         * 3. Then: DB에서 유저를 다시 조회하여 {@code notificationEnabled} 값이 false로 변경되었는지 확인합니다.
         */
        @Test
        @DisplayName("알림 수신 여부를 ON -> OFF로 성공적으로 변경")
        void updateNotificationSuccessTest() {
            //given
            User user = createTestUser("notify@test.com", UserStatus.ACTIVE);
            userRepository.save(user);
            em.flush();
            em.clear();
            log.info("알림 설정 변경 테스트 시작 (True -> False)");

            //when
            userService.updateNotification(user.getUsersId(), false);

            //then
            em.flush();
            em.clear();
            User updatedUser = userRepository.findById(user.getUsersId()).orElseThrow();
            assertThat(updatedUser.getNotificationEnabled()).isFalse();
            log.info("알림 설정 변경(OFF) DB 반영 확인 완료");
        }

        /**
         * 존재하지 않는 유저 ID로 설정을 변경하려 할 때, 예외가 적절히 발생하는지 검증합니다.
         * <p>
         * 1. Given: 임의의 생성된 UUID(DB에 없음)를 준비합니다.
         * 2. When: 해당 ID로 설정 변경을 시도합니다.
         * 3. Then: {@link UserException}이 발생하고, 에러 코드가 {@link UserErrorCode#USER_NOT_FOUND}인지 확인합니다.
         */
        @Test
        @DisplayName("존재하지 않는 유저의 설정을 변경하려 하면 예외 발생")
        void updateNotificationUserNotFoundTest() {
            //given
            UUID nonExistentUserId = UUID.randomUUID();
            log.info("존재하지 않는 유저 알림 변경 테스트 시작");

            //when
            UserException exception = assertThrows(UserException.class, () -> {
                userService.updateNotification(nonExistentUserId, false);
            });

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            log.info("존재하지 않는 유저 예외 발생 확인 완료");
        }
    }

    /**
     * 닉네임 중복 확인 기능을 검증하는 내부 테스트 클래스입니다.
     */
    @Nested
    @DisplayName("닉네임 중복 확인 테스트")
    class CheckNicknameDuplicationTest {

        /**
         * 이미 존재하는 닉네임을 확인하면 중복 여부가 true로 반환되는지 검증합니다.
         */
        @Test
        @DisplayName("이미 존재하는 닉네임이면 duplicated true를 반환한다")
        void checkNicknameDuplicationDuplicatedTest() {
            //given
            String nickname = "중복닉네임";
            User user = User.builder()
                    .email("nickname-check@test.com")
                    .name("닉네임테스터")
                    .nickname(nickname)
                    .profileUrl("")
                    .region("서울")
                    .notificationEnabled(true)
                    .role(UserRole.USER)
                    .userStatus(UserStatus.ACTIVE)
                    .userCreatedAt(LocalDateTime.now())
                    .hasFamily(false)
                    .build();
            userRepository.save(user);

            //when
            UserResponse.NicknameCheckResponse response = userService.checkNicknameDuplication(nickname);

            //then
            assertThat(response.getNickname()).isEqualTo(nickname);
            assertThat(response.getDuplicated()).isTrue();
        }

        /**
         * 존재하지 않는 닉네임을 확인하면 중복 여부가 false로 반환되는지 검증합니다.
         */
        @Test
        @DisplayName("존재하지 않는 닉네임이면 duplicated false를 반환한다")
        void checkNicknameDuplicationAvailableTest() {
            //given
            String nickname = "사용가능";

            //when
            UserResponse.NicknameCheckResponse response = userService.checkNicknameDuplication(nickname);

            //then
            assertThat(response.getNickname()).isEqualTo(nickname);
            assertThat(response.getDuplicated()).isFalse();
        }

        /**
         * 공백만 있는 닉네임을 확인하면 잘못된 요청 예외가 발생하는지 검증합니다.
         */
        @Test
        @DisplayName("공백 닉네임이면 UserException이 발생한다")
        void checkNicknameDuplicationBlankNicknameTest() {
            //given
            String nickname = "   ";

            //when
            UserException exception = assertThrows(UserException.class, () -> {
                userService.checkNicknameDuplication(nickname);
            });

            //then
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_REQUEST);
        }

        /**
         * 길이 조건을 만족하지 않는 닉네임을 확인하면 잘못된 요청 예외가 발생하는지 검증합니다.
         */
        @Test
        @DisplayName("길이 조건을 만족하지 않는 닉네임이면 UserException이 발생한다")
        void checkNicknameDuplicationInvalidLengthTest() {
            //given
            String nickname = "가";

            //when
            UserException exception = assertThrows(UserException.class, () -> {
                userService.checkNicknameDuplication(nickname);
            });

            //then
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_REQUEST);
        }

        /**
         * 허용되지 않는 문자가 포함된 닉네임을 확인하면 잘못된 요청 예외가 발생하는지 검증합니다.
         */
        @Test
        @DisplayName("허용되지 않는 문자가 포함된 닉네임이면 UserException이 발생한다")
        void checkNicknameDuplicationInvalidPatternTest() {
            //given
            String nickname = "도도!";

            //when
            UserException exception = assertThrows(UserException.class, () -> {
                userService.checkNicknameDuplication(nickname);
            });

            //then
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * 테스트에서 공통으로 사용하는 유저 엔티티를 생성합니다.
     * <p>
     * REGISTER 상태일 때는 추가 정보 입력 전 상태를 표현하기 위해 빈 닉네임을 사용하고,
     * 그 외 상태에서는 중복을 피하기 위해 UUID 기반 닉네임을 생성합니다.
     *
     * @param email  테스트 유저 이메일
     * @param status 테스트 유저 계정 상태
     * @return 테스트용 유저 엔티티
     */
    private User createTestUser(String email, UserStatus status) {
        return User.builder()
                .email(email)
                .name("테스트유저")
                .nickname(status == UserStatus.REGISTER ? "" : "Nick_" + UUID.randomUUID().toString().substring(0, 8))
                .profileUrl("")
                .region("서울")
                .notificationEnabled(true)
                .role(UserRole.USER)
                .userStatus(status)
                .userCreatedAt(LocalDateTime.now())
                .hasFamily(false)
                .build();
    }
}
