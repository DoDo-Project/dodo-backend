package com.dodo.backend.report.controller;

import com.dodo.backend.common.exception.ErrorResponse;
import com.dodo.backend.report.dto.request.ReportRequest.ReportCreateRequest;
import com.dodo.backend.report.dto.response.ReportResponse.ReportSimpleResponse;
import com.dodo.backend.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 신고 API 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
@Tag(name = "Report API", description = "신고 관련 API")
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * 특정 게시글을 신고합니다.
     *
     * @param boardId     신고 대상 게시글 ID
     * @param request     신고 요청 DTO
     * @param userDetails 인증 사용자 정보
     * @return 신고 처리 응답
     */
    @Operation(summary = "게시글 신고", description = "인증 사용자가 특정 게시글을 신고합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "신고가 성공적으로 접수되었습니다.",
                    content = @Content(schema = @Schema(implementation = ReportSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "신고할 게시글을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 신고한 대상입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/board/{boardId}")
    public ResponseEntity<ReportSimpleResponse> reportBoard(
            @PathVariable Long boardId,
            @Valid @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID reporterId = UUID.fromString(userDetails.getUsername());
        log.info("게시글 신고 요청 - Reporter: {}, BoardId: {}", reporterId, boardId);

        return ResponseEntity.ok(reportService.reportBoard(reporterId, boardId, request));
    }

    /**
     * 특정 유저를 신고합니다.
     *
     * @param userId      신고 대상 유저 ID
     * @param request     신고 요청 DTO
     * @param userDetails 인증 사용자 정보
     * @return 신고 처리 응답
     */
    @Operation(summary = "유저 신고", description = "인증 사용자가 특정 유저를 신고합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "신고가 성공적으로 접수되었습니다.",
                    content = @Content(schema = @Schema(implementation = ReportSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "신고할 유저를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 신고한 유저입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/user/{userId}")
    public ResponseEntity<ReportSimpleResponse> reportUser(
            @PathVariable UUID userId,
            @Valid @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID reporterId = UUID.fromString(userDetails.getUsername());
        log.info("유저 신고 요청 - Reporter: {}, ReportedUser: {}", reporterId, userId);

        return ResponseEntity.ok(reportService.reportUser(reporterId, userId, request));
    }

    /**
     * 특정 댓글을 신고합니다.
     *
     * @param commentId   신고 대상 댓글 ID
     * @param request     신고 요청 DTO
     * @param userDetails 인증 사용자 정보
     * @return 신고 처리 응답
     */
    @Operation(summary = "댓글 신고", description = "인증 사용자가 특정 댓글을 신고합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "신고가 성공적으로 접수되었습니다.",
                    content = @Content(schema = @Schema(implementation = ReportSimpleResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 기능입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "신고할 댓글을 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 신고한 댓글입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/comment/{commentId}")
    public ResponseEntity<ReportSimpleResponse> reportComment(
            @PathVariable Long commentId,
            @Valid @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID reporterId = UUID.fromString(userDetails.getUsername());
        log.info("댓글 신고 요청 - Reporter: {}, CommentId: {}", reporterId, commentId);

        return ResponseEntity.ok(reportService.reportComment(reporterId, commentId, request));
    }
}
