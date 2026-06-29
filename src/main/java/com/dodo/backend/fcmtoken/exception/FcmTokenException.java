package com.dodo.backend.fcmtoken.exception;

import lombok.Getter;

/**
 * FCM 토큰 도메인 비즈니스 예외입니다.
 */
@Getter
public class FcmTokenException extends RuntimeException {

    private final FcmTokenErrorCode errorCode;

    public FcmTokenException(FcmTokenErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
