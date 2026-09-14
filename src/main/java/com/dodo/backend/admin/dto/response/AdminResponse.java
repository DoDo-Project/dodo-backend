package com.dodo.backend.admin.dto.response;

import com.dodo.backend.board.entity.Board;
import com.dodo.backend.comment.entity.Comment;
import com.dodo.backend.report.entity.Report;
import com.dodo.backend.report.entity.ReportReason;
import com.dodo.backend.report.entity.ReportStatus;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 관리자 API에서 사용하는 응답 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "관리자 응답 DTO 그룹")
public class AdminResponse {

    /**
     * 단순 메시지 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "관리자 단순 응답")
    public static class AdminSimpleResponse {
        private String message;

        public static AdminSimpleResponse toDto(String message) {
            return AdminSimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 페이지 정보 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "페이지 정보")
    public static class PageInfoResponse {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        public static PageInfoResponse toDto(int page, int size, long totalElements) {
            int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
            return PageInfoResponse.builder()
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .build();
        }

        public static PageInfoResponse toDto(Page<?> page) {
            return PageInfoResponse.builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .build();
        }
    }

    /**
     * 유저 요약 정보 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "유저 요약 정보")
    public static class UserInfoResponse {
        private String userId;
        private String nickname;

        public static UserInfoResponse toDto(User user) {
            if (user == null) {
                return null;
            }
            return UserInfoResponse.builder()
                    .userId(user.getUsersId() == null ? null : user.getUsersId().toString())
                    .nickname(user.getNickname())
                    .build();
        }
    }

    /**
     * 관리자 유저 목록의 개별 유저 정보입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "관리자 유저 목록 아이템")
    public static class UserListItemResponse {
        private UUID userId;
        private String email;
        private String name;
        private String nickname;
        private String region;
        private String profileUrl;
        private UserRole role;
        private UserStatus status;
        private LocalDateTime userCreatedAt;
        private LocalDateTime userStatusUpdatedAt;
        private LocalDateTime suspendedEndAt;

        /**
         * 유저 엔티티를 관리자 목록 아이템으로 변환합니다.
         *
         * @param user 변환할 유저 엔티티
         * @return 관리자 유저 목록 아이템
         */
        public static UserListItemResponse toDto(User user) {
            return UserListItemResponse.builder()
                    .userId(user.getUsersId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .nickname(user.getNickname())
                    .region(user.getRegion())
                    .profileUrl(user.getProfileUrl())
                    .role(user.getRole())
                    .status(user.getUserStatus())
                    .userCreatedAt(user.getUserCreatedAt())
                    .userStatusUpdatedAt(user.getUserStatusUpdatedAt())
                    .suspendedEndAt(user.getSuspendedEndAt())
                    .build();
        }
    }

