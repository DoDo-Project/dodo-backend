package com.dodo.backend.healthanalysis.service;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.auth.client.GptClient;
import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AiReportCreateRequest;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AiReportCreateResponse;
import com.dodo.backend.healthanalysis.entity.AnalysisStatus;
import com.dodo.backend.healthanalysis.entity.AnalysisType;
import com.dodo.backend.healthanalysis.entity.HealthAnalysis;
import com.dodo.backend.healthanalysis.exception.HealthAnalysisException;
import com.dodo.backend.healthanalysis.repository.HealthAnalysisRepository;
import com.dodo.backend.heartrate.entity.HeartRate;
import com.dodo.backend.heartrate.repository.HeartRateRepository;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.petweight.entity.PetWeight;
import com.dodo.backend.petweight.repository.PetWeightRepository;
import com.dodo.backend.userpet.service.UserPetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.ACCESS_DENIED;
import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.INVALID_REQUEST;
import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.PET_NOT_FOUND;

/**
 * {@link HealthAnalysisService} 구현체입니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HealthAnalysisServiceImpl implements HealthAnalysisService {

    private final HealthAnalysisRepository healthAnalysisRepository;
    private final PetService petService;
    private final UserPetService userPetService;
    private final GptClient gptClient;
    private final PetWeightRepository petWeightRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final HeartRateRepository heartRateRepository;

    /**
     * 반려동물 건강 데이터를 수집하여 GPT 분석 결과를 생성하고 분석 리포트를 저장합니다.
     *
     * @param userId  요청 사용자 ID
     * @param petId   반려동물 ID
     * @param request 분석 생성 요청 정보
     * @return 생성된 분석 리포트 식별자와 완료 메시지
     * @throws HealthAnalysisException 요청값이 유효하지 않거나 접근 권한/반려동물 검증에 실패한 경우
     */
    @Override
    @Transactional
    public AiReportCreateResponse createAiReport(UUID userId, Long petId, AiReportCreateRequest request) {
        if (request == null || request.getAnalysisType() == null || request.getAnalysisType().isBlank()) {
            throw new HealthAnalysisException(INVALID_REQUEST);
        }

        String normalizedType = request.getAnalysisType().trim().toUpperCase(Locale.ROOT);
        if (!gptClient.isSupportedAnalysisType(normalizedType)) {
            throw new HealthAnalysisException(INVALID_REQUEST);
        }

        if (!petService.existsPetById(petId)) {
            throw new HealthAnalysisException(PET_NOT_FOUND);
        }

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new HealthAnalysisException(ACCESS_DENIED);
        }

        Map<String, Object> healthData = buildHealthData(normalizedType, petId);
        Map<String, Object> generated = gptClient.generateHealthAnalysis(normalizedType, petId, healthData);

        HealthAnalysis saved = healthAnalysisRepository.save(
                HealthAnalysis.builder()
                        .pet(Pet.builder().petId(petId).build())
                        .healthAnalysisTitle((String) generated.get("title"))
                        .healthAnalysisSummary((String) generated.get("summary"))
                        .healthAnalysisFullContent((String) generated.get("fullContent"))
                        .analysisDate(LocalDateTime.now())
                        .analysisType(AnalysisType.valueOf(normalizedType))
                        .analysisStatus(AnalysisStatus.COMPLETED)
                        .healthAnalysisContent((String) generated.get("content"))
                        .build()
        );

        log.info("AI 건강 분석 리포트 생성 완료 - User: {}, PetId: {}, AnalysisId: {}", userId, petId, saved.getAnalysisId());

        return AiReportCreateResponse.toDto(saved.getAnalysisId(), "건강 분석 보고서 생성이 완료되었습니다.");
    }

    /**
     * 분석 단위(DAILY/WEEKLY/MONTHLY)에 따라 데이터를 조회하고 GPT 전달용 payload를 구성합니다.
     *
     * @param analysisType 분석 단위
     * @param petId        반려동물 ID
     * @return GPT 전달용 데이터 맵
     */
    private Map<String, Object> buildHealthData(String analysisType, Long petId) {
        LocalDate today = LocalDate.now();
        String petName = petService.getPetById(petId).getPetName();
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        LocalDate startDate;
        LocalDate endDate;

        if ("DAILY".equals(analysisType)) {
            startDate = today;
            endDate = today.plusDays(1);
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.atStartOfDay();
        } else if ("WEEKLY".equals(analysisType)) {
            startDate = today.minusDays(6);
            endDate = today.plusDays(1);
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.atStartOfDay();
        } else {
            startDate = today.minusDays(29);
            endDate = today.plusDays(1);
            startDateTime = startDate.atStartOfDay();
            endDateTime = endDate.atStartOfDay();
        }

        List<PetWeight> petWeights;
        List<ActivityHistory> activityHistories;
        List<HeartRate> heartRates;

        if ("DAILY".equals(analysisType)) {
            petWeights = petWeightRepository.findAllByPet_PetIdAndPetWeightsMeasuredAtGreaterThanEqualAndPetWeightsMeasuredAtLessThanOrderByPetWeightsMeasuredAtAsc(
                    petId,
                    startDate,
                    endDate
            );
            activityHistories = activityHistoryRepository.findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualAndActivityHistoryStartAtLessThanOrderByActivityHistoryStartAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
            heartRates = heartRateRepository.findAllByActivityHistory_Pet_PetIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
        } else if ("WEEKLY".equals(analysisType)) {
            petWeights = petWeightRepository.findAllByPet_PetIdAndPetWeightsMeasuredAtGreaterThanEqualOrderByPetWeightsMeasuredAtAsc(
                    petId,
                    startDate
            );
            activityHistories = activityHistoryRepository.findAllByPet_PetIdAndActivityHistoryStartAtGreaterThanEqualOrderByActivityHistoryStartAtAsc(
                    petId,
                    startDateTime
            );
            heartRates = heartRateRepository.findAllByActivityHistory_Pet_PetIdAndMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(
                    petId,
                    startDateTime
            );
        } else {
            petWeights = petWeightRepository.findAllByPet_PetIdAndPetWeightsMeasuredAtBetweenOrderByPetWeightsMeasuredAtAsc(
                    petId,
                    startDate,
                    endDate
            );
            activityHistories = activityHistoryRepository.findAllByPet_PetIdAndActivityHistoryStartAtBetweenOrderByActivityHistoryStartAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
            heartRates = heartRateRepository.findAllByActivityHistory_Pet_PetIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                    petId,
                    startDateTime,
                    endDateTime
            );
        }

        List<Map<String, Object>> petWeightData = toPetWeightData(petWeights);
        List<Map<String, Object>> activityHistoryData = toActivityHistoryData(activityHistories);
        List<Map<String, Object>> heartRateData = toHeartRateData(heartRates);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("petId", petId);
        payload.put("petName", petName);
        payload.put("analysisType", analysisType);
        payload.put("reportDate", today.toString());
        payload.put("periodStart", startDateTime);
        payload.put("periodEnd", endDateTime);
        payload.put("petWeights", petWeightData);
        payload.put("activityHistories", activityHistoryData);
        payload.put("heartRates", heartRateData);
        return payload;
    }

    /**
     * 체중 엔티티 목록을 GPT 전달용 맵 목록으로 변환합니다.
     *
     * @param petWeights 체중 엔티티 목록
     * @return 변환된 데이터 목록
     */
    private List<Map<String, Object>> toPetWeightData(List<PetWeight> petWeights) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PetWeight petWeight : petWeights) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("weightId", petWeight.getWeightId());
            row.put("weight", petWeight.getWeight());
            row.put("measuredAt", petWeight.getPetWeightsMeasuredAt());
            result.add(row);
        }
        return result;
    }

    /**
     * 활동 기록 엔티티 목록을 GPT 전달용 맵 목록으로 변환합니다.
     *
     * @param activityHistories 활동 기록 엔티티 목록
     * @return 변환된 데이터 목록
     */
    private List<Map<String, Object>> toActivityHistoryData(List<ActivityHistory> activityHistories) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityHistory activityHistory : activityHistories) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("historyId", activityHistory.getHistoryId());
            row.put("distance", activityHistory.getDistance());
            row.put("startAt", activityHistory.getActivityHistoryStartAt());
            row.put("endAt", activityHistory.getActivityHistoryEndAt());
            row.put("startLatitude", activityHistory.getStartLatitude());
            row.put("startLongitude", activityHistory.getStartLongitude());
            row.put("status", activityHistory.getActivityHistoryStatus() == null ? null : activityHistory.getActivityHistoryStatus().name());
            row.put("activityType", activityHistory.getActivityType() == null ? null : activityHistory.getActivityType().name());
            result.add(row);
        }
        return result;
    }

    /**
     * 심박수 엔티티 목록을 GPT 전달용 맵 목록으로 변환합니다.
     *
     * @param heartRates 심박수 엔티티 목록
     * @return 변환된 데이터 목록
     */
    private List<Map<String, Object>> toHeartRateData(List<HeartRate> heartRates) {
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
