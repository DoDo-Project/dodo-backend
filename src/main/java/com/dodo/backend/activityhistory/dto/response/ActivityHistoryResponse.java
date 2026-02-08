package com.dodo.backend.activityhistory.dto.response;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityType;
import com.dodo.backend.pet.entity.Pet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 활동기록 도메인과 관련된 응답 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "활동기록 관련 응답 DTO 그룹")
public class ActivityHistoryResponse {

    /**
     * 활동 기록 생성 성공 시 반환되는 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 기록 생성 성공 응답")
    public static class ActivityCreateResponse {

        @Schema(description = "응답 메시지", example = "활동 기록이 성공적으로 생성되었습니다.")
        private String message;

        @Schema(description = "생성된 활동 기록 ID", example = "1234")
        private Long historyId;

        @Schema(description = "활동 유형", example = "WALKING")
        private String activityType;

        /**
         * {@link ActivityHistory} 엔티티를 생성 응답 DTO로 변환합니다.
         *
         * @param activityHistory 변환할 활동 기록 엔티티
         * @param message         성공 메시지
         * @return 변환된 ActivityCreateResponse 객체
         */
        public static ActivityCreateResponse toDto(ActivityHistory activityHistory, String message) {
            return ActivityCreateResponse.builder()
                    .message(message)
                    .historyId(activityHistory.getHistoryId())
                    .activityType(activityHistory.getActivityType().name())
                    .build();
        }
    }

    /**
     * 데이터 없이 성공 메시지만 반환할 때 사용하는 간단한 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 관련 작업 성공 응답")
    public static class ActivitySimpleResponse {

        @Schema(description = "응답 메시지", example = "활동 기록이 (시작, 재개, 삭제)되었습니다")
        private String message;

        /**
         * 전달받은 메시지를 포함하는 단순 응답 DTO를 생성합니다.
         *
         * @param message 클라이언트에게 전달할 처리 결과 메시지
         * @return 메시지가 설정된 {@link ActivitySimpleResponse} 객체
         */
        public static ActivitySimpleResponse toDto(String message) {
            return ActivitySimpleResponse.builder()
                    .message(message)
                    .build();
        }
    }

    /**
     * 활동 종료 성공 시 반환되는 상세 응답 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 기록 종료 성공 응답")
    public static class ActivityFinishResponse {

        @Schema(description = "응답 메시지", example = "활동 기록이 성공적으로 종료되었습니다.")
        private String message;

        @Schema(description = "활동 기록 ID", example = "101")
        private Long historyId;

        @Schema(description = "활동 유형", example = "WALKING")
        private String activityType;

        @Schema(description = "이동 거리(km)", example = "5.235")
        private BigDecimal distance;

        @Schema(description = "활동 시작 시간", example = "2025-10-01T20:30:00")
        private LocalDateTime activityHistoryStartAt;

        @Schema(description = "활동 종료 시간", example = "2025-10-01T21:30:00")
        private LocalDateTime activityHistoryEndAt;

        @Schema(description = "활동 상태", example = "COMPLETED")
        private String activityHistoryStatus;

        /**
         * 활동 종료 정보를 기반으로 클라이언트에게 전달할 응답 DTO를 생성합니다.
         * <p>
         * 엔티티 객체 전체를 노출하지 않고, 필요한 필드 값들만 개별적으로 전달받아 객체를 구성합니다.
         * </p>
         *
         * @param historyId              활동 기록의 고유 ID
         * @param activityType           활동 유형 (Enum)
         * @param distance               총 이동 거리 (km)
         * @param activityHistoryStartAt 활동 시작 시간
         * @param activityHistoryEndAt   활동 종료 시간
         * @param activityHistoryStatus  최종 변경된 활동 상태 (문자열)
         * @param message                성공 메시지
         * @return 생성된 {@link ActivityFinishResponse} 객체
         */
        public static ActivityFinishResponse toDto(Long historyId,
                                                   ActivityType activityType,
                                                   BigDecimal distance,
                                                   LocalDateTime activityHistoryStartAt,
                                                   LocalDateTime activityHistoryEndAt,
                                                   String activityHistoryStatus,
                                                   String message) {
            return ActivityFinishResponse.builder()
                    .message(message)
                    .historyId(historyId)
                    .activityType(activityType.name())
                    .distance(distance)
                    .activityHistoryStartAt(activityHistoryStartAt)
                    .activityHistoryEndAt(activityHistoryEndAt)
                    .activityHistoryStatus(activityHistoryStatus)
                    .build();
        }
    }

    /**
     * 활동 기록 목록 조회 시 반환되는 페이지네이션 응답 DTO입니다.
     * <p>
     * 페이징 정보(전체 페이지, 요소 수 등)와 활동 기록 요약 목록({@link ActivityHistorySummary})을 포함합니다.
     * </p>
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 기록 페이지 응답")
    public static class ActivityHistoryPageResponse {

        @Schema(description = "응답 메시지", example = "활동 기록 목록을 성공적으로 조회했습니다.")
        private String message;

        @Schema(description = "활동 기록 리스트")
        private List<ActivityHistorySummary> histories;

        @Schema(description = "전체 페이지 수", example = "5")
        private int totalPages;

        @Schema(description = "전체 데이터 수", example = "48")
        private long totalElements;

        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        /**
         * Page 객체와 변환된 DTO 리스트, 응답 메시지를 조합하여 클라이언트에게 반환할 페이지네이션 응답 DTO를 생성합니다.
         *
         * @param message   클라이언트에게 전달할 성공 메시지 (예: "활동 기록 목록을 성공적으로 조회했습니다.")
         * @param page      JPA Page 객체 (전체 페이지 수, 전체 데이터 수 등 페이징 메타데이터 추출용)
         * @param histories 엔티티에서 변환된 활동 기록 요약 정보({@link ActivityHistorySummary}) 리스트
         * @return 초기화된 {@link ActivityHistoryPageResponse} 객체
         */
        public static ActivityHistoryPageResponse toDto(String message, Page<ActivityHistory> page, List<ActivityHistorySummary> histories) {
            return ActivityHistoryPageResponse.builder()
                    .message(message)
                    .histories(histories)
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .currentPage(page.getNumber())
                    .pageSize(page.getSize())
                    .build();
        }
    }

    /**
     * 활동 기록 목록의 개별 항목 정보를 담는 요약 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 기록 요약 정보")
    public static class ActivityHistorySummary {

        @Schema(description = "활동 기록 ID", example = "101")
        private Long historyId;

        @Schema(description = "활동 유형", example = "WALKING")
        private String activityType;

        @Schema(description = "이동 거리 (km)", example = "5.7")
        private BigDecimal distance;

        @Schema(description = "활동 시작 시간", example = "2025-09-30T10:00:00")
        private LocalDateTime activityHistoryStartAt;

        @Schema(description = "활동 종료 시간", example = "2025-09-30T11:15:00")
        private LocalDateTime activityHistoryEndAt;

        @Schema(description = "활동 상태", example = "COMPLETED")
        private String activityHistoryStatus;

        @Schema(description = "반응 수 (좋아요 등)", example = "12")
        private Integer reactionCount;

        @Schema(description = "평균 심박수", example = "125")
        private Integer heartAverage;

        @Schema(description = "활동한 반려동물 정보")
        private PetInfo pet;

        /**
         * ActivityHistory 엔티티와 반려동물 프로필 이미지 URL을 조합하여 요약 정보 DTO로 변환합니다.
         * <p>
         * 이 메서드는 엔티티의 데이터를 클라이언트 응답 형식에 맞게 가공하며,
         * 반려동물 정보는 내부의 {@link PetInfo#toDto} 메서드를 통해 변환됩니다.
         * </p>
         *
         * @param history            변환할 활동 기록 엔티티
         * @param petProfileImageUrl 반려동물 프로필 이미지 URL (이미지가 없을 경우 null)
         * @return 변환된 {@link ActivityHistorySummary} 객체
         */
        public static ActivityHistorySummary toDto(ActivityHistory history, String petProfileImageUrl) {
            return ActivityHistorySummary.builder()
                    .historyId(history.getHistoryId())
                    .activityType(history.getActivityType().name())
                    .distance(history.getDistance())
                    .activityHistoryStartAt(history.getActivityHistoryStartAt())
                    .activityHistoryEndAt(history.getActivityHistoryEndAt())
                    .activityHistoryStatus(history.getActivityHistoryStatus().name())
                    .reactionCount(0)
                    .heartAverage(0)
                    .pet(PetInfo.toDto(history.getPet(), petProfileImageUrl))
                    .build();
        }
    }

    /**
     * 활동 기록 내 반려동물 정보를 담는 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "반려동물 요약 정보")
    public static class PetInfo {

        @Schema(description = "반려동물 ID", example = "101")
        private Long id;

        @Schema(description = "이름", example = "보리")
        private String name;

        @Schema(description = "나이", example = "5")
        private Integer age;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profiles/kim.jpg")
        private String profileImageUrl;

        /**
         * Pet 엔티티와 프로필 이미지 URL을 사용하여 반려동물 요약 정보 DTO를 생성합니다.
         *
         * @param pet             변환할 반려동물 엔티티
         * @param profileImageUrl 프로필 이미지 URL
         * @return 생성된 {@link PetInfo} 객체
         */
        public static PetInfo toDto(Pet pet, String profileImageUrl) {
            return PetInfo.builder()
                    .id(pet.getPetId())
                    .name(pet.getPetName())
                    .age(pet.getAge())
                    .profileImageUrl(profileImageUrl)
                    .build();
        }
    }

    /**
     * 활동 기록의 상세 정보를 클라이언트에게 전달하기 위한 응답 DTO 클래스입니다.
     * <p>
     * 활동의 기본 정보(ID, 거리, 시간 등)와 위치 정보,
     * 그리고 사용자 반응(좋아요 수, 본인 좋아요 여부) 정보를 포함합니다.
     * </p>
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "활동 기록 상세 조회 응답 DTO")
    public static class ActivityHistoryDetailResponse {

        @Schema(description = "응답 메시지", example = "해당 활동 정보를 성공적으로 조회했습니다.")
        private final String message;

        @Schema(description = "활동 기록 ID", example = "101")
        private final Long historyId;

        @Schema(description = "반려동물 ID", example = "12")
        private final Long petId;

        @Schema(description = "이동 거리 (km)", example = "5.25")
        private final BigDecimal distance;

        @Schema(description = "활동 시작 시간", example = "2025-09-30T14:00:00")
        private final LocalDateTime activityHistoryStartAt;

        @Schema(description = "활동 종료 시간", example = "2025-09-30T14:35:00")
        private final LocalDateTime activityHistoryEndAt;

        @Schema(description = "시작 위도", example = "37.4979")
        private final BigDecimal startLatitude;

        @Schema(description = "시작 경도", example = "127.0276")
        private final BigDecimal startLongitude;

        @Schema(description = "반응(좋아요) 수", example = "28")
        private final Integer reactionCount;

        @Schema(description = "내가 좋아요를 눌렀는지 여부", example = "true")
        private final Boolean isLikedByMe;

        /**
         * 개별 데이터를 받아 상세 조회 응답 DTO 객체를 생성합니다.
         *
         * @param message                응답 메시지
         * @param historyId              활동 기록 ID
         * @param petId                  반려동물 ID
         * @param distance               이동 거리
         * @param activityHistoryStartAt 활동 시작 시간
         * @param activityHistoryEndAt   활동 종료 시간
         * @param startLatitude          시작 위도
         * @param startLongitude         시작 경도
         * @param reactionCount          좋아요 수
         * @param isLikedByMe            본인 좋아요 여부
         * @return 생성된 {@link ActivityHistoryDetailResponse} 객체
         */
        public static ActivityHistoryDetailResponse toDto(
                String message,
                Long historyId,
                Long petId,
                BigDecimal distance,
                LocalDateTime activityHistoryStartAt,
                LocalDateTime activityHistoryEndAt,
                BigDecimal startLatitude,
                BigDecimal startLongitude,
                Integer reactionCount,
                Boolean isLikedByMe
        ) {
            return ActivityHistoryDetailResponse.builder()
                    .message(message)
                    .historyId(historyId)
                    .petId(petId)
                    .distance(distance)
                    .activityHistoryStartAt(activityHistoryStartAt)
                    .activityHistoryEndAt(activityHistoryEndAt)
                    .startLatitude(startLatitude)
                    .startLongitude(startLongitude)
                    .reactionCount(reactionCount)
                    .isLikedByMe(isLikedByMe)
                    .build();
        }
    }
}