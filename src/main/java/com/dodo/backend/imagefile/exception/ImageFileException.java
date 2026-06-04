package com.dodo.backend.imagefile.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 이미지 파일(ImageFile) 도메인 비즈니스 로직 수행 중 발생하는 전용 예외 클래스입니다.
 * <p>
 * 이미지 업로드, 프로필 이미지 저장 및 수정 등 이미지 파일 관련 서비스 로직에서
 * 예외 상황 발생 시 이 클래스를 throw 합니다.
 * {@code GlobalExceptionHandler}에서 이 예외를 가로채어
 * {@link ImageFileErrorCode}에 정의된 표준 응답으로 변환합니다.
 */
@AllArgsConstructor
@Getter
public class ImageFileException extends RuntimeException {

    /**
     * 발생한 예외의 구체적인 종류(상태 코드, 메시지)를 담고 있는 Enum입니다.
     */
    private final ImageFileErrorCode errorCode;
}
