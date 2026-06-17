package com.dodo.backend.report.repository;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.report.entity.Report;
import com.dodo.backend.report.entity.ReportStatus;
import com.dodo.backend.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * {@link Report} 엔티티의 데이터베이스 접근을 담당하는 repository입니다.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 특정 유저가 특정 게시글을 이미 신고했는지 확인합니다.
     *
     * @param reporter 신고자
     * @param board    신고 대상 게시글
     * @return 이미 신고했으면 true
     */
    boolean existsByReporterAndBoard(User reporter, Board board);

    /**
     * 특정 유저가 특정 유저를 이미 신고했는지 확인합니다.
     *
     * @param reporter     신고자
     * @param reportedUser 신고 대상 유저
     * @return 이미 신고했으면 true
     */
    boolean existsByReporterAndReportedUser(User reporter, User reportedUser);

    /**
     * 특정 유저가 특정 댓글을 이미 신고했는지 확인합니다.
     *
     * @param reporter 신고자
     * @param comment  신고 대상 댓글
     * @return 이미 신고했으면 true
     */
    boolean existsByReporterAndComment(User reporter, Comment comment);

    /**
     * 특정 게시글에 대한 신고 목록을 조회합니다.
     *
     * @param boardId 게시글 ID
     * @return 신고 목록
     */
    @EntityGraph(attributePaths = {"reporter", "board", "board.user"})
    List<Report> findAllByBoard_BoardIdOrderByCreatedAtDesc(Long boardId);

    /**
     * 특정 유저에 대한 신고 목록을 조회합니다.
     *
     * @param userId 유저 ID
     * @return 신고 목록
     */
    @EntityGraph(attributePaths = {"reporter", "reportedUser"})
    List<Report> findAllByReportedUser_UsersIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 특정 댓글에 대한 신고 목록을 조회합니다.
     *
     * @param commentId 댓글 ID
     * @return 신고 목록
     */
    @EntityGraph(attributePaths = {"reporter", "comment", "comment.user"})
    List<Report> findAllByComment_CommentIdOrderByCreatedAtDesc(Long commentId);

    /**
     * 게시글 신고 목록을 조회합니다.
     *
     * @return 게시글 신고 목록
     */
    @EntityGraph(attributePaths = {"reporter", "board", "board.user"})
    List<Report> findAllByBoardIsNotNull();

    /**
     * 특정 상태의 게시글 신고 목록을 조회합니다.
     *
     * @param reportStatus 신고 처리 상태
     * @return 게시글 신고 목록
     */
    @EntityGraph(attributePaths = {"reporter", "board", "board.user"})
    List<Report> findAllByBoardIsNotNullAndReportStatus(ReportStatus reportStatus);

    /**
     * 유저 신고 목록을 조회합니다.
     *
     * @return 유저 신고 목록
     */
    @EntityGraph(attributePaths = {"reporter", "reportedUser"})
    List<Report> findAllByReportedUserIsNotNull();

    /**
     * 특정 상태의 유저 신고 목록을 조회합니다.
     *
     * @param reportStatus 신고 처리 상태
     * @return 유저 신고 목록
     */
    @EntityGraph(attributePaths = {"reporter", "reportedUser"})
    List<Report> findAllByReportedUserIsNotNullAndReportStatus(ReportStatus reportStatus);

    /**
     * 댓글 신고 목록을 조회합니다.
     *
     * @return 댓글 신고 목록
     */
    @EntityGraph(attributePaths = {"reporter", "comment", "comment.user"})
    List<Report> findAllByCommentIsNotNull();

    /**
     * 특정 상태의 댓글 신고 목록을 조회합니다.
     *
     * @param reportStatus 신고 처리 상태
     * @return 댓글 신고 목록
     */
    @EntityGraph(attributePaths = {"reporter", "comment", "comment.user"})
    List<Report> findAllByCommentIsNotNullAndReportStatus(ReportStatus reportStatus);

    /**
     * 댓글에 연결된 신고 내역을 삭제합니다.
     *
     * @param comment 삭제 대상 댓글
     */
    void deleteAllByComment(Comment comment);
}
