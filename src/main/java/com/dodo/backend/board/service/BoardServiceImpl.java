package com.dodo.backend.board.service;

import com.dodo.backend.board.dto.request.BoardRequest.BoardUpdateRequest;
import com.dodo.backend.board.entity.Board;
import com.dodo.backend.board.entity.BoardStatus;
import com.dodo.backend.board.exception.BoardException;
import com.dodo.backend.board.mapper.BoardMapper;
import com.dodo.backend.board.repository.BoardRepository;
import com.dodo.backend.board.dto.request.BoardRequest.BoardCreateRequest;
import com.dodo.backend.board.dto.response.BoardResponse.BoardDetailResponse;
import com.dodo.backend.board.dto.response.BoardResponse.BoardSimpleResponse;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.dodo.backend.board.exception.BoardErrorCode.BOARD_NOT_FOUND;
import static com.dodo.backend.board.exception.BoardErrorCode.INVALID_REQUEST;
import static com.dodo.backend.board.exception.BoardErrorCode.DELETE_PERMISSION_DENIED;
import static com.dodo.backend.board.exception.BoardErrorCode.UPDATE_PERMISSION_DENIED;
import static com.dodo.backend.board.exception.BoardErrorCode.VIEW_PERMISSION_DENIED;

/**
 * {@link BoardService} 구현체입니다.

 * 게시글 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    /**
     * 게시글 저장 / 조회를 위한 Repository
     */
    private final BoardRepository boardRepository;

    /**
     * 사용자 조회를 위한 UserService
     */
    private final UserService userService;

    /**
     * 게시글 이미지 저장 및 조회를 위한 ImageFileService
     */
    private final ImageFileService imageFileService;

    /**
     * 게시글 수정/삭제 쿼리를 수행하는 MyBatis Mapper
     */
    private final BoardMapper boardMapper;

    /**
     * 게시글 ID로 게시글 엔티티 조회
     */
    @Transactional(readOnly = true)
    @Override
    public Board getBoardById(Long boardId) {

        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BOARD_NOT_FOUND));
    }

    /**
     * 게시글 생성
     *
     * @param userId 요청 사용자 ID
     * @param request 게시글 생성 요청 DTO
     * @return 생성된 게시글 ID
     */
    @Override
    @Transactional
    public Long createBoard(UUID userId, BoardCreateRequest request) {

        User user = userService.getUserById(userId);

        Board board = request.toEntity(user);

        Board savedBoard = boardRepository.save(board);

        imageFileService.saveBoardImages(savedBoard, request.getImageFileUrls());

        return savedBoard.getBoardId();
    }

    /**
     * 특정 게시글의 상세 정보를 조회합니다.
     * <p>
     * 게시글 ID가 유효하지 않으면 잘못된 요청 예외를 발생시키고,
     * 게시글이 존재하지 않으면 게시글 없음 예외를 발생시킵니다.
     * 현재 상세 조회는 공개 상태({@link BoardStatus#PUBLISHED})인 게시글만 허용합니다.
     *
     * @param boardId 조회할 게시글 ID
     * @return 게시글 상세 조회 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 조회 권한 없음인 경우 발생
     */
    @Override
    @Transactional
    public BoardDetailResponse getBoardDetail(UUID userId, Long boardId) {
        Board board = findBoardById(boardId);

        if (board.getBoardStatus() != BoardStatus.PUBLISHED) {
            throw new BoardException(VIEW_PERMISSION_DENIED);
        }

        Integer responseViewCount = board.getViewCount();
        if (!isBoardOwner(userId, board)) {
            boardMapper.increaseViewCount(boardId);
            responseViewCount = responseViewCount == null ? 1 : responseViewCount + 1;
        }

        var imageFileUrls = imageFileService.getBoardImageUrls(boardId);

        return BoardDetailResponse.toDto(board, imageFileUrls, "게시글 상세 조회에 성공했습니다.", responseViewCount);
    }

    /**
     * 특정 게시글을 수정합니다.
     * <p>
     * 작성자 본인만 수정할 수 있으며, 삭제된 게시글은 수정할 수 없습니다.
     * 제목/내용은 값이 전달된 항목만 변경하고, 이미지 URL 목록이 전달되면 기존 게시글 이미지를 교체합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 수정할 게시글 ID
     * @param request 게시글 수정 요청 DTO
     * @return 게시글 수정 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 수정 권한 없음인 경우 발생
     */
    @Override
    @Transactional
    public BoardSimpleResponse updateBoard(UUID userId, Long boardId, BoardUpdateRequest request) {
        if (request == null) {
            throw new BoardException(INVALID_REQUEST);
        }

        Board board = findBoardById(boardId);
        validateBoardOwner(userId, board, UPDATE_PERMISSION_DENIED);

        if (board.getBoardStatus() == BoardStatus.DELETED) {
            throw new BoardException(UPDATE_PERMISSION_DENIED);
        }

        boolean hasBoardUpdateFields = hasBoardTableUpdateFields(request);
        boolean hasImageUpdateFields = hasImageUpdateFields(request);

        if (!hasBoardUpdateFields && !hasImageUpdateFields) {
            throw new BoardException(INVALID_REQUEST);
        }

        if (hasBoardUpdateFields) {
            boardMapper.updateBoard(boardId, request);
        }

        imageFileService.replaceBoardImages(board, request.getImageFileUrls());

        return BoardSimpleResponse.toDto("게시글이 성공적으로 수정되었습니다.");
    }

    /**
     * 특정 게시글을 삭제 상태로 변경합니다.
     * <p>
     * 작성자 본인만 삭제할 수 있으며, 게시글에 연결된 이미지도 함께 삭제합니다.
     *
     * @param userId  요청 사용자 ID
     * @param boardId 삭제할 게시글 ID
     * @return 게시글 삭제 응답 DTO
     * @throws BoardException 잘못된 요청, 게시글 없음, 삭제 권한 없음인 경우 발생
     */
    @Override
    @Transactional
    public BoardSimpleResponse deleteBoard(UUID userId, Long boardId) {
        Board board = findBoardById(boardId);
        validateBoardOwner(userId, board, DELETE_PERMISSION_DENIED);

        if (board.getBoardStatus() == BoardStatus.DELETED) {
            throw new BoardException(DELETE_PERMISSION_DENIED);
        }

        boardMapper.deleteBoard(boardId, BoardStatus.DELETED.name());
        imageFileService.deleteBoardImages(boardId);

        return BoardSimpleResponse.toDto("게시글이 성공적으로 삭제되었습니다.");
    }

    /**
     * 게시글 ID로 게시글을 조회하고 ID 유효성을 검증합니다.
     *
     * @param boardId 조회할 게시글 ID
     * @return 게시글 엔티티
     */
    private Board findBoardById(Long boardId) {
        if (boardId == null || boardId <= 0) {
            throw new BoardException(INVALID_REQUEST);
        }

        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BOARD_NOT_FOUND));
    }

    /**
     * 요청 사용자가 게시글 작성자인지 검증합니다.
     *
     * @param userId    요청 사용자 ID
     * @param board     권한을 검증할 게시글
     * @param errorCode 권한이 없을 때 반환할 에러 코드
     */
    private void validateBoardOwner(UUID userId, Board board, com.dodo.backend.board.exception.BoardErrorCode errorCode) {
        if (userId == null || board.getUser() == null || !userId.equals(board.getUser().getUsersId())) {
            throw new BoardException(errorCode);
        }
    }

    /**
     * 게시글 테이블에 반영할 수정 필드가 있는지 확인합니다.
     * <p>
     * 실제 컬럼 반영 여부는 MyBatis Mapper XML의 동적 update 문에서 판단합니다.
     *
     * @param request 게시글 수정 요청 DTO
     * @return {@code board} 테이블 업데이트 대상 필드가 하나 이상 있으면 true
     */
    private boolean hasBoardTableUpdateFields(BoardUpdateRequest request) {
        return request.getBoardTitle() != null
                || request.getBoardContent() != null;
    }

    /**
     * 게시글 이미지에 반영할 수정 필드가 있는지 확인합니다.
     * <p>
     * null은 이미지 미수정, 빈 리스트는 전체 이미지 삭제 요청으로 처리합니다.
     *
     * @param request 게시글 수정 요청 DTO
     * @return 이미지 URL 목록 필드가 전달되었으면 true
     */
    private boolean hasImageUpdateFields(BoardUpdateRequest request) {
        return request.getImageFileUrls() != null;
    }

    /**
     * 요청 사용자가 게시글 작성자인지 확인합니다.
     *
     * @param userId 요청 사용자 ID
     * @param board  작성자 여부를 확인할 게시글
     * @return 요청 사용자가 게시글 작성자이면 true
     */
    private boolean isBoardOwner(UUID userId, Board board) {
        return userId != null && board.getUser() != null && userId.equals(board.getUser().getUsersId());
    }
}
