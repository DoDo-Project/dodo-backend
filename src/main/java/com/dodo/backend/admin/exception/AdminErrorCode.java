package com.dodo.backend.admin.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 관리자 도메인에서 발생하는 예외 상황을 관리하는 에러 코드입니다.
 */
@Getter
@AllArgsConstructor
public enum AdminErrorCode implements BaseErrorCode {

    /**
     * 요청 값이 올바르지 않은 경우 사용합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 신고당한 게시글이 존재하지 않는 경우 사용합니다.
     */
    REPORTED_BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "신고당한 게시글이 존재하지 않습니다."),

    /**
     * 신고당한 유저가 존재하지 않는 경우 사용합니다.
     */
    REPORTED_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "신고당한 유저가 존재하지 않습니다."),

    /**
     * 신고당한 댓글이 존재하지 않는 경우 사용합니다.
     */
    REPORTED_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고당한 댓글이 존재하지 않습니다."),

    /**
     * 유저가 존재하지 않는 경우 사용합니다.
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),

    /**
     * 게시글이 존재하지 않는 경우 사용합니다.
     */
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),

    /**
     * 댓글이 존재하지 않는 경우 사용합니다.
     */
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),

    /**
     * 신고 내역이 존재하지 않는 경우 사용합니다.
     */
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 신고 내역입니다."),

    /**
     * 공지가 존재하지 않는 경우 사용합니다.
     */
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공지입니다."),

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
