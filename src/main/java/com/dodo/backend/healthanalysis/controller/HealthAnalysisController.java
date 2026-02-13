package com.dodo.backend.healthanalysis.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AiReportCreateRequest;
import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AnalysisUpdateRequest;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisDetailResponse;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AiReportCreateResponse;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisUpdateResponse;
import com.dodo.backend.healthanalysis.service.HealthAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 건강 분석(HealthAnalysis) 도메인의 HTTP 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/health/analysis")
@Tag(name = "Health Analysis API", description = "건강 분석 관련 API")
@Slf4j
public class HealthAnalysisController {

    private final HealthAnalysisService healthAnalysisService;

    /**
     * AI 건강 분석 보고서 생성을 요청합니다.
     *
     * @param petId       분석 대상 반려동물 ID
     * @param request     분석 생성 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 생성 완료 메시지와 분석 ID
     */
    @Operation(summary = "AI 건강 분석 보고서 생성", description = "반려동물 기준으로 AI 건강 분석 보고서를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "건강 분석 보고서 생성이 완료되었습니다.",
                    content = @Content(schema = @Schema(implementation = AiReportCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 반려동물을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 반려동물을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PostMapping("/ai-report/{petId}")
    public ResponseEntity<AiReportCreateResponse> createAiReport(
            @PathVariable Long petId,
            @RequestBody AiReportCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("AI 건강 분석 리포트 생성 요청 - User: {}, PetId: {}", userId, petId);

        return ResponseEntity.ok(healthAnalysisService.createAiReport(userId, petId, request));
    }

    /**
     * 건강 분석 상세 조회를 요청합니다.
     *
     * @param analysisId  조회할 분석 보고서 ID
     * @param userDetails 인증된 사용자 정보
     * @return 건강 분석 상세 응답
     */
    @Operation(summary = "건강 분석 상세 조회", description = "analysisId 기준으로 건강 분석 상세 데이터를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "건강 분석 상세 조회에 성공했습니다.",
                    content = @Content(schema = @Schema(implementation = AnalysisDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "조회 권한이 없는 분석입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"조회 권한이 없는 분석입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 분석을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 분석을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalysisDetailResponse> getAnalysisDetail(
            @PathVariable Long analysisId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("건강 분석 상세 조회 요청 - User: {}, AnalysisId: {}", userId, analysisId);
        return ResponseEntity.ok(healthAnalysisService.getAnalysisDetail(userId, analysisId));
    }

    /**
     * 건강 분석 결과(제목/요약)를 수정합니다.
     *
     * @param analysisId 수정할 분석 보고서 ID
     * @param request 수정 요청 DTO
     * @param userDetails 인증된 사용자 정보
     * @return 수정 결과 응답
     */
    @Operation(summary = "건강 분석 결과 수정", description = "analysisId 기준으로 건강 분석 제목/요약을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 내용이 수정되었습니다.",
                    content = @Content(schema = @Schema(implementation = AnalysisUpdateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 Bad Request", value = "{\"status\": 400, \"message\": \"잘못된 요청입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "401 Unauthorized", value = "{\"status\": 401, \"message\": \"로그인이 필요한 기능입니다.\"}"))),
            @ApiResponse(responseCode = "403", description = "수정 권한이 없는 분석입니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "403 Forbidden", value = "{\"status\": 403, \"message\": \"수정 권한이 없는 분석입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "해당 분석을 찾을 수 없습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 Not Found", value = "{\"status\": 404, \"message\": \"해당 분석을 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "500 Internal Server Error", value = "{\"status\": 500, \"message\": \"서버 내부 오류가 발생했습니다.\"}")))
    })
    @PatchMapping("/{analysisId}")
    public ResponseEntity<AnalysisUpdateResponse> updateAnalysis(
            @PathVariable Long analysisId,
            @RequestBody AnalysisUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        log.info("건강 분석 수정 요청 - User: {}, AnalysisId: {}", userId, analysisId);
        return ResponseEntity.ok(healthAnalysisService.updateAnalysis(userId, analysisId, request));
    }
}
