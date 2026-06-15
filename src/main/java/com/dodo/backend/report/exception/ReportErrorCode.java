package com.dodo.backend.report.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 신고 도메인에서 발생하는 예외 상황을 관리하는 에러 코드입니다.
 */
@AllArgsConstructor
@Getter
public enum ReportErrorCode implements BaseErrorCode {

    /**
     * 잘못된 신고 요청인 경우 사용합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 신고할 게시글을 찾을 수 없는 경우 사용합니다.
     */
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "신고할 게시글을 찾을 수 없습니다."),

    /**
     * 신고할 유저를 찾을 수 없는 경우 사용합니다.
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "신고할 유저를 찾을 수 없습니다."),

    /**
     * 신고할 댓글을 찾을 수 없는 경우 사용합니다.
     */
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고할 댓글을 찾을 수 없습니다."),

    /**
     * 이미 신고한 대상인 경우 사용합니다.
     */
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신고한 대상입니다."),

    /**
     * 서버 내부 오류가 발생한 경우 사용합니다.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    /**
     * HTTP 상태 코드입니다.
     */
    private final HttpStatus httpStatus;

    /**
     * 에러 응답 메시지입니다.
     */
    private final String message;
}
