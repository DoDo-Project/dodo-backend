package com.dodo.backend.board.service;

import com.dodo.backend.board.dto.response.BoardResponse.BoardListQueryResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardListResponse;
import com.dodo.backend.board.exception.BoardErrorCode;
import com.dodo.backend.board.exception.BoardException;
import com.dodo.backend.board.mapper.BoardMapper;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

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
 * 내가 쓴 게시글 목록 조회 비즈니스 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class BoardMyBoardServiceTest {

    @InjectMocks
    private BoardServiceImpl boardService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserService userService;

    @Mock
    private ImageFileService imageFileService;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 내가 쓴 게시글 목록 조회 시 페이지 정보와 게시글 목록을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("내가 쓴 게시글 목록 조회 성공: 게시글 목록과 페이지 정보를 반환한다.")
    void getMyBoards_Success() {
        UUID userId = UUID.randomUUID();
        BoardListQueryResponse queryResponse = BoardListQueryResponse.builder()
                .boardId(1L)
                .boardTitle("우리 강아지 자랑합니다")
                .boardContent("오늘 산책하다가 찍은 사진이에요. 너무 귀엽죠?")
                .thumbnailImageUrl("https://example.com/images/bori_1.jpg")
                .nickname("자유로운산책")
                .viewCount(51)
                .commentCount(3L)
                .likeCount(12L)
                .dislikeCount(1L)
                .createdAt(LocalDateTime.of(2026, 1, 31, 13, 52, 32))
                .modifiedAt(LocalDateTime.of(2026, 1, 31, 14, 10, 12))
                .build();

        given(boardMapper.findMyBoardList(userId, 0, 10)).willReturn(List.of(queryResponse));
        given(boardMapper.countMyBoards(userId)).willReturn(1L);

        BoardListResponse response = boardService.getMyBoards(userId, 0, 10);

        assertNotNull(response);
        assertEquals("내가 쓴 게시글 목록을 성공적으로 조회했습니다.", response.getMessage());
        assertEquals(1, response.getBoards().size());
        assertEquals(1L, response.getBoards().get(0).getBoardId());
        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());

        verify(boardMapper).findMyBoardList(userId, 0, 10);
        verify(boardMapper).countMyBoards(userId);
    }

    /**
     * 사용자 ID가 없으면 잘못된 요청 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("내가 쓴 게시글 목록 조회 실패: 사용자 ID가 없으면 예외가 발생한다.")
    void getMyBoards_Fail_UserIdNull() {
        BoardException exception = assertThrows(BoardException.class,
                () -> boardService.getMyBoards(null, 0, 10));

        assertEquals(BoardErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(boardMapper, never()).findMyBoardList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }
}
