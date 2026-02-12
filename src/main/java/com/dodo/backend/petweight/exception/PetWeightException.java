package com.dodo.backend.petweight.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 반려동물 체중(PetWeight) 도메인 비즈니스 로직 수행 중 발생하는 전용 예외 클래스입니다.
 * <p>
 * 체중 기록의 추가, 조회, 수정, 삭제 등 로직에서 예외 상황 발생 시 이 클래스를 throw 합니다.
 * {@code GlobalExceptionHandler}에서 이 예외를 가로채어 {@link PetWeightErrorCode}에 정의된 표준 응답으로 변환합니다.
 */
@AllArgsConstructor
@Getter
public class PetWeightException extends RuntimeException {

    /**
     * 발생한 예외의 구체적인 종류(상태 코드, 메시지)를 담고 있는 Enum입니다.
     */
    private final PetWeightErrorCode errorCode;
}