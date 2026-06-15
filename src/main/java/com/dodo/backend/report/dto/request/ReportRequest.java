package com.dodo.backend.report.dto.request;

import com.dodo.backend.report.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신고 API에서 사용하는 요청 DTO를 모아 둔 클래스입니다.
 */
@Schema(description = "신고 요청 DTO 그룹")
public class ReportRequest {

    /**
     * 신고 생성 요청 DTO입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "신고 생성 요청")
    public static class ReportCreateRequest {

        @NotNull(message = "신고 사유는 필수입니다.")
        @Schema(description = "신고 사유", example = "SPAM")
        private ReportReason reportReason;
    }
}
