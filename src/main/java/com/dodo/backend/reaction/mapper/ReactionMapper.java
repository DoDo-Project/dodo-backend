package com.dodo.backend.reaction.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * 반응(Reaction) 도메인의 수정 쿼리를 담당하는 MyBatis Mapper 인터페이스입니다.
 */
@Mapper
public interface ReactionMapper {

    /**
     * 특정 사용자가 특정 활동 기록에 남긴 반응 유형을 변경합니다.
     *
     * @param userId       반응을 변경하는 사용자 ID
     * @param historyId    반응 대상 활동 기록 ID
     * @param reactionType 변경할 반응 유형 문자열
     * @return 업데이트된 행 수
     */
    int updateHistoryReactionType(@Param("userId") UUID userId,
                                  @Param("historyId") Long historyId,
                                  @Param("reactionType") String reactionType);

    /**
     * 특정 사용자가 특정 게시물에 남긴 반응 유형을 변경합니다.
     *
     * @param userId       반응을 변경하는 사용자 ID
     * @param boardId      반응 대상 게시물 ID
     * @param reactionType 변경할 반응 유형 문자열
     * @return 업데이트된 행 수
     */
    int updateBoardReactionType(@Param("userId") UUID userId,
                                @Param("boardId") Long boardId,
                                @Param("reactionType") String reactionType);
}
