package com.dodo.backend.admin.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.Getter;

/**
 * 관리자 도메인 비즈니스 로직에서 발생하는 예외입니다.
 */
@Getter
public class AdminException extends RuntimeException {

    private final BaseErrorCode errorCode;

    /**
     * 관리자 도메인 예외를 생성합니다.
     *
     * @param errorCode 에러 코드
     */
    public AdminException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
