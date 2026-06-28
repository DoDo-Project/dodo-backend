package com.dodo.backend.fcmtoken.service;

import com.dodo.backend.fcmtoken.dto.request.FcmTokenRequest.FcmTokenRegisterRequest;
import com.dodo.backend.fcmtoken.dto.response.FcmTokenResponse.FcmTokenSimpleResponse;
import com.dodo.backend.fcmtoken.entity.DeviceType;
import com.dodo.backend.fcmtoken.entity.FcmToken;
import com.dodo.backend.fcmtoken.exception.FcmTokenException;
import com.dodo.backend.fcmtoken.repository.FcmTokenRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FCM 토큰 서비스 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FcmTokenServiceImpl fcmTokenService;

    /**
     * 신규 FCM 토큰 등록 시 저장 메서드가 호출되는지 검증합니다.
     */
    @Test
    @DisplayName("FCM 토큰 신규 등록 성공")
    void registerToken_CreateSuccess() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        FcmTokenRegisterRequest request = new FcmTokenRegisterRequest("token-value", DeviceType.ANDROID, "Galaxy");

        given(userService.getUserById(userId)).willReturn(user);
        given(fcmTokenRepository.findByToken("token-value")).willReturn(Optional.empty());

        FcmTokenSimpleResponse response = fcmTokenService.registerToken(userId, request);

        assertEquals("푸시 토큰이 성공적으로 등록되었습니다.", response.getMessage());
        verify(fcmTokenRepository).save(any(FcmToken.class));
    }

    /**
     * 기존 FCM 토큰 등록 시 장치 정보가 갱신되는지 검증합니다.
     */
    @Test
    @DisplayName("FCM 토큰 갱신 성공")
    void registerToken_UpdateSuccess() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        FcmToken fcmToken = FcmToken.builder()
                .user(user)
                .token("token-value")
                .deviceType(DeviceType.IOS)
                .deviceName("iPhone")
                .build();
        FcmTokenRegisterRequest request = new FcmTokenRegisterRequest("token-value", DeviceType.ANDROID, "Galaxy");

        given(userService.getUserById(userId)).willReturn(user);
        given(fcmTokenRepository.findByToken("token-value")).willReturn(Optional.of(fcmToken));

        fcmTokenService.registerToken(userId, request);

        assertEquals(DeviceType.ANDROID, fcmToken.getDeviceType());
        assertEquals("Galaxy", fcmToken.getDeviceName());
    }

    /**
     * FCM 토큰 삭제 시 로그인 사용자의 토큰만 삭제되는지 검증합니다.
     */
    @Test
    @DisplayName("FCM 토큰 삭제 성공")
    void deleteToken_Success() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        FcmToken fcmToken = FcmToken.builder()
                .user(user)
                .token("token-value")
                .deviceType(DeviceType.ANDROID)
                .build();

        given(userService.getUserById(userId)).willReturn(user);
        given(fcmTokenRepository.findByTokenAndUser("token-value", user)).willReturn(Optional.of(fcmToken));

        fcmTokenService.deleteToken(userId, "token-value");

        verify(fcmTokenRepository).delete(fcmToken);
    }

    /**
     * 존재하지 않는 FCM 토큰 삭제 시 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("FCM 토큰 삭제 실패 - 토큰 없음")
    void deleteToken_NotFound() {
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);

        given(userService.getUserById(userId)).willReturn(user);
        given(fcmTokenRepository.findByTokenAndUser("missing-token", user)).willReturn(Optional.empty());

        assertThrows(FcmTokenException.class, () -> fcmTokenService.deleteToken(userId, "missing-token"));
    }

    /**
     * 필수 값이 없는 FCM 토큰 등록 요청 시 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("FCM 토큰 등록 실패 - 잘못된 요청")
    void registerToken_InvalidRequest() {
        UUID userId = UUID.randomUUID();
        FcmTokenRegisterRequest request = new FcmTokenRegisterRequest("", DeviceType.ANDROID, "Galaxy");

        assertThrows(FcmTokenException.class, () -> fcmTokenService.registerToken(userId, request));
    }

    private User createUser(UUID userId) {
        return User.builder()
                .usersId(userId)
                .nickname("테스터")
                .build();
    }
}
