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

import java.util.HashMap;
import java.util.Map;

/**
 * {@link RoutePointService}의 구현체입니다.
 * <p>
 * 실시간 활동 경로(RoutePoint)를 저장하고, 동시에 {@link HeartRateService}를 호출하여
 * 심박수 데이터 저장 및 부정맥 분석을 위임합니다.
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
     *
     * @param historyId 활동 기록 ID
     * @param request   수신된 위치 및 심박수 데이터 (DTO)
     * @return 상태(status)와 경로 지점 ID(routePointId)를 포함하는 Map 객체
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
}