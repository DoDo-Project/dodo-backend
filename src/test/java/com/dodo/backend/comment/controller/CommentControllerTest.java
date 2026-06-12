package com.dodo.backend.comment.controller;

import com.dodo.backend.comment.dto.request.CommentRequest.CommentCreateRequest;
import com.dodo.backend.comment.dto.request.CommentRequest.CommentUpdateRequest;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentAuthorResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentCreateResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentItemResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentListResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.CommentSimpleResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.PageInfoResponse;
import com.dodo.backend.comment.service.CommentService;
import lombok.extern.slf4j.Slf4j;
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
 * {@link CommentController}의 HTTP 요청 처리 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    /**
     * 댓글 작성 요청 시 200 상태코드와 작성된 댓글 정보를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 작성 성공: 200 상태코드와 작성된 댓글 정보를 반환한다.")
    void createComment_Success() {
        log.info("테스트 시작: 댓글 작성 성공 시나리오");

        // given
        CommentController commentController = new CommentController(commentService);
        UUID userId = UUID.randomUUID();
        UserDetails userDetails = createUserDetails(userId);

        CommentCreateRequest request = CommentCreateRequest.builder()
                .boardId(1L)
                .commentContent("좋은 정보 감사합니다!")
                .parentCommentId(null)
                .build();

        CommentCreateResponse serviceResponse = CommentCreateResponse.builder()
                .message("댓글이 성공적으로 작성되었습니다.")
                .commentId(123L)
                .commentContent("좋은 정보 감사합니다!")
                .userId(userId.toString())
                .nickname("멍멍이집사")
                .build();

        given(commentService.createComment(userId, request)).willReturn(serviceResponse);

        // when
        ResponseEntity<CommentCreateResponse> response = commentController.createComment(request, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("댓글이 성공적으로 작성되었습니다.", response.getBody().getMessage());
        assertEquals(123L, response.getBody().getCommentId());
        assertEquals("멍멍이집사", response.getBody().getNickname());

        verify(commentService).createComment(userId, request);

        log.info("테스트 종료: 댓글 작성 성공 시나리오");
    }

    /**
     * 댓글 목록 조회 요청 시 200 상태코드와 댓글 목록을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 목록 조회 성공: 200 상태코드와 댓글 목록을 반환한다.")
    void getComments_Success() {
        log.info("테스트 시작: 댓글 목록 조회 성공 시나리오");

        // given
        CommentController commentController = new CommentController(commentService);
        UUID userId = UUID.randomUUID();
        Long boardId = 1L;
        UserDetails userDetails = createUserDetails(userId);

        CommentItemResponse itemResponse = CommentItemResponse.builder()
                .commentId(102L)
                .parentCommentId(null)
                .commentContent("두 번째 댓글입니다.")
                .author(CommentAuthorResponse.toDto("uuid-user-2", "산책왕뽀삐"))
                .createdAt(LocalDateTime.of(2025, 10, 14, 14, 30))
                .build();

        CommentListResponse serviceResponse = CommentListResponse.builder()
                .message("댓글 목록을 성공적으로 조회했습니다.")
                .pageInfo(PageInfoResponse.toDto(0, 10, 1L))
                .data(List.of(itemResponse))
                .build();

        given(commentService.getComments(boardId, 0, 10)).willReturn(serviceResponse);

        // when
        ResponseEntity<CommentListResponse> response = commentController.getComments(boardId, 0, 10, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("댓글 목록을 성공적으로 조회했습니다.", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
        assertEquals(102L, response.getBody().getData().get(0).getCommentId());

        verify(commentService).getComments(boardId, 0, 10);

        log.info("테스트 종료: 댓글 목록 조회 성공 시나리오");
    }

    /**
     * 댓글 수정 요청 시 200 상태코드와 수정 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 수정 성공: 200 상태코드와 수정 성공 메시지를 반환한다.")
    void updateComment_Success() {
        log.info("테스트 시작: 댓글 수정 성공 시나리오");

        // given
        CommentController commentController = new CommentController(commentService);
        UUID userId = UUID.randomUUID();
        Long commentId = 123L;
        UserDetails userDetails = createUserDetails(userId);

        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .commentContent("수정입니다.")
                .build();

        CommentSimpleResponse serviceResponse = CommentSimpleResponse.toDto("댓글이 성공적으로 수정되었습니다.");
        given(commentService.updateComment(userId, commentId, request)).willReturn(serviceResponse);

        // when
        ResponseEntity<CommentSimpleResponse> response = commentController.updateComment(commentId, request, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("댓글이 성공적으로 수정되었습니다.", response.getBody().getMessage());

        verify(commentService).updateComment(userId, commentId, request);

        log.info("테스트 종료: 댓글 수정 성공 시나리오");
    }

    /**
     * 댓글 삭제 요청 시 200 상태코드와 삭제 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("댓글 삭제 성공: 200 상태코드와 삭제 성공 메시지를 반환한다.")
    void deleteComment_Success() {
        log.info("테스트 시작: 댓글 삭제 성공 시나리오");

        // given
        CommentController commentController = new CommentController(commentService);
        UUID userId = UUID.randomUUID();
        Long commentId = 123L;
        UserDetails userDetails = createUserDetails(userId);

        CommentSimpleResponse serviceResponse = CommentSimpleResponse.toDto("댓글이 성공적으로 삭제되었습니다.");
        given(commentService.deleteComment(userId, commentId)).willReturn(serviceResponse);

        // when
        ResponseEntity<CommentSimpleResponse> response = commentController.deleteComment(commentId, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("댓글이 성공적으로 삭제되었습니다.", response.getBody().getMessage());

        verify(commentService).deleteComment(userId, commentId);

        log.info("테스트 종료: 댓글 삭제 성공 시나리오");
    }

    /**
     * 테스트용 인증 사용자 정보를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 인증 사용자 정보
     */
    private UserDetails createUserDetails(UUID userId) {
        return User.withUsername(userId.toString())
                .password("password")
                .authorities(List.of())
                .build();
    }
}
