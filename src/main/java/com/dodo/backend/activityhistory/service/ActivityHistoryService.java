package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 활동 기록(ActivityHistory) 관련 비즈니스 로직을 처리하는 서비스 인터페이스입니다.
 */
public interface ActivityHistoryService {

    /**
     * 새로운 활동 기록을 생성합니다.
     * <p>
     * <ol>
     * <li>사용자(User) 및 반려동물(Pet) 존재 여부 검증</li>
     * <li>소유권(UserPet) 검증</li>
     * <li>이미 진행 중인 활동(IN_PROGRESS) 여부 확인</li>
     * <li>활동 기록 엔티티 생성 및 저장 (초기 상태: BEFORE)</li>
     * </ol>
     *
     * @param userId  요청한 사용자의 UUID
     * @param request 활동 생성 요청 정보 (petId, activityType)
     * @return 생성된 활동 기록의 응답 DTO (HistoryId 포함)
     */
    ActivityCreateResponse createActivity(UUID userId, ActivityCreateRequest request);

    /**
     * 활동 기록을 시작(IN_PROGRESS)하거나, 중단된 활동을 재개합니다.
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 활동 기록 ID
     * @param request   시작 위치 정보
     * @return 성공 메시지가 담긴 단순 응답 DTO
     */
    ActivitySimpleResponse startActivity(UUID userId, Long historyId, ActivityStartRequest request);

    /**
     * 진행 중인 활동을 완료(COMPLETED) 상태로 변경하고 종료 처리를 수행합니다.
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 활동 기록 ID
     * @return 종료된 활동 기록의 상세 정보 DTO
     */
    ActivityFinishResponse finishActivity(UUID userId, Long historyId);

    /**
     * 진행 중인 활동 기록을 취소(중단)합니다.
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 활동 기록 ID
     * @return 성공 메시지가 담긴 단순 응답 DTO
     */
    ActivitySimpleResponse cancelActivity(UUID userId, Long historyId);

    /**
     * 활동 기록을 삭제합니다.
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 삭제할 활동 기록 ID
     */
    ActivitySimpleResponse deleteActivity(UUID userId, Long historyId);

    /**
     * 내 활동 기록을 페이징하여 조회합니다.
     *
     * @param userId   요청한 사용자의 UUID
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 페이징된 활동 기록 응답 DTO
     */
    ActivityHistoryPageResponse getMyActivityHistory(UUID userId, Pageable pageable);

    /**
     * 특정 활동 기록의 상세 정보를 조회합니다.
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 조회할 활동 기록의 ID
     * @return 활동 기록의 상세 정보 DTO
     */
    ActivityHistoryDetailResponse getActivityHistoryDetail(UUID userId, Long historyId);

    /**
     * 특정 반려동물의 현재 활동 상태를 조회합니다.
     *
     * @param userId 요청한 사용자의 UUID
     * @param petId  상태를 조회할 반려동물의 ID
     * @return 활동 상태 응답 DTO
     */
    ActivityStatusResponse getPetActivityStatus(UUID userId, Long petId);
}