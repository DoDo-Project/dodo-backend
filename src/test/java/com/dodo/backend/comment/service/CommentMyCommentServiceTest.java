package com.dodo.backend.comment.service;

import com.dodo.backend.board.service.BoardService;
import com.dodo.backend.comment.dto.response.CommentResponse.MyCommentListQueryResponse;
import com.dodo.backend.comment.dto.response.CommentResponse.MyCommentListResponse;
import com.dodo.backend.comment.exception.CommentErrorCode;
import com.dodo.backend.comment.exception.CommentException;
import com.dodo.backend.comment.mapper.CommentMapper;
import com.dodo.backend.comment.repository.CommentRepository;
import com.dodo.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 내가 쓴 댓글 목록 조회 비즈니스 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class CommentMyCommentServiceTest {

    @InjectMocks
    private CommentServiceImpl commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private BoardService boardService;

    @Mock
    private UserService userService;

    /**
     * 내가 쓴 댓글 목록 조회 시 게시글 정보가 포함된 댓글 목록을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("내가 쓴 댓글 목록 조회 성공: 게시글 정보가 포함된 댓글 목록을 반환한다.")
    void getMyComments_Success() {
        UUID userId = UUID.randomUUID();
        MyCommentListQueryResponse queryResponse = MyCommentListQueryResponse.builder()
                .commentId(10L)
                .boardId(1L)
                .boardTitle("우리 강아지 자랑합니다")
                .parentCommentId(null)
                .commentContent("좋은 정보 감사합니다.")
                .createdAt(LocalDateTime.of(2026, 1, 31, 13, 52, 32))
                .build();

        given(commentMapper.findMyComments(userId, 0, 10)).willReturn(List.of(queryResponse));
        given(commentMapper.countMyComments(userId)).willReturn(1L);

        MyCommentListResponse response = commentService.getMyComments(userId, 0, 10);

        assertNotNull(response);
        assertEquals("내가 쓴 댓글 목록을 성공적으로 조회했습니다.", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals(10L, response.getData().get(0).getCommentId());
        assertEquals(1L, response.getData().get(0).getBoardId());
        assertEquals("우리 강아지 자랑합니다", response.getData().get(0).getBoardTitle());

        verify(commentMapper).findMyComments(userId, 0, 10);
        verify(commentMapper).countMyComments(userId);
    }

    /**
     * 사용자 ID가 없으면 잘못된 요청 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("내가 쓴 댓글 목록 조회 실패: 사용자 ID가 없으면 예외가 발생한다.")
    void getMyComments_Fail_UserIdNull() {
        CommentException exception = assertThrows(CommentException.class,
                () -> commentService.getMyComments(null, 0, 10));

        assertEquals(CommentErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(commentMapper, never()).findMyComments(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }
}
