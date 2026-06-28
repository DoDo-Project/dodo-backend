package com.dodo.backend.fcmtoken.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * FCM 토큰 도메인에서 발생하는 예외 상황을 관리하는 에러 코드입니다.
 */
@Getter
@AllArgsConstructor
public enum FcmTokenErrorCode implements BaseErrorCode {

    /**
     * 요청 값이 올바르지 않은 경우 사용합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 삭제할 토큰을 찾을 수 없는 경우 사용합니다.
     */
    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 토큰을 찾을 수 없습니다."),

    /**
     * 서버 내부 오류가 발생한 경우 사용합니다.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
