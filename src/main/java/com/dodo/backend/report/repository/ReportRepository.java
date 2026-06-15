package com.dodo.backend.report.repository;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.report.entity.Report;
import com.dodo.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
