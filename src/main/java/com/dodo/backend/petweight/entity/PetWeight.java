package com.dodo.backend.petweight.entity;

import com.dodo.backend.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * 반려동물의 체중(Weight) 기록을 관리하는 엔티티 클래스입니다.
 * <p>
 * 체중 정보는 이력(History)으로 관리되므로, 별도의 테이블로 분리하여
 * 측정 일시와 함께 저장합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "pet_weight")
public class PetWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weight_id")
    private Long weightId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "pet_weights_measured_at", nullable = false, updatable = false)
    private LocalDate petWeightsMeasuredAt;
}