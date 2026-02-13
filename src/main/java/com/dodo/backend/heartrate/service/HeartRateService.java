package com.dodo.backend.heartrate.service;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 심박수 데이터 처리를 담당하는 비즈니스 로직 인터페이스입니다.
 * <p>
 * 실시간으로 수신된 심박수 데이터를 검증하고 데이터베이스에 저장하는 기능을 제공합니다.
 */
public interface HeartRateService {

    /**
     * 측정된 심박수 데이터를 저장합니다.
     * <p>
     * 저장 시 내부 알고리즘을 통해 반려동물의 평소 심박수 데이터를 참고하여
     * 부정맥(비정상 심박) 여부를 자동으로 판단하고 기록합니다.
     *
     * @param activityHistory 심박수가 측정된 활동 기록 엔티티
     * @param heartRateValue  측정된 심박수 값 (BPM)
     * @param measuredAt      심박수가 측정된 정확한 시각
     */
    void saveHeartRate(ActivityHistory activityHistory, Integer heartRateValue, LocalDateTime measuredAt);

    /**
     * 건강 분석용 심박수 데이터를 조회합니다.
     *
     * @param petId         반려동물 ID
     * @param analysisType  분석 단위 (DAILY/WEEKLY/MONTHLY)
     * @param startDateTime 조회 시작 시각(포함)
     * @param endDateTime   조회 종료 시각(미포함)
     * @return 심박수 데이터 목록 (Map 형태)
     */
    List<Map<String, Object>> getHeartRatesForAnalysis(Long petId, String analysisType, LocalDateTime startDateTime, LocalDateTime endDateTime);
}
