package com.dodo.backend.activityhistory.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 활동 기록(ActivityHistory) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 * <p>
 * HTTP 상태 코드와 클라이언트에게 전달할 메시지를 {@code Key-Value} 형태로 관리합니다.
 */
@AllArgsConstructor
@Getter
public enum ActivityHistoryErrorCode implements BaseErrorCode {

    /**
     * 클라이언트의 요청 형식이 잘못되었거나 파라미터가 유효하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 인증되지 않은 사용자가 로그인이 필요한 기능을 요청했을 때 사용합니다.
     * <p>
     * HTTP {@code 401 Unauthorized}를 반환합니다.
     */
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 기능입니다."),

    /**
     * 반려동물의 주인이 아니거나 권한이 없는 사용자가 활동 기록을 생성하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    CREATE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "활동을 기록할 권한이 없습니다."),

    /**
     * 권한이 없는 사용자가 활동 기록을 시작(START)하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    START_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "활동을 시작할 권한이 없습니다."),

    /**
     * 권한이 없는 사용자가 진행 중인 활동을 중단(STOP)하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    STOP_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 활동 기록을 중단할 권한이 없습니다."),

    /**
     * 다른 사용자의 활동 기록을 조회하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    VIEW_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 활동 기록을 조회할 권한이 없습니다."),

    /**
     * 권한이 없는 사용자가 활동 기록을 삭제하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    DELETE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 활동 기록을 삭제할 권한이 없습니다."),

    /**
     * 사용자가 해당 활동 알림이나 데이터를 수신할 권한이 없을 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    RECEIVE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 활동을 수신받을 권한이 없습니다."),

    /**
     * 요청한 ID에 해당하는 활동 기록이 존재하지 않을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 활동 기록을 찾을 수 없습니다."),

    /**
     * 아직 시작되지 않은 활동(BEFORE)을 종료하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    ACTIVITY_NOT_STARTED(HttpStatus.CONFLICT, "아직 기록을 시작하지 않은 활동입니다."),

    /**
     * 이미 진행 중인 활동(IN_PROGRESS)이 있는데 새로운 활동을 시작하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "진행 중인 활동 기록이 이미 존재합니다."),

    /**
     * 이미 종료(COMPLETED)되거나 취소(CANCELED)된 활동을 변경하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 종료된 활동 기록입니다."),

    /**
     * 이미 이미 생성되어 시작 대기 중인 활동이 있을 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    ALREADY_EXISTS_BEFORE(HttpStatus.CONFLICT, "이미 시작 전인 활동 기록이 존재합니다."),

    /**
     * 단시간에 과도한 요청이 발생하여 요청을 제한할 때 사용합니다.
     * <p>
     * HTTP {@code 429 Too Many Requests}를 반환합니다.
     */
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."),

    /**
     * 서버 내부에서 예상치 못한 오류가 발생했을 때 사용합니다.
     * <p>
     * HTTP {@code 500 Internal Server Error}를 반환합니다.
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    /**
     * 에러 상황에 해당하는 HTTP 상태 코드입니다.
     */
    private final HttpStatus httpStatus;

    /**
     * 클라이언트에게 전달할 상세 에러 메시지입니다.
     */
    private final String message;
}