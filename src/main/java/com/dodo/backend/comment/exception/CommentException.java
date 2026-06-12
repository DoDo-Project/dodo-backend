package com.dodo.backend.comment.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 댓글(Comment) 도메인 비즈니스 로직 수행 중 발생하는 전용 예외 클래스입니다.
 */
@AllArgsConstructor
@Getter
public class CommentException extends RuntimeException {

    /**
     * 발생한 예외의 구체적인 종류를 담고 있는 Enum입니다.
     */
    private final CommentErrorCode errorCode;
}
