package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.*;
import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.activityhistory.exception.ActivityHistoryException;
import com.dodo.backend.activityhistory.mapper.ActivityHistoryMapper;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.routepoint.service.RoutePointService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.*;
import static com.dodo.backend.activityhistory.exception.ActivityHistoryErrorCode.*;

/**
 * {@link ActivityHistoryService} 인터페이스의 구현체 클래스입니다.
 * <p>
 * 반려동물의 활동 기록(ActivityHistory)의 생성(Create), 시작(Start/Resume), 중단(Cancel), 종료(Finish) 등
 * 활동 생명주기를 관리하는 핵심 비즈니스 로직을 수행합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityHistoryServiceImpl implements ActivityHistoryService {

    private final ActivityHistoryRepository activityHistoryRepository;
    private final PetService petService;
    private final UserPetService userPetService;
    private final UserService userService;
    private final ImageFileService imageFileService;
    private final ActivityHistoryMapper activityHistoryMapper;
    private final RoutePointService routePointService;

    /**
     * 새로운 활동 기록을 생성합니다.
     * <p>
     * <ol>
     * <li>사용자(User) 및 반려동물(Pet) 정보를 조회합니다.</li>
     * <li>요청한 유저가 해당 반려동물의 승인된(APPROVED) 주인인지 검증합니다.</li>
     * <li>해당 반려동물이 이미 진행 중(IN_PROGRESS)이거나 대기 중(BEFORE)인 활동이 있는지 확인하여 중복 생성을 방지합니다.</li>
     * <li>검증이 완료되면, 활동 상태를 '시작 전(BEFORE)'으로 설정하여 DB에 저장합니다.</li>
     * </ol>
     *
     * @param userId  요청을 수행하는 사용자의 UUID
     * @param request 생성할 활동 정보가 담긴 요청 DTO (petId, activityType)
     * @return 생성된 활동 기록의 ID와 유형을 포함한 응답 DTO
     * @throws ActivityHistoryException 권한이 없거나({@code CREATE_PERMISSION_DENIED}),
     * 이미 진행 중({@code ALREADY_IN_PROGRESS}) 또는 대기 중({@code ALREADY_EXISTS_BEFORE})인 활동이 존재할 경우
     */
    @Transactional
    @Override
    public ActivityCreateResponse createActivity(
            UUID userId,
            ActivityCreateRequest request) {

        User user = userService.getUserById(userId);
        Pet pet = petService.getPetById(request.getPetId());

        boolean isOwner = userPetService.isApprovedPetOwner(user.getUsersId(), pet.getPetId());

        if (!isOwner) {
            throw new ActivityHistoryException(CREATE_PERMISSION_DENIED);
        }

        if (activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.IN_PROGRESS)) {
            throw new ActivityHistoryException(ALREADY_IN_PROGRESS);
        }

        if (activityHistoryRepository.existsByPetAndActivityHistoryStatus(pet, ActivityHistoryStatus.BEFORE)) {
            throw new ActivityHistoryException(ALREADY_EXISTS_BEFORE);
        }

        ActivityHistory activityHistory = request.toEntity(user, pet);
        ActivityHistory savedHistory = activityHistoryRepository.save(activityHistory);

        log.info("활동 기록 생성 완료 - HistoryId: {}, PetId: {}, User: {}",
                savedHistory.getHistoryId(), pet.getPetId(), userId);

        return ActivityCreateResponse.toDto(savedHistory, "활동 기록이 성공적으로 생성되었습니다.");
    }

    /**
     * 활동 기록을 시작(IN_PROGRESS)하거나, 중단된 활동을 재개합니다.
     * <p>
     * 활동의 현재 상태에 따라 두 가지 로직으로 분기됩니다:
     * <ul>
     * <li><b>시작 전(BEFORE):</b> 최초 시작으로 간주하여 시작 시간과 위치 정보를 기록하고 상태를 변경합니다.</li>
     * <li><b>취소됨(CANCELED):</b> 활동 재개로 간주하여 상태를 변경하고 종료 시간을 초기화합니다. (기존 시작 정보 유지)</li>
     * </ul>
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 활동 기록 ID
     * @param request   시작 시점의 GPS 위치 정보(위도, 경도)
     * @return 처리 결과 메시지가 담긴 단순 응답 DTO
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: 해당 ID의 활동 기록이 없는 경우</li>
     * <li>{@code START_PERMISSION_DENIED}: 활동 기록의 소유자가 아닌 경우</li>
     * <li>{@code ALREADY_IN_PROGRESS}: 이미 진행 중이거나 종료된 활동인 경우</li>
     * </ul>
     */
    @Transactional
    @Override
    public ActivitySimpleResponse startActivity(UUID userId, Long historyId, ActivityStartRequest request) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!activityHistory.getUser().getUsersId().equals(userId)) {
            throw new ActivityHistoryException(START_PERMISSION_DENIED);
        }

        ActivityHistoryStatus status = activityHistory.getActivityHistoryStatus();
        String message;

        if (status == ActivityHistoryStatus.BEFORE) {
            activityHistoryMapper.startActivity(
                    historyId,
                    ActivityHistoryStatus.IN_PROGRESS.name(),
                    request.getStartLatitude(),
                    request.getStartLongitude()
            );
            log.info("활동 최초 시작 - HistoryId: {}, User: {}", historyId, userId);
            message = "활동 기록이 시작되었습니다.";

        } else if (status == ActivityHistoryStatus.CANCELED) {
            activityHistoryMapper.resumeActivity(
                    historyId,
                    ActivityHistoryStatus.IN_PROGRESS.name()
            );
            log.info("활동 재개 - HistoryId: {}, User: {}", historyId, userId);
            message = "활동 기록이 재개되었습니다.";
        } else {
            throw new ActivityHistoryException(ALREADY_IN_PROGRESS);
        }

        return ActivitySimpleResponse.toDto(message);
    }

    /**
     * 진행 중인 활동을 취소(중단) 상태로 변경합니다.
     * <p>
     * 활동 상태를 '취소됨(CANCELED)'으로 변경하고, 중단된 시점(종료 시간)을 기록합니다.
     * </p>
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 활동 기록 ID
     * @return 처리 결과 메시지가 담긴 단순 응답 DTO
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: 해당 ID의 활동 기록이 없는 경우</li>
     * <li>{@code STOP_PERMISSION_DENIED}: 활동 기록의 소유자가 아닌 경우</li>
     * <li>{@code ALREADY_COMPLETED}: 진행 중인 활동(IN_PROGRESS)이 아닌 경우</li>
     * </ul>
     */
    @Transactional
    @Override
    public ActivitySimpleResponse cancelActivity(UUID userId, Long historyId) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!activityHistory.getUser().getUsersId().equals(userId)) {
            throw new ActivityHistoryException(STOP_PERMISSION_DENIED);
        }

        if (activityHistory.getActivityHistoryStatus() != ActivityHistoryStatus.IN_PROGRESS) {
            throw new ActivityHistoryException(ALREADY_COMPLETED);
        }

        activityHistoryMapper.cancelActivity(historyId, ActivityHistoryStatus.CANCELED.name());

        log.info("활동 중단(취소) 완료 - HistoryId: {}, User: {}", historyId, userId);

        return ActivitySimpleResponse.toDto("활동 기록이 성공적으로 중단되었습니다.");
    }

    /**
     * 진행 중인 활동을 완료(COMPLETED) 상태로 변경하고 종료 처리를 수행합니다.
     * <p>
     * <ol>
     * <li>활동 기록 존재 여부 및 요청자(User)의 권한(소유권)을 검증합니다.</li>
     * <li>활동 상태가 '시작 전(BEFORE)'이거나 이미 '종료(COMPLETED)'된 경우 예외를 발생시킵니다.</li>
     * <li>{@link RoutePointService}를 호출하여 총 이동 거리(Distance)를 계산합니다.</li>
     * <li>활동 상태를 '완료(COMPLETED)'로 변경하고 서버 시간(NOW)으로 종료 시간을 기록합니다.</li>
     * <li>종료된 활동 정보를 담은 응답 DTO를 반환합니다.</li>
     * </ol>
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 활동 기록 ID
     * @return 종료된 활동 기록의 상세 정보 DTO (이동 거리 포함)
     */
    @Transactional
    @Override
    public ActivityFinishResponse finishActivity(UUID userId, Long historyId) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!activityHistory.getUser().getUsersId().equals(userId)) {
            throw new ActivityHistoryException(STOP_PERMISSION_DENIED);
        }

        if (activityHistory.getActivityHistoryStatus() == ActivityHistoryStatus.BEFORE) {
            throw new ActivityHistoryException(ACTIVITY_NOT_STARTED);
        }

        if (activityHistory.getActivityHistoryStatus() != ActivityHistoryStatus.IN_PROGRESS) {
            throw new ActivityHistoryException(ALREADY_COMPLETED);
        }

        BigDecimal totalDistance = routePointService.calculateTotalDistance(historyId);

        LocalDateTime endTime = LocalDateTime.now();

        activityHistoryMapper.finishActivity(
                historyId,
                "COMPLETED",
                endTime,
                totalDistance
        );

        log.info("활동 종료 완료 - HistoryId: {}, User: {}, Distance: {}m", historyId, userId, totalDistance);

        return ActivityFinishResponse.toDto(
                activityHistory.getHistoryId(),
                activityHistory.getActivityType(),
                totalDistance,
                activityHistory.getActivityHistoryStartAt(),
                endTime,
                "활동 기록이 성공적으로 종료되었습니다."
        );
    }

    /**
     * 활동 기록을 삭제합니다.
     * <p>
     * 활동 기록 존재 여부와 요청자(User)의 소유권을 검증한 후,
     * <b>JPA Repository</b>를 사용하여 데이터를 삭제합니다.
     * </p>
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 삭제할 활동 기록 ID
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: 해당 ID의 활동 기록이 없는 경우</li>
     * <li>{@code DELETE_PERMISSION_DENIED}: 활동 기록의 소유자가 아닌 경우</li>
     * </ul>
     */
    @Transactional
    @Override
    public ActivitySimpleResponse deleteActivity(UUID userId, Long historyId) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!activityHistory.getUser().getUsersId().equals(userId)) {
            throw new ActivityHistoryException(DELETE_PERMISSION_DENIED);
        }

        activityHistoryRepository.delete(activityHistory);

        log.info("활동 기록 삭제 완료 (JPA) - HistoryId: {}, User: {}", historyId, userId);

        return ActivitySimpleResponse.toDto("활동 기록이 성공적으로 삭제되었습니다.");
    }

    /**
     * 내 활동 기록을 조회합니다. (페이지네이션 지원)
     * <p>
     * 1. 사용자 ID로 활동 기록을 페이징 조회합니다. (JPA가 정렬 처리)
     * 2. 조회된 기록에서 반려동물 ID를 추출하여 프로필 이미지를 일괄 조회합니다 (N+1 방지).
     * 3. 엔티티를 DTO로 변환하여 반환합니다.
     * </p>
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityHistoryPageResponse getMyActivityHistory(UUID userId, Pageable pageable) {

        User user = userService.getUserById(userId);

        Page<ActivityHistory> historyPage = activityHistoryRepository.findAllByUser(user, pageable);

        List<Long> petIds = historyPage.getContent().stream()
                .map(history -> history.getPet().getPetId())
                .distinct()
                .toList();

        Map<Long, String> petImageMap = imageFileService.getProfileUrlsByPetIds(petIds);

        List<ActivityHistorySummary> summaries = historyPage.getContent().stream()
                .map(history -> {
                    String profileUrl = petImageMap.get(history.getPet().getPetId());
                    return ActivityHistorySummary.toDto(history, profileUrl);
                })
                .toList();

        return ActivityHistoryPageResponse.toDto(
                "활동 기록 목록을 성공적으로 조회했습니다.",
                historyPage,
                summaries
        );
    }

    /**
     * 특정 활동 기록의 상세 정보를 조회합니다.
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 조회할 활동 기록의 ID
     * @return 활동 기록의 상세 정보 DTO {@link ActivityHistoryDetailResponse}
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: 해당 ID의 활동 기록이 존재하지 않는 경우</li>
     * <li>{@code VIEW_PERMISSION_DENIED}: 요청자가 해당 반려동물의 가족 구성원이 아닌 경우</li>
     * <li>{@code ACTIVITY_NOT_STARTED}: 활동이 아직 시작되지 않은 경우</li>
     * <li>{@code INVALID_REQUEST}: 활동이 진행 중이거나 취소된 상태인 경우</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityHistoryDetailResponse getActivityHistoryDetail(UUID userId, Long historyId) {
        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));

        if (!userPetService.isApprovedPetOwner(userId, activityHistory.getPet().getPetId())) {
            throw new ActivityHistoryException(VIEW_PERMISSION_DENIED);
        }

        String status = activityHistory.getActivityHistoryStatus().name();

        if ("BEFORE".equals(status)) {
            throw new ActivityHistoryException(ACTIVITY_NOT_STARTED);
        }

        if ("IN_PROGRESS".equals(status) || "CANCELED".equals(status)) {
            throw new ActivityHistoryException(INVALID_REQUEST);
        }

        return ActivityHistoryDetailResponse.toDto(
                "해당 활동 정보를 성공적으로 조회했습니다.",
                activityHistory.getHistoryId(),
                activityHistory.getPet().getPetId(),
                activityHistory.getDistance(),
                activityHistory.getActivityHistoryStartAt(),
                activityHistory.getActivityHistoryEndAt(),
                activityHistory.getStartLatitude(),
                activityHistory.getStartLongitude(),
                0,
                false
        );
    }

    /**
     * 특정 반려동물의 현재 활동 상태를 조회합니다.
     * <p>
     * 1. 반려동물 존재 여부와 요청자의 조회 권한을 검증합니다.
     * 2. 가장 최근의 활동 기록을 조회하여 상태별로 메시지와 ID 포함 여부를 결정합니다.
     * - IN_PROGRESS, BEFORE, CANCELED: 액션이 필요한 상태이므로 historyId를 반환합니다.
     * - COMPLETED: 완료된 상태이므로 ID를 반환하지 않습니다.
     * </p>
     *
     * @param userId 요청한 사용자의 UUID
     * @param petId  상태를 조회할 반려동물의 ID
     * @return 활동 상태 응답 DTO {@link ActivityStatusResponse}
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityStatusResponse getPetActivityStatus(UUID userId, Long petId) {

        Pet pet = petService.getPetById(petId);

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new ActivityHistoryException(VIEW_PERMISSION_DENIED);
        }

        return activityHistoryRepository.findFirstByPetOrderByHistoryIdDesc(pet)
                .map(history -> {
                    String status = history.getActivityHistoryStatus().name();

                    if ("IN_PROGRESS".equals(status)) {
                        return ActivityStatusResponse.toDto("활동 기록중인 애완동물입니다.", history.getHistoryId());
                    } else if ("BEFORE".equals(status)) {
                        return ActivityStatusResponse.toDto("활동 시작 전 상태입니다.", history.getHistoryId());
                    } else if ("CANCELED".equals(status)) {
                        return ActivityStatusResponse.toDto("활동이 중단된 상태입니다.", history.getHistoryId());
                    } else {
                        return ActivityStatusResponse.toDto("현재 진행 중인 활동이 없습니다.", null);
                    }
                })
                .orElseGet(() -> ActivityStatusResponse.toDto("활동 기록이 없습니다.", null));
    }

    /**
     * 특정 활동 기록의 상세 경로(GPS 좌표 리스트)를 조회합니다.
     * <p>
     * 1. 활동 기록(History)의 존재 여부를 확인합니다.
     * 2. 요청한 사용자(User)가 해당 반려동물의 승인된 보호자인지 권한을 검증합니다.
     * 3. RoutePointService를 통해 경로 데이터를 Map 리스트 형태로 조회합니다. (Entity 직접 의존 제거)
     * 4. 조회된 데이터를 Response DTO로 변환하여 반환합니다.
     * </p>
     *
     * @param userId    요청한 사용자의 UUID
     * @param historyId 조회할 활동 기록의 ID
     * @return 상세 경로 및 활동 정보 응답 DTO {@link ActivityRouteResponse}
     * @throws ActivityHistoryException
     * <ul>
     * <li>{@code HISTORY_NOT_FOUND}: 해당 ID의 활동 기록이 존재하지 않는 경우</li>
     * <li>{@code VIEW_PERMISSION_DENIED}: 요청자가 해당 반려동물의 보호자가 아닌 경우</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityRouteResponse getActivityRoute(UUID userId, Long historyId) {

        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));


        if (!userPetService.isApprovedPetOwner(userId, activityHistory.getPet().getPetId())) {
            throw new ActivityHistoryException(VIEW_PERMISSION_DENIED);
        }

        List<Map<String, Object>> routePointMaps = routePointService.getRoutePoints(historyId);

        List<RoutePointDto> routePointDtos = routePointMaps.stream()
                .map(map -> RoutePointDto.builder()
                        .routePointId((Long) map.get("routePointId"))
                        .latitude((BigDecimal) map.get("latitude"))
                        .longitude((BigDecimal) map.get("longitude"))
                        .measuredAt((LocalDateTime) map.get("measuredAt"))
                        .build())
                .toList();

        return ActivityRouteResponse.toDto(
                activityHistory,
                routePointDtos,
                "상세 경로를 가져오는데 성공했습니다."
        );
    }

    /**
     * 건강 분석 리포트 생성을 위해 분석 단위별 활동 기록을 조회하고 Map 형태로 변환합니다.
     * <p>
     * 분석 단위에 따라 서로 다른 Repository 메서드를 호출하며,
     * 반환 시 엔티티를 외부로 노출하지 않고 필요한 필드만 추출합니다.
     * </p>
     *
     * @param petId         반려동물 ID
     * @param analysisType  분석 단위 (DAILY/WEEKLY/MONTHLY)
     * @param startDateTime 조회 시작 시각 (포함)
     * @param endDateTime   조회 종료 시각 (미포함 또는 범위 상한)
     * @return 활동 데이터 목록 (historyId, distance, startAt, endAt, status, activityType 등)
     */
    @Transactional(readOnly = true)
    @Override
    public List<Map<String, Object>> getActivitiesForAnalysis(
            Long petId,
            String analysisType,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        List<ActivityHistory> activityHistories;

        if ("DAILY".equals(analysisType)) {
            activityHistories = activityHistoryRepository.findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualAndActivityHistoryStartAtLessThanOrderByActivityHistoryStartAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
        } else if ("WEEKLY".equals(analysisType)) {
            activityHistories = activityHistoryRepository.findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualOrderByActivityHistoryStartAtAsc(
                    petId,
                    startDateTime
            );
        } else {
            activityHistories = activityHistoryRepository.findAllByPet_PetIdAndActivityHistoryStartAtBetweenOrderByActivityHistoryStartAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (ActivityHistory activityHistory : activityHistories) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("historyId", activityHistory.getHistoryId());
            row.put("distance", activityHistory.getDistance());
            row.put("startAt", activityHistory.getActivityHistoryStartAt());
            row.put("endAt", activityHistory.getActivityHistoryEndAt());
            row.put("startLatitude", activityHistory.getStartLatitude());
            row.put("startLongitude", activityHistory.getStartLongitude());
            row.put("status", activityHistory.getActivityHistoryStatus() == null ? null : activityHistory.getActivityHistoryStatus().name());
            row.put("activityType", activityHistory.getActivityType() == null ? null : activityHistory.getActivityType().name());
            result.add(row);
        }
        return result;
    }
}
