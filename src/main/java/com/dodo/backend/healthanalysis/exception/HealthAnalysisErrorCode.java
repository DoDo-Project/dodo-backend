package com.dodo.backend.healthanalysis.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 건강 분석(HealthAnalysis) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 */
@AllArgsConstructor
@Getter
public enum HealthAnalysisErrorCode implements BaseErrorCode {

    /**
     * 클라이언트의 요청 형식이 잘못되었거나 필수 파라미터가 누락된 경우 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 인증되지 않은 사용자가 로그인이 필요한 기능을 요청한 경우 사용합니다.
     * <p>
     * HTTP {@code 401 Unauthorized}를 반환합니다.
     */
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 기능입니다."),

    /**
     * 요청 리소스에 대한 접근 권한이 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    /**
     * 건강 분석 리포트 이력 조회 권한이 없는 반려동물에 접근한 경우 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    REPORT_HISTORY_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "건강 분석 리포트 이력 조회를 할 수 없는 반려동물입니다."),

    /**
     * 특정 분석 리소스 조회 권한이 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    VIEW_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "조회 권한이 없는 분석입니다."),

    /**
     * 특정 분석 리소스 수정 권한이 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    UPDATE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "수정 권한이 없는 분석입니다."),

    /**
     * 특정 분석 리소스 삭제 권한이 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 403 Forbidden}을 반환합니다.
     */
    DELETE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "삭제 권한이 없는 분석입니다."),

    /**
     * 요청에 해당하는 반려동물을 찾을 수 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 반려동물을 찾을 수 없습니다."),

    /**
     * 요청에 해당하는 건강 분석 데이터를 찾을 수 없는 경우 사용합니다.
     * <p>
     * HTTP {@code 404 Not Found}를 반환합니다.
     */
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 분석을 찾을 수 없습니다."),

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
