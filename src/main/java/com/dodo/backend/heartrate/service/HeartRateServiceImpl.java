package com.dodo.backend.heartrate.service;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.heartrate.entity.HeartRate;
import com.dodo.backend.heartrate.repository.HeartRateRepository;
import com.dodo.backend.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link HeartRateService}의 구현체입니다.
 * <p>
 * 심박수 데이터를 DB에 저장하며, {@link PetService}와 연동하여
 * 반려동물의 기준 심박수와 비교분석을 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartRateServiceImpl implements HeartRateService {

    private final HeartRateRepository heartRateRepository;
    private final PetService petService;

    /**
     * 부정맥 판단 기준값 (BPM 차이)
     * <p>
     * 기준 심박수와 현재 심박수의 차이가 이 값 이상일 경우 부정맥으로 의심합니다.
     */
    private static final int ARRHYTHMIA_THRESHOLD = 30;

    @Transactional
    @Override
    public void saveHeartRate(ActivityHistory activityHistory, Integer heartRateValue, LocalDateTime measuredAt) {

        if (heartRateValue == null) {
            return;
        }

        Long petId = activityHistory.getPet().getPetId();
        boolean isArrhythmia = checkArrhythmia(petId, heartRateValue);

        HeartRate heartRate = HeartRate.builder()
                .activityHistory(activityHistory)
                .heartRateValue(heartRateValue)
                .arrhythmia(isArrhythmia)
                .measuredAt(measuredAt != null ? measuredAt : LocalDateTime.now())
                .build();

        heartRateRepository.save(heartRate);

        if (isArrhythmia) {
            log.warn("부정맥 의심 신호 감지! HistoryId: {}, BPM: {}", activityHistory.getHistoryId(), heartRateValue);
        }
    }

    /**
     * 서버 측 부정맥 판단 알고리즘
     * <p>
     * {@link PetService}를 통해 해당 펫의 기준 심박수(Reference Heart Rate)를 가져와
     * 현재 측정값과 비교합니다.
     *
     * @param petId      반려동물 ID
     * @param currentBpm 현재 측정된 심박수
     * @return 부정맥 의심 여부 (true: 의심, false: 정상)
     */
    private boolean checkArrhythmia(Long petId, Integer currentBpm) {

        Integer refBpm = petService.getAverageHeartRate(petId);

        if (refBpm == null) {
            return false;
        }

        int diff = Math.abs(currentBpm - refBpm);

        return diff >= ARRHYTHMIA_THRESHOLD;
    }

    /**
     * 건강 분석 리포트 생성을 위해 분석 단위별 심박수 기록을 조회하고 Map 형태로 변환합니다.
     * <p>
     * 분석 단위에 따라 서로 다른 Repository 메서드를 호출하며,
     * 심박수, 부정맥 여부, 측정 시각 등 핵심 필드를 추출하여 반환합니다.
     * </p>
     *
     * @param petId         반려동물 ID
     * @param analysisType  분석 단위 (DAILY/WEEKLY/MONTHLY)
     * @param startDateTime 조회 시작 시각 (포함)
     * @param endDateTime   조회 종료 시각 (미포함 또는 범위 상한)
     * @return 심박수 데이터 목록 (heartId, historyId, heartRate, arrhythmia, measuredAt)
     */
    @Transactional(readOnly = true)
    @Override
    public List<Map<String, Object>> getHeartRatesForAnalysis(
            Long petId,
            String analysisType,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        List<HeartRate> heartRates;

        if ("DAILY".equals(analysisType)) {
            heartRates = heartRateRepository.findAllByActivityHistory_Pet_PetIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
        } else if ("WEEKLY".equals(analysisType)) {
            heartRates = heartRateRepository.findAllByActivityHistory_Pet_PetIdAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(
                    petId,
                    startDateTime
            );
        } else {
            heartRates = heartRateRepository.findAllByActivityHistory_Pet_PetIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (HeartRate heartRate : heartRates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("heartId", heartRate.getId());
            row.put("historyId", heartRate.getActivityHistory() == null ? null : heartRate.getActivityHistory().getHistoryId());
            row.put("heartRate", heartRate.getHeartRateValue());
            row.put("arrhythmia", heartRate.getArrhythmia());
            row.put("measuredAt", heartRate.getMeasuredAt());
            result.add(row);
        }
        return result;
    }
}
