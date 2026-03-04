package com.dodo.backend.reaction.service;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.service.ActivityHistoryService;
import com.dodo.backend.reaction.dto.request.ReactionRequest.HistoryReactionCreateRequest;
import com.dodo.backend.reaction.dto.response.ReactionResponse.ReactionSimpleResponse;
import com.dodo.backend.reaction.entity.Reaction;
import com.dodo.backend.reaction.exception.ReactionException;
import com.dodo.backend.reaction.repository.ReactionRepository;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.dodo.backend.reaction.exception.ReactionErrorCode.ACCESS_DENIED;
import static com.dodo.backend.reaction.exception.ReactionErrorCode.ACTIVITY_REACTION_ALREADY_EXISTS;
import static com.dodo.backend.reaction.exception.ReactionErrorCode.INVALID_REQUEST;

/**
 * {@link ReactionService} 인터페이스의 구현체로, 반응 도메인의 비즈니스 로직을 수행합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final ActivityHistoryService activityHistoryService;
    private final UserService userService;

    /**
     * {@inheritDoc}
     * <p>
     * 처리 순서는 요청값 검증, 사용자/활동 조회, 접근 권한 검증, 중복 반응 검증, 반응 저장 순으로 진행됩니다.
     */
    @Transactional
    @Override
    public ReactionSimpleResponse createHistoryReaction(UUID userId, HistoryReactionCreateRequest request) {
        if (request == null || request.getHistoryId() == null || request.getReactionType() == null) {
            throw new ReactionException(INVALID_REQUEST);
        }

        User user = userService.getUserById(userId);
        ActivityHistory history = activityHistoryService.getActivityHistoryById(request.getHistoryId());

        if (history.getUser().getUsersId().equals(userId)) {
            throw new ReactionException(ACCESS_DENIED);
        }

        if (reactionRepository.existsByUserAndHistory(user, history)) {
            throw new ReactionException(ACTIVITY_REACTION_ALREADY_EXISTS);
        }

        Reaction reaction = request.toEntity(user, history);
        reactionRepository.save(reaction);

        log.info("활동 반응 추가 완료 - User: {}, HistoryId: {}, ReactionType: {}",
                userId, request.getHistoryId(), reaction.getReactionType());

        return ReactionSimpleResponse.toDto("반응이 성공적으로 추가되었습니다.");
    }
}
