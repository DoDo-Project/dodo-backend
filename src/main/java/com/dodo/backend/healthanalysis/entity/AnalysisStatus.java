package com.dodo.backend.healthanalysis.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 건강 분석 상태를 정의하는 Enum입니다.
 */
@AllArgsConstructor
@Getter
public enum AnalysisStatus {
    PENDING,
    COMPLETED,
    FAILED
}
