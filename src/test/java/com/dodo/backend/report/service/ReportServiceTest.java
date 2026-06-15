package com.dodo.backend.report.service;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.comment.repository.CommentRepository;
import com.dodo.backend.report.dto.request.ReportRequest.ReportCreateRequest;
import com.dodo.backend.report.dto.response.ReportResponse.ReportSimpleResponse;
import com.dodo.backend.report.entity.ReportReason;
import com.dodo.backend.report.exception.ReportException;
import com.dodo.backend.report.repository.ReportRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.dodo.backend.report.exception.ReportErrorCode.INVALID_REQUEST;
import static com.dodo.backend.report.exception.ReportErrorCode.REPORT_ALREADY_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 신고 서비스 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private BoardService boardService;

    @Mock
    private UserService userService;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    /**
     * 게시글 신고 성공 시 신고를 저장하고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 신고 성공: 신고를 저장하고 성공 메시지를 반환한다.")
    void reportBoard_Success() {
        UUID reporterId = UUID.randomUUID();
        Long boardId = 1L;
        User reporter = createUser(reporterId);
        Board board = createBoard(boardId);
        ReportCreateRequest request = new ReportCreateRequest(ReportReason.SPAM);

        given(userService.getUserById(reporterId)).willReturn(reporter);
        given(boardService.getBoardById(boardId)).willReturn(board);
        given(reportRepository.existsByReporterAndBoard(reporter, board)).willReturn(false);

        ReportSimpleResponse response = reportService.reportBoard(reporterId, boardId, request);

        assertNotNull(response);
        assertEquals("신고가 성공적으로 접수되었습니다.", response.getMessage());
        verify(reportRepository).save(any());
    }

    /**
     * 이미 신고한 게시글이면 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 신고 실패: 이미 신고한 게시글이면 예외가 발생한다.")
    void reportBoard_Fail_AlreadyExists() {
        UUID reporterId = UUID.randomUUID();
        Long boardId = 1L;
        User reporter = createUser(reporterId);
        Board board = createBoard(boardId);
        ReportCreateRequest request = new ReportCreateRequest(ReportReason.SPAM);

        given(userService.getUserById(reporterId)).willReturn(reporter);
        given(boardService.getBoardById(boardId)).willReturn(board);
        given(reportRepository.existsByReporterAndBoard(reporter, board)).willReturn(true);

        ReportException exception = assertThrows(ReportException.class,
                () -> reportService.reportBoard(reporterId, boardId, request));

        assertEquals(REPORT_ALREADY_EXISTS, exception.getErrorCode());
        verify(reportRepository, never()).save(any());
    }

    /**
     * 게시글 신고에서 허용되지 않는 신고 사유면 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 신고 실패: 게시글에 허용되지 않는 신고 사유면 예외가 발생한다.")
    void reportBoard_Fail_InvalidReason() {
        UUID reporterId = UUID.randomUUID();
        ReportCreateRequest request = new ReportCreateRequest(ReportReason.IMPERSONATION);

        ReportException exception = assertThrows(ReportException.class,
                () -> reportService.reportBoard(reporterId, 1L, request));

        assertEquals(INVALID_REQUEST, exception.getErrorCode());
        verify(reportRepository, never()).save(any());
    }

    /**
     * 유저 신고 성공 시 신고를 저장하고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("유저 신고 성공: 신고를 저장하고 성공 메시지를 반환한다.")
    void reportUser_Success() {
        UUID reporterId = UUID.randomUUID();
        UUID reportedUserId = UUID.randomUUID();
        User reporter = createUser(reporterId);
        User reportedUser = createUser(reportedUserId);
        ReportCreateRequest request = new ReportCreateRequest(ReportReason.IMPERSONATION);

        given(userService.getUserById(reporterId)).willReturn(reporter);
        given(userService.getUserById(reportedUserId)).willReturn(reportedUser);
        given(reportRepository.existsByReporterAndReportedUser(reporter, reportedUser)).willReturn(false);

        ReportSimpleResponse response = reportService.reportUser(reporterId, reportedUserId, request);

        assertNotNull(response);
        assertEquals("신고가 성공적으로 접수되었습니다.", response.getMessage());
        verify(reportRepository).save(any());
    }

    /**
     * 자기 자신을 신고하면 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("유저 신고 실패: 자기 자신을 신고하면 예외가 발생한다.")
    void reportUser_Fail_SelfReport() {
        UUID reporterId = UUID.randomUUID();
        ReportCreateRequest request = new ReportCreateRequest(ReportReason.IMPERSONATION);

        ReportException exception = assertThrows(ReportException.class,
                () -> reportService.reportUser(reporterId, reporterId, request));

        assertEquals(INVALID_REQUEST, exception.getErrorCode());
        verify(reportRepository, never()).save(any());
    }

    /**
     * 댓글 신고 성공 시 신고를 저장하고 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 신고 성공: 신고를 저장하고 성공 메시지를 반환한다.")
    void reportComment_Success() {
        UUID reporterId = UUID.randomUUID();
        Long commentId = 10L;
        User reporter = createUser(reporterId);
        Comment comment = createComment(commentId);
        ReportCreateRequest request = new ReportCreateRequest(ReportReason.SPAM_ADVERTISING);

        given(userService.getUserById(reporterId)).willReturn(reporter);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(reportRepository.existsByReporterAndComment(reporter, comment)).willReturn(false);

        ReportSimpleResponse response = reportService.reportComment(reporterId, commentId, request);

        assertNotNull(response);
        assertEquals("신고가 성공적으로 접수되었습니다.", response.getMessage());
        verify(reportRepository).save(any());
    }

    private User createUser(UUID userId) {
        return User.builder()
                .usersId(userId)
                .build();
    }

    private Board createBoard(Long boardId) {
        return Board.builder()
                .boardId(boardId)
                .build();
    }

    private Comment createComment(Long commentId) {
        return Comment.builder()
                .commentId(commentId)
                .build();
    }
}
