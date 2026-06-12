package com.dodo.backend.board.controller;

import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardTempSaveRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardUpdateRequest;
import com.dodo.backend.board.dto.response.BoardResponse.BoardCreateResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardListItemResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardListResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardSimpleResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardTempSaveDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardTempSaveResponse;
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

import java.time.LocalDateTime;
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
     * 게시글 목록 조회 요청 시 200 상태코드와 게시글 목록을 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 목록 조회 성공: 200 상태코드와 게시글 목록을 반환한다.")
    void getBoardList_Success() {
        log.info("테스트 시작: 게시글 목록 조회 성공 시나리오");

        // given
        BoardController boardController = new BoardController(boardService);
        UUID userId = UUID.randomUUID();
        UserDetails userDetails = createUserDetails(userId);

        BoardListItemResponse itemResponse = BoardListItemResponse.builder()
                .boardId(1L)
                .boardTitle("우리 강아지 자랑합니다")
                .boardContentPreview("오늘 산책하다가 찍은 사진이에요")
                .thumbnailImageUrl("https://example.com/images/bori_1.jpg")
                .nickname("자유로운산책")
                .viewCount(51)
                .commentCount(3L)
                .likeCount(0L)
                .dislikeCount(0L)
                .createdAt(LocalDateTime.of(2026, 1, 31, 13, 52, 32))
                .modifiedAt(LocalDateTime.of(2026, 1, 31, 14, 10, 12))
                .build();

        BoardListResponse serviceResponse = BoardListResponse.builder()
                .message("게시글 목록 조회를 성공했습니다.")
                .boards(List.of(itemResponse))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .pageSize(10)
                .build();

        given(boardService.getBoardList(0, 10)).willReturn(serviceResponse);

        // when
        ResponseEntity<BoardListResponse> response = boardController.getBoardList(0, 10, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("게시글 목록 조회를 성공했습니다.", response.getBody().getMessage());
        assertEquals(1, response.getBody().getBoards().size());
        assertEquals(1L, response.getBody().getBoards().get(0).getBoardId());

        verify(boardService).getBoardList(0, 10);

        log.info("테스트 종료: 게시글 목록 조회 성공 시나리오");
    }

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
        UserDetails userDetails = createUserDetails(userId);

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
        ResponseEntity<BoardCreateResponse> response = boardController.createBoard(request, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("게시글이 성공적으로 작성되었습니다.", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getBoardId());

        verify(boardService).createBoard(userId, request);

        log.info("테스트 종료: 게시글 작성 성공 시나리오");
    }

    /**
     * 게시글 임시 저장 요청 시 200 상태코드와 세션 키를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 임시 저장 성공: 200 상태코드와 세션 키를 반환한다.")
    void tempSaveBoard_Success() {
        log.info("테스트 시작: 게시글 임시 저장 성공 시나리오");

        // given
        BoardController boardController = new BoardController(boardService);
        UUID userId = UUID.randomUUID();
        Long boardId = 1L;
        UserDetails userDetails = createUserDetails(userId);

        BoardTempSaveRequest request = BoardTempSaveRequest.builder()
                .boardTitle("임시 저장 제목")
                .boardContent("임시 저장 내용")
                .imageFileUrl("https://example.com/images/bori.jpg")
                .build();

        BoardTempSaveResponse serviceResponse = BoardTempSaveResponse.toDto(
                "session-key",
                "게시글이 성공적으로 임시 저장되었습니다."
        );

        given(boardService.tempSaveBoard(userId, boardId, request)).willReturn(serviceResponse);

        // when
        ResponseEntity<BoardTempSaveResponse> response = boardController.tempSaveBoard(boardId, request, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("게시글이 성공적으로 임시 저장되었습니다.", response.getBody().getMessage());
        assertEquals("session-key", response.getBody().getSessionKey());

        verify(boardService).tempSaveBoard(userId, boardId, request);

        log.info("테스트 종료: 게시글 임시 저장 성공 시나리오");
    }

    /**
     * 임시 저장 게시글 조회 요청 시 200 상태코드와 저장된 데이터를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("임시 저장 게시글 조회 성공: 200 상태코드와 저장된 데이터를 반환한다.")
    void getTempSavedBoard_Success() {
        log.info("테스트 시작: 임시 저장 게시글 조회 성공 시나리오");

        // given
        BoardController boardController = new BoardController(boardService);
        UUID userId = UUID.randomUUID();
        String sessionKey = "session-key";
        UserDetails userDetails = createUserDetails(userId);

        BoardTempSaveDetailResponse serviceResponse = BoardTempSaveDetailResponse.builder()
                .message("임시 저장된 게시글을 성공적으로 불러왔습니다.")
                .boardTitle("임시 저장 제목")
                .boardContent("임시 저장 내용")
                .imageFileUrl("https://example.com/images/bori.jpg")
                .build();

        given(boardService.getTempSavedBoard(userId, sessionKey)).willReturn(serviceResponse);

        // when
        ResponseEntity<BoardTempSaveDetailResponse> response = boardController.getTempSavedBoard(sessionKey, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("임시 저장된 게시글을 성공적으로 불러왔습니다.", response.getBody().getMessage());
        assertEquals("임시 저장 제목", response.getBody().getBoardTitle());

        verify(boardService).getTempSavedBoard(userId, sessionKey);

        log.info("테스트 종료: 임시 저장 게시글 조회 성공 시나리오");
    }

    /**
     * 게시글 상세 조회 요청 시 200 상태코드와 게시글 상세 정보를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 상세 조회 성공: 200 상태코드와 상세 정보를 반환한다.")
    void getBoardDetail_Success() {
        log.info("테스트 시작: 게시글 상세 조회 성공 시나리오");

        // given
        BoardController boardController = new BoardController(boardService);
        UUID userId = UUID.randomUUID();
        Long boardId = 123L;
        UserDetails userDetails = createUserDetails(userId);

        LocalDateTime createdAt = LocalDateTime.of(2025, 10, 6, 10, 0);
        BoardDetailResponse detailResponse = BoardDetailResponse.builder()
                .message("게시글 상세 조회에 성공했습니다.")
                .boardId(boardId)
                .boardTitle("저희 강아지 자랑합니다!")
                .boardContent("오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?")
                .imageFileUrls(List.of(
                        "https://example.com/images/bori_1.jpg",
                        "https://example.com/images/bori_2.jpg"
                ))
                .nickname("자유로운영혼")
                .viewCount(51)
                .boardCreatedAt(createdAt)
                .modifiedAt(null)
                .build();

        given(boardService.getBoardDetail(userId, boardId)).willReturn(detailResponse);

        // when
        ResponseEntity<BoardDetailResponse> response = boardController.getBoardDetail(boardId, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("게시글 상세 조회에 성공했습니다.", response.getBody().getMessage());
        assertEquals(boardId, response.getBody().getBoardId());
        assertEquals("저희 강아지 자랑합니다!", response.getBody().getBoardTitle());
        assertEquals("자유로운영혼", response.getBody().getNickname());

        verify(boardService).getBoardDetail(userId, boardId);
        log.info("테스트 종료: 게시글 상세 조회 성공 시나리오");
    }

    /**
     * 게시글 수정 요청 시 200 상태코드와 수정 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 수정 성공: 200 상태코드와 수정 성공 메시지를 반환한다.")
    void updateBoard_Success() {
        log.info("테스트 시작: 게시글 수정 성공 시나리오");

        // given
        BoardController boardController = new BoardController(boardService);
        UUID userId = UUID.randomUUID();
        Long boardId = 123L;
        UserDetails userDetails = createUserDetails(userId);

        BoardUpdateRequest request = BoardUpdateRequest.builder()
                .boardTitle("우와 우리 애가!")
                .boardContent("산책을 했어요!!")
                .imageFileUrls(List.of(
                        "https://example.com/images/bori_1.jpg",
                        "https://example.com/images/bori_2.jpg"
                ))
                .build();

        BoardSimpleResponse serviceResponse = BoardSimpleResponse.toDto("게시글이 성공적으로 수정되었습니다.");
        given(boardService.updateBoard(userId, boardId, request)).willReturn(serviceResponse);

        // when
        ResponseEntity<BoardSimpleResponse> response = boardController.updateBoard(boardId, request, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("게시글이 성공적으로 수정되었습니다.", response.getBody().getMessage());

        verify(boardService).updateBoard(userId, boardId, request);
        log.info("테스트 종료: 게시글 수정 성공 시나리오");
    }

    /**
     * 게시글 삭제 요청 시 200 상태코드와 삭제 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 삭제 성공: 200 상태코드와 삭제 성공 메시지를 반환한다.")
    void deleteBoard_Success() {
        log.info("테스트 시작: 게시글 삭제 성공 시나리오");

        // given
        BoardController boardController = new BoardController(boardService);
        UUID userId = UUID.randomUUID();
        Long boardId = 123L;
        UserDetails userDetails = createUserDetails(userId);

        BoardSimpleResponse serviceResponse = BoardSimpleResponse.toDto("게시글이 성공적으로 삭제되었습니다.");
        given(boardService.deleteBoard(userId, boardId)).willReturn(serviceResponse);

        // when
        ResponseEntity<BoardSimpleResponse> response = boardController.deleteBoard(boardId, userDetails);

        // then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("게시글이 성공적으로 삭제되었습니다.", response.getBody().getMessage());

        verify(boardService).deleteBoard(userId, boardId);
        log.info("테스트 종료: 게시글 삭제 성공 시나리오");
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
