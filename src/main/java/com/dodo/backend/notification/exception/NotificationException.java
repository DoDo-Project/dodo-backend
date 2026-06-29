package com.dodo.backend.notification.exception;

import lombok.Getter;

/**
 * 알림 도메인 비즈니스 예외입니다.
 */
@Getter
public class NotificationException extends RuntimeException {

    private final NotificationErrorCode errorCode;

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
