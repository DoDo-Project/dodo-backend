package com.dodo.backend.fence.entity;

import com.dodo.backend.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 반려동물의 울타리(Geofence) 설정 정보를 저장하는 엔티티입니다.
 * <p>
 * 울타리 중심 좌표, 반경, 활성화 여부를 포함하며 특정 {@link Pet}에 종속됩니다.
 * 데이터베이스의 {@code fence} 테이블과 매핑됩니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "fence")
public class Fence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fence_id")
    private Long fenceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false, unique = true)
    private Pet pet;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "center_latitude", precision = 10, scale = 8, nullable = false)
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", precision = 11, scale = 8, nullable = false)
    private BigDecimal centerLongitude;

    @Column(name = "radius", nullable = false)
    private Integer radius;

    @CreatedDate
    @Column(name = "fence_created_at", nullable = false, updatable = false)
    private LocalDateTime fenceCreatedAt;

    @Builder.Default
    @Column(name = "fence_is_active", nullable = false)
    private Boolean fenceIsActive = false;
}
