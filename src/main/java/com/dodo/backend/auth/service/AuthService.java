package com.dodo.backend.auth.service;

import com.dodo.backend.auth.dto.request.AuthRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.AdminLoginRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.DeviceAuthRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.LogoutRequest;
import com.dodo.backend.auth.dto.request.AuthRequest.ReissueRequest;
import com.dodo.backend.auth.dto.response.AuthResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.AdminLoginResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.DeviceAuthResponse;
import com.dodo.backend.auth.dto.response.AuthResponse.TokenResponse;
import org.springframework.http.ResponseEntity;

/**
 * 소셜 로그인 및 인증 토큰 발급을 담당하는 서비스 인터페이스입니다.
 * <p>
 * 다양한 소셜 인증 제공자로부터 전달받은 인가 정보를 처리하고,
 * 시스템 내부 권한 체계에 맞는 토큰을 생성하여 반환하는 기능을 정의합니다.
 */
public interface AuthService {

    /**
     * 소셜 로그인 인증을 수행하고 결과에 따라 로그인 또는 회원가입 응답을 반환합니다.
     *
     * @param request provider(GOOGLE/NAVER)와 인가 코드(code)를 포함한 요청 객체
     * @return
     * - 기존 회원: 200 OK + Access/Refresh Token <br>
     * - 신규 회원: 202 Accepted + Registration Token (이메일, 이름 포함)
     */
    ResponseEntity<?> socialLogin(AuthRequest.SocialLoginRequest request);

    AdminLoginResponse adminLogin(AdminLoginRequest request);

    /**
     * 클라이언트 IP를 기반으로 요청 횟수를 검증하여 비정상적인 접근을 제한합니다.
     * <p>
     * 단시간 내에 발생하는 과도한 요청을 감지하고, 시스템 보안 정책에 따라
     * 일정 시간 동안 해당 IP의 서비스 이용을 차단합니다.
     *
     * @param clientIp 요청을 보낸 클라이언트의 IP 주소
     * @throws com.dodo.backend.auth.exception.AuthException 요청 횟수가 임계치를 초과했거나 차단된 IP일 경우 발생
     */
    void checkRateLimit(String clientIp);

    /**
     * 로그아웃을 수행합니다.
     * Refresh Token 삭제 및 Access Token 블랙리스트 처리를 수행합니다.
     *
     * @param request 로그아웃할 리프레시 토큰 DTO
     * @param accessToken 현재 사용 중인 액세스 토큰 (Header에서 추출)
     */
    void logout(LogoutRequest request, String accessToken);

    /**
     * 리프레시 토큰을 검증하고 새로운 액세스 토큰과 리프레시 토큰을 발급합니다.
     * <p>
     * 보안 강화를 위해 <b>RTR(Refresh Token Rotation)</b> 방식을 사용하여,
     * 재발급 요청 시 기존 리프레시 토큰은 파기되고 새로운 리프레시 토큰이 발급됩니다.
     *
     * @param request 유효한 리프레시 토큰이 담긴 요청 객체
     * @return 새로 발급된 토큰 쌍(Access/Refresh)과 만료 시간을 담은 응답 DTO
     * @throws com.dodo.backend.auth.exception.AuthException 토큰이 유효하지 않거나 만료된 경우, 또는 Redis에 존재하지 않는 경우
     */
    TokenResponse reissueToken(ReissueRequest request);

    /**
     * 장치(임베디드) 로그인을 수행합니다.
     * <p>
     * 디바이스 ID를 기반으로 등록된 반려동물을 조회하고,
     * 해당 디바이스 권한(ROLE_DEVICE)으로 토큰을 발급합니다.
     *
     * @param request 디바이스 ID 정보가 담긴 요청 객체
     * @return 토큰 및 연결된 펫 정보
     */
    DeviceAuthResponse deviceLogin(DeviceAuthRequest request);

    /**
     * 장치(Device) 전용 토큰 재발급을 수행합니다.
     * <p>
     * Authorization Header로 전달된 Refresh Token을 검증하고,
     * RTR(Refresh Token Rotation) 정책에 따라 새로운 Access/Refresh Token을 발급합니다.
     *
     * @param request 유효한 리프레시 토큰이 담긴 요청 객체
     * @return 새로 발급된 토큰 쌍(Access/Refresh)과 만료 시간을 담은 응답 DTO
     * @throws com.dodo.backend.auth.exception.AuthException 토큰이 유효하지 않거나 만료된 경우, 또는 Redis에 존재하지 않는 경우
     */
    TokenResponse deviceReissueToken(ReissueRequest request);
}
