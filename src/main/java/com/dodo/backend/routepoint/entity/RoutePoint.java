package com.dodo.backend.routepoint.entity;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 반려동물 활동 중 기록되는 실시간 경로 좌표(GPS) 엔티티입니다.
 * <p>
 * 웹소켓을 통해 전달받은 위도, 경도 데이터를 저장하며,
 * 특정 활동 기록({@link ActivityHistory})과 다대일(N:1) 관계를 맺습니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "route_point")
public class RoutePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_point_id")
    private Long routePointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private ActivityHistory activityHistory;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime routePointsMeasuredAt;

}