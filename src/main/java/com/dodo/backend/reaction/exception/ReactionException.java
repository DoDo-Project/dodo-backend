package com.dodo.backend.reaction.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 반응(Reaction) 도메인 비즈니스 로직 수행 중 발생하는 전용 예외 클래스입니다.
 * <p>
 * 반응 등록, 취소, 조회와 관련한 예외 상황을 {@link ReactionErrorCode}로 표현하며,
 * 전역 예외 처리기에서 표준 에러 응답으로 변환됩니다.
 */
@AllArgsConstructor
@Getter
public class ReactionException extends RuntimeException {

    /**
     * 발생한 예외의 구체적인 분류를 나타내는 에러 코드입니다.
     */
    private final ReactionErrorCode errorCode;
}
