package com.dodo.backend.admin.controller;

import com.dodo.backend.admin.dto.request.AdminRequest.ReportStatusUpdateRequest;
import com.dodo.backend.admin.dto.response.AdminResponse.AdminSimpleResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.AnnouncementDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.AnnouncementListResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.BoardReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.PageInfoResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.ReportListResponse;
import com.dodo.backend.admin.service.AdminService;
import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationScheduleCreateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleCreateResponse;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;
import com.dodo.backend.notification.service.NotificationScheduleService;
import com.dodo.backend.report.entity.ReportStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 컨트롤러 API 경로를 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private NotificationScheduleService notificationScheduleService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService, notificationScheduleService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    /**
     * 게시글 신고 상세 조회 API가 200을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 신고 상세 조회 API 성공")
    void getBoardReportDetail_Success() throws Exception {
        BoardReportDetailResponse response = BoardReportDetailResponse.builder()
                .boardId(1L)
                .totalReportCount(0)
                .reports(List.of())
                .build();
        given(adminService.getBoardReportDetail(1L)).willReturn(response);

        mockMvc.perform(get("/admin/reports/board/{boardId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardId").value(1));

        verify(adminService).getBoardReportDetail(1L);
    }

    /**
     * 신고 목록 조회 API가 200을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("신고 목록 조회 API 성공")
    void getReportList_Success() throws Exception {
        ReportListResponse response = ReportListResponse.builder()
                .pageInfo(PageInfoResponse.toDto(0, 10, 0))
                .data(List.of())
                .build();
        given(adminService.getReportList(any(), eq(null), eq(0), eq(10), eq("lastReportedAt,desc"))).willReturn(response);

        mockMvc.perform(get("/admin/reports")
                        .param("reportType", "BOARD")
                        .param("sort", "lastReportedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageInfo.page").value(0));
    }

    /**
     * 신고 처리 상태 변경 API가 200을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("신고 처리 상태 변경 API 성공")
    void updateReportStatus_Success() throws Exception {
        given(adminService.updateReportStatus(eq(10L), any(ReportStatusUpdateRequest.class)))
                .willReturn(AdminSimpleResponse.toDto("성공적으로 상태를 변경했습니다."));

        mockMvc.perform(patch("/admin/reports/{reportId}/status", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReportStatusUpdateRequest(ReportStatus.COMPLETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("성공적으로 상태를 변경했습니다."));
    }

    /**
     * 게시글 강제 삭제 API가 200과 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 강제 삭제 API 성공")
    void deleteBoard_Success() throws Exception {
        mockMvc.perform(delete("/admin/boards/{boardId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글이 성공적으로 강제 삭제되었습니다."));

        verify(adminService).deleteBoard(1L);
    }

    @Test
    @DisplayName("알림 스케줄 등록 API 성공")
    void createNotificationSchedule_Success() throws Exception {
        LocalDateTime scheduledAt = LocalDateTime.of(2099, 1, 1, 14, 30);
        NotificationScheduleCreateResponse response = NotificationScheduleCreateResponse.builder()
                .message("알림 스케줄이 성공적으로 등록되었습니다.")
                .scheduleId(1L)
                .scheduleStatus(NotificationScheduleStatus.PENDING)
                .scheduledAt(scheduledAt)
                .build();

        given(notificationScheduleService.createSchedule(eq(adminId), any(NotificationScheduleCreateRequest.class)))
                .willReturn(response);

        String requestBody = """
                {
                  "title": "공지 알림",
                  "body": "새로운 공지가 등록되었습니다.",
                  "notificationType": "SYSTEM",
                  "targetType": "ALL",
                  "scheduledAt": "2099-01-01T14:30:00",
                  "repeatType": "NONE"
                }
                """;

        mockMvc.perform(post("/admin/notification-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("알림 스케줄이 성공적으로 등록되었습니다."))
                .andExpect(jsonPath("$.scheduleId").value(1))
                .andExpect(jsonPath("$.scheduleStatus").value("PENDING"));

        verify(notificationScheduleService).createSchedule(eq(adminId), any(NotificationScheduleCreateRequest.class));
    }

    /**
     * 공지 목록 조회 API가 명세의 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("공지 목록 조회 API 성공")
    void getAnnouncementList_Success() throws Exception {
        AnnouncementListResponse response = AnnouncementListResponse.builder()
                .pageInfo(PageInfoResponse.toDto(0, 10, 0))
                .data(List.of())
                .message("공지 목록을 조회했습니다.")
                .build();
        given(adminService.getAnnouncementList(0, 10, "registrationUpdatedAt,desc")).willReturn(response);

        mockMvc.perform(get("/admin/announcements")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "registrationUpdatedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공지 목록을 조회했습니다."));

        verify(adminService).getAnnouncementList(0, 10, "registrationUpdatedAt,desc");
    }

    /**
     * 공지 상세 조회 API가 명세의 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("공지 상세 조회 API 성공")
    void getAnnouncementDetail_Success() throws Exception {
        AnnouncementDetailResponse response = AnnouncementDetailResponse.builder()
                .boardId(31L)
                .boardTitle("공지 제목입니다.")
                .boardContent("공지 전체 내용입니다.")
                .message("공지 상세보기에 성공했습니다.")
                .build();
        given(adminService.getAnnouncementDetail(31L)).willReturn(response);

        mockMvc.perform(get("/admin/announcements/{boardId}", 31L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공지 상세보기에 성공했습니다."));

        verify(adminService).getAnnouncementDetail(31L);
    }

    private HandlerMethodArgumentResolver authenticationPrincipalResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return User.withUsername(adminId.toString()).password("").roles("ADMIN").build();
            }
        };
    }
}
