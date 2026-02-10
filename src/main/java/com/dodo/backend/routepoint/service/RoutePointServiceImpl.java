package com.dodo.backend.routepoint.service;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.heartrate.service.HeartRateService;
import com.dodo.backend.routepoint.entity.RoutePoint;
import com.dodo.backend.routepoint.repository.RoutePointRepository;
import com.dodo.backend.routepoint.socket.request.WebSocketRequest.RouteDataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RoutePointService}의 구현체 클래스입니다.
 * <p>
 * 실시간 활동 경로(RoutePoint)를 DB에 저장하고, 활동 종료 시 총 이동 거리를 계산하는 역할을 수행합니다.
 * 또한 {@link HeartRateService}를 호출하여 심박수 데이터 저장 및 부정맥 분석을 위임합니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutePointServiceImpl implements RoutePointService {

    private final RoutePointRepository routePointRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final HeartRateService heartRateService;

    /**
     * 활동 상태를 확인하고, 활동이 진행 중인 경우 경로와 심박수 데이터를 저장한 후 상태와 생성된 경로 ID를 반환합니다.
     * <p>
     * 1. 활동 기록(ActivityHistory) 존재 여부를 확인합니다.<br>
     * 2. 활동 상태가 '진행 중(IN_PROGRESS)'인지 검증합니다. (종료된 경우 저장하지 않음)<br>
     * 3. 위도/경도 데이터가 존재하면 {@link RoutePoint} 엔티티로 변환하여 저장합니다.<br>
     * 4. 심박수 데이터가 존재하면 {@link HeartRateService}로 처리를 위임합니다.
     * </p>
     *
     * @param historyId 활동 기록 ID
     * @param request   수신된 위치 및 심박수 데이터 (DTO)
     * @return 상태(status)와 생성된 경로 지점 ID(routePointId)를 포함하는 Map 객체
     * @throws IllegalArgumentException historyId에 해당하는 활동 기록이 존재하지 않을 경우 발생
     */
    @Transactional
    @Override
    public Map<String, Object> saveRouteAndGetStatus(Long historyId, RouteDataRequest request) {
        Map<String, Object> result = new HashMap<>();

        ActivityHistory history = activityHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 활동 기록을 찾을 수 없습니다. ID: " + historyId));

        String currentStatus = history.getActivityHistoryStatus().name();

        if (!"IN_PROGRESS".equals(currentStatus)) {
            log.warn("활동이 종료된 상태이므로 데이터를 저장하지 않습니다. Status: {}", currentStatus);
            result.put("status", currentStatus);
            result.put("routePointId", null);
            return result;
        }

        Long routePointId = null;

        if (request.getLatitude() != null && request.getLongitude() != null) {
            RoutePoint routePoint = request.toEntity(history);
            RoutePoint savedPoint = routePointRepository.save(routePoint);
            routePointId = savedPoint.getRoutePointId();
        }

        if (request.getHeartrate() != null) {
            heartRateService.saveHeartRate(
                    history,
                    request.getHeartrate(),
                    request.getMeasuredAt()
            );
        }

        result.put("status", "IN_PROGRESS");
        result.put("routePointId", routePointId);

        return result;
    }

    /**
     * 특정 활동 기록의 총 이동 거리를 계산합니다. (Haversine 공식 적용)
     * <p>
     * 1. 저장된 모든 경로 지점을 시간순(measuredAt ASC)으로 조회합니다.<br>
     * 2. 인접한 두 좌표(Point A -> Point B) 간의 거리를 하버사인 공식을 통해 계산하여 누적합니다.<br>
     * 3. 데이터가 없거나 1개뿐인 경우 이동 거리는 0으로 간주합니다.
     * </p>
     *
     * @param historyId 계산할 활동 기록의 ID
     * @return 총 이동 거리 (단위: 미터, BigDecimal 타입)
     */
    @Transactional(readOnly = true)
    @Override
    public BigDecimal calculateTotalDistance(Long historyId) {
        List<RoutePoint> points = routePointRepository.findAllByActivityHistory_HistoryIdOrderByRoutePointsMeasuredAtAsc(historyId);

        if (points.size() < 2) {
            return BigDecimal.ZERO;
        }

        double totalDistance = 0.0;

        for (int i = 0; i < points.size() - 1; i++) {
            RoutePoint p1 = points.get(i);
            RoutePoint p2 = points.get(i + 1);

            totalDistance += haversine(
                    p1.getLatitude().doubleValue(), p1.getLongitude().doubleValue(),
                    p2.getLatitude().doubleValue(), p2.getLongitude().doubleValue()
            );
        }

        return BigDecimal.valueOf(totalDistance)
                .setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * 두 위도/경도 좌표 간의 거리를 계산하는 하버사인(Haversine) 공식입니다.
     * <p>
     * 지구의 곡률을 고려하여 두 지점 사이의 대원 거리(Great-circle distance)를 구합니다.
     *
     * @param lat1 지점 1의 위도
     * @param lon1 지점 1의 경도
     * @param lat2 지점 2의 위도
     * @param lon2 지점 2의 경도
     * @return 두 지점 사이의 거리 (단위: 미터)
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000;
    }
}