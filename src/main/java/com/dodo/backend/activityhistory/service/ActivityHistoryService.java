package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.*;
import com.dodo.backend.activityhistory.entity.ActivityHistory;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    /**
     * 디바이스 토큰 기반으로 특정 반려동물의 현재 활동 상태를 조회합니다.
     *
     * @param devicePrincipal 디바이스 토큰 Principal(subject)
     * @param petId           상태를 조회할 반려동물의 ID
     * @return 활동 상태 응답 DTO
     */
    ActivityStatusResponse getPetActivityStatusForDevice(String devicePrincipal, Long petId);

    /**
     * 특정 활동 기록의 상세 이동 경로(GPS 좌표 리스트)를 조회합니다.
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 조회할 활동 기록의 ID
     * @return 상세 경로 및 활동 정보 응답 DTO
     */
    ActivityRouteResponse getActivityRoute(UUID userId, Long historyId);

    /**
     * 주변 인기 활동 기록 목록을 커서 기반으로 조회합니다.
     *
     * @param userId       요청 사용자 UUID
     * @param latitude     사용자 현재 위도
     * @param longitude    사용자 현재 경도
     * @param limit        페이지 크기
     * @param reactionType 정렬 기준 반응 타입 (LIKE/DISLIKE)
     * @param cursor       마지막으로 조회한 historyId (없으면 null)
     * @return 주변 인기 활동 목록 응답
     */
    PopularActivityHistoryResponse getPopularActivities(
            UUID userId,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer limit,
            String reactionType,
            Long cursor
    );

    /**
     * 건강 분석용 활동 기록 데이터를 조회합니다.
     *
     * @param petId        반려동물 ID
     * @param analysisType 분석 단위 (DAILY/WEEKLY/MONTHLY)
     * @param startDateTime 조회 시작 시각(포함)
     * @param endDateTime   조회 종료 시각(미포함)
     * @return 활동 기록 데이터 목록 (Map 형태)
     */
    List<Map<String, Object>> getActivitiesForAnalysis(Long petId, String analysisType, LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * 디바이스 토큰 subject(UUID)와 활동 기록의 반려동물 ID 매핑 일치 여부를 검증합니다.
     *
     * @param historyId       활동 기록 ID
     * @param devicePrincipal 웹소켓 Principal 이름(디바이스 UUID 문자열)
     * @return 매핑이 일치하면 true, 아니면 false
     */
    boolean isDeviceAuthorizedForHistory(Long historyId, String devicePrincipal);

    /**
     * 활동 기록 ID로 활동 기록 엔티티를 조회합니다.
     *
     * @param historyId 조회할 활동 기록 ID
     * @return 조회된 활동 기록 엔티티
     */
    ActivityHistory getActivityHistoryById(Long historyId);
}
