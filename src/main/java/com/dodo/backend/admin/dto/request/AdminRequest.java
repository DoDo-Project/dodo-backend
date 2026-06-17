package com.dodo.backend.admin.dto.request;

import com.dodo.backend.board.entity.NoticeTag;
import com.dodo.backend.report.entity.ReportStatus;
import com.dodo.backend.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 API에서 사용하는 요청 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "관리자 요청 DTO 그룹")
public class AdminRequest {

    /**
     * 유저 계정 상태 변경 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "유저 계정 상태 변경 요청")
    public static class UserStatusUpdateRequest {

        @NotNull(message = "상태 값은 필수입니다.")
        @Schema(description = "변경할 유저 상태", example = "SUSPENDED")
        private UserStatus status;
    }

    /**
     * 신고 처리 상태 변경 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "신고 처리 상태 변경 요청")
    public static class ReportStatusUpdateRequest {

        @NotNull(message = "상태 값은 필수입니다.")
        @Schema(description = "변경할 신고 처리 상태", example = "COMPLETED")
        private ReportStatus status;
    }

    /**
     * 공지 작성 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공지 작성 요청")
    public static class AnnouncementCreateRequest {

        @NotBlank(message = "공지 제목은 필수입니다.")
        @Schema(description = "공지 제목", example = "공지 제목")
        private String boardTitle;

        @NotBlank(message = "공지 내용은 필수입니다.")
        @Schema(description = "공지 내용", example = "공지 내용입니다.")
        private String boardContent;

        @Schema(description = "공지 이미지 URL", example = "https://example.com/images/notice.jpg")
        private String imageFileUrl;

        @Schema(description = "조회 수", example = "10")
        private Integer viewCount;

        @Schema(description = "공지 태그", example = "INFO")
        private NoticeTag noticeTag;
    }

    /**
     * 공지 수정 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공지 수정 요청")
    public static class AnnouncementUpdateRequest {

        @Schema(description = "수정할 공지 제목", example = "수정된 공지입니다.")
        private String boardTitle;

        @Schema(description = "수정할 공지 내용", example = "수정수정수정")
        private String boardContent;

        @Schema(description = "수정할 공지 이미지 URL", example = "https://example.com/images/bori.jpg")
        private String imageFileUrl;
    }
}
