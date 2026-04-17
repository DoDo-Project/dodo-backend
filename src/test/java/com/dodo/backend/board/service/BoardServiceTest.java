package com.dodo.backend.board.service;

import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

/**
 * {@link BoardService}의 비즈니스 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @InjectMocks
    private BoardServiceImpl boardService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserService userService;

    /**
     * 게시글 작성 요청 시 작성자를 조회하고 게시글을 저장한 뒤 게시글 ID를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 작성 성공: 게시글이 정상적으로 저장되고 게시글 ID를 반환한다.")
    void createBoard_Success() {
        log.info("테스트 시작: 게시글 작성 성공");

        // given
        UUID userId = UUID.randomUUID();

        BoardCreateRequest request = BoardCreateRequest.builder()
                .boardTitle("저희 강아지 자랑합니다!")
                .boardContent("오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?")
                .imageFileUrls(List.of(
                        "https://example.com/image1.jpg",
                        "https://example.com/image2.jpg"
                ))
                .build();

        User user = mock(User.class);

        Board savedBoard = Board.builder()
                .boardId(1L)
                .user(user)
                .boardTitle(request.getBoardTitle())
                .boardContent(request.getBoardContent())
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        given(userService.getUserById(userId)).willReturn(user);
        given(boardRepository.save(any(Board.class))).willReturn(savedBoard);

        // when
        Long boardId = boardService.createBoard(userId, request);

        // then
        assertNotNull(boardId);
        assertEquals(1L, boardId);

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepository).save(boardCaptor.capture());
        verify(userService).getUserById(userId);

        Board board = boardCaptor.getValue();
        assertEquals(user, board.getUser());
        assertEquals("저희 강아지 자랑합니다!", board.getBoardTitle());
        assertEquals("오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?", board.getBoardContent());
        assertEquals(BoardStatus.PUBLISHED, board.getBoardStatus());
        assertEquals(BoardType.FREE, board.getBoardType());
        assertEquals(0, board.getViewCount());

        log.info("테스트 종료: 게시글 작성 성공 검증 완료");
    }
}