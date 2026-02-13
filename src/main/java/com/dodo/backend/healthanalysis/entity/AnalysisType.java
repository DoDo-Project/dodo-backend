package com.dodo.backend.healthanalysis.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 건강 분석 주기를 정의하는 Enum입니다.
 */
@AllArgsConstructor
@Getter
public enum AnalysisType {
    DAILY,
    WEEKLY,
    MONTHLY
}
