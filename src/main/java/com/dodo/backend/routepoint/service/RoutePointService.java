package com.dodo.backend.routepoint.service;

import com.dodo.backend.routepoint.entity.RoutePoint;
import com.dodo.backend.routepoint.socket.request.WebSocketRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RoutePointService {

    /**
     * 활동 상태를 확인하고, 진행 중인 경우에만 경로 데이터를 저장합니다.
     *
     * @param historyId 활동 기록 ID
     * @param request   수신된 위치 및 심박수 데이터 (RouteDataRequest)
     * @return 상태(status)와 생성된 경로 ID(routePointId)를 담은 Map
     */
    Map<String, Object> saveRouteAndGetStatus(Long historyId, WebSocketRequest.RouteDataRequest request);

    /**
     * 특정 활동 기록의 총 이동 거리를 계산합니다.
     *
     * @param historyId 계산할 활동 기록의 ID
     * @return 소수점 셋째 자리까지 반올림한 총 이동 거리 (단위: km, BigDecimal)
     */
    BigDecimal calculateTotalDistance(Long historyId);

    /**
     * 특정 활동 기록의 모든 이동 경로(GPS 좌표)를 시간순으로 조회합니다.
     * <p>
     * Map의 Key 구성:
     * <ul>
     * <li><b>routePointId</b> (Long): 경로 지점 고유 ID</li>
     * <li><b>latitude</b> (BigDecimal): 위도</li>
     * <li><b>longitude</b> (BigDecimal): 경도</li>
     * <li><b>measuredAt</b> (LocalDateTime): 측정 시간</li>
     * </ul>
     * </p>
     *
     * @param historyId 조회할 활동 기록의 ID
     * @return 시간순 정렬된 경로 데이터 Map 리스트
     */
    List<Map<String, Object>> getRoutePoints(Long historyId);

    /**
     * 여러 활동 기록의 경로 좌표를 조회합니다.
     *
     * @param historyIds 조회할 활동 기록 ID 목록
     * @return historyId 및 측정 시간 기준 오름차순 좌표 리스트
     */
    List<RoutePoint> getRoutePointsByHistoryIds(List<Long> historyIds);
}
