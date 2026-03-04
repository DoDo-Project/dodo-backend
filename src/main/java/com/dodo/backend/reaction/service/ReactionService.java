package com.dodo.backend.reaction.service;

import com.dodo.backend.reaction.dto.request.ReactionRequest.HistoryReactionCreateRequest;
import com.dodo.backend.reaction.dto.request.ReactionRequest.HistoryReactionUpdateRequest;
import com.dodo.backend.reaction.dto.request.ReactionRequest.BoardReactionCreateRequest;
import com.dodo.backend.reaction.dto.request.ReactionRequest.BoardReactionUpdateRequest;
import com.dodo.backend.reaction.dto.response.ReactionResponse.ReactionSimpleResponse;

import java.util.UUID;

/**
 * 반응(Reaction) 도메인의 비즈니스 로직을 처리하는 서비스 인터페이스입니다.
 */
public interface ReactionService {

    /**
     * 특정 활동 기록에 반응을 추가합니다.
     *
     * @param userId  요청 사용자 ID
     * @param request 반응 추가 요청 DTO
     * @return 처리 결과 메시지 응답 DTO
     */
    ReactionSimpleResponse createHistoryReaction(UUID userId, HistoryReactionCreateRequest request);

    /**
     * 특정 활동 기록에 남긴 반응을 변경합니다.
     *
     * @param userId  요청 사용자 ID
     * @param historyId 반응 대상 활동 기록 ID
     * @param request 반응 변경 요청 DTO
     * @return 처리 결과 메시지 응답 DTO
     */
    ReactionSimpleResponse updateHistoryReaction(UUID userId, Long historyId, HistoryReactionUpdateRequest request);

    /**
     * 특정 활동 기록에 남긴 반응을 취소합니다.
     *
     * @param userId 요청 사용자 ID
     * @param historyId 반응 대상 활동 기록 ID
     * @return 처리 결과 메시지 응답 DTO
     */
    ReactionSimpleResponse cancelHistoryReaction(UUID userId, Long historyId);

    /**
     * 특정 게시물에 반응을 추가합니다.
     *
     * @param userId  요청 사용자 ID
     * @param request 반응 추가 요청 DTO
     * @return 처리 결과 메시지 응답 DTO
     */
    ReactionSimpleResponse createBoardReaction(UUID userId, BoardReactionCreateRequest request);

    /**
     * 특정 게시물에 남긴 반응을 변경합니다.
     *
     * @param userId 요청 사용자 ID
     * @param boardId 반응 대상 게시물 ID
     * @param request 반응 변경 요청 DTO
     * @return 처리 결과 메시지 응답 DTO
     */
    ReactionSimpleResponse updateBoardReaction(UUID userId, Long boardId, BoardReactionUpdateRequest request);

    /**
     * 특정 게시물에 남긴 반응을 취소합니다.
     *
     * @param userId 요청 사용자 ID
     * @param boardId 반응 대상 게시물 ID
     * @return 처리 결과 메시지 응답 DTO
     */
    ReactionSimpleResponse cancelBoardReaction(UUID userId, Long boardId);
}
