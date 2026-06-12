package com.dodo.backend.comment.service;

import com.dodo.backend.comment.dto.request.CommentRequest.CommentCreateRequest;
import com.dodo.backend.comment.dto.request.CommentRequest.CommentUpdateRequest;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentCreateResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentListResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentSimpleResponse;
import com.dodo.backend.comment.exception.CommentException;

import java.util.UUID;

/**
 * 댓글(Comment) 도메인의 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface CommentService {

    /**
     * 댓글을 작성합니다.
     *
     * @param userId  요청 사용자 ID
     * @param request 댓글 작성 요청 DTO
     * @return 댓글 작성 응답 DTO
     * @throws CommentException 잘못된 요청 또는 부모 댓글이 없는 경우
     */
    CommentCreateResponse createComment(UUID userId, CommentCreateRequest request);

    /**
     * 특정 게시글의 댓글 목록을 조회합니다.
     *
     * @param boardId 댓글을 조회할 게시글 ID
     * @param page    페이지 번호
     * @param size    페이지 크기
     * @return 댓글 목록 조회 응답 DTO
     */
    CommentListResponse getComments(Long boardId, int page, int size);

    /**
     * 특정 댓글을 수정합니다.
     *
     * @param userId    요청 사용자 ID
     * @param commentId 수정할 댓글 ID
     * @param request   댓글 수정 요청 DTO
     * @return 댓글 수정 응답 DTO
     * @throws CommentException 잘못된 요청, 댓글 없음, 수정 권한 없음인 경우
     */
    CommentSimpleResponse updateComment(UUID userId, Long commentId, CommentUpdateRequest request);

    /**
     * 특정 댓글을 삭제 상태로 변경합니다.
     *
     * @param userId    요청 사용자 ID
     * @param commentId 삭제할 댓글 ID
     * @return 댓글 삭제 응답 DTO
     * @throws CommentException 잘못된 요청, 댓글 없음, 삭제 권한 없음인 경우
     */
    CommentSimpleResponse deleteComment(UUID userId, Long commentId);
}
