package com.dodo.backend.admin.service;

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
import com.dodo.backend.report.entity.ReportStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 관리자 API 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface AdminService {

    BoardReportDetailResponse getBoardReportDetail(Long boardId);

    UserReportDetailResponse getUserReportDetail(UUID userId);

    CommentReportDetailResponse getCommentReportDetail(Long commentId);

    ReportListResponse getReportList(AdminReportType reportType, ReportStatus reportStatus, int page, int size, String sort);

    AdminSimpleResponse updateUserStatus(UUID userId, UserStatusUpdateRequest request);

    void deleteBoard(Long boardId);

    void deleteComment(Long commentId);

    AdminSimpleResponse updateReportStatus(Long reportId, ReportStatusUpdateRequest request);

    AdminSimpleResponse createAnnouncement(UUID adminId, AnnouncementCreateRequest request);

    void deleteAnnouncement(Long boardId);

    void updateAnnouncement(Long boardId, AnnouncementUpdateRequest request);

    AnnouncementListResponse getAnnouncementList(Pageable pageable);

    AnnouncementDetailResponse getAnnouncementDetail(Long boardId);
}
