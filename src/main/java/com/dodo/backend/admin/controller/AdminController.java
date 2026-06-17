package com.dodo.backend.admin.controller;

import com.dodo.backend.admin.dto.request.AdminRequest.AnnouncementCreateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.AnnouncementUpdateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.ReportStatusUpdateRequest;
import com.dodo.backend.admin.dto.request.AdminRequest.UserStatusUpdateRequest;
import com.dodo.backend.admin.dto.response.AdminResponse.AdminSimpleResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.AnnouncementDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.AnnouncementListResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.BoardReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.CommentReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.ReportListResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.UserReportDetailResponse;
import com.dodo.backend.admin.entity.AdminReportType;
import com.dodo.backend.admin.service.AdminService;
import com.dodo.backend.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 관리자 API 요청을 처리하는 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@Tag(name = "Admin API", description = "관리자 기능 API")
public class AdminController {

    private final AdminService adminService;

    /**
     * 특정 게시글의 신고 상세 내역을 조회합니다.
     *
     * @param boardId 신고 상세 내역을 조회할 게시글 ID
     * @return 게시글 신고 상세 내역
     */
    @Operation(summary = "게시글 신고 상세 조회", description = "특정 게시글에 접수된 신고 상세 내역을 조회합니다.")
    @GetMapping("/reports/board/{boardId}")
    public ResponseEntity<BoardReportDetailResponse> getBoardReportDetail(@PathVariable Long boardId) {
        log.info("관리자 게시글 신고 상세 조회 요청 - BoardId: {}", boardId);
        return ResponseEntity.ok(adminService.getBoardReportDetail(boardId));
    }

    /**
     * 특정 유저의 신고 상세 내역을 조회합니다.
     *
     * @param userId 신고 상세 내역을 조회할 유저 ID
     * @return 유저 신고 상세 내역
     */
    @Operation(summary = "유저 신고 상세 조회", description = "특정 유저에 접수된 신고 상세 내역을 조회합니다.")
    @GetMapping("/reports/user/{userId}")
    public ResponseEntity<UserReportDetailResponse> getUserReportDetail(@PathVariable UUID userId) {
        log.info("관리자 유저 신고 상세 조회 요청 - UserId: {}", userId);
        return ResponseEntity.ok(adminService.getUserReportDetail(userId));
    }

    /**
     * 특정 댓글의 신고 상세 내역을 조회합니다.
     *
     * @param commentId 신고 상세 내역을 조회할 댓글 ID
     * @return 댓글 신고 상세 내역
     */
    @Operation(summary = "댓글 신고 상세 조회", description = "특정 댓글에 접수된 신고 상세 내역을 조회합니다.")
    @GetMapping("/reports/comment/{commentId}")
    public ResponseEntity<CommentReportDetailResponse> getCommentReportDetail(@PathVariable Long commentId) {
        log.info("관리자 댓글 신고 상세 조회 요청 - CommentId: {}", commentId);
        return ResponseEntity.ok(adminService.getCommentReportDetail(commentId));
    }

    /**
     * 신고 목록을 조회합니다.
     *
     * @param reportType 조회할 신고 대상 유형
     * @param reportStatus 조회할 신고 처리 상태
     * @param page 조회할 페이지 번호
     * @param size 페이지당 신고 목록 개수
     * @param sort 정렬 조건
     * @return 신고 목록 조회 결과
     */
    @Operation(summary = "신고 목록 조회", description = "신고 유형과 처리 상태에 따라 신고 목록을 조회합니다.")
    @GetMapping("/reports")
    public ResponseEntity<ReportListResponse> getReportList(
            @RequestParam AdminReportType reportType,
            @RequestParam(required = false) ReportStatus reportStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        log.info("관리자 신고 목록 조회 요청 - Type: {}, Status: {}, Page: {}, Size: {}, Sort: {}",
                reportType, reportStatus, page, size, sort);
        return ResponseEntity.ok(adminService.getReportList(reportType, reportStatus, page, size, sort));
    }

