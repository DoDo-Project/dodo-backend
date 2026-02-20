package com.dodo.backend.main.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 메인 페이지 응답 DTO 모음입니다.
 */
@Schema(description = "메인 페이지 응답 DTO 모음")
public class MainResponse {

    /**
     * 메인 페이지 응답 본문입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "메인 페이지 응답")
    public static class MainPageResponse {

        @Schema(description = "응답 메시지", example = "메인 페이지 정보를 가져오는데 성공했습니다.")
        private String message;

        @Schema(description = "반려동물 프로필 목록")
        private List<PetProfile> petProfiles;

        @Schema(description = "건강 리포트 목록")
        private List<HealthReport> healthReports;

        @Schema(description = "공지사항 목록")
        private List<Announcement> announcement;

        /**
         * 메인 페이지 응답 DTO를 생성합니다.
         *
         * @param message       응답 메시지
         * @param petProfiles   반려동물 프로필 요약
         * @param healthReports 건강 리포트 요약
         * @param announcement  공지사항 요약
         * @return 메인 페이지 응답 DTO
         */
        public static MainPageResponse toDto(String message,
                                             List<PetProfile> petProfiles,
                                             List<HealthReport> healthReports,
                                             List<Announcement> announcement) {
            return MainPageResponse.builder()
                    .message(message)
                    .petProfiles(petProfiles)
                    .healthReports(healthReports)
                    .announcement(announcement)
                    .build();
        }
    }

    /**
     * 메인 페이지에 노출되는 반려동물 프로필 요약입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 프로필 요약")
    public static class PetProfile {

        @Schema(description = "반려동물 ID", example = "101")
        private Long petId;

        @Schema(description = "반려동물 이름", example = "보리")
        private String name;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/images/bori.jpg")
        private String imageFileUrl;

        @Schema(description = "품종", example = "말티즈")
        private String breed;

        @Schema(description = "나이", example = "5")
        private Integer age;

        @Schema(description = "종", example = "CANINE")
        private String spercies;

        @Schema(description = "성별", example = "MALE")
        private String sex;

        @Schema(description = "체중", example = "4.2")
        private Double weight;
    }

    /**
     * 메인 페이지에 노출되는 건강 리포트 요약입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "건강 리포트 요약")
    public static class HealthReport {

        @Schema(description = "반려동물 ID", example = "101")
        private Long petId;

        @Schema(description = "반려동물 이름", example = "보리")
        private String petName;

        @Schema(description = "대시보드 ID", example = "201")
        private Long dashboardId;

        @Schema(description = "리포트 제목", example = "정기 건강검진 요약")
        private String healthReportTitle;

        @Schema(description = "리포트 요약", example = "전반적으로 양호하나, 체중 관리가 필요합니다.")
        private String healthReportSummary;

        @Schema(description = "리포트 본문", example = "")
        private String healthReportContent;

        @Schema(description = "검진 일자", example = "2025-09-15")
        private LocalDate checkupDate;
    }

    /**
     * 메인 페이지에 노출되는 공지사항 요약입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "공지사항 요약")
    public static class Announcement {

        @Schema(description = "제목", example = "공지 제목")
        private String boardTitle;

        @Schema(description = "내용", example = "공지 내용입니다.")
        private String boardContent;

        @Schema(description = "이미지 URL", example = "http://www.asdkfjasdfas")
        private String imageFileUrl;

        @Schema(description = "조회 수", example = "10")
        private Integer viewCount;
    }
}
