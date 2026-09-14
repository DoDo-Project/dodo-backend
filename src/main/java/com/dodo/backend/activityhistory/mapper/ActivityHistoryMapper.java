package com.dodo.backend.activityhistory.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface ActivityHistoryMapper {

    /**
     * 활동 기록을 시작 상태로 변경하고, 시작 시간(NOW()) 및 위치 정보를 기록합니다.
     *
     * @param historyId      활동 기록 ID
     * @param status         변경할 상태 (IN_PROGRESS)
     * @param startLatitude  시작 위도
     * @param startLongitude 시작 경도
     */
    void startActivity(@Param("historyId") Long historyId,
                       @Param("status") String status,
                       @Param("startLatitude") BigDecimal startLatitude,
                       @Param("startLongitude") BigDecimal startLongitude);

    /**
     * 중단된 활동을 다시 진행 중으로 변경합니다. (재개)
     * 종료 시간(endAt)은 초기화하고 상태를 변경합니다.
     */
    void resumeActivity(@Param("historyId") Long historyId,
                        @Param("status") String status);

    /**
     * 활동을 중단(취소) 상태로 변경하고 종료 시간을 기록합니다.
     *
     * @param historyId 활동 기록 ID
     * @param status    변경할 상태 (CANCELED)
     */
    void cancelActivity(@Param("historyId") Long historyId,
                        @Param("status") String status);

    /**
     * 활동을 완료(COMPLETED) 상태로 변경하고, 종료 시간 및 총 이동 거리를 기록합니다.
     *
     * @param historyId 활동 기록 ID
     * @param status    변경할 상태 (COMPLETED)
     * @param endAt     활동 종료 시간
     * @param distance  총 이동 거리 (km, BigDecimal 타입)
     */
    void finishActivity(@Param("historyId") Long historyId,
                        @Param("status") String status,
                        @Param("endAt") LocalDateTime endAt,
                        @Param("distance") BigDecimal distance);
}
