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
import com.dodo.backend.admin.dto.response.AdminResponse.UserListResponse;
import com.dodo.backend.admin.entity.AdminReportType;
import com.dodo.backend.report.entity.ReportStatus;
import com.dodo.backend.user.entity.UserStatus;

import java.util.UUID;

/**
 * 관리자 API 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface AdminService {

    BoardReportDetailResponse getBoardReportDetail(Long boardId);

    UserReportDetailResponse getUserReportDetail(UUID userId);

    CommentReportDetailResponse getCommentReportDetail(Long commentId);

    ReportListResponse getReportList(AdminReportType reportType, ReportStatus reportStatus, int page, int size, String sort);

    /**
     * 일반 유저 목록을 조회하고 검색 조건과 계정 상태 필터를 적용합니다.
     *
     * @param keyword 이메일, 이름, 닉네임에 적용할 부분 일치 검색어
     * @param status 조회할 계정 상태, 전체 상태 조회 시 {@code null}
     * @param page 조회할 페이지 번호(0부터 시작)
     * @param size 페이지당 유저 수
     * @param sort 정렬 조건({@code 필드,방향})
     * @return 페이지 정보와 유저 목록
     */
    UserListResponse getUserList(String keyword, UserStatus status, int page, int size, String sort);

    AdminSimpleResponse updateUserStatus(UUID userId, UserStatusUpdateRequest request);

    void deleteBoard(Long boardId);

    void deleteComment(Long commentId);

    AdminSimpleResponse updateReportStatus(Long reportId, ReportStatusUpdateRequest request);

    AdminSimpleResponse createAnnouncement(UUID adminId, AnnouncementCreateRequest request);

    void deleteAnnouncement(Long boardId);

    void updateAnnouncement(Long boardId, AnnouncementUpdateRequest request);

    AnnouncementListResponse getAnnouncementList(int page, int size, String sort);

    AnnouncementDetailResponse getAnnouncementDetail(Long boardId);
}
