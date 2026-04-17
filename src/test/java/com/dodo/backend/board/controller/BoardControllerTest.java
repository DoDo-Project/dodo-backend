package com.dodo.backend.board.controller;

import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.dto.response.BoardResponse;
import com.dodo.backend.board.service.BoardService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link BoardController}의 HTTP 요청 처리 로직을 검증하는 테스트 클래스입니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private BoardService boardService;

    /**
     * 게시글 작성 요청 시 200 상태코드와 생성된 게시글 ID를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 작성 성공: 200 상태코드와 게시글 ID를 반환한다.")
    void createBoard_Success() {
        log.info("테스트 시작: 게시글 작성 성공 시나리오");

        // given
        BoardController boardController = new BoardController(boardService);

        UUID userId = UUID.randomUUID();

        UserDetails userDetails = User.withUsername(userId.toString())
                .password("password")
                .authorities(List.of())
                .build();

        BoardCreateRequest request = BoardCreateRequest.builder()
                .boardTitle("저희 강아지 자랑합니다!")
                .boardContent("오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?")
                .imageFileUrls(List.of(
                        "https://example.com/image1.jpg",
                        "https://example.com/image2.jpg"
                ))
                .build();

        given(boardService.createBoard(userId, request)).willReturn(1L);

        // when
        ResponseEntity<BoardResponse.BoardCreateResponse> response =
                boardController.createBoard(request, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("게시글이 성공적으로 작성되었습니다.", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getBoardId());

        verify(boardService).createBoard(userId, request);

        log.info("테스트 종료: 게시글 작성 성공 시나리오");
    }
}