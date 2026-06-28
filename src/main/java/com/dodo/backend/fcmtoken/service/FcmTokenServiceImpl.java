package com.dodo.backend.fcmtoken.service;

import com.dodo.backend.fcmtoken.dto.request.FcmTokenRequest.FcmTokenRegisterRequest;
import com.dodo.backend.fcmtoken.dto.response.FcmTokenResponse.FcmTokenSimpleResponse;
import com.dodo.backend.fcmtoken.entity.FcmToken;
import com.dodo.backend.fcmtoken.exception.FcmTokenException;
import com.dodo.backend.fcmtoken.repository.FcmTokenRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.dodo.backend.fcmtoken.exception.FcmTokenErrorCode.FCM_TOKEN_NOT_FOUND;
import static com.dodo.backend.fcmtoken.exception.FcmTokenErrorCode.INVALID_REQUEST;

@Service
@RequiredArgsConstructor
public class FcmTokenServiceImpl implements FcmTokenService {

    private static final String REGISTER_SUCCESS_MESSAGE = "푸시 토큰이 성공적으로 등록되었습니다.";

    private final FcmTokenRepository fcmTokenRepository;
    private final UserService userService;

    @Transactional
    @Override
    public FcmTokenSimpleResponse registerToken(UUID userId, FcmTokenRegisterRequest request) {
        validateRegisterRequest(userId, request);
        User user = findUser(userId);

        FcmToken fcmToken = fcmTokenRepository.findByToken(request.getToken())
                .map(existingToken -> {
                    existingToken.updateTokenInfo(user, request.getDeviceType(), normalizeDeviceName(request.getDeviceName()));
                    return existingToken;
                })
                .orElseGet(() -> FcmToken.builder()
                        .user(user)
                        .token(request.getToken())
                        .deviceType(request.getDeviceType())
                        .deviceName(normalizeDeviceName(request.getDeviceName()))
                        .build());

        fcmTokenRepository.save(fcmToken);
        return FcmTokenSimpleResponse.toDto(REGISTER_SUCCESS_MESSAGE);
    }

    @Transactional
    @Override
    public void deleteToken(UUID userId, String token) {
        if (userId == null || token == null || token.isBlank()) {
            throw new FcmTokenException(INVALID_REQUEST);
        }
        User user = findUser(userId);
        FcmToken fcmToken = fcmTokenRepository.findByTokenAndUser(token, user)
                .orElseThrow(() -> new FcmTokenException(FCM_TOKEN_NOT_FOUND));

        fcmTokenRepository.delete(fcmToken);
    }

    private void validateRegisterRequest(UUID userId, FcmTokenRegisterRequest request) {
        if (userId == null || request == null || request.getToken() == null || request.getToken().isBlank() || request.getDeviceType() == null) {
            throw new FcmTokenException(INVALID_REQUEST);
        }
        if (request.getToken().length() > 512 || (request.getDeviceName() != null && request.getDeviceName().length() > 100)) {
            throw new FcmTokenException(INVALID_REQUEST);
        }
    }

    private User findUser(UUID userId) {
        try {
            return userService.getUserById(userId);
        } catch (UserException e) {
            throw new FcmTokenException(INVALID_REQUEST);
        }
    }

    private String normalizeDeviceName(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            return null;
        }
        return deviceName;
    }
}
