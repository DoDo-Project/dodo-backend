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
}