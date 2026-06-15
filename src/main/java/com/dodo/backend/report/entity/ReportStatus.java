package com.dodo.backend.report.entity;

/**
 * 신고 처리 상태를 정의하는 열거형입니다.
 */
public enum ReportStatus {
    /**
     * 신고 접수 후 처리 대기 상태입니다.
     */
    PENDING,

    /**
     * 신고 처리가 완료된 상태입니다.
     */
    COMPLETED
}
