package com.dodo.backend.reaction.dto.request;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.reaction.entity.Reaction;
import com.dodo.backend.reaction.entity.ReactionType;
import com.dodo.backend.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * 반응(Reaction) 도메인과 관련된 요청 데이터를 캡슐화하는 DTO 그룹 클래스입니다.
 */
@Schema(description = "반응 도메인 요청 DTO 그룹")
public class ReactionRequest {

    /**
     * 특정 활동 기록에 반응을 추가할 때 사용하는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "활동 반응 추가 요청 DTO")
    public static class HistoryReactionCreateRequest {

        @Schema(description = "활동 기록 ID", example = "101")
        @NotNull(message = "잘못된 요청입니다.")
        private Long historyId;

        @Schema(description = "반응 유형 (LIKE, DISLIKE)", example = "LIKE")
        @NotNull(message = "잘못된 요청입니다.")
        @Pattern(regexp = "^(?i)(LIKE|DISLIKE)$", message = "잘못된 요청입니다.")
        private String reactionType;

        /**
         * 요청 데이터를 기반으로 활동 반응 엔티티를 생성합니다.
         *
         * @param user    반응을 남긴 사용자 엔티티
         * @param history 반응 대상 활동 기록 엔티티
         * @return 생성된 반응 엔티티
         */
        public Reaction toEntity(User user, ActivityHistory history) {
            return Reaction.builder()
                    .user(user)
                    .history(history)
                    .reactionType(ReactionType.valueOf(this.reactionType.trim().toUpperCase(Locale.ROOT)))
                    .build();
        }
    }

    /**
     * 특정 활동 기록에 남긴 반응을 변경할 때 사용하는 요청 DTO입니다.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "활동 반응 변경 요청 DTO")
    public static class HistoryReactionUpdateRequest {

        @Schema(description = "변경할 반응 유형 (LIKE, DISLIKE)", example = "LIKE")
        @NotNull(message = "잘못된 요청입니다.")
        @Pattern(regexp = "^(?i)(LIKE|DISLIKE)$", message = "잘못된 요청입니다.")
        private String reactionType;

        /**
         * 요청 문자열 반응 유형을 열거형으로 변환합니다.
         *
         * @return 변환된 반응 유형
         */
        public ReactionType toReactionType() {
            return ReactionType.valueOf(this.reactionType.trim().toUpperCase(Locale.ROOT));
        }
    }
}
