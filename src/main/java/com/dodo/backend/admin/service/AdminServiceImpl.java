package com.dodo.backend.admin.service;

import com.dodo.backend.admin.dto.request.AdminRequest.AnnouncementCreateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.AnnouncementUpdateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.ReportStatusUpdateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.UserStatusUpdateRequest;
import com.dodo.backend.admin.dto.response.AdminResponse.AdminSimpleResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.AnnouncementDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.AnnouncementItemResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.AnnouncementListResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.BoardInfoResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.BoardReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.CommentReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.PageInfoResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.ReportDetailItemResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.ReportListItemResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.ReportListResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.ReportTargetInfoResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.UserInfoResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.UserReportDetailResponse;
import com.dodo.backend.admin.entity.AdminReportType;
import com.dodo.backend.admin.exception.AdminException;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.board.mapper.BoardMapper;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.comment.repository.CommentRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.report.entity.Report;
import com.dodo.backend.report.entity.ReportStatus;
import com.dodo.backend.report.repository.ReportRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.mapper.UserMapper;
import com.dodo.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dodo.backend.admin.exception.AdminErrorCode.ANNOUNCEMENT_NOT_FOUND;
import static com.dodo.backend.admin.exception.AdminErrorCode.BOARD_NOT_FOUND;
import static com.dodo.backend.admin.exception.AdminErrorCode.COMMENT_NOT_FOUND;
import static com.dodo.backend.admin.exception.AdminErrorCode.INVALID_REQUEST;
import static com.dodo.backend.admin.exception.AdminErrorCode.REPORTED_BOARD_NOT_FOUND;
import static com.dodo.backend.admin.exception.AdminErrorCode.REPORTED_COMMENT_NOT_FOUND;
import static com.dodo.backend.admin.exception.AdminErrorCode.REPORTED_USER_NOT_FOUND;
import static com.dodo.backend.admin.exception.AdminErrorCode.REPORT_NOT_FOUND;
import static com.dodo.backend.admin.exception.AdminErrorCode.USER_NOT_FOUND;

