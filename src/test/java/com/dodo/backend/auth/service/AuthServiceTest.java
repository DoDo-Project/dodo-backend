package com.dodo.backend.auth.service;

import com.dodo.backend.auth.dto.request.AuthRequest.AdminLoginRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.DeviceAuthRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.LogoutRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest;
import com.dodo.backend.auth.entity.RefreshToken;
import com.dodo.backend.auth.exception.AuthErrorCode;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.auth.repository.RefreshTokenRepository;
import com.dodo.backend.common.jwt.JwtTokenProvider;
import com.dodo.backend.pet.service.PetServiceImpl;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.dodo.backend.auth.dto.response.AuthResponse.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link AuthServiceImpl}의 비즈니스 로직을 검증하는 단위 테스트 클래스입니다.
 * <p>
 * 주로 로그아웃 처리 시 리프레시 토큰 삭제 및 액세스 토큰의 블랙리스트 처리 로직을 중점적으로 테스트합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private PetServiceImpl petService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("관리자 로그인 성공 - ADMIN 권한 토큰 발급")
    void adminLogin_Success() {
        UUID adminId = UUID.randomUUID();
        String email = "admin@dodo.com";
        String password = "admin-password";
        String accessToken = "admin-access-token";
        String refreshToken = "admin-refresh-token";

        ReflectionTestUtils.setField(authService, "adminLoginEmail", email);
        ReflectionTestUtils.setField(authService, "adminLoginPassword", password);

        AdminLoginRequest request = AdminLoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        User admin = User.builder()
                .usersId(adminId)
                .email(email)
                .role(UserRole.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(admin));
        given(jwtTokenProvider.createAccessToken(adminId, "ADMIN")).willReturn(accessToken);
        given(jwtTokenProvider.createRefreshToken(adminId)).willReturn(refreshToken);
        given(jwtTokenProvider.getAccessTokenValidityInMilliseconds()).willReturn(3600000L);

        AdminLoginResponse response = authService.adminLogin(request);

        assertThat(response.getMessage()).isEqualTo("관리자 로그인이 완료되었습니다.");
        assertThat(response.getAccessToken()).isEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(response.getRole()).isEqualTo("ADMIN");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    /**
     * 로그아웃 성공 시나리오를 테스트합니다.
     * <p>
     * 1. DB에서 리프레시 토큰이 정상적으로 삭제되어야 합니다.<br>
     * 2. 남은 유효 기간이 있는 액세스 토큰은 Redis 블랙리스트에 등록되어야 합니다.
     */
    @Test
    @DisplayName("로그아웃 성공 - 리프레시 토큰 삭제 및 액세스 토큰 블랙리스트 등록")
    void logout_Success() {
        log.info("로그아웃 성공 테스트를 시작합니다.");
        // given
        String refreshTokenStr = "valid-refresh-token";
        String accessTokenStr = "valid-access-token";

        String userId = UUID.randomUUID().toString();
        long remainingTime = 3600000L;

        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(refreshTokenStr)
                .build();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .usersId(userId)
                .refreshToken(refreshTokenStr)
                .build();

        log.info("리프레시 토큰이 DB에 존재하고 액세스 토큰의 유효시간이 남아있다고 설정합니다.");
        given(refreshTokenRepository.findByRefreshToken(refreshTokenStr))
                .willReturn(Optional.of(refreshTokenEntity));

        given(jwtTokenProvider.getRemainingValidTime(accessTokenStr))
                .willReturn(remainingTime);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        log.info("로그아웃 서비스를 호출합니다.");
        authService.logout(request, accessTokenStr);

        // then
        log.info("DB에서 리프레시 토큰이 삭제되었는지, Redis 블랙리스트에 등록되었는지 검증합니다.");
        verify(refreshTokenRepository).delete(refreshTokenEntity);

        verify(valueOperations).set(
                eq("blacklist:" + accessTokenStr),
                eq("logout"),
                eq(remainingTime),
                eq(TimeUnit.MILLISECONDS)
        );
        log.info("로그아웃 성공 테스트가 통과되었습니다.");
    }

    /**
     * 이미 만료된 액세스 토큰으로 로그아웃을 시도하는 시나리오를 테스트합니다.
     * <p>
     * 리프레시 토큰은 삭제되어야 하지만, 액세스 토큰은 이미 만료되었으므로
     * 불필요한 리소스 낭비를 막기 위해 블랙리스트에는 등록되지 않아야 합니다.
     */
    @Test
    @DisplayName("로그아웃 성공 - 액세스 토큰이 이미 만료된 경우 (블랙리스트 등록 안 함)")
    void logout_Success_ExpiredAccessToken() {
        log.info("만료된 토큰으로 로그아웃 시도 성공 테스트를 시작합니다.");
        // given
        String refreshTokenStr = "valid-refresh-token";
        String accessTokenStr = "expired-access-token";
        String userId = UUID.randomUUID().toString();

        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(refreshTokenStr)
                .build();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .usersId(userId)
                .refreshToken(refreshTokenStr)
                .build();

        log.info("리프레시 토큰은 유효하지만 액세스 토큰은 만료되었다고 설정합니다.");
        given(refreshTokenRepository.findByRefreshToken(refreshTokenStr))
                .willReturn(Optional.of(refreshTokenEntity));

        given(jwtTokenProvider.getRemainingValidTime(accessTokenStr))
                .willReturn(0L);
        // when
        log.info("로그아웃 서비스를 호출합니다.");
        authService.logout(request, accessTokenStr);

        // then
        log.info("리프레시 토큰 삭제는 호출되지만 블랙리스트 등록은 호출되지 않았는지 검증합니다.");
        verify(refreshTokenRepository).delete(refreshTokenEntity);

        verify(redisTemplate, never()).opsForValue();
        log.info("만료된 토큰 로그아웃 테스트가 통과되었습니다.");
    }

    /**
     * 유효하지 않은(존재하지 않는) 리프레시 토큰으로 로그아웃을 시도하는 시나리오를 테스트합니다.
     * <p>
     * {@link AuthException} 예외가 발생해야 하며, 에러 코드는 {@link AuthErrorCode#TOKEN_NOT_FOUND}여야 합니다.
     */
    @Test
    @DisplayName("로그아웃 실패 - 존재하지 않는 리프레시 토큰")
    void logout_Fail_NotFoundRefreshToken() {
        log.info("존재하지 않는 리프레시 토큰으로 인한 로그아웃 실패 테스트를 시작합니다.");
        // given
        String invalidRefreshToken = "invalid-token";
        String accessTokenStr = "any-access-token";

        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(invalidRefreshToken)
                .build();

        log.info("DB에 해당 리프레시 토큰이 없다고 설정합니다.");
        given(refreshTokenRepository.findByRefreshToken(invalidRefreshToken))
                .willReturn(Optional.empty());

        // when & then
        log.info("로그아웃 호출 시 토큰 미발견 예외가 발생하는지 확인합니다.");
        assertThatThrownBy(() -> authService.logout(request, accessTokenStr))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.TOKEN_NOT_FOUND);

        verify(refreshTokenRepository, never()).delete(any());
        verify(redisTemplate, never()).opsForValue();
        log.info("리프레시 토큰 미발견 실패 테스트가 통과되었습니다.");
    }

    /**
     * 토큰 재발급 성공 시나리오를 테스트합니다.
     * <p>
     * 리프레시 토큰이 유효하고 Redis에 존재할 경우,
     * 기존 토큰을 삭제(RTR)하고 새로운 액세스/리프레시 토큰을 발급해야 합니다.
     */
    @Test
    @DisplayName("토큰 재발급 성공 - RTR 적용 및 신규 토큰 발급")
    void reissueToken_Success() {
        log.info("토큰 재발급 성공 테스트를 시작합니다.");
        // given
        String oldRefreshTokenStr = "valid-old-refresh-token";
        String newAccessTokenStr = "new-access-token";
        String newRefreshTokenStr = "new-refresh-token";
        String userId = UUID.randomUUID().toString();
        String role = "USER";

        ReissueRequest request =
                new ReissueRequest(oldRefreshTokenStr);

        RefreshToken oldTokenEntity = RefreshToken.builder()
                .usersId(userId)
                .refreshToken(oldRefreshTokenStr)
                .role(role)
                .build();

        log.info("리프레시 토큰이 유효하고 정상적으로 새 토큰이 발급된다고 설정합니다.");
        given(jwtTokenProvider.validateToken(oldRefreshTokenStr))
                .willReturn(true);
        given(refreshTokenRepository.findByRefreshToken(oldRefreshTokenStr))
                .willReturn(Optional.of(oldTokenEntity));
        given(jwtTokenProvider.createAccessToken(UUID.fromString(userId), role))
                .willReturn(newAccessTokenStr);
        given(jwtTokenProvider.createRefreshToken(UUID.fromString(userId)))
                .willReturn(newRefreshTokenStr);
        given(jwtTokenProvider.getAccessTokenValidityInMilliseconds())
                .willReturn(3600000L);

        // when
        log.info("토큰 재발급 서비스를 호출합니다.");
        TokenResponse response =
                authService.reissueToken(request);

        // then
        log.info("기존 토큰 삭제 및 새 토큰 저장, 그리고 응답값이 일치하는지 검증합니다.");
        verify(refreshTokenRepository).delete(oldTokenEntity);
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        assertThat(response.getAccessToken()).isEqualTo(newAccessTokenStr);
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshTokenStr);
        log.info("토큰 재발급 성공 테스트가 통과되었습니다.");
    }

    /**
     * 유효하지 않은 리프레시 토큰으로 재발급을 시도하는 시나리오를 테스트합니다.
     * <p>
     * 토큰 서명이 틀리거나 만료된 경우 {@link AuthException}(EXPIRED_REFRESH_TOKEN)이 발생해야 합니다.
     */
    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 Refresh Token")
    void reissueToken_Fail_InvalidToken() {
        log.info("유효하지 않은 토큰으로 인한 재발급 실패 테스트를 시작합니다.");
        // given
        String invalidToken = "invalid-token";
        ReissueRequest request =
                new ReissueRequest(invalidToken);

        log.info("토큰 검증 결과가 실패라고 설정합니다.");
        given(jwtTokenProvider.validateToken(invalidToken))
                .willReturn(false);

        // when & then
        log.info("재발급 호출 시 만료된 토큰 예외가 발생하는지 확인합니다.");
        assertThatThrownBy(() -> authService.reissueToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.EXPIRED_REFRESH_TOKEN);

        verify(refreshTokenRepository, never()).delete(any());
        log.info("유효하지 않은 토큰 실패 테스트가 통과되었습니다.");
    }

    /**
     * Redis에 저장되어 있지 않은 리프레시 토큰으로 재발급을 시도하는 시나리오를 테스트합니다.
     * <p>
     * 토큰 형식은 유효하지만 DB에 없는 경우(이미 로그아웃됨 등),
     * {@link AuthException}(TOKEN_NOT_FOUND)이 발생해야 합니다.
     */
    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 존재하지 않는 Refresh Token")
    void reissueToken_Fail_NotFoundInRedis() {
        log.info("Redis 미발견으로 인한 재발급 실패 테스트를 시작합니다.");
        // given
        String notFoundToken = "valid-format-token";
        ReissueRequest request =
                new ReissueRequest(notFoundToken);

        log.info("토큰 형식은 맞지만 DB 조회 결과가 없다고 설정합니다.");
        given(jwtTokenProvider.validateToken(notFoundToken))
                .willReturn(true);
        given(refreshTokenRepository.findByRefreshToken(notFoundToken))
                .willReturn(Optional.empty());

        // when & then
        log.info("재발급 호출 시 토큰 미발견 예외가 발생하는지 확인합니다.");
        assertThatThrownBy(() -> authService.reissueToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.TOKEN_NOT_FOUND);

        verify(refreshTokenRepository, never()).delete(any());
        log.info("Redis 미발견 실패 테스트가 통과되었습니다.");
    }

    /**
     * 장치 로그인 성공 시나리오를 테스트합니다.
     * <p>
     * 유효한 Device ID로 요청 시, PetService를 통해 Pet ID를 조회하고
     * 정상적으로 토큰을 발급하여 Redis에 저장해야 합니다.
     */
    @Test
    @DisplayName("장치 로그인 성공 - 펫 ID 반환 및 토큰 발급")
    void deviceLogin_Success() {
        log.info("장치 로그인 성공 테스트를 시작합니다.");
        // given
        String deviceId = "valid-device-123";
        Long petId = 100L;
        String accessToken = "device-access-token";
        String refreshToken = "device-refresh-token";
        String role = "ROLE_DEVICE";

        DeviceAuthRequest request = DeviceAuthRequest.builder()
                .deviceId(deviceId)
                .build();

        log.info("디바이스 ID로 펫 정보가 조회되고 토큰이 정상 발급된다고 설정합니다.");
        given(petService.findPetIdByDeviceId(deviceId))
                .willReturn(Optional.of(petId));

        given(jwtTokenProvider.createAccessToken(any(UUID.class), eq(role)))
                .willReturn(accessToken);
        given(jwtTokenProvider.createRefreshToken(any(UUID.class)))
                .willReturn(refreshToken);
        given(jwtTokenProvider.getAccessTokenValidityInMilliseconds())
                .willReturn(3600000L);

        // when
        log.info("장치 로그인 서비스를 호출합니다.");
        DeviceAuthResponse response = authService.deviceLogin(request);

        // then
        log.info("응답 토큰과 펫 ID가 일치하는지 확인하고 Redis 저장이 수행되었는지 검증합니다.");
        assertThat(response.getAccessToken()).isEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(response.getPetId()).isEqualTo(petId);
        assertThat(response.getMessage()).isEqualTo("로그인이 완료되었습니다.");

        verify(petService).findPetIdByDeviceId(deviceId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        log.info("장치 로그인 성공 테스트가 통과되었습니다.");
    }

    /**
     * 등록되지 않은 디바이스로 로그인을 시도하는 시나리오를 테스트합니다.
     * <p>
     * PetService 조회 결과가 없을 경우 {@link AuthException}(DEVICE_NOT_FOUND)가 발생해야 합니다.
     */
    @Test
    @DisplayName("장치 로그인 실패 - 등록되지 않은 디바이스")
    void deviceLogin_Fail_DeviceNotFound() {
        log.info("등록되지 않은 디바이스 로그인 실패 테스트를 시작합니다.");
        // given
        String unknownDeviceId = "unknown-device";
        DeviceAuthRequest request = DeviceAuthRequest.builder()
                .deviceId(unknownDeviceId)
                .build();

        log.info("디바이스 ID 조회 결과가 없다고 설정합니다.");
        given(petService.findPetIdByDeviceId(unknownDeviceId))
                .willReturn(Optional.empty());

        // when & then
        log.info("로그인 호출 시 디바이스 미발견 예외가 발생하는지 확인합니다.");
        assertThatThrownBy(() -> authService.deviceLogin(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.DEVICE_NOT_FOUND);

        verify(petService).findPetIdByDeviceId(unknownDeviceId);
        verify(refreshTokenRepository, never()).save(any());
        log.info("등록되지 않은 디바이스 실패 테스트가 통과되었습니다.");
    }

    /**
     * 장치 토큰 재발급 성공 시나리오를 테스트합니다.
     * <p>
     * 1. 토큰이 유효하고 Redis에 존재해야 합니다.
     * 2. 토큰의 권한이 ROLE_DEVICE여야 합니다.
     * 3. RTR이 적용되어 기존 토큰은 삭제되고 신규 토큰이 저장되어야 합니다.
     */
    @Test
    @DisplayName("장치 토큰 재발급 성공 - Role 검증 및 RTR 적용")
    void deviceReissueToken_Success() {
        log.info("장치 토큰 재발급 성공 테스트를 시작합니다.");
        // given
        String oldRefreshTokenStr = "device-old-refresh-token";
        String newAccessTokenStr = "device-new-access-token";
        String newRefreshTokenStr = "device-new-refresh-token";
        String deviceUuid = UUID.randomUUID().toString();
        String role = "ROLE_DEVICE";

        ReissueRequest request = new ReissueRequest(oldRefreshTokenStr);

        RefreshToken oldTokenEntity = RefreshToken.builder()
                .usersId(deviceUuid)
                .refreshToken(oldRefreshTokenStr)
                .role(role)
                .build();

        log.info("리프레시 토큰이 유효하고 권한이 장치 권한이라고 설정합니다.");
        given(jwtTokenProvider.validateToken(oldRefreshTokenStr))
                .willReturn(true);
        given(refreshTokenRepository.findByRefreshToken(oldRefreshTokenStr))
                .willReturn(Optional.of(oldTokenEntity));

        given(jwtTokenProvider.createAccessToken(UUID.fromString(deviceUuid), role))
                .willReturn(newAccessTokenStr);
        given(jwtTokenProvider.createRefreshToken(UUID.fromString(deviceUuid)))
                .willReturn(newRefreshTokenStr);
        given(jwtTokenProvider.getAccessTokenValidityInMilliseconds())
                .willReturn(3600000L);

        // when
        log.info("장치 재발급 서비스를 호출합니다.");
        TokenResponse response = authService.deviceReissueToken(request);

        // then
        log.info("기존 토큰 삭제, 신규 토큰 저장, 그리고 ROLE_DEVICE 권한 유지가 되었는지 검증합니다.");
        verify(refreshTokenRepository).delete(oldTokenEntity);
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        assertThat(response.getAccessToken()).isEqualTo(newAccessTokenStr);
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshTokenStr);
        log.info("장치 토큰 재발급 성공 테스트가 통과되었습니다.");
    }

    /**
     * 장치 권한이 아닌 토큰(예: 일반 유저)으로 장치 재발급을 시도하는 시나리오를 테스트합니다.
     * <p>
     * {@link AuthException}(INVALID_REQUEST)가 발생해야 합니다.
     */
    @Test
    @DisplayName("장치 토큰 재발급 실패 - 장치 권한이 아닌 토큰")
    void deviceReissueToken_Fail_InvalidRole() {
        log.info("일반 유저 토큰으로 장치 재발급 시도 실패 테스트를 시작합니다.");
        // given
        String userRefreshToken = "user-refresh-token";
        String userId = UUID.randomUUID().toString();
        String role = "USER";

        ReissueRequest request = new ReissueRequest(userRefreshToken);

        RefreshToken userTokenEntity = RefreshToken.builder()
                .usersId(userId)
                .refreshToken(userRefreshToken)
                .role(role)
                .build();

        log.info("토큰은 유효하지만 권한이 USER라고 설정합니다.");
        given(jwtTokenProvider.validateToken(userRefreshToken))
                .willReturn(true);
        given(refreshTokenRepository.findByRefreshToken(userRefreshToken))
                .willReturn(Optional.of(userTokenEntity));

        // when & then
        log.info("재발급 호출 시 잘못된 요청 예외가 발생하는지 확인합니다.");
        assertThatThrownBy(() -> authService.deviceReissueToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_REQUEST);

        verify(refreshTokenRepository, never()).delete(any());
        verify(refreshTokenRepository, never()).save(any());
        log.info("권한 불일치 실패 테스트가 통과되었습니다.");
    }

    /**
     * 유효하지 않은 토큰으로 장치 재발급을 시도하는 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("장치 토큰 재발급 실패 - 유효하지 않은 Refresh Token")
    void deviceReissueToken_Fail_InvalidToken() {
        log.info("유효하지 않은 토큰으로 장치 재발급 실패 테스트를 시작합니다.");
        // given
        String invalidToken = "invalid-token";
        ReissueRequest request =
                new ReissueRequest(invalidToken);

        given(jwtTokenProvider.validateToken(invalidToken))
                .willReturn(false);

        // when & then
        log.info("재발급 호출 시 만료된 토큰 예외가 발생하는지 확인합니다.");
        assertThatThrownBy(() -> authService.deviceReissueToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.EXPIRED_REFRESH_TOKEN);

        log.info("유효하지 않은 토큰 실패 테스트가 통과되었습니다.");
    }
}
