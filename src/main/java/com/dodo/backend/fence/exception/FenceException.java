package com.dodo.backend.fence.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 울타리(Fence) 도메인 비즈니스 로직에서 발생하는 전용 예외 클래스입니다.
 * <p>
 * 울타리 생성/조회/수정/거리 범위 설정 로직에서 예외 상황이 발생하면
 * 이 예외를 던지고, 전역 예외 처리기에서 {@link FenceErrorCode}에 따라 응답합니다.
 */
@AllArgsConstructor
@Getter
public class FenceException extends RuntimeException {

    /**
     * 발생한 울타리 도메인 예외의 상세 코드입니다.
     */
    private final FenceErrorCode errorCode;
}
