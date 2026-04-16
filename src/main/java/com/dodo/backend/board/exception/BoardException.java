package com.dodo.backend.board.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 게시판(Board) 도메인 비즈니스 로직 수행 중 발생하는 전용 예외 클래스입니다.
 * <p>
 * 게시글 작성, 조회, 수정, 삭제 및 임시 저장 등 게시판 관련 서비스 로직에서
 * 예외 상황 발생 시 이 클래스를 throw 합니다.
 * {@code GlobalExceptionHandler}에서 이 예외를 가로채어
 * {@link BoardErrorCode}에 정의된 표준 응답으로 변환합니다.
 */
@AllArgsConstructor
@Getter
public class BoardException extends RuntimeException {

    /**
     * 발생한 예외의 구체적인 종류(상태 코드, 메시지)를 담고 있는 Enum입니다.
     */
    private final BoardErrorCode errorCode;
}