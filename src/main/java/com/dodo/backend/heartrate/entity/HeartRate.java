package com.dodo.backend.heartrate.entity;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 활동 중 실시간으로 측정된 심박수 및 생체 신호를 저장하는 엔티티입니다.
 * <p>
 * {@link ActivityHistory}와 다대일(N:1) 관계를 가지며,
 * 특정 시점({@code measuredAt})의 심박수({@code heartRateValue})와 부정맥 여부 등을 기록합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "heart_rate")
public class HeartRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "heart_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private ActivityHistory activityHistory;

    @Column(name = "heartrate")
    private Integer heartRateValue;

    @Column(name = "arrhythmia")
    private Boolean arrhythmia;

    @Column(name = "heartrate_measured_at")
    private LocalDateTime measuredAt;
}