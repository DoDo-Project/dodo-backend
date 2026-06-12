package com.dodo.backend.comment.exception;

import com.dodo.backend.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 댓글(Comment) 도메인에서 발생하는 예외 상황을 관리하는 에러 코드 정의 클래스입니다.
 */
@AllArgsConstructor
@Getter
public enum CommentErrorCode implements BaseErrorCode {

    /**
     * 클라이언트 요청이 잘못되었을 때 사용합니다.
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /**
     * 인증되지 않은 사용자가 댓글 API를 요청했을 때 사용합니다.
     */
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 기능입니다."),

    /**
     * 요청한 댓글을 찾을 수 없을 때 사용합니다.
     */
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 ID의 댓글을 찾을 수 없습니다."),

    /**
     * 댓글 수정 권한이 없을 때 사용합니다.
     */
    UPDATE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "댓글을 수정할 권한이 없습니다."),

    /**
     * 댓글 삭제 권한이 없을 때 사용합니다.
     */
    DELETE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "댓글을 삭제할 권한이 없습니다."),

    /**
     * 서버 내부에서 예상치 못한 오류가 발생했을 때 사용합니다.
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