/**
 * {@link AdminService} 구현체입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String STATUS_UPDATE_SUCCESS_MESSAGE = "성공적으로 상태를 변경했습니다.";

    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BoardMapper boardMapper;
    private final ImageFileService imageFileService;

    /**
     * 특정 게시글의 신고 상세 내역을 조회합니다.
     *
     * @param boardId 신고 상세 내역을 조회할 게시글 ID
     * @return 게시글 신고 상세 내역
     */
    @Transactional(readOnly = true)
    @Override
    public BoardReportDetailResponse getBoardReportDetail(Long boardId) {
        validatePositiveId(boardId);
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new AdminException(REPORTED_BOARD_NOT_FOUND));
        List<Report> reports = reportRepository.findAllByBoard_BoardIdOrderByCreatedAtDesc(boardId);

        return BoardReportDetailResponse.builder()
                .boardId(board.getBoardId())
                .boardInfo(BoardInfoResponse.toDto(board))
                .reportedUserInfo(UserInfoResponse.toDto(board.getUser()))
                .totalReportCount(reports.size())
                .reports(reports.stream().map(ReportDetailItemResponse::toDto).toList())
                .build();
    }

    /**
     * 특정 유저의 신고 상세 내역을 조회합니다.
     *
     * @param userId 신고 상세 내역을 조회할 유저 ID
     * @return 유저 신고 상세 내역
     */
    @Transactional(readOnly = true)
    @Override
    public UserReportDetailResponse getUserReportDetail(UUID userId) {
        if (userId == null) {
            throw new AdminException(INVALID_REQUEST);
        }

        User reportedUser = userRepository.findById(userId)
                .orElseThrow(() -> new AdminException(REPORTED_USER_NOT_FOUND));
        List<Report> reports = reportRepository.findAllByReportedUser_UsersIdOrderByCreatedAtDesc(userId);

        return UserReportDetailResponse.builder()
                .reportedUserInfo(UserInfoResponse.toDto(reportedUser))
                .totalReportCount(reports.size())
                .reports(reports.stream().map(ReportDetailItemResponse::toDto).toList())
                .build();
    }

    /**
     * 특정 댓글의 신고 상세 내역을 조회합니다.
     *
     * @param commentId 신고 상세 내역을 조회할 댓글 ID
     * @return 댓글 신고 상세 내역
     */
    @Transactional(readOnly = true)
    @Override
    public CommentReportDetailResponse getCommentReportDetail(Long commentId) {
        validatePositiveId(commentId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AdminException(REPORTED_COMMENT_NOT_FOUND));
        List<Report> reports = reportRepository.findAllByComment_CommentIdOrderByCreatedAtDesc(commentId);

        return CommentReportDetailResponse.toDto(comment, reports);
    }

    /**
     * 신고 유형별 신고 목록을 조회합니다.
     *
     * @param reportType 조회할 신고 대상 유형
     * @param reportStatus 조회할 신고 처리 상태
     * @param page 조회할 페이지 번호
     * @param size 페이지당 신고 목록 개수
     * @param sort 정렬 조건
     * @return 신고 목록 조회 결과
     */
    @Transactional(readOnly = true)
    @Override
    public ReportListResponse getReportList(AdminReportType reportType, ReportStatus reportStatus, int page, int size, String sort) {
        validatePageRequest(reportType, page, size);

        List<Report> reports = findReports(reportType, reportStatus);
        List<ReportListItemResponse> groupedItems = groupReports(reportType, reports);
        groupedItems.sort(buildReportListComparator(sort));

        int fromIndex = Math.min(page * size, groupedItems.size());
        int toIndex = Math.min(fromIndex + size, groupedItems.size());
        List<ReportListItemResponse> pageItems = groupedItems.subList(fromIndex, toIndex);

        return ReportListResponse.builder()
                .pageInfo(PageInfoResponse.toDto(page, size, groupedItems.size()))
                .data(pageItems)
                .build();
    }

    /**
     * 유저 계정 상태를 변경합니다.
     *
     * @param userId 상태를 변경할 유저 ID
     * @param request 변경할 유저 상태 요청
     * @return 상태 변경 성공 메시지
     */
    @Transactional
    @Override
    public AdminSimpleResponse updateUserStatus(UUID userId, UserStatusUpdateRequest request) {
        if (userId == null || request == null || request.getStatus() == null) {
            throw new AdminException(INVALID_REQUEST);
        }
        if (!userRepository.existsById(userId)) {
            throw new AdminException(USER_NOT_FOUND);
        }

        userMapper.updateUserStatus(userId, request.getStatus().name());
        return AdminSimpleResponse.toDto(STATUS_UPDATE_SUCCESS_MESSAGE);
    }

    /**
     * 게시글을 강제로 삭제합니다.
     *
     * @param boardId 삭제할 게시글 ID
     */
    @Transactional
    @Override
    public void deleteBoard(Long boardId) {
        validatePositiveId(boardId);
        if (!boardRepository.existsById(boardId)) {
            throw new AdminException(BOARD_NOT_FOUND);
        }

        boardMapper.deleteBoard(boardId, BoardStatus.DELETED.name());
        imageFileService.deleteBoardImages(boardId);
    }

    /**
     * 댓글을 강제로 삭제합니다.
     *
     * @param commentId 삭제할 댓글 ID
     */
    @Transactional
    @Override
    public void deleteComment(Long commentId) {
        validatePositiveId(commentId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AdminException(COMMENT_NOT_FOUND));

        reportRepository.deleteAllByComment(comment);
        commentRepository.delete(comment);
    }

    /**
     * 신고 처리 상태를 변경합니다.
     *
     * @param reportId 상태를 변경할 신고 ID
     * @param request 변경할 신고 처리 상태 요청
     * @return 상태 변경 성공 메시지
     */
    @Transactional
    @Override
    public AdminSimpleResponse updateReportStatus(Long reportId, ReportStatusUpdateRequest request) {
        validatePositiveId(reportId);
        if (request == null || request.getStatus() == null) {
            throw new AdminException(INVALID_REQUEST);
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AdminException(REPORT_NOT_FOUND));
        report.updateReportStatus(request.getStatus());

        return AdminSimpleResponse.toDto(STATUS_UPDATE_SUCCESS_MESSAGE);
    }

    /**
     * 공지를 작성합니다.
     *
     * @param adminId 공지를 작성하는 관리자 ID
     * @param request 공지 작성 요청
     * @return 공지 작성 성공 메시지
     */
    @Transactional
    @Override
    public AdminSimpleResponse createAnnouncement(UUID adminId, AnnouncementCreateRequest request) {
        validateAnnouncementCreateRequest(adminId, request);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AdminException(USER_NOT_FOUND));
        Board board = Board.builder()
                .user(admin)
                .boardTitle(request.getBoardTitle())
                .boardContent(request.getBoardContent())
                .viewCount(request.getViewCount() == null ? 0 : request.getViewCount())
                .boardStatus(BoardStatus.PUBLISHED)
                .boardStatusUpdatedAt(LocalDateTime.now())
                .boardType(BoardType.NOTICE)
                .noticeTag(request.getNoticeTag())
                .build();

        Board savedBoard = boardRepository.save(board);
        imageFileService.saveBoardImages(savedBoard, toImageList(request.getImageFileUrl()));

        return AdminSimpleResponse.toDto("공지가 성공적으로 작성되었습니다.");
    }

    /**
     * 공지를 삭제합니다.
     *
     * @param boardId 삭제할 공지 게시글 ID
     */
    @Transactional
    @Override
    public void deleteAnnouncement(Long boardId) {
        Board board = findAnnouncement(boardId);
        board.updateBoardStatus(BoardStatus.DELETED);
        imageFileService.deleteBoardImages(boardId);
    }

    /**
     * 공지를 수정합니다.
     *
     * @param boardId 수정할 공지 게시글 ID
     * @param request 공지 수정 요청
     */
    @Transactional
    @Override
    public void updateAnnouncement(Long boardId, AnnouncementUpdateRequest request) {
        validateAnnouncementUpdateRequest(request);

        Board board = findAnnouncement(boardId);
        board.updateAnnouncement(request.getBoardTitle(), request.getBoardContent());
        if (request.getImageFileUrl() != null) {
            imageFileService.replaceBoardImages(board, toImageList(request.getImageFileUrl()));
        }
    }

    /**
     * 공지 목록을 조회합니다.
     *
     * @param pageable 공지 목록 페이지 요청 정보
     * @return 공지 목록 조회 결과
     */
    @Transactional(readOnly = true)
    @Override
    public AnnouncementListResponse getAnnouncementList(Pageable pageable) {
        if (pageable == null || pageable.getPageNumber() < 0 || pageable.getPageSize() <= 0 || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new AdminException(INVALID_REQUEST);
        }

        Page<Board> page = boardRepository.findAllByBoardTypeAndBoardStatus(BoardType.NOTICE, BoardStatus.PUBLISHED, pageable);
        List<AnnouncementItemResponse> items = page.getContent().stream()
                .map(board -> AnnouncementItemResponse.toDto(board, firstImageUrl(board.getBoardId())))
                .toList();

        return AnnouncementListResponse.builder()
                .pageInfo(PageInfoResponse.toDto(page))
                .data(items)
                .build();
    }

    /**
     * 공지 상세를 조회합니다.
     *
     * @param boardId 조회할 공지 게시글 ID
     * @return 공지 상세 정보
     */
    @Transactional(readOnly = true)
    @Override
    public AnnouncementDetailResponse getAnnouncementDetail(Long boardId) {
        Board board = findAnnouncement(boardId);
        return AnnouncementDetailResponse.toDto(board, firstImageUrl(boardId));
    }

    private List<Report> findReports(AdminReportType reportType, ReportStatus reportStatus) {
        return switch (reportType) {
            case BOARD -> reportStatus == null
                    ? reportRepository.findAllByBoardIsNotNull()
                    : reportRepository.findAllByBoardIsNotNullAndReportStatus(reportStatus);
            case USER -> reportStatus == null
                    ? reportRepository.findAllByReportedUserIsNotNull()
                    : reportRepository.findAllByReportedUserIsNotNullAndReportStatus(reportStatus);
            case COMMENT -> reportStatus == null
                    ? reportRepository.findAllByCommentIsNotNull()
                    : reportRepository.findAllByCommentIsNotNullAndReportStatus(reportStatus);
        };
    }

    private List<ReportListItemResponse> groupReports(AdminReportType reportType, List<Report> reports) {
        Function<Report, Object> classifier = switch (reportType) {
            case BOARD -> report -> report.getBoard().getBoardId();
            case USER -> report -> report.getReportedUser().getUsersId();
            case COMMENT -> report -> report.getComment().getCommentId();
        };

        return reports.stream()
                .collect(Collectors.groupingBy(classifier))
                .values()
                .stream()
                .map(group -> toReportListItem(reportType, group))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ReportListItemResponse toReportListItem(AdminReportType reportType, List<Report> reports) {
        List<Report> sortedReports = new ArrayList<>(reports);
        sortedReports.sort(Comparator.comparing(Report::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        Report latestReport = sortedReports.get(0);

        return ReportListItemResponse.builder()
                .reportType(reportType.name())
                .targetInfo(buildTargetInfo(reportType, latestReport))
                .reportedUser(buildReportedUserInfo(reportType, latestReport))
                .totalReportCount(sortedReports.size())
                .representativeReason(latestReport.getReportReason())
                .reportStatus(resolveAggregateStatus(sortedReports))
                .lastReportedAt(latestReport.getCreatedAt())
                .build();
    }

    private ReportTargetInfoResponse buildTargetInfo(AdminReportType reportType, Report report) {
        return switch (reportType) {
            case BOARD -> ReportTargetInfoResponse.builder()
                    .id(report.getBoard().getBoardId())
                    .summary(report.getBoard().getBoardTitle())
                    .build();
            case USER -> ReportTargetInfoResponse.builder()
                    .id(report.getReportedUser().getUsersId().toString())
                    .summary("사용자 닉네임: " + report.getReportedUser().getNickname())
                    .build();
            case COMMENT -> ReportTargetInfoResponse.builder()
                    .id(report.getComment().getCommentId())
                    .summary(report.getComment().getCommentContent())
                    .build();
        };
    }

    private UserInfoResponse buildReportedUserInfo(AdminReportType reportType, Report report) {
        return switch (reportType) {
            case BOARD -> UserInfoResponse.toDto(report.getBoard().getUser());
            case USER -> UserInfoResponse.toDto(report.getReportedUser());
            case COMMENT -> UserInfoResponse.toDto(report.getComment().getUser());
        };
    }

    private ReportStatus resolveAggregateStatus(List<Report> reports) {
        return reports.stream().anyMatch(report -> report.getReportStatus() == ReportStatus.PENDING)
                ? ReportStatus.PENDING
                : ReportStatus.COMPLETED;
    }

    private Comparator<ReportListItemResponse> buildReportListComparator(String sort) {
        String normalized = sort == null || sort.isBlank() ? "lastReportedAt,desc" : sort;
        String[] tokens = normalized.split(",");
        String property = tokens[0].trim();
        boolean descending = tokens.length < 2 || !"asc".equalsIgnoreCase(tokens[1].trim());

        Comparator<ReportListItemResponse> comparator = switch (property) {
            case "totalReportCount" -> Comparator.comparing(ReportListItemResponse::getTotalReportCount);
            case "lastReportedAt" -> Comparator.comparing(ReportListItemResponse::getLastReportedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> throw new AdminException(INVALID_REQUEST);
        };

        return descending ? comparator.reversed() : comparator;
    }

    private Board findAnnouncement(Long boardId) {
        validatePositiveId(boardId);
        return boardRepository.findByBoardIdAndBoardType(boardId, BoardType.NOTICE)
                .filter(board -> board.getBoardStatus() != BoardStatus.DELETED)
                .orElseThrow(() -> new AdminException(ANNOUNCEMENT_NOT_FOUND));
    }

    private String firstImageUrl(Long boardId) {
        List<String> imageUrls = imageFileService.getBoardImageUrls(boardId);
        return imageUrls.isEmpty() ? null : imageUrls.get(0);
    }

    private List<String> toImageList(String imageFileUrl) {
        if (imageFileUrl == null || imageFileUrl.isBlank()) {
            return List.of();
        }
        return List.of(imageFileUrl);
    }

    private void validateAnnouncementCreateRequest(UUID adminId, AnnouncementCreateRequest request) {
        if (adminId == null || request == null || isBlank(request.getBoardTitle()) || isBlank(request.getBoardContent())) {
            throw new AdminException(INVALID_REQUEST);
        }
        if (request.getViewCount() != null && request.getViewCount() < 0) {
            throw new AdminException(INVALID_REQUEST);
        }
    }

    private void validateAnnouncementUpdateRequest(AnnouncementUpdateRequest request) {
        if (request == null) {
            throw new AdminException(INVALID_REQUEST);
        }
        boolean hasTitle = request.getBoardTitle() != null;
        boolean hasContent = request.getBoardContent() != null;
        boolean hasImage = request.getImageFileUrl() != null;

        if (!hasTitle && !hasContent && !hasImage) {
            throw new AdminException(INVALID_REQUEST);
        }
        if ((hasTitle && request.getBoardTitle().isBlank()) || (hasContent && request.getBoardContent().isBlank())) {
            throw new AdminException(INVALID_REQUEST);
        }
    }

    private void validatePageRequest(AdminReportType reportType, int page, int size) {
        if (reportType == null || page < 0 || size <= 0 || size > MAX_PAGE_SIZE || page > Integer.MAX_VALUE / size) {
            throw new AdminException(INVALID_REQUEST);
        }
    }

    private void validatePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new AdminException(INVALID_REQUEST);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
