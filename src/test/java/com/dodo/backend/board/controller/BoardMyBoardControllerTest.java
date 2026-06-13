package com.dodo.backend.board.controller;

import com.dodo.backend.board.dto.response.BoardResponse.BoardListItemResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardListResponse;
import com.dodo.backend.board.service.BoardService;
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
 * 내가 쓴 게시글 목록 조회 컨트롤러 로직을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class BoardMyBoardControllerTest {

    @Mock
    private BoardService boardService;

    /**
     * 내가 쓴 게시글 목록 조회 요청 시 200 상태 코드와 목록 응답을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("내가 쓴 게시글 목록 조회 성공: 200 상태 코드와 목록을 반환한다.")
    void getMyBoards_Success() {
        BoardController boardController = new BoardController(boardService);
        UUID userId = UUID.randomUUID();
        UserDetails userDetails = createUserDetails(userId);
        BoardListResponse serviceResponse = createBoardListResponse();

        given(boardService.getMyBoards(userId, 0, 10)).willReturn(serviceResponse);

        ResponseEntity<BoardListResponse> response = boardController.getMyBoards(0, 10, userDetails);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getBoards().size());
        assertEquals(1L, response.getBody().getBoards().get(0).getBoardId());

        verify(boardService).getMyBoards(userId, 0, 10);
    }

    private UserDetails createUserDetails(UUID userId) {
        return User.withUsername(userId.toString())
                .password("password")
                .authorities(List.of())
                .build();
    }

    private BoardListResponse createBoardListResponse() {
        BoardListItemResponse itemResponse = BoardListItemResponse.builder()
                .boardId(1L)
                .boardTitle("우리 강아지 자랑합니다")
                .boardContentPreview("오늘 산책하다가 찍은 사진이에요. 너")
                .thumbnailImageUrl("https://example.com/images/bori_1.jpg")
                .nickname("자유로운산책")
                .viewCount(51)
                .commentCount(3L)
                .likeCount(12L)
                .dislikeCount(1L)
                .createdAt(LocalDateTime.of(2026, 1, 31, 13, 52, 32))
                .modifiedAt(LocalDateTime.of(2026, 1, 31, 14, 10, 12))
                .build();

        return BoardListResponse.builder()
                .message("내가 쓴 게시글 목록을 성공적으로 조회했습니다.")
                .boards(List.of(itemResponse))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .pageSize(10)
                .build();
    }
}
