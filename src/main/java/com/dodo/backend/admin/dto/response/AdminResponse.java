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
        @Schema(description = "요청 처리 결과 메시지", example = "성공적으로 상태를 변경했습니다.")
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
        @Schema(description = "현재 페이지 번호. 0부터 시작합니다.", example = "0")
        private int page;
        @Schema(description = "페이지당 데이터 개수", example = "20")
        private int size;
        @Schema(description = "조회 조건에 맞는 전체 데이터 개수", example = "57")
        private long totalElements;
        @Schema(description = "전체 페이지 수", example = "3")
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
        @Schema(description = "유저 UUID 문자열", example = "3eb3581d-b046-11f1-bae4-7085c2970281")
        private String userId;
        @Schema(description = "유저 닉네임", example = "도도집사")
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
        @Schema(description = "유저 고유 UUID", example = "3eb3581d-b046-11f1-bae4-7085c2970281")
        private UUID userId;
        @Schema(description = "유저 이메일", example = "user@dodo.com")
        private String email;
        @Schema(description = "유저 이름", example = "홍길동")
        private String name;
        @Schema(description = "서비스에서 사용하는 유저 닉네임", example = "도도집사")
        private String nickname;
        @Schema(description = "유저 활동 지역", example = "서울특별시 성동구")
        private String region;
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private String profileUrl;
        @Schema(description = "유저 권한. 관리자 목록 API에서는 USER만 반환됩니다.", example = "USER")
        private UserRole role;
        @Schema(description = "현재 계정 상태", example = "ACTIVE")
        private UserStatus status;
        @Schema(description = "가입 일시", example = "2026-09-01T10:30:00")
        private LocalDateTime userCreatedAt;
        @Schema(description = "계정 상태가 마지막으로 변경된 일시. 변경 이력이 없으면 null입니다.", example = "2026-09-10T14:20:00", nullable = true)
        private LocalDateTime userStatusUpdatedAt;
        @Schema(description = "정지 종료 일시. 기간 정지 상태가 아니면 null입니다.", example = "2026-09-17T14:20:00", nullable = true)
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
        @Schema(description = "유저 목록의 페이지 정보")
        private PageInfoResponse pageInfo;
        @Schema(description = "검색 및 상태 조건에 맞는 일반 유저 목록")
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
        @Schema(description = "신고 대상 게시글 제목", example = "산책 중 만난 친구")
        private String boardTitle;
        @Schema(description = "신고 대상 게시글 내용", example = "오늘 산책 중에 새로운 친구를 만났어요.")
        private String boardContent;
        @Schema(description = "신고 대상 게시글 작성 일시", example = "2026-09-01T10:30:00")
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
        @Schema(description = "신고 고유 ID", example = "10")
        private Long reportId;
        @Schema(description = "신고를 접수한 유저 정보")
        private UserInfoResponse reporterInfo;
        @Schema(description = "신고 사유", example = "SPAM")
        private ReportReason reportReason;
        @Schema(description = "신고 처리 상태", example = "PENDING")
        private ReportStatus reportStatus;
        @Schema(description = "신고 접수 일시", example = "2026-09-02T11:20:00")
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
        @Schema(description = "신고 대상 게시글 ID", example = "123")
        private Long boardId;
        @Schema(description = "신고 대상 게시글 정보")
        private BoardInfoResponse boardInfo;
        @Schema(description = "게시글 작성자이자 신고 대상인 유저 정보")
        private UserInfoResponse reportedUserInfo;
        @Schema(description = "해당 게시글에 접수된 전체 신고 건수", example = "3")
        private long totalReportCount;
        @Schema(description = "해당 게시글에 접수된 신고 상세 목록")
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
        @Schema(description = "신고 대상 유저 정보")
        private UserInfoResponse reportedUserInfo;
        @Schema(description = "해당 유저에게 접수된 전체 신고 건수", example = "3")
        private long totalReportCount;
        @Schema(description = "해당 유저에게 접수된 신고 상세 목록")
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
        @Schema(description = "신고 고유 ID", example = "10")
        private Long reportId;
        @Schema(description = "신고를 접수한 유저의 닉네임. 유저 정보가 없으면 null입니다.", example = "도도집사", nullable = true)
        private String reporterNickname;
        @Schema(description = "신고 사유", example = "ABUSE")
        private ReportReason reportReason;
        @Schema(description = "신고 접수 일시", example = "2026-09-02T11:20:00")
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
        @Schema(description = "신고 대상 댓글 ID", example = "456")
        private Long commentId;
        @Schema(description = "신고 대상 댓글 내용", example = "신고된 댓글 내용입니다.")
        private String commentContent;
        @Schema(description = "댓글 작성자이자 신고 대상인 유저의 닉네임. 유저 정보가 없으면 null입니다.", example = "도도집사", nullable = true)
        private String reportedNickname;
        @Schema(description = "해당 댓글에 접수된 전체 신고 건수", example = "2")
        private long totalReportCount;
        @Schema(description = "해당 댓글에 접수된 신고 상세 목록")
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
        @Schema(description = "신고 대상 ID. BOARD와 COMMENT의 숫자 ID도 문자열로 반환되며 USER는 UUID 문자열입니다.", example = "123")
        private String id;
        @Schema(description = "신고 대상을 식별하기 위한 요약. 게시글 제목, 유저 닉네임 또는 댓글 내용이 들어갑니다.", example = "산책 중 만난 친구")
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
        @Schema(description = "신고 대상 유형", example = "BOARD", allowableValues = {"BOARD", "USER", "COMMENT"})
        private String reportType;
        @Schema(description = "신고 대상의 ID와 요약 정보")
        private ReportTargetInfoResponse targetInfo;
        @Schema(description = "신고 대상 콘텐츠의 작성자 또는 신고 대상 유저 정보")
        private UserInfoResponse reportedUser;
        @Schema(description = "동일 대상에 접수된 전체 신고 건수", example = "5")
        private long totalReportCount;
        @Schema(description = "가장 최근에 접수된 신고의 사유", example = "SPAM")
        private ReportReason representativeReason;
        @Schema(description = "묶인 신고의 통합 처리 상태. 하나라도 미처리이면 PENDING입니다.", example = "PENDING")
        private ReportStatus reportStatus;
        @Schema(description = "해당 대상에 가장 최근 신고가 접수된 일시", example = "2026-09-02T11:20:00")
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
        @Schema(description = "신고 목록의 페이지 정보")
        private PageInfoResponse pageInfo;
        @Schema(description = "신고 대상별로 묶인 신고 목록")
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
        @Schema(description = "공지 게시글 ID", example = "31")
        private Long boardId;
        @Schema(description = "공지 제목", example = "서비스 점검 안내")
        private String boardTitle;
        @Schema(description = "공지 내용", example = "9월 20일 새벽에 서비스 점검이 진행됩니다.")
        private String boardContent;
        @Schema(description = "공지 대표 이미지 URL. 이미지가 없으면 null입니다.", example = "https://example.com/notice.jpg", nullable = true)
        private String imageFileUrl;
        @Schema(description = "공지 조회 수", example = "120")
        private Integer viewCount;
        @Schema(description = "공지 작성 일시", example = "2026-09-01T10:30:00")
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
        @Schema(description = "공지 목록의 페이지 정보")
        private PageInfoResponse pageInfo;
        @Schema(description = "공지 목록")
        private List<AnnouncementItemResponse> data;
        @Schema(description = "공지 목록 조회 결과 메시지", example = "공지 목록을 조회했습니다.")
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
        @Schema(description = "공지 게시글 ID", example = "31")
        private Long boardId;
        @Schema(description = "공지 제목", example = "서비스 점검 안내")
        private String boardTitle;
        @Schema(description = "공지 전체 내용", example = "9월 20일 새벽에 서비스 점검이 진행됩니다.")
        private String boardContent;
        @Schema(description = "공지 이미지 URL. 이미지가 없으면 null입니다.", example = "https://example.com/notice.jpg", nullable = true)
        private String imageFileUrl;
        @Schema(description = "공지 조회 수", example = "120")
        private Integer viewCount;
        @Schema(description = "공지 작성 일시", example = "2026-09-01T10:30:00")
        private LocalDateTime boardCreatedAt;
        @Schema(description = "공지 최종 수정 일시", example = "2026-09-02T09:00:00")
        private LocalDateTime boardModifiedAt;
        @Schema(description = "공지 상세 조회 결과 메시지", example = "공지 상세보기에 성공했습니다.")
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
