package com.dodo.backend.activityhistory.service;

import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityCreateRequest;
import com.dodo.backend.activityhistory.dto.request.ActivityHistoryRequest.ActivityStartRequest;
import com.dodo.backend.activityhistory.dto.response.ActivityHistoryResponse.*;
import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.activityhistory.exception.ActivityHistoryException;
import com.dodo.backend.activityhistory.mapper.ActivityHistoryMapper;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.imagefile.service.ImageFileService;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.reaction.entity.ReactionType;
import com.dodo.backend.routepoint.entity.RoutePoint;
import com.dodo.backend.routepoint.service.RoutePointService;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.service.UserService;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.dodo.backend.activityhistory.exception.ActivityHistoryErrorCode.*;

/**
 * {@link ActivityHistoryService} 구현체입니다.
 *
 * <p>반려동물의 활동 기록 생성, 시작/재개, 취소, 종료, 조회 기능을 제공합니다.</p>
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
     * 활동 기록을 생성합니다.
     *
     * @param userId 요청 사용자 ID
     * @param request 활동 기록 생성 요청
     * @return 생성된 활동 기록 응답
     */
    @Transactional
    @Override
    public ActivityCreateResponse createActivity(UUID userId, ActivityCreateRequest request) {
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

        ActivityHistory savedHistory = activityHistoryRepository.save(request.toEntity(user, pet));

        log.info("활동 기록 생성 완료 - HistoryId: {}, PetId: {}, User: {}",
                savedHistory.getHistoryId(), pet.getPetId(), userId);

        return ActivityCreateResponse.toDto(savedHistory, "활동 기록이 성공적으로 생성되었습니다.");
    }

    /**
     * 활동 기록을 시작하거나 재개합니다.
     *
     * @param userId 요청 사용자 ID
     * @param historyId 활동 기록 ID
     * @param request 시작 위치 요청
     * @return 처리 결과 응답
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
            log.info("활동 시작 완료 - HistoryId: {}, User: {}", historyId, userId);
            message = "활동 기록이 성공적으로 시작되었습니다.";
        } else if (status == ActivityHistoryStatus.CANCELED) {
            activityHistoryMapper.resumeActivity(historyId, ActivityHistoryStatus.IN_PROGRESS.name());
            log.info("활동 재개 완료 - HistoryId: {}, User: {}", historyId, userId);
            message = "활동 기록이 성공적으로 재개되었습니다.";
        } else {
            throw new ActivityHistoryException(ALREADY_IN_PROGRESS);
        }

        return ActivitySimpleResponse.toDto(message);
    }

    /**
     * 진행 중인 활동을 취소합니다.
     *
     * @param userId 요청 사용자 ID
     * @param historyId 활동 기록 ID
     * @return 처리 결과 응답
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
        log.info("활동 취소 완료 - HistoryId: {}, User: {}", historyId, userId);

        return ActivitySimpleResponse.toDto("활동 기록이 성공적으로 중단되었습니다.");
    }

    /**
     * 활동을 종료 처리합니다.
     *
     * @param userId 요청 사용자 ID
     * @param historyId 활동 기록 ID
     * @return 종료 결과 응답
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

        activityHistoryMapper.finishActivity(historyId, "COMPLETED", endTime, totalDistance);
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
     *
     * @param userId 요청 사용자 ID
     * @param historyId 활동 기록 ID
     * @return 처리 결과 응답
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
        log.info("활동 기록 삭제 완료 - HistoryId: {}, User: {}", historyId, userId);

        return ActivitySimpleResponse.toDto("활동 기록이 성공적으로 삭제되었습니다.");
    }

    /**
     * 내 활동 기록 목록을 조회합니다.
     *
     * @param userId 요청 사용자 ID
     * @param pageable 페이지 정보
     * @return 페이징된 활동 기록 응답
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
                .map(history -> ActivityHistorySummary.toDto(history, petImageMap.get(history.getPet().getPetId())))
                .toList();

        return ActivityHistoryPageResponse.toDto(
                "활동 기록 목록을 성공적으로 조회했습니다.",
                historyPage,
                summaries
        );
    }

    /**
     * 활동 기록 상세를 조회합니다.
     *
     * @param userId 요청 사용자 ID
     * @param historyId 활동 기록 ID
     * @return 활동 기록 상세 응답
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
     * 반려동물의 현재 활동 상태를 조회합니다.
     *
     * @param userId 요청 사용자 ID
     * @param petId 반려동물 ID
     * @return 활동 상태 응답
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityStatusResponse getPetActivityStatus(UUID userId, Long petId) {
        Pet pet = petService.getPetById(petId);

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new ActivityHistoryException(VIEW_PERMISSION_DENIED);
        }

        return buildActivityStatusResponse(pet);
    }

    /**
     * 디바이스 토큰 기반으로 반려동물의 현재 활동 상태를 조회합니다.
     *
     * @param devicePrincipal 디바이스 principal
     * @param petId 반려동물 ID
     * @return 활동 상태 응답
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityStatusResponse getPetActivityStatusForDevice(String devicePrincipal, Long petId) {
        Pet pet = petService.getPetById(petId);

        if (!petService.isDevicePrincipalMatchedPet(devicePrincipal, petId)) {
            throw new ActivityHistoryException(VIEW_PERMISSION_DENIED);
        }

        return buildActivityStatusResponse(pet);
    }

    /**
     * 활동 기록의 경로 정보를 조회합니다.
     *
     * @param userId 요청 사용자 ID
     * @param historyId 활동 기록 ID
     * @return 활동 경로 응답
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
     * 주변 인기 활동 기록을 조회합니다.
     */
    @Transactional(readOnly = true)
    @Override
    public PopularActivityHistoryResponse getPopularActivities(
            UUID userId,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer limit,
            String reactionType,
            Long cursor
    ) {
        if (latitude == null || longitude == null || reactionType == null) {
            throw new ActivityHistoryException(INVALID_REQUEST);
        }

        int safeLimit = (limit == null) ? 10 : limit;
        if (safeLimit <= 0) {
            throw new ActivityHistoryException(INVALID_REQUEST);
        }
        ReactionType parsedReactionType = toReactionType(reactionType);

        Pageable pageable = PageRequest.of(0, safeLimit + 1);
        List<ActivityHistory> candidates = activityHistoryRepository.findPopularByReactionTypeWithCursor(
                ActivityHistoryStatus.COMPLETED,
                parsedReactionType,
                cursor,
                pageable
        );

        boolean hasNext = candidates.size() > safeLimit;
        List<ActivityHistory> activities = hasNext ? candidates.subList(0, safeLimit) : candidates;
        Long nextCursor = hasNext ? activities.get(activities.size() - 1).getHistoryId() : null;

        List<Long> historyIds = activities.stream()
                .map(ActivityHistory::getHistoryId)
                .toList();

        if (historyIds.isEmpty()) {
            return PopularActivityHistoryResponse.toDto(
                    "성공적으로 데이터를 조회했습니다.",
                    null,
                    false,
                    List.of()
            );
        }

        Map<Long, Long> likeCountMap = activityHistoryRepository.countGroupedByHistoryIdsAndReactionType(historyIds, ReactionType.LIKE)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
        Map<Long, Long> dislikeCountMap = activityHistoryRepository.countGroupedByHistoryIdsAndReactionType(historyIds, ReactionType.DISLIKE)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
        Map<Long, String> myReactionMap = activityHistoryRepository.findMyReactionsByUserAndHistoryIds(userId, historyIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((ReactionType) row[1]).name(),
                        (left, right) -> left
                ));

        Map<Long, List<SimpleRoutePointDto>> routePointMap = routePointService.getRoutePointsByHistoryIds(historyIds)
                .stream()
                .collect(Collectors.groupingBy(
                        routePoint -> routePoint.getActivityHistory().getHistoryId(),
                        Collectors.mapping(
                                routePoint -> SimpleRoutePointDto.builder()
                                        .latitude(routePoint.getLatitude())
                                        .longitude(routePoint.getLongitude())
                                        .build(),
                                Collectors.toList()
                        )
                ));

        List<PopularActivityItem> items = activities.stream()
                .map(history -> {
                    Long historyId = history.getHistoryId();
                    BigDecimal baseLatitude = history.getStartLatitude();
                    BigDecimal baseLongitude = history.getStartLongitude();

                    if ((baseLatitude == null || baseLongitude == null)
                            && routePointMap.containsKey(historyId)
                            && !routePointMap.get(historyId).isEmpty()) {
                        SimpleRoutePointDto firstPoint = routePointMap.get(historyId).get(0);
                        baseLatitude = firstPoint.getLatitude();
                        baseLongitude = firstPoint.getLongitude();
                    }

                    BigDecimal distanceFromUser = calculateDistanceInKm(latitude, longitude, baseLatitude, baseLongitude);

                    return PopularActivityItem.toDto(
                            history,
                            distanceFromUser,
                            routePointMap.getOrDefault(historyId, List.of()),
                            likeCountMap.getOrDefault(historyId, 0L),
                            dislikeCountMap.getOrDefault(historyId, 0L),
                            myReactionMap.getOrDefault(historyId, "NONE")
                    );
                })
                .toList();

        return PopularActivityHistoryResponse.toDto(
                "성공적으로 데이터를 조회했습니다.",
                nextCursor,
                hasNext,
                items
        );
    }

    /**
     * 건강 분석 범위에 해당하는 활동 기록을 조회합니다.
     *
     * @param petId 반려동물 ID
     * @param analysisType 분석 범위(DAILY, WEEKLY, MONTHLY)
     * @param startDateTime 조회 시작 시각
     * @param endDateTime 조회 종료 시각
     * @return 분석용 활동 기록 목록
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
            activityHistories = activityHistoryRepository
                    .findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualAndActivityHistoryStartAtLessThanOrderByActivityHistoryStartAtAsc(
                            petId,
                            startDateTime,
                            endDateTime
                    );
        } else if ("WEEKLY".equals(analysisType)) {
            activityHistories = activityHistoryRepository
                    .findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualOrderByActivityHistoryStartAtAsc(
                            petId,
                            startDateTime
                    );
        } else {
            activityHistories = activityHistoryRepository
                    .findAllByPet_PetIdAndActivityHistoryStartAtBetweenOrderByActivityHistoryStartAtAsc(
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

    /**
     * 반려동물의 최신 활동 상태 메시지를 생성합니다.
     *
     * @param pet 반려동물 엔티티
     * @return 활동 상태 응답
     */
    private ActivityStatusResponse buildActivityStatusResponse(Pet pet) {
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
     * 활동 기록에 연결된 반려동물의 디바이스 principal 권한을 확인합니다.
     *
     * @param historyId 활동 기록 ID
     * @param devicePrincipal 디바이스 principal
     * @return 권한 여부
     */
    @Transactional(readOnly = true)
    @Override
    public boolean isDeviceAuthorizedForHistory(Long historyId, String devicePrincipal) {
        ActivityHistory activityHistory = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 활동 기록을 찾을 수 없습니다."));

        Long petId = activityHistory.getPet().getPetId();
        String expectedDevicePrincipal = UUID.nameUUIDFromBytes(("DEVICE:" + petId).getBytes(StandardCharsets.UTF_8))
                .toString();

        return expectedDevicePrincipal.equals(devicePrincipal);
    }

    /**
     * 활동 기록 ID로 엔티티를 조회합니다.
     *
     * @param historyId 활동 기록 ID
     * @return 활동 기록 엔티티
     */
    @Transactional(readOnly = true)
    @Override
    public ActivityHistory getActivityHistoryById(Long historyId) {
        return activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ActivityHistoryException(HISTORY_NOT_FOUND));
    }

    /**
     * 두 좌표 간 거리를 하버사인 공식으로 계산합니다. 단위는 km입니다.
     */
    private BigDecimal calculateDistanceInKm(
            BigDecimal userLatitude,
            BigDecimal userLongitude,
            BigDecimal targetLatitude,
            BigDecimal targetLongitude
    ) {
        if (targetLatitude == null || targetLongitude == null) {
            return BigDecimal.ZERO;
        }

        double earthRadiusKm = 6371.0d;
        double lat1 = Math.toRadians(userLatitude.doubleValue());
        double lon1 = Math.toRadians(userLongitude.doubleValue());
        double lat2 = Math.toRadians(targetLatitude.doubleValue());
        double lon2 = Math.toRadians(targetLongitude.doubleValue());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = earthRadiusKm * c;

        return BigDecimal.valueOf(distanceKm).setScale(1, RoundingMode.HALF_UP);
    }

    private ReactionType toReactionType(String reactionType) {
        String normalized = reactionType.trim().toUpperCase(Locale.ROOT);
        if ("LIKE".equals(normalized)) {
            return ReactionType.LIKE;
        }
        if ("DISLIKE".equals(normalized)) {
            return ReactionType.DISLIKE;
        }
        throw new ActivityHistoryException(INVALID_REQUEST);
    }
}
