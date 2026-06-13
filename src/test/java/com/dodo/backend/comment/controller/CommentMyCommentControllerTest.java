package com.dodo.backend.comment.controller;

import com.dodo.backend.comment.dto.response.CommentResponse.MyCommentItemResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.MyCommentListResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.PageInfoResponse;
import com.dodo.backend.comment.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 내가 쓴 댓글 목록 조회 컨트롤러 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class CommentMyCommentControllerTest {

    @Mock
    private CommentService commentService;

    /**
     * 내가 쓴 댓글 목록 조회 요청 시 200 상태 코드와 목록 응답을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("내가 쓴 댓글 목록 조회 성공: 200 상태 코드와 목록을 반환한다.")
    void getMyComments_Success() {
        CommentController commentController = new CommentController(commentService);
        UUID userId = UUID.randomUUID();
        UserDetails userDetails = createUserDetails(userId);
        MyCommentListResponse serviceResponse = createMyCommentListResponse();

        given(commentService.getMyComments(userId, 0, 10)).willReturn(serviceResponse);

        ResponseEntity<MyCommentListResponse> response = commentController.getMyComments(0, 10, userDetails);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getData().size());
        assertEquals(10L, response.getBody().getData().get(0).getCommentId());
        assertEquals(1L, response.getBody().getData().get(0).getBoardId());

        verify(commentService).getMyComments(userId, 0, 10);
    }

    private UserDetails createUserDetails(UUID userId) {
        return User.withUsername(userId.toString())
                .password("password")
                .authorities(List.of())
                .build();
    }

    private MyCommentListResponse createMyCommentListResponse() {
        MyCommentItemResponse itemResponse = MyCommentItemResponse.builder()
                .commentId(10L)
                .boardId(1L)
                .boardTitle("우리 강아지 자랑합니다")
                .parentCommentId(null)
                .commentContent("좋은 정보 감사합니다.")
                .createdAt(LocalDateTime.of(2026, 1, 31, 13, 52, 32))
                .build();

        return MyCommentListResponse.builder()
                .message("내가 쓴 댓글 목록을 성공적으로 조회했습니다.")
                .pageInfo(PageInfoResponse.toDto(0, 10, 1L))
                .data(List.of(itemResponse))
                .build();
    }
}
