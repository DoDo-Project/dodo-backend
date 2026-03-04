package com.dodo.backend.reaction.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 반응(Reaction) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 열거형입니다.
 * <p>
 * 활동 반응 및 게시물 반응의 등록/취소 과정에서 발생하는 오류를
 * HTTP 상태 코드와 클라이언트 응답 메시지로 함께 관리합니다.
 */
@AllArgsConstructor
@Getter
public enum ReactionErrorCode implements BaseErrorCode {

    /**
     * 클라이언트의 요청 형식이 잘못되었거나 파라미터가 유효하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 인증되지 않은 사용자가 로그인 필요한 기능을 요청할 때 사용합니다.
     * <p>
     * HTTP {@code 401 Unauthorized}를 반환합니다.
     */
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 기능입니다."),

    /**
     * 요청한 리소스 또는 기능에 대한 접근 권한이 없을 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    /**
     * 요청한 활동 기록을 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "활동을 찾을 수 없습니다."),

    /**
     * 동일 활동에 이미 반응이 존재하는 상태에서 다시 반응을 등록할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    ACTIVITY_REACTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 반응을 누른 활동입니다."),

    /**
     * 활동 반응 취소 요청 시 기존 반응 기록이 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    ACTIVITY_REACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "반응을 누른 기록이 없습니다."),

    /**
     * 요청한 게시물을 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "게시물을 찾을 수 없습니다."),

    /**
     * 동일 게시물에 이미 반응이 존재하는 상태에서 다시 반응을 등록할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    BOARD_REACTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 반응을 누른 게시물입니다."),

    /**
     * 게시물 반응 취소 요청 시 기존 반응 기록이 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    BOARD_REACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "반응을 누른 기록이 없습니다."),

    /**
     * 서버 내부에서 예기치 못한 오류가 발생했을 때 사용합니다.
     * <p>
     * HTTP {@code 500 Internal Server Error}를 반환합니다.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    /**
     * 에러 상황에 해당하는 HTTP 상태 코드입니다.
     */
    private final HttpStatus httpStatus;

    /**
     * 클라이언트에 전달할 에러 메시지입니다.
     */
    private final String message;
}