    /**
     * 관리자 유저 목록 및 검색 응답입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "관리자 유저 목록 응답")
    public static class UserListResponse {
        private PageInfoResponse pageInfo;
        private List<UserListItemResponse> data;
    }

    /**
     * 신고 게시글 정보 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "신고 게시글 정보")
    public static class BoardInfoResponse {
        private String boardTitle;
        private String boardContent;
        private LocalDateTime boardCreatedAt;

        public static BoardInfoResponse toDto(Board board) {
            return BoardInfoResponse.builder()
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .boardCreatedAt(board.getBoardCreatedAt())
                    .build();
        }
    }

    /**
     * 신고 상세 아이템 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "신고 상세 아이템")
    public static class ReportDetailItemResponse {
        private Long reportId;
        private UserInfoResponse reporterInfo;
        private ReportReason reportReason;
        private ReportStatus reportStatus;
        private LocalDateTime reportCreatedAt;

        public static ReportDetailItemResponse toDto(Report report) {
            return ReportDetailItemResponse.builder()
                    .reportId(report.getReportId())
                    .reporterInfo(UserInfoResponse.toDto(report.getReporter()))
                    .reportReason(report.getReportReason())
                    .reportStatus(report.getReportStatus())
                    .reportCreatedAt(report.getCreatedAt())
                    .build();
        }
    }

    /**
     * 게시글 신고 상세 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "게시글 신고 상세 응답")
    public static class BoardReportDetailResponse {
        private Long boardId;
        private BoardInfoResponse boardInfo;
        private UserInfoResponse reportedUserInfo;
        private long totalReportCount;
        private List<ReportDetailItemResponse> reports;
    }

    /**
     * 유저 신고 상세 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "유저 신고 상세 응답")
    public static class UserReportDetailResponse {
        private UserInfoResponse reportedUserInfo;
        private long totalReportCount;
        private List<ReportDetailItemResponse> reports;
    }

    /**
     * 댓글 신고 상세 아이템 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "댓글 신고 상세 아이템")
    public static class CommentReportItemResponse {
        private Long reportId;
        private String reporterNickname;
        private ReportReason reportReason;
        private LocalDateTime reportCreatedAt;

        public static CommentReportItemResponse toDto(Report report) {
            return CommentReportItemResponse.builder()
                    .reportId(report.getReportId())
                    .reporterNickname(report.getReporter() == null ? null : report.getReporter().getNickname())
                    .reportReason(report.getReportReason())
                    .reportCreatedAt(report.getCreatedAt())
                    .build();
        }
    }

    /**
     * 댓글 신고 상세 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "댓글 신고 상세 응답")
    public static class CommentReportDetailResponse {
        private Long commentId;
        private String commentContent;
        private String reportedNickname;
        private long totalReportCount;
        private List<CommentReportItemResponse> reports;

        public static CommentReportDetailResponse toDto(Comment comment, List<Report> reports) {
            return CommentReportDetailResponse.builder()
                    .commentId(comment.getCommentId())
                    .commentContent(comment.getCommentContent())
                    .reportedNickname(comment.getUser() == null ? null : comment.getUser().getNickname())
                    .totalReportCount(reports.size())
                    .reports(reports.stream().map(CommentReportItemResponse::toDto).toList())
                    .build();
        }
    }

    /**
     * 신고 목록 대상 정보 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "신고 목록 대상 정보")
    public static class ReportTargetInfoResponse {
        private Object id;
        private String summary;
    }

    /**
     * 신고 목록 아이템 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "신고 목록 아이템")
    public static class ReportListItemResponse {
        private String reportType;
        private ReportTargetInfoResponse targetInfo;
        private UserInfoResponse reportedUser;
        private long totalReportCount;
        private ReportReason representativeReason;
        private ReportStatus reportStatus;
        private LocalDateTime lastReportedAt;
    }

    /**
     * 신고 목록 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "신고 목록 응답")
    public static class ReportListResponse {
        private PageInfoResponse pageInfo;
        private List<ReportListItemResponse> data;
    }

    /**
     * 공지 목록 아이템 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "공지 목록 아이템")
    public static class AnnouncementItemResponse {
        private Long boardId;
        private String boardTitle;
        private String boardContent;
        private String imageFileUrl;
        private Integer viewCount;
        private LocalDateTime boardCreatedAt;

        public static AnnouncementItemResponse toDto(Board board, String imageFileUrl) {
            return AnnouncementItemResponse.builder()
                    .boardId(board.getBoardId())
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .imageFileUrl(imageFileUrl)
                    .viewCount(board.getViewCount())
                    .boardCreatedAt(board.getBoardCreatedAt())
                    .build();
        }
    }

    /**
     * 공지 목록 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "공지 목록 응답")
    public static class AnnouncementListResponse {
        private PageInfoResponse pageInfo;
        private List<AnnouncementItemResponse> data;
        private String message;
    }

    /**
     * 공지 상세 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "공지 상세 응답")
    public static class AnnouncementDetailResponse {
        private Long boardId;
        private String boardTitle;
        private String boardContent;
        private String imageFileUrl;
        private Integer viewCount;
        private LocalDateTime boardCreatedAt;
        private LocalDateTime boardModifiedAt;
        private String message;

        public static AnnouncementDetailResponse toDto(Board board, String imageFileUrl, String message) {
            return AnnouncementDetailResponse.builder()
                    .boardId(board.getBoardId())
                    .boardTitle(board.getBoardTitle())
                    .boardContent(board.getBoardContent())
                    .imageFileUrl(imageFileUrl)
                    .viewCount(board.getViewCount())
                    .boardCreatedAt(board.getBoardCreatedAt())
                    .boardModifiedAt(board.getModifiedAt())
                    .message(message)
                    .build();
        }
    }
}
