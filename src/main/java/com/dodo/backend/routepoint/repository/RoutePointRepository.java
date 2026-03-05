package com.dodo.backend.routepoint.repository;

import com.dodo.backend.routepoint.entity.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * {@link RoutePoint} 엔티티를 관리하는 JPA 리포지토리입니다.
 * <p>
 * 이동 경로 좌표의 저장 및 조회를 담당합니다.
 */
public interface RoutePointRepository extends JpaRepository<RoutePoint, Long> {

    /**
     * 특정 활동 기록(History)에 속한 모든 경로 지점을 측정 시간(measuredAt) 오름차순으로 조회합니다.
     * <p>
     * 이동 거리 계산 시 순차적인 좌표 데이터가 필요하므로 시간순 정렬이 필수적입니다.
     *
     * @param historyId 조회할 활동 기록의 ID
     * @return 시간순으로 정렬된 RoutePoint 리스트
     */
    List<RoutePoint> findAllByActivityHistory_HistoryIdOrderByRoutePointsMeasuredAtAsc(Long historyId);

    /**
     * 여러 활동 기록의 경로 좌표를 조회합니다.
     * <p>
     * historyId 기준 오름차순, 좌표 측정 시각 오름차순으로 정렬합니다.
     * </p>
     */
    List<RoutePoint> findAllByActivityHistory_HistoryIdInOrderByActivityHistory_HistoryIdAscRoutePointsMeasuredAtAsc(
            Collection<Long> historyIds
    );
}
