package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityFinishRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.ActivityCreateResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.ActivitySimpleResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.ActivityFinishResponse;

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
     * 진행 중인 활동 기록을 완료(종료)합니다.
     * <p>
     * 활동 상태를 '완료(COMPLETED)'로 변경하고 종료 시간 및 최종 상태를 기록합니다.
     * </p>
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 활동 기록 ID
     * @param request   종료 시간 및 상태 정보가 담긴 요청 DTO
     * @return 종료된 활동 기록의 상세 정보(거리, 시간 등)를 포함한 응답 DTO
     */
    ActivityFinishResponse finishActivity(UUID userId, Long historyId, ActivityFinishRequest request);

    /**
     * 진행 중인 활동 기록을 취소(중단)합니다.
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 활동 기록 ID
     * @return 성공 메시지가 담긴 단순 응답 DTO
     */
    ActivitySimpleResponse cancelActivity(UUID userId, Long historyId);

}