package com.dodo.backend.report.service;

import com.dodo.backend.report.dto.request.ReportRequest.ReportCreateRequest;
import com.dodo.backend.report.dto.response.ReportResponse.ReportSimpleResponse;
import com.dodo.backend.report.exception.ReportException;

import java.util.UUID;

/**
 * 신고 도메인의 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface ReportService {

    /**
     * 특정 게시글을 신고합니다.
     *
     * @param reporterId 신고자 ID
     * @param boardId    신고 대상 게시글 ID
     * @param request    신고 요청 DTO
     * @return 신고 처리 응답 DTO
     * @throws ReportException 잘못된 요청, 게시글 없음, 중복 신고인 경우
     */
    ReportSimpleResponse reportBoard(UUID reporterId, Long boardId, ReportCreateRequest request);

    /**
     * 특정 유저를 신고합니다.
     *
     * @param reporterId 신고자 ID
     * @param userId     신고 대상 유저 ID
     * @param request    신고 요청 DTO
     * @return 신고 처리 응답 DTO
     * @throws ReportException 잘못된 요청, 유저 없음, 중복 신고인 경우
     */
    ReportSimpleResponse reportUser(UUID reporterId, UUID userId, ReportCreateRequest request);

    /**
     * 특정 댓글을 신고합니다.
     *
     * @param reporterId 신고자 ID
     * @param commentId  신고 대상 댓글 ID
     * @param request    신고 요청 DTO
     * @return 신고 처리 응답 DTO
     * @throws ReportException 잘못된 요청, 댓글 없음, 중복 신고인 경우
     */
    ReportSimpleResponse reportComment(UUID reporterId, Long commentId, ReportCreateRequest request);
}
