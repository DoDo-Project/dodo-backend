package com.dodo.backend.healthanalysis.repository;

import com.dodo.backend.healthanalysis.entity.HealthAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthAnalysisRepository extends JpaRepository<HealthAnalysis, Long> {
}
