package com.dodo.backend.petweight.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 반려동물 체중(PetWeight) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 * <p>
 * 체중 기록 추가, 조회, 수정, 삭제 과정에서 발생하는 예외를 포함하며,
 * HTTP 상태 코드와 클라이언트에게 전달할 메시지를 {@code Key-Value} 형태로 관리합니다.
 */
@AllArgsConstructor
@Getter
public enum PetWeightErrorCode implements BaseErrorCode {

    /**
     * 클라이언트의 요청 형식이 잘못되었거나 유효성 검사를 통과하지 못했을 때 사용합니다.
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
     * 해당 반려동물의 가족(소유자)이 아닌 사용자가 체중 기록을 조작하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 반려동물에 대한 권한이 없습니다."),

    /**
     * 요청한 식별자(ID)에 해당하는 반려동물을 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 반려동물을 찾을 수 없습니다."),

    /**
     * 요청한 식별자(ID)에 해당하는 체중 기록을 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    WEIGHT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 몸무게 기록을 찾을 수 없습니다."),

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