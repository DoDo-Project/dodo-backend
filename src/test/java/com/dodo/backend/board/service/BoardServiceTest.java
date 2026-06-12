package com.dodo.backend.board.service;

import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardTempSaveRequest;
import com.dodo.backend.board.dto.request.BoardRequest.BoardUpdateRequest;
import com.dodo.backend.board.dto.response.BoardResponse.BoardDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardListQueryResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardListResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardSimpleResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardTempSaveDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardTempSaveResponse;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.entity.BoardType;
import com.dodo.backend.board.exception.BoardErrorCode;
import com.dodo.backend.board.exception.BoardException;
import com.dodo.backend.board.mapper.BoardMapper;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

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

    @Mock
    private ImageFileService imageFileService;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    /**
     * 게시글 목록 조회 시 게시글 목록과 페이지 정보를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 목록 조회 성공: 게시글 목록과 페이지 정보를 반환한다.")
    void getBoardList_Success() {
        log.info("테스트 시작: 게시글 목록 조회 성공");

        // given
        BoardListQueryResponse queryResponse = BoardListQueryResponse.builder()
                .boardId(1L)
                .boardTitle("우리 강아지 자랑합니다")
                .boardContent("오늘 산책하다가 찍은 사진이에요. 너무 귀엽죠?")
                .thumbnailImageUrl("https://example.com/images/bori_1.jpg")
                .nickname("자유로운산책")
                .viewCount(51)
                .commentCount(3L)
                .likeCount(0L)
                .dislikeCount(0L)
                .createdAt(LocalDateTime.of(2026, 1, 31, 13, 52, 32))
                .modifiedAt(LocalDateTime.of(2026, 1, 31, 14, 10, 12))
                .build();

        given(boardMapper.findBoardList(0, 10)).willReturn(List.of(queryResponse));
        given(boardMapper.countPublishedBoards()).willReturn(1L);

        // when
        BoardListResponse response = boardService.getBoardList(0, 10);

        // then
        assertNotNull(response);
        assertEquals("게시글 목록 조회를 성공했습니다.", response.getMessage());
        assertEquals(1, response.getBoards().size());
        assertEquals(1, response.getTotalPages());
        assertEquals(1L, response.getTotalElements());
        assertEquals(0, response.getCurrentPage());
        assertEquals(10, response.getPageSize());
        assertEquals("우리 강아지 자랑합니다", response.getBoards().get(0).getBoardTitle());
        assertEquals("오늘 산책하다가 찍은 사진이에요. 너", response.getBoards().get(0).getBoardContentPreview());

        verify(boardMapper).findBoardList(0, 10);
        verify(boardMapper).countPublishedBoards();

        log.info("테스트 종료: 게시글 목록 조회 성공");
    }

    /**
     * 게시글 목록 조회 시 잘못된 페이지 크기이면 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 목록 조회 실패: 페이지 크기가 100보다 크면 예외가 발생한다.")
    void getBoardList_Fail_InvalidSize() {
        log.info("테스트 시작: 게시글 목록 조회 실패 - 잘못된 페이지 크기");

        // when
        BoardException exception = assertThrows(BoardException.class,
                () -> boardService.getBoardList(0, 101));

        // then
        assertEquals(BoardErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(boardMapper, never()).findBoardList(0, 101);

        log.info("테스트 종료: 게시글 목록 조회 실패 - 잘못된 페이지 크기");
    }

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
        verify(imageFileService).saveBoardImages(savedBoard, request.getImageFileUrls());

        Board board = boardCaptor.getValue();
        assertEquals(user, board.getUser());
        assertEquals("저희 강아지 자랑합니다!", board.getBoardTitle());
        assertEquals("오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?", board.getBoardContent());
        assertEquals(BoardStatus.PUBLISHED, board.getBoardStatus());
        assertEquals(BoardType.FREE, board.getBoardType());
        assertEquals(0, board.getViewCount());

        log.info("테스트 종료: 게시글 작성 성공 검증 완료");
    }

    /**
     * 공개 상태의 게시글 상세 조회 시 게시글 정보와 작성자 닉네임을 응답으로 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 상세 조회 성공: 게시글 상세 정보를 반환한다.")
    void getBoardDetail_Success() {
        log.info("테스트 시작: 게시글 상세 조회 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 123L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(userId);
        given(user.getNickname()).willReturn("자유로운영혼");
        given(user.getProfileUrl()).willReturn("https://example.com/profiles/writer.jpg");

        LocalDateTime createdAt = LocalDateTime.of(2025, 10, 6, 10, 0);
        Board board = Board.builder()
                .boardId(boardId)
                .user(user)
                .boardTitle("저희 강아지 자랑합니다!")
                .boardContent("오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?")
                .viewCount(51)
                .boardCreatedAt(createdAt)
                .modifiedAt(null)
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));
        given(imageFileService.getBoardImageUrls(boardId)).willReturn(List.of(
                "https://example.com/images/bori_1.jpg",
                "https://example.com/images/bori_2.jpg"
        ));

        // when
        BoardDetailResponse response = boardService.getBoardDetail(userId, boardId);

        // then
        assertNotNull(response);
        assertEquals("게시글 상세 조회에 성공했습니다.", response.getMessage());
        assertEquals(boardId, response.getBoardId());
        assertEquals("저희 강아지 자랑합니다!", response.getBoardTitle());
        assertEquals("오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?", response.getBoardContent());
        assertEquals("자유로운영혼", response.getNickname());
        assertEquals("https://example.com/profiles/writer.jpg", response.getProfileUrl());
        assertEquals(51, response.getViewCount());
        assertEquals(createdAt, response.getBoardCreatedAt());
        assertEquals(2, response.getImageFileUrls().size());
        assertEquals("https://example.com/images/bori_1.jpg", response.getImageFileUrls().get(0));
        assertEquals("https://example.com/images/bori_2.jpg", response.getImageFileUrls().get(1));

        verify(boardRepository).findById(boardId);
        verify(imageFileService).getBoardImageUrls(boardId);
        verify(boardMapper, never()).increaseViewCount(boardId);
        log.info("테스트 종료: 게시글 상세 조회 성공 검증 완료");
    }

    /**
     * 작성자가 아닌 사용자가 공개 게시글을 상세 조회하면 조회수가 1 증가하고 증가된 조회수가 응답으로 반환되는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 상세 조회 성공: 작성자가 아닌 사용자가 조회하면 조회수가 증가한다.")
    void getBoardDetail_Success_IncreaseViewCount() {
        log.info("테스트 시작: 게시글 상세 조회 시 조회수 증가");

        // given
        UUID ownerId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();
        Long boardId = 123L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(ownerId);
        given(user.getNickname()).willReturn("자유로운영혼");

        Board board = Board.builder()
                .boardId(boardId)
                .user(user)
                .boardTitle("저희 강아지 자랑합니다!")
                .boardContent("오늘 산책하다 찍은 사진이에요. 너무 귀엽죠?")
                .viewCount(51)
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));
        given(imageFileService.getBoardImageUrls(boardId)).willReturn(List.of(
                "https://example.com/images/bori_1.jpg"
        ));

        // when
        BoardDetailResponse response = boardService.getBoardDetail(requestUserId, boardId);

        // then
        assertNotNull(response);
        assertEquals(52, response.getViewCount());
        verify(boardRepository).findById(boardId);
        verify(boardMapper).increaseViewCount(boardId);
        verify(imageFileService).getBoardImageUrls(boardId);
        log.info("테스트 종료: 게시글 상세 조회 시 조회수 증가 검증 완료");
    }

    /**
     * 게시글 ID가 null이거나 0 이하이면 잘못된 요청 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 상세 조회 실패: 잘못된 게시글 ID면 예외가 발생한다.")
    void getBoardDetail_Fail_InvalidBoardId() {
        log.info("테스트 시작: 게시글 상세 조회 실패 - 잘못된 ID");

        // when
        BoardException exception = assertThrows(BoardException.class,
                () -> boardService.getBoardDetail(UUID.randomUUID(), 0L));

        // then
        assertEquals(BoardErrorCode.INVALID_REQUEST, exception.getErrorCode());
        log.info("테스트 종료: 게시글 상세 조회 실패 - 잘못된 ID 검증 완료");
    }

    /**
     * 조회할 게시글이 존재하지 않으면 게시글 없음 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 상세 조회 실패: 게시글을 찾을 수 없으면 예외가 발생한다.")
    void getBoardDetail_Fail_BoardNotFound_Duplicate() {
        log.info("테스트 시작: 게시글 상세 조회 실패 - 게시글 없음");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 999L;
        given(boardRepository.findById(boardId)).willReturn(Optional.empty());

        // when
        BoardException exception = assertThrows(BoardException.class,
                () -> boardService.getBoardDetail(userId, boardId));

        // then
        assertEquals(BoardErrorCode.BOARD_NOT_FOUND, exception.getErrorCode());
        verify(boardRepository).findById(boardId);
        log.info("테스트 종료: 게시글 상세 조회 실패 - 게시글 없음 검증 완료");
    }

    /**
     * 공개 상태가 아닌 게시글을 조회하면 조회 권한 없음 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 상세 조회 실패: 공개 상태가 아니면 예외가 발생한다.")
    void getBoardDetail_Fail_ViewPermissionDenied() {
        log.info("테스트 시작: 게시글 상세 조회 실패 - 조회 권한 없음");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 2L;
        Board board = Board.builder()
                .boardId(boardId)
                .user(mock(User.class))
                .boardTitle("임시 저장 글")
                .boardContent("임시 저장 내용")
                .boardStatus(BoardStatus.DRAFT)
                .boardType(BoardType.FREE)
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));

        // when
        BoardException exception = assertThrows(BoardException.class,
                () -> boardService.getBoardDetail(userId, boardId));

        // then
        assertEquals(BoardErrorCode.VIEW_PERMISSION_DENIED, exception.getErrorCode());
        verify(boardRepository).findById(boardId);
        log.info("테스트 종료: 게시글 상세 조회 실패 - 조회 권한 없음 검증 완료");
    }
    /**
     * 조회할 게시글이 존재하지 않으면 게시글 없음 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 상세 조회 실패: 게시글을 찾을 수 없으면 예외가 발생한다.")
    void getBoardDetail_Fail_BoardNotFound() {
        log.info("테스트 시작: 게시글 상세 조회 실패 - 게시글 없음");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 999L;
        given(boardRepository.findById(boardId)).willReturn(Optional.empty());

        // when
        BoardException exception = assertThrows(BoardException.class,
                () -> boardService.getBoardDetail(userId, boardId));

        // then
        assertEquals(BoardErrorCode.BOARD_NOT_FOUND, exception.getErrorCode());
        verify(boardRepository).findById(boardId);
        log.info("테스트 종료: 게시글 상세 조회 실패 - 게시글 없음 검증 완료");
    }


    /**
     * 작성자 본인이 게시글 제목, 내용, 이미지를 수정하면 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 수정 성공: 작성자 본인이 게시글을 수정한다.")
    void updateBoard_Success_Duplicate() {
        log.info("테스트 시작: 게시글 수정 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 1L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(userId);

        Board board = Board.builder()
                .boardId(boardId)
                .user(user)
                .boardTitle("기존 제목")
                .boardContent("기존 내용")
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        BoardUpdateRequest request = BoardUpdateRequest.builder()
                .boardTitle("우와 우리 애가!")
                .boardContent("산책을 했어요!!")
                .imageFileUrls(List.of(
                        "https://example.com/images/bori_1.jpg",
                        "https://example.com/images/bori_2.jpg"
                ))
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));

        // when
        BoardSimpleResponse response = boardService.updateBoard(userId, boardId, request);

        // then
        assertEquals("게시글이 성공적으로 수정되었습니다.", response.getMessage());
        verify(boardRepository).findById(boardId);
        verify(boardMapper).updateBoard(boardId, request);
        verify(imageFileService).replaceBoardImages(board, request.getImageFileUrls());

        log.info("테스트 종료: 게시글 수정 성공 검증 완료");
    }

    /**
     * 작성자가 아닌 사용자가 게시글을 수정하면 수정 권한 없음 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 수정 실패: 작성자가 아니면 예외가 발생한다.")
    void updateBoard_Fail_PermissionDenied_Duplicate() {
        log.info("테스트 시작: 게시글 수정 실패 - 권한 없음");

        // given
        UUID ownerId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();
        Long boardId = 1L;
        User owner = mock(User.class);
        given(owner.getUsersId()).willReturn(ownerId);

        Board board = Board.builder()
                .boardId(boardId)
                .user(owner)
                .boardTitle("기존 제목")
                .boardContent("기존 내용")
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        BoardUpdateRequest request = BoardUpdateRequest.builder()
                .boardTitle("수정 제목")
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));

        // when
        BoardException exception = assertThrows(BoardException.class,
                () -> boardService.updateBoard(requestUserId, boardId, request));

        // then
        assertEquals(BoardErrorCode.UPDATE_PERMISSION_DENIED, exception.getErrorCode());
        verify(boardRepository).findById(boardId);

        log.info("테스트 종료: 게시글 수정 실패 - 권한 없음 검증 완료");
    }

    /**
     * 작성자 본인이 게시글 제목, 내용, 이미지를 수정하면 성공 메시지를 반환하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 수정 성공: 작성자 본인이 게시글을 수정한다.")
    void updateBoard_Success() {
        log.info("테스트 시작: 게시글 수정 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 1L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(userId);

        Board board = Board.builder()
                .boardId(boardId)
                .user(user)
                .boardTitle("기존 제목")
                .boardContent("기존 내용")
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        BoardUpdateRequest request = BoardUpdateRequest.builder()
                .boardTitle("우와 우리 애가!")
                .boardContent("산책을 했어요!!")
                .imageFileUrls(List.of(
                        "https://example.com/images/bori_1.jpg",
                        "https://example.com/images/bori_2.jpg"
                ))
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));

        // when
        BoardSimpleResponse response = boardService.updateBoard(userId, boardId, request);

        // then
        assertEquals("게시글이 성공적으로 수정되었습니다.", response.getMessage());
        verify(boardRepository).findById(boardId);
        verify(boardMapper).updateBoard(boardId, request);
        verify(imageFileService).replaceBoardImages(board, request.getImageFileUrls());
        log.info("테스트 종료: 게시글 수정 성공 검증 완료");
    }

    /**
     * 이미지 URL 목록이 빈 리스트로 전달되면 기존 이미지를 모두 삭제하는 수정 요청으로 처리되는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 수정 성공: 이미지 URL 빈 리스트로 모든 이미지를 삭제한다.")
    void updateBoard_Success_EmptyImageList() {
        log.info("테스트 시작: 게시글 이미지 전체 삭제 수정 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 1L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(userId);

        Board board = Board.builder()
                .boardId(boardId)
                .user(user)
                .boardTitle("기존 제목")
                .boardContent("기존 내용")
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        BoardUpdateRequest request = BoardUpdateRequest.builder()
                .imageFileUrls(List.of())
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));

        // when
        BoardSimpleResponse response = boardService.updateBoard(userId, boardId, request);

        // then
        assertEquals("게시글이 성공적으로 수정되었습니다.", response.getMessage());
        verify(boardRepository).findById(boardId);
        verify(boardMapper, never()).updateBoard(boardId, request);
        verify(imageFileService).replaceBoardImages(board, request.getImageFileUrls());
        log.info("테스트 종료: 게시글 이미지 전체 삭제 수정 성공 검증 완료");
    }

    /**
     * 작성자가 아닌 사용자가 게시글을 수정하면 수정 권한 없음 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 수정 실패: 작성자가 아니면 예외가 발생한다.")
    void updateBoard_Fail_PermissionDenied() {
        log.info("테스트 시작: 게시글 수정 실패 - 권한 없음");

        // given
        UUID ownerId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();
        Long boardId = 1L;
        User owner = mock(User.class);
        given(owner.getUsersId()).willReturn(ownerId);

        Board board = Board.builder()
                .boardId(boardId)
                .user(owner)
                .boardTitle("기존 제목")
                .boardContent("기존 내용")
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        BoardUpdateRequest request = BoardUpdateRequest.builder()
                .boardTitle("수정 제목")
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));

        // when
        BoardException exception = assertThrows(BoardException.class,
                () -> boardService.updateBoard(requestUserId, boardId, request));

        // then
        assertEquals(BoardErrorCode.UPDATE_PERMISSION_DENIED, exception.getErrorCode());
        verify(boardRepository).findById(boardId);
        log.info("테스트 종료: 게시글 수정 실패 - 권한 없음 검증 완료");
    }

    /**
     * 작성자 본인이 게시글을 삭제하면 삭제 상태로 변경되고 이미지가 삭제되는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 삭제 성공: 작성자 본인이 게시글을 삭제한다.")
    void deleteBoard_Success() {
        log.info("테스트 시작: 게시글 삭제 성공");

        // given
        UUID userId = UUID.randomUUID();
        Long boardId = 1L;
        User user = mock(User.class);
        given(user.getUsersId()).willReturn(userId);

        Board board = Board.builder()
                .boardId(boardId)
                .user(user)
                .boardTitle("삭제할 제목")
                .boardContent("삭제할 내용")
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));

        // when
        BoardSimpleResponse response = boardService.deleteBoard(userId, boardId);

        // then
        assertEquals("게시글이 성공적으로 삭제되었습니다.", response.getMessage());
        verify(boardRepository).findById(boardId);
        verify(boardMapper).deleteBoard(boardId, BoardStatus.DELETED.name());
        verify(imageFileService).deleteBoardImages(boardId);
        log.info("테스트 종료: 게시글 삭제 성공 검증 완료");
    }

    /**
     * 작성자가 아닌 사용자가 게시글을 삭제하면 삭제 권한 없음 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("게시글 삭제 실패: 작성자가 아니면 예외가 발생한다.")
    void deleteBoard_Fail_PermissionDenied() {
        log.info("테스트 시작: 게시글 삭제 실패 - 권한 없음");

        // given
        UUID ownerId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();
        Long boardId = 1L;
        User owner = mock(User.class);
        given(owner.getUsersId()).willReturn(ownerId);

        Board board = Board.builder()
                .boardId(boardId)
                .user(owner)
                .boardTitle("삭제할 제목")
                .boardContent("삭제할 내용")
                .boardStatus(BoardStatus.PUBLISHED)
                .boardType(BoardType.FREE)
                .build();

        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));

        // when
        BoardException exception = assertThrows(BoardException.class,
                () -> boardService.deleteBoard(requestUserId, boardId));

        // then
        assertEquals(BoardErrorCode.DELETE_PERMISSION_DENIED, exception.getErrorCode());
        verify(boardRepository).findById(boardId);
        log.info("테스트 종료: 게시글 삭제 실패 - 권한 없음 검증 완료");
    }
}
