package com.dodo.backend.report.controller;

import com.dodo.backend.report.dto.request.ReportRequest.ReportCreateRequest;
import com.dodo.backend.report.dto.response.ReportResponse.ReportSimpleResponse;
import com.dodo.backend.report.entity.ReportReason;
import com.dodo.backend.report.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 신고 컨트롤러 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ReportController reportController = new ReportController(reportService);
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 게시글 신고 요청 시 200 상태 코드와 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 신고 성공: 200 상태 코드와 성공 메시지를 반환한다.")
    void reportBoard_Success() throws Exception {
        UUID reporterId = UUID.randomUUID();
        Long boardId = 1L;
        UserDetails userDetails = createUserDetails(reporterId);
        ReportCreateRequest request = new ReportCreateRequest(ReportReason.SPAM);
        ReportSimpleResponse serviceResponse = ReportSimpleResponse.toDto("신고가 성공적으로 접수되었습니다.");
        setAuthentication(userDetails);

        given(reportService.reportBoard(eq(reporterId), eq(boardId), any(ReportCreateRequest.class)))
                .willReturn(serviceResponse);

        mockMvc.perform(post("/reports/board/{boardId}", boardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("신고가 성공적으로 접수되었습니다."));

        verify(reportService).reportBoard(eq(reporterId), eq(boardId), any(ReportCreateRequest.class));
    }

    /**
     * 유저 신고 요청 시 200 상태 코드와 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("유저 신고 성공: 200 상태 코드와 성공 메시지를 반환한다.")
    void reportUser_Success() throws Exception {
        UUID reporterId = UUID.randomUUID();
        UUID reportedUserId = UUID.randomUUID();
        UserDetails userDetails = createUserDetails(reporterId);
        ReportCreateRequest request = new ReportCreateRequest(ReportReason.IMPERSONATION);
        ReportSimpleResponse serviceResponse = ReportSimpleResponse.toDto("신고가 성공적으로 접수되었습니다.");
        setAuthentication(userDetails);

        given(reportService.reportUser(eq(reporterId), eq(reportedUserId), any(ReportCreateRequest.class)))
                .willReturn(serviceResponse);

        mockMvc.perform(post("/reports/user/{userId}", reportedUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("신고가 성공적으로 접수되었습니다."));

        verify(reportService).reportUser(eq(reporterId), eq(reportedUserId), any(ReportCreateRequest.class));
    }

    /**
     * 댓글 신고 요청 시 200 상태 코드와 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 신고 성공: 200 상태 코드와 성공 메시지를 반환한다.")
    void reportComment_Success() throws Exception {
        UUID reporterId = UUID.randomUUID();
        Long commentId = 10L;
        UserDetails userDetails = createUserDetails(reporterId);
        ReportCreateRequest request = new ReportCreateRequest(ReportReason.SPAM_ADVERTISING);
        ReportSimpleResponse serviceResponse = ReportSimpleResponse.toDto("신고가 성공적으로 접수되었습니다.");
        setAuthentication(userDetails);

        given(reportService.reportComment(eq(reporterId), eq(commentId), any(ReportCreateRequest.class)))
                .willReturn(serviceResponse);

        mockMvc.perform(post("/reports/comment/{commentId}", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("신고가 성공적으로 접수되었습니다."));

        verify(reportService).reportComment(eq(reporterId), eq(commentId), any(ReportCreateRequest.class));
    }

    private void setAuthentication(UserDetails userDetails) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                userDetails,
                userDetails.getPassword(),
                userDetails.getAuthorities()
        ));
        SecurityContextHolder.setContext(context);
    }

    private UserDetails createUserDetails(UUID userId) {
        return User.withUsername(userId.toString())
                .password("password")
                .authorities(List.of())
                .build();
    }
}
