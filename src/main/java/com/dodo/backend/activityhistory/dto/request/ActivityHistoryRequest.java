package com.dodo.backend.activityhistory.dto.request;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.activityhistory.entity.ActivityType;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 활동기록 도메인과 관련된 요청 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "활동기록 관련 요청 DTO 그룹")
public class ActivityHistoryRequest {

    /**
     * 새로운 활동 기록 생성을 위해 클라이언트로부터 전달받는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "활동 기록 생성 요청")
    public static class ActivityCreateRequest {

        @Schema(description = "반려동물 ID", example = "1")
        @NotNull(message = "반려동물 ID는 필수입니다.")
        private Long petId;

        @Schema(description = "활동 유형 (WALKING, SLEEPING)", example = "WALKING")
        @NotNull(message = "활동 유형은 필수입니다.")
        private String activityType;

        /**
         * 요청 DTO의 데이터를 기반으로 새로운 {@link ActivityHistory} 엔티티를 생성합니다.
         * <p>
         * 초기 상태는 '시작 전(BEFORE)'으로 설정되며, 시작 시간 등은 추후 업데이트됩니다.
         * </p>
         *
         * @param user 활동을 생성하는 사용자 엔티티
         * @param pet  활동 대상 반려동물 엔티티
         * @return 초기화된 ActivityHistory 엔티티 (상태: BEFORE)
         */
        public ActivityHistory toEntity(User user, Pet pet) {
            return ActivityHistory.builder()
                    .user(user)
                    .pet(pet)
                    .activityType(ActivityType.valueOf(this.activityType))
                    .activityHistoryStatus(ActivityHistoryStatus.BEFORE)
                    .build();
        }
    }

    /**
     * 생성된 활동 기록을 시작(IN_PROGRESS)하기 위해 클라이언트로부터 전달받는 요청 DTO입니다.
     * <p>
     * 활동 시작 시점의 필수 GPS 위치 정보(위도, 경도)를 포함합니다.
     * </p>
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "활동 시작 요청 데이터 (위치 정보 포함)")
    public static class ActivityStartRequest {

        @Schema(description = "시작 위도", example = "37.5665")
        @NotNull(message = "시작 위도는 필수입니다.")
        private BigDecimal startLatitude;

        @Schema(description = "시작 경도", example = "126.9780")
        @NotNull(message = "시작 경도는 필수입니다.")
        private BigDecimal startLongitude;
    }

}