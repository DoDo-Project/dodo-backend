package com.dodo.backend.admin.controller;

import com.dodo.backend.admin.dto.request.AdminRequest.ReportStatusUpdateRequest;
import com.dodo.backend.admin.dto.response.AdminResponse.AdminSimpleResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.BoardReportDetailResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.PageInfoResponse;
import com.dodo.backend.admin.dto.response.AdminResponse.ReportListResponse;
import com.dodo.backend.admin.service.AdminService;
import com.dodo.backend.report.entity.ReportStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 컨트롤러 API 경로를 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
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
     * 게시글 강제 삭제 API가 204를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 강제 삭제 API 성공")
    void deleteBoard_Success() throws Exception {
        mockMvc.perform(delete("/admin/boards/{boardId}", 1L))
                .andExpect(status().isNoContent());

        verify(adminService).deleteBoard(1L);
    }
}
