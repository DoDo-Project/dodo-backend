package com.dodo.backend.report.service;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.exception.BoardException;
import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.comment.repository.CommentRepository;
import com.dodo.backend.report.dto.request.ReportRequest.ReportCreateRequest;
import com.dodo.backend.report.dto.response.ReportResponse.ReportSimpleResponse;
import com.dodo.backend.report.entity.Report;
import com.dodo.backend.report.entity.ReportReason;
import com.dodo.backend.report.exception.ReportException;
import com.dodo.backend.report.repository.ReportRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.exception.UserException;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static com.dodo.backend.report.entity.ReportReason.ABUSE;
import static com.dodo.backend.report.entity.ReportReason.IMPERSONATION;
import static com.dodo.backend.report.entity.ReportReason.INAPPROPRIATE_PROFILE;
import static com.dodo.backend.report.entity.ReportReason.OBSCENITY;
import static com.dodo.backend.report.entity.ReportReason.SPAM;
import static com.dodo.backend.report.entity.ReportReason.SPAM_ADVERTISING;
import static com.dodo.backend.report.exception.ReportErrorCode.BOARD_NOT_FOUND;
import static com.dodo.backend.report.exception.ReportErrorCode.COMMENT_NOT_FOUND;
import static com.dodo.backend.report.exception.ReportErrorCode.INVALID_REQUEST;
import static com.dodo.backend.report.exception.ReportErrorCode.REPORT_ALREADY_EXISTS;
import static com.dodo.backend.report.exception.ReportErrorCode.USER_NOT_FOUND;

/**
 * {@link ReportService} 구현체입니다.
 * <p>
 * 게시글, 유저, 댓글 신고 생성과 중복 신고 검증을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final String REPORT_SUCCESS_MESSAGE = "신고가 성공적으로 접수되었습니다.";
    private static final Set<ReportReason> BOARD_REPORT_REASONS = EnumSet.of(SPAM, OBSCENITY, ABUSE);
    private static final Set<ReportReason> USER_REPORT_REASONS = EnumSet.of(INAPPROPRIATE_PROFILE, SPAM_ADVERTISING, IMPERSONATION);
    private static final Set<ReportReason> COMMENT_REPORT_REASONS = EnumSet.of(SPAM_ADVERTISING, ABUSE, OBSCENITY);

    private final ReportRepository reportRepository;
    private final BoardService boardService;
    private final UserService userService;
    private final CommentRepository commentRepository;

    /**
     * 특정 게시글을 신고합니다.
     *
     * @param reporterId 신고자 ID
     * @param boardId    신고 대상 게시글 ID
     * @param request    신고 요청 DTO
     * @return 신고 처리 응답 DTO
     */
    @Transactional
    @Override
    public ReportSimpleResponse reportBoard(UUID reporterId, Long boardId, ReportCreateRequest request) {
        validateRequest(reporterId, request);
        validateId(boardId);
        validateReason(request.getReportReason(), BOARD_REPORT_REASONS);

        User reporter = findReporter(reporterId);
        Board board = findBoard(boardId);

        if (reportRepository.existsByReporterAndBoard(reporter, board)) {
            throw new ReportException(REPORT_ALREADY_EXISTS);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .board(board)
                .reportReason(request.getReportReason())
                .build();

        reportRepository.save(report);
        return ReportSimpleResponse.toDto(REPORT_SUCCESS_MESSAGE);
    }

    /**
     * 특정 유저를 신고합니다.
     *
     * @param reporterId 신고자 ID
     * @param userId     신고 대상 유저 ID
     * @param request    신고 요청 DTO
     * @return 신고 처리 응답 DTO
     */
    @Transactional
    @Override
    public ReportSimpleResponse reportUser(UUID reporterId, UUID userId, ReportCreateRequest request) {
        validateRequest(reporterId, request);
        if (userId == null || reporterId.equals(userId)) {
            throw new ReportException(INVALID_REQUEST);
        }
        validateReason(request.getReportReason(), USER_REPORT_REASONS);

        User reporter = findReporter(reporterId);
        User reportedUser = findReportedUser(userId);

        if (reportRepository.existsByReporterAndReportedUser(reporter, reportedUser)) {
            throw new ReportException(REPORT_ALREADY_EXISTS);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportReason(request.getReportReason())
                .build();

        reportRepository.save(report);
        return ReportSimpleResponse.toDto(REPORT_SUCCESS_MESSAGE);
    }

    /**
     * 특정 댓글을 신고합니다.
     *
     * @param reporterId 신고자 ID
     * @param commentId  신고 대상 댓글 ID
     * @param request    신고 요청 DTO
     * @return 신고 처리 응답 DTO
     */
    @Transactional
    @Override
    public ReportSimpleResponse reportComment(UUID reporterId, Long commentId, ReportCreateRequest request) {
        validateRequest(reporterId, request);
        validateId(commentId);
        validateReason(request.getReportReason(), COMMENT_REPORT_REASONS);

        User reporter = findReporter(reporterId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ReportException(COMMENT_NOT_FOUND));

        if (reportRepository.existsByReporterAndComment(reporter, comment)) {
            throw new ReportException(REPORT_ALREADY_EXISTS);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .comment(comment)
                .reportReason(request.getReportReason())
                .build();

        reportRepository.save(report);
        return ReportSimpleResponse.toDto(REPORT_SUCCESS_MESSAGE);
    }

    /**
     * 신고 요청의 공통 필수 값을 검증합니다.
     *
     * @param reporterId 신고자 ID
     * @param request    신고 요청 DTO
     */
    private void validateRequest(UUID reporterId, ReportCreateRequest request) {
        if (reporterId == null || request == null || request.getReportReason() == null) {
            throw new ReportException(INVALID_REQUEST);
        }
    }

    /**
     * 숫자 ID 요청 값을 검증합니다.
     *
     * @param id 검증할 ID
     */
    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new ReportException(INVALID_REQUEST);
        }
    }

    /**
     * 대상 유형에서 허용하는 신고 사유인지 검증합니다.
     *
     * @param reportReason   요청 신고 사유
     * @param allowedReasons 허용 신고 사유 목록
     */
    private void validateReason(ReportReason reportReason, Set<ReportReason> allowedReasons) {
        if (!allowedReasons.contains(reportReason)) {
            throw new ReportException(INVALID_REQUEST);
        }
    }

    /**
     * 신고자 유저를 조회합니다.
     *
     * @param reporterId 신고자 ID
     * @return 신고자 유저 엔티티
     */
    private User findReporter(UUID reporterId) {
        try {
            return userService.getUserById(reporterId);
        } catch (UserException e) {
            throw new ReportException(INVALID_REQUEST);
        }
    }

    /**
     * 신고 대상 게시글을 조회합니다.
     *
     * @param boardId 게시글 ID
     * @return 게시글 엔티티
     */
    private Board findBoard(Long boardId) {
        try {
            return boardService.getBoardById(boardId);
        } catch (BoardException e) {
            throw new ReportException(BOARD_NOT_FOUND);
        }
    }

    /**
     * 신고 대상 유저를 조회합니다.
     *
     * @param userId 신고 대상 유저 ID
     * @return 신고 대상 유저 엔티티
     */
    private User findReportedUser(UUID userId) {
        try {
            return userService.getUserById(userId);
        } catch (UserException e) {
            throw new ReportException(USER_NOT_FOUND);
        }
    }
}
