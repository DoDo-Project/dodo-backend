package com.dodo.backend.healthanalysis.entity;

import com.dodo.backend.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 반려동물 건강 분석 결과를 저장하는 엔티티입니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "health_analysis")
public class HealthAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "health_analysis_title", length = 255)
    private String healthAnalysisTitle;

    @Column(name = "health_analysis_summary", columnDefinition = "TEXT")
    private String healthAnalysisSummary;

    @Column(name = "health_analysis_full_content", columnDefinition = "json")
    private String healthAnalysisFullContent;

    @Column(name = "analysis_date", nullable = false)
    private LocalDateTime analysisDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false)
    private AnalysisType analysisType;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false)
    private AnalysisStatus analysisStatus;

    @Column(name = "health_analysis_content", nullable = false, columnDefinition = "TEXT")
    private String healthAnalysisContent;
}
