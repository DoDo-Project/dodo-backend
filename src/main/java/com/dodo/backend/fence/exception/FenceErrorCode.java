package com.dodo.backend.fence.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 울타리(Fence) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 * <p>
 * 울타리 생성/조회/수정 및 거리 범위 설정 과정에서 발생하는 예외를
 * HTTP 상태 코드와 메시지로 관리합니다.
 */
@AllArgsConstructor
@Getter
public enum FenceErrorCode implements BaseErrorCode {

    /**
     * 클라이언트 요청 형식이 잘못되었거나 필수 값이 누락된 경우 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 반려동물에 이미 울타리가 설정되어 중복 생성이 불가능한 경우 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    FENCE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 울타리가 존재합니다."),

    /**
     * 인증되지 않은 사용자가 로그인이 필요한 기능을 호출한 경우 사용합니다.
     * <p>
     * HTTP {@code 401 Unauthorized}를 반환합니다.
     */
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 기능입니다."),

    /**
     * 요청 사용자가 대상 반려동물에 대한 권한이 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    PET_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 반려동물에 대한 권한이 없습니다."),

    /**
     * 요청 사용자가 대상 울타리에 대한 접근 권한이 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    FENCE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 울타리에 대한 접근 권한이 없습니다."),

    /**
     * 요청 사용자가 대상 울타리에 대한 권한이 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    FENCE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 울타리에 대한 권한이 없습니다."),

    /**
     * 요청한 반려동물 정보를 찾을 수 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 반려동물입니다."),

    /**
     * 요청한 울타리 상세 정보를 찾을 수 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    FENCE_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 울타리 정보입니다."),

    /**
     * 요청한 울타리를 찾을 수 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    FENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 울타리입니다."),

    /**
     * 요청한 반려동물 또는 울타리 리소스를 찾을 수 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    PET_OR_FENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 반려동물 또는 울타리입니다."),

    /**
     * 서버 내부에서 예상치 못한 오류가 발생한 경우 사용합니다.
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