    /**
     * 유저 계정 상태를 변경합니다.
     *
     * @param userId 상태를 변경할 유저 ID
     * @param request 변경할 유저 상태 요청
     * @return 상태 변경 성공 메시지
     */
    @Operation(summary = "유저 계정 상태 변경", description = "관리자가 유저 계정 상태를 변경합니다.")
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<AdminSimpleResponse> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        log.info("관리자 유저 상태 변경 요청 - UserId: {}, Status: {}", userId, request.getStatus());
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request));
    }

    /**
     * 게시글을 강제로 삭제합니다.
     *
     * @param boardId 삭제할 게시글 ID
     * @return 응답 본문이 없는 204 응답
     */
    @Operation(summary = "게시글 강제 삭제", description = "관리자가 게시글을 삭제 상태로 변경합니다.")
    @DeleteMapping("/boards/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId) {
        log.info("관리자 게시글 강제 삭제 요청 - BoardId: {}", boardId);
        adminService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 댓글을 강제로 삭제합니다.
     *
     * @param commentId 삭제할 댓글 ID
     * @return 응답 본문이 없는 204 응답
     */
    @Operation(summary = "댓글 강제 삭제", description = "관리자가 댓글을 강제로 삭제합니다.")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        log.info("관리자 댓글 강제 삭제 요청 - CommentId: {}", commentId);
        adminService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 신고 처리 상태를 변경합니다.
     *
     * @param reportId 상태를 변경할 신고 ID
     * @param request 변경할 신고 처리 상태 요청
     * @return 상태 변경 성공 메시지
     */
    @Operation(summary = "신고 처리 상태 변경", description = "관리자가 신고 처리 상태를 변경합니다.")
    @PatchMapping("/reports/{reportId}/status")
    public ResponseEntity<AdminSimpleResponse> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportStatusUpdateRequest request
    ) {
        log.info("관리자 신고 상태 변경 요청 - ReportId: {}, Status: {}", reportId, request.getStatus());
        return ResponseEntity.ok(adminService.updateReportStatus(reportId, request));
    }

    /**
     * 공지를 작성합니다.
     *
     * @param request 공지 작성 요청
     * @param userDetails 인증된 관리자 정보
     * @return 공지 작성 성공 메시지
     */
    @Operation(summary = "공지 작성", description = "관리자가 공지를 작성합니다.")
    @PostMapping("/announcements")
    public ResponseEntity<AdminSimpleResponse> createAnnouncement(
            @Valid @RequestBody AnnouncementCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        log.info("관리자 공지 작성 요청 - AdminId: {}", adminId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(adminService.createAnnouncement(adminId, request));
    }

    /**
     * 공지를 삭제합니다.
     *
     * @param boardId 삭제할 공지 게시글 ID
     * @return 응답 본문이 없는 204 응답
     */
    @Operation(summary = "공지 삭제", description = "관리자가 공지를 삭제합니다.")
    @DeleteMapping("/announcements/{boardId}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long boardId) {
        log.info("관리자 공지 삭제 요청 - BoardId: {}", boardId);
        adminService.deleteAnnouncement(boardId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 공지를 수정합니다.
     *
     * @param boardId 수정할 공지 게시글 ID
     * @param request 공지 수정 요청
     * @return 응답 본문이 없는 204 응답
     */
    @Operation(summary = "공지 수정", description = "관리자가 공지를 수정합니다.")
    @PatchMapping("/announcements/{boardId}")
    public ResponseEntity<Void> updateAnnouncement(
            @PathVariable Long boardId,
            @RequestBody AnnouncementUpdateRequest request
    ) {
        log.info("관리자 공지 수정 요청 - BoardId: {}", boardId);
        adminService.updateAnnouncement(boardId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 공지 목록을 조회합니다.
     *
     * @param pageable 공지 목록 페이지 요청 정보
     * @return 공지 목록 조회 결과
     */
    @Operation(summary = "공지 목록 조회", description = "공지 목록을 페이지 단위로 조회합니다.")
    @GetMapping("/announcements")
    public ResponseEntity<AnnouncementListResponse> getAnnouncementList(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        log.info("관리자 공지 목록 조회 요청 - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(adminService.getAnnouncementList(pageable));
    }

    /**
     * 공지 상세를 조회합니다.
     *
     * @param boardId 조회할 공지 게시글 ID
     * @return 공지 상세 정보
     */
    @Operation(summary = "공지 상세 조회", description = "공지 상세 내용을 조회합니다.")
    @GetMapping("/announcements/{boardId}")
    public ResponseEntity<AnnouncementDetailResponse> getAnnouncementDetail(@PathVariable Long boardId) {
        log.info("관리자 공지 상세 조회 요청 - BoardId: {}", boardId);
        return ResponseEntity.ok(adminService.getAnnouncementDetail(boardId));
    }
}
