package com.dodo.backend.fcmtoken.service;

import com.dodo.backend.fcmtoken.dto.request.FcmTokenRequest.FcmTokenRegisterRequest;
import com.dodo.backend.fcmtoken.dto.response.FcmTokenResponse.FcmTokenSimpleResponse;

import java.util.UUID;

/**
 * FCM 토큰 API 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface FcmTokenService {

    FcmTokenSimpleResponse registerToken(UUID userId, FcmTokenRegisterRequest request);

    void deleteToken(UUID userId, String token);
}
