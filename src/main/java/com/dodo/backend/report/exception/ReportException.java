package com.dodo.backend.report.exception;

import lombok.Getter;

/**
 * 신고 도메인 비즈니스 로직에서 발생하는 예외입니다.
 */
@Getter
public class ReportException extends RuntimeException {

    private final ReportErrorCode errorCode;

    /**
     * 신고 도메인 예외를 생성합니다.
     *
     * @param errorCode 신고 에러 코드
     */
    public ReportException(ReportErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
