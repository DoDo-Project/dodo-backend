package com.dodo.backend.auth.service;

import com.dodo.backend.auth.client.SocialApiClient;
import com.dodo.backend.auth.dto.request.AuthRequest.AdminLoginRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.LogoutRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.SocialLoginRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.DeviceAuthRequest;
import com.dodo.backend.auth.dto.response.AuthResponse.AdminLoginResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialLoginResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.SocialRegisterResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.TokenResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.DeviceAuthResponse;
import com.dodo.backend.auth.entity.RefreshToken;
import com.dodo.backend.auth.exception.AuthException;
import com.dodo.backend.auth.repository.RefreshTokenRepository;
import com.dodo.backend.common.jwt.JwtTokenProvider;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.repository.UserRepository;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.dodo.backend.auth.exception.AuthErrorCode.*;

/**
 * {@link AuthService}의 구현체로, 다양한 인증 방식(소셜 로그인, 장치 인증 등)과 토큰 관리 로직을 수행합니다.
 * <p>
 * 전략 패턴을 통한 소셜 로그인 처리, JWT 기반의 토큰 발급 및 재발급(RTR),
 * 그리고 Redis를 활용한 토큰 저장 및 블랙리스트 관리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final PetService petService;
    private final RateLimitService rateLimitService;
    private final List<SocialApiClient> socialApiClients;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${admin.login.email:}")
    private String adminLoginEmail;

    @Value("${admin.login.password:}")
    private String adminLoginPassword;

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>요청된 Provider(NAVER, GOOGLE)를 지원하는 {@link SocialApiClient} 구현체를 동적으로 선택합니다. (지원 불가 시 {@code INVALID_REQUEST} 예외)</li>
     * <li>선택된 클라이언트를 통해 인가 코드로 AccessToken을 발급받고, 유저 프로필 정보를 조회합니다.</li>
     * <li>이메일을 기준으로 {@link UserService}를 통해 유저 정보를 조회하거나 신규 유저 데이터를 생성합니다.</li>
     * <li>유저의 상태(status)를 검증하여 정지/탈퇴 계정일 경우 {@code ACCOUNT_RESTRICTED} 예외를 발생시킵니다.</li>
     * <li>신규 회원인 경우: 회원가입용 임시 토큰을 발급하고 {@code 202 Accepted}를 반환합니다.</li>
     * <li>기존 회원인 경우: 서비스 이용을 위한 Access/Refresh Token을 발급하고 Redis에 저장한 뒤 {@code 200 OK}를 반환합니다.</li>
     * </ol>
     */
    @Override
    public ResponseEntity<?> socialLogin(SocialLoginRequest request) {

        SocialApiClient client = socialApiClients.stream()
                .filter(c -> c.support(request.getProvider()))
                .findFirst()
                .orElseThrow(() -> new AuthException(INVALID_REQUEST));

        String socialAccessToken = client.getAccessToken(request.getCode());
        Map<String, Object> profile = client.getUserProfile(socialAccessToken);

        Map<String, Object> userInfo = userService.findOrSaveSocialUser(
                (String) profile.get("email"),
                (String) profile.get("name"),
                (String) profile.get("profileUrl")
        );

        String status = String.valueOf(userInfo.get("status"));
        validateUserStatus(status, (String) profile.get("email"));

        boolean isNewMember = (boolean) userInfo.get("isNewMember");

        if (isNewMember) {
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String registrationToken = jwtTokenProvider.createRegisterToken(email);

            return ResponseEntity.status(202).body(
                    SocialRegisterResponse.toDto(
                            "추가 정보가 필요합니다.",
                            email,
                            name,
                            registrationToken,
                            jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 2000
                    )
            );
        } else {
            UUID userId = (UUID) userInfo.get("userId");
            String role = (String) userInfo.get("role");
            String profileUrl = (String) userInfo.get("profileUrl");

            String accessToken = jwtTokenProvider.createAccessToken(userId, role);
            String refreshToken = jwtTokenProvider.createRefreshToken(userId);

            refreshTokenRepository.save(RefreshToken.builder()
                    .usersId(userId.toString())
                    .refreshToken(refreshToken)
                    .role(role)
                    .build());

            return ResponseEntity.ok(
                    SocialLoginResponse.toDto(
                            "로그인이 완료되었습니다.",
                            profileUrl,
                            accessToken,
                            refreshToken,
                            jwtTokenProvider.getAccessTokenValidityInMilliseconds()
                    )
            );
        }
    }

    @Transactional
    @Override
    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        if (!StringUtils.hasText(adminLoginPassword)
                || !adminLoginPassword.equals(request.getPassword())
                || (StringUtils.hasText(adminLoginEmail) && !adminLoginEmail.equals(request.getEmail()))) {
            throw new AuthException(LOGIN_FAILED);
        }

        User admin = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException(LOGIN_FAILED));

        validateUserStatus(admin.getUserStatus().name(), admin.getEmail());

        if (admin.getRole() != UserRole.ADMIN) {
            throw new AuthException(LOGIN_FAILED);
        }

        String role = admin.getRole().name();
        String accessToken = jwtTokenProvider.createAccessToken(admin.getUsersId(), role);
        String refreshToken = jwtTokenProvider.createRefreshToken(admin.getUsersId());

        refreshTokenRepository.save(RefreshToken.builder()
                .usersId(admin.getUsersId().toString())
                .refreshToken(refreshToken)
                .role(role)
                .build());

        return AdminLoginResponse.toDto(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenValidityInMilliseconds(),
                role
        );
    }

    /**
     * 유저의 계정 상태가 로그인 가능한 상태인지 검증합니다.
     * <p>
     * 문자열 비교를 통해 정지(SUSPENDED), 휴면(DORMANT), 삭제(DELETED) 상태일 경우
     * {@link AuthException} (ACCOUNT_RESTRICTED)을 발생시킵니다.
     *
     * @param status 유저의 현재 상태 문자열
     * @param email  로그인을 시도하는 이메일 (로깅 및 식별용)
     */
    private void validateUserStatus(String status, String email) {
        if ("SUSPENDED".equals(status)
                || "DORMANT".equals(status)
                || "DELETED".equals(status)) {
            log.warn("계정 사용 제한 유저 접속 시도 - 이메일: {}, 상태: {}", email, status);
            throw new AuthException(ACCOUNT_RESTRICTED);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>{@link RateLimitService}를 통해 해당 클라이언트 IP의 차단(Ban) 여부를 우선 확인합니다.</li>
     * <li>이미 차단된 IP일 경우 즉시 {@code TOO_MANY_REQUESTS} 예외를 발생시켜 접근을 거부합니다.</li>
     * <li>차단되지 않은 경우 요청 횟수를 1 증가시킵니다.</li>
     * <li>요청 횟수가 임계치(5회)에 도달하면 해당 IP를 10분간 차단하고, 기존 카운트 기록을 초기화한 후 예외를 발생시킵니다.</li>
     * </ol>
     */
    @Override
    public void checkRateLimit(String clientIp) {

        if (rateLimitService.isIpBanned(clientIp)) {
            log.warn("차단된 IP의 접근 시도 차단 - IP: {}", clientIp);
            throw new AuthException(TOO_MANY_REQUESTS);
        }

        Long count = rateLimitService.incrementAttempt(clientIp, 1);

        if (count != null && count >= 5) {
            log.warn("IP 차단 실행 (5회 초과) - IP: {}, 기간: 10분", clientIp);
            rateLimitService.banIp(clientIp, 10);
            rateLimitService.deleteAttempts(clientIp);
            throw new AuthException(TOO_MANY_REQUESTS);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>요청받은 Refresh Token을 사용하여 Redis에 저장된 토큰 정보를 조회합니다. (없을 시 {@code TOKEN_NOT_FOUND} 예외)</li>
     * <li>조회된 Refresh Token 데이터를 Redis에서 삭제하여 더 이상 재발급에 사용할 수 없도록 만듭니다.</li>
     * <li>현재 사용 중인 Access Token의 남은 유효 시간을 계산합니다.</li>
     * <li>남은 시간이 있다면 Redis 블랙리스트에 해당 Access Token을 등록하여 접근을 차단합니다.</li>
     * </ol>
     */
    @Transactional
    @Override
    public void logout(LogoutRequest request, String accessToken) {

        log.info("로그아웃 요청 수신");

        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException(TOKEN_NOT_FOUND));
        refreshTokenRepository.delete(refreshToken);

        long expiration = jwtTokenProvider.getRemainingValidTime(accessToken);
        if (expiration > 0) {
            redisTemplate.opsForValue()
                    .set("blacklist:" + accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
            log.info("Access Token 블랙리스트 등록 완료 (남은 시간: {}ms)", expiration);
        }

        log.info("로그아웃 완료 - User ID: {}", refreshToken.getUsersId());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>전달받은 Refresh Token의 유효성(서명, 만료 여부)을 JWT 자체 검증 로직으로 확인합니다. (실패 시 {@code EXPIRED_REFRESH_TOKEN} 예외)</li>
     * <li>Redis를 조회하여 해당 토큰이 실제로 저장되어 있고 유효한지 확인합니다. (없을 시 {@code TOKEN_NOT_FOUND} 예외)</li>
     * <li><b>RTR(Refresh Token Rotation):</b> 보안을 위해 기존 Refresh Token을 삭제합니다.</li>
     * <li>동일한 유저 정보(ID, Role)로 새로운 Access Token과 Refresh Token을 생성합니다.</li>
     * <li>새로운 Refresh Token을 Redis에 저장하고, 갱신된 토큰 정보를 반환합니다.</li>
     * </ol>
     */
    @Transactional
    @Override
    public TokenResponse reissueToken(ReissueRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            log.warn("재발급 실패: 유효하지 않거나 만료된 Refresh Token입니다.");
            throw new AuthException(EXPIRED_REFRESH_TOKEN);
        }

        RefreshToken storedToken = refreshTokenRepository.findByRefreshToken(requestRefreshToken)
                .orElseThrow(() -> {
                    log.warn("재발급 실패: Redis에서 토큰을 찾을 수 없습니다.");
                    return new AuthException(TOKEN_NOT_FOUND);
                });

        refreshTokenRepository.delete(storedToken);

        UUID userId = UUID.fromString(storedToken.getUsersId());
        String role = storedToken.getRole();

        String newAccessToken = jwtTokenProvider.createAccessToken(userId, role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenRepository.save(RefreshToken.builder()
                .usersId(userId.toString())
                .refreshToken(newRefreshToken)
                .role(role)
                .build());

        log.info("토큰 재발급 성공 - User ID: {}", userId);

        long expiresInSeconds = jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000;

        return TokenResponse.toDto(newAccessToken, newRefreshToken, expiresInSeconds, "성공적으로 토큰이 재발급되었습니다.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>{@link PetService#findPetIdByDeviceId}를 호출하여 디바이스 ID와 매핑된 펫 ID를 조회합니다.</li>
     * <li>조회 결과가 없을 경우 {@code DEVICE_NOT_FOUND} 예외를 발생시켜 로그인을 실패 처리합니다.</li>
     * <li>획득한 펫 ID를 기반으로 고유 UUID를 생성하고, 권한을 `ROLE_DEVICE`로 설정하여 AccessToken을 발급합니다.</li>
     * <li>Refresh Token을 생성하고 Redis에 저장합니다. (User ID 필드에 디바이스 UUID 저장)</li>
     * <li>생성된 토큰들과 펫 ID 정보를 응답 객체에 담아 반환합니다.</li>
     * </ol>
     */
    @Transactional
    @Override
    public DeviceAuthResponse deviceLogin(DeviceAuthRequest request) {

        String deviceId = request.getDeviceId();

        Long petId = petService.findPetIdByDeviceId(deviceId)
                .orElseThrow(() -> {
                    log.warn("장치 로그인 실패 - 등록되지 않은 디바이스 ID: {}", deviceId);
                    return new AuthException(DEVICE_NOT_FOUND);
                });

        UUID deviceUuid = UUID.nameUUIDFromBytes(("DEVICE:" + petId).getBytes());
        String role = "ROLE_DEVICE";

        String accessToken = jwtTokenProvider.createAccessToken(deviceUuid, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(deviceUuid);

        refreshTokenRepository.save(RefreshToken.builder()
                .usersId(deviceUuid.toString())
                .refreshToken(refreshToken)
                .role(role)
                .build());

        log.info("장치 로그인 성공 - Device ID: {}, Pet ID: {}", deviceId, petId);

        long expiresInSeconds = jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000;

        return DeviceAuthResponse.toDto(
                "로그인이 완료되었습니다.",
                accessToken,
                refreshToken,
                expiresInSeconds,
                petId
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * <ol>
     * <li>헤더에서 추출한 Refresh Token의 서명 및 만료 여부를 검증합니다.</li>
     * <li>Redis에서 해당 토큰이 존재하는지 확인합니다. (없을 경우 예외 발생)</li>
     * <li>저장된 토큰의 권한이 <b>ROLE_DEVICE</b>인지 확인하여 장치 토큰임을 보장합니다.</li>
     * <li><b>RTR 적용:</b> 기존 토큰을 삭제하고 새로운 장치용 토큰 쌍을 생성하여 저장합니다.</li>
     * </ol>
     */
    @Transactional
    @Override
    public TokenResponse deviceReissueToken(ReissueRequest request) {

        String requestRefreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(requestRefreshToken)) {
            log.warn("장치 재발급 실패: 유효하지 않거나 만료된 Refresh Token입니다.");
            throw new AuthException(EXPIRED_REFRESH_TOKEN);
        }

        RefreshToken storedToken = refreshTokenRepository.findByRefreshToken(requestRefreshToken)
                .orElseThrow(() -> {
                    log.warn("장치 재발급 실패: Redis에서 토큰을 찾을 수 없습니다.");
                    return new AuthException(TOKEN_NOT_FOUND);
                });

        if (!"ROLE_DEVICE".equals(storedToken.getRole())) {
            log.warn("장치 재발급 실패: 장치 권한이 아닌 토큰입니다. Role: {}", storedToken.getRole());
            throw new AuthException(INVALID_REQUEST);
        }

        refreshTokenRepository.delete(storedToken);

        UUID deviceUuid = UUID.fromString(storedToken.getUsersId());
        String role = storedToken.getRole();

        String newAccessToken = jwtTokenProvider.createAccessToken(deviceUuid, role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(deviceUuid);

        refreshTokenRepository.save(RefreshToken.builder()
                .usersId(deviceUuid.toString())
                .refreshToken(newRefreshToken)
                .role(role)
                .build());

        log.info("장치 토큰 재발급 성공 - Device UUID: {}", deviceUuid);

        long expiresInSeconds = jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000;

        return TokenResponse.toDto(newAccessToken, newRefreshToken, expiresInSeconds, "성공적으로 토큰이 재발급되었습니다.");
    }
}
