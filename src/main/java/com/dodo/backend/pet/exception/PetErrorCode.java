package com.dodo.backend.pet.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 펫(Pet) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 * <p>
 * 펫의 등록, 수정, 조회 및 디바이스 관리 과정에서 발생하는 예외를 포함하며,
 * HTTP 상태 코드와 클라이언트에게 전달할 메시지를 {@code Key-Value} 형태로 관리합니다.
 */
@AllArgsConstructor
@Getter
public enum PetErrorCode implements BaseErrorCode {

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
     * 자신이 직접 등록(생성)하지 않은 반려동물 정보를 수정하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    UPDATE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "자신이 등록한 반려동물만 수정할 수 있습니다."),

    /**
     * 해당 반려동물에 대한 조회 권한(가족 멤버십 등)이 없을 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    VIEW_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 반려동물에 대한 조회 권한이 없습니다."),

    /**
     * 해당 반려동물에 대한 특정 작업(삭제, 디바이스 제어 등)을 수행할 권한이 없을 때 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    ACTION_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "해당 반려동물에 대한 권한이 없습니다."),

    /**
     * 요청과 관련된 사용자 정보를 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    /**
     * 요청한 식별자(ID)에 해당하는 반려동물을 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 ID의 반려동물을 찾을 수 없습니다."),

    /**
     * 요청한 특이사항 ID에 해당하는 데이터를 찾을 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    PET_SIGNIFICANT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 특이사항입니다."),

    /**
     * 이미 시스템에 존재하는 반려동물 등록번호로 등록을 시도할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    REGISTRATION_NUMBER_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 등록번호입니다."),

    /**
     * 이미 다른 반려동물에 등록되어 사용 중인 디바이스 ID를 등록하려 할 때 사용합니다.
     * <p>
     * HTTP {@code 409 Conflict}를 반환합니다.
     */
    DEVICE_ID_DUPLICATED(HttpStatus.CONFLICT, "이미 다른 반려동물에 등록된 디바이스 ID입니다."),

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
