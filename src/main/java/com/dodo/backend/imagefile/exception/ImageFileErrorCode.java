package com.dodo.backend.imagefile.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 이미지 파일(ImageFile) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 * <p>
 * 이미지 업로드, 프로필 이미지 저장 및 수정 과정에서 발생할 수 있는 예외를 포함하며,
 * HTTP 상태 코드와 클라이언트에게 전달할 메시지를 {@code Key-Value} 형태로 관리합니다.
 */
@AllArgsConstructor
@Getter
public enum ImageFileErrorCode implements BaseErrorCode {

    /**
     * 클라이언트의 요청 형식이 잘못되었거나 이미지 파일 검증을 통과하지 못했을 때 사용합니다.
     * <p>
     * HTTP {@code 400 Bad Request}를 반환합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 이미지 업로드 결과에서 유효한 이미지 URL을 확인할 수 없을 때 사용합니다.
     * <p>
     * HTTP {@code 500 Internal Server Error}를 반환합니다.
     */
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드 중 오류가 발생했습니다."),

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
