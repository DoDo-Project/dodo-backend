package com.dodo.backend.admin.service;

import com.dodo.backend.admin.dto.request.AdminRequest.ReportStatusUpdateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.UserStatusUpdateRequest;
import com.dodo.backend.admin.dto.response.AdminResponse.BoardReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.ReportListResponse;
import com.dodo.backend.admin.entity.AdminReportType;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.board.mapper.BoardMapper;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.comment.repository.CommentRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.report.entity.Report;
import com.dodo.backend.report.entity.ReportReason;
import com.dodo.backend.report.entity.ReportStatus;
import com.dodo.backend.report.repository.ReportRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.mapper.UserMapper;
import com.dodo.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 관리자 서비스 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private ImageFileService imageFileService;

    @InjectMocks
    private AdminServiceImpl adminService;

    /**
     * 게시글 신고 상세 조회 시 게시글과 신고 목록을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 신고 상세 조회 성공")
    void getBoardReportDetail_Success() {
        User writer = createUser(UUID.randomUUID(), "작성자");
        User reporter = createUser(UUID.randomUUID(), "신고자");
        Board board = createBoard(1L, writer);
        Report report = createBoardReport(10L, reporter, board, ReportReason.ABUSE);

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(reportRepository.findAllByBoard_BoardIdOrderByCreatedAtDesc(1L)).willReturn(List.of(report));

        BoardReportDetailResponse response = adminService.getBoardReportDetail(1L);

        assertNotNull(response);
        assertEquals(1L, response.getBoardId());
        assertEquals(1, response.getTotalReportCount());
        assertEquals("작성자", response.getReportedUserInfo().getNickname());
    }

    /**
     * 신고 목록 조회 시 대상별로 신고가 묶이는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 신고 목록 조회 성공")
    void getReportList_Success() {
        User writer = createUser(UUID.randomUUID(), "작성자");
        User reporter = createUser(UUID.randomUUID(), "신고자");
        Board board = createBoard(1L, writer);
        Report report = createBoardReport(10L, reporter, board, ReportReason.SPAM);

        given(reportRepository.findAllByBoardIsNotNull()).willReturn(List.of(report));

        ReportListResponse response = adminService.getReportList(AdminReportType.BOARD, null, 0, 10, "lastReportedAt,desc");

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("BOARD", response.getData().get(0).getReportType());
        assertEquals(1, response.getData().get(0).getTotalReportCount());
    }

    /**
     * 유저 상태 변경 시 UserMapper가 호출되는지 검증합니다.
     */
    @Test
    @DisplayName("유저 계정 상태 변경 성공")
    void updateUserStatus_Success() {
        UUID userId = UUID.randomUUID();
        given(userRepository.existsById(userId)).willReturn(true);

        adminService.updateUserStatus(userId, new UserStatusUpdateRequest(UserStatus.SUSPENDED));

        verify(userMapper).updateUserStatus(userId, UserStatus.SUSPENDED.name());
    }

    /**
     * 신고 상태 변경 시 엔티티의 상태가 변경되는지 검증합니다.
     */
    @Test
    @DisplayName("신고 처리 상태 변경 성공")
    void updateReportStatus_Success() {
        Report report = createBoardReport(
                10L,
                createUser(UUID.randomUUID(), "신고자"),
                createBoard(1L, createUser(UUID.randomUUID(), "작성자")),
                ReportReason.SPAM
        );
        given(reportRepository.findById(10L)).willReturn(Optional.of(report));

        adminService.updateReportStatus(10L, new ReportStatusUpdateRequest(ReportStatus.COMPLETED));

        assertEquals(ReportStatus.COMPLETED, report.getReportStatus());
    }

    private User createUser(UUID userId, String nickname) {
        return User.builder()
                .usersId(userId)
                .nickname(nickname)
                .build();
    }

    private Board createBoard(Long boardId, User user) {
        return Board.builder()
                .boardId(boardId)
                .user(user)
                .boardTitle("게시글 제목")
                .boardContent("게시글 내용")
                .boardType(BoardType.FREE)
                .boardStatus(BoardStatus.PUBLISHED)
                .boardCreatedAt(LocalDateTime.of(2025, 10, 1, 10, 0))
                .build();
    }

    private Report createBoardReport(Long reportId, User reporter, Board board, ReportReason reason) {
        return Report.builder()
                .reportId(reportId)
                .reporter(reporter)
                .board(board)
                .reportReason(reason)
                .reportStatus(ReportStatus.PENDING)
                .createdAt(LocalDateTime.of(2025, 10, 1, 11, 0))
                .build();
    }
}
