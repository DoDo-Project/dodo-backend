package com.dodo.backend.routepoint.service;

import com.dodo.backend.routepoint.socket.request.WebSocketRequest;

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
}