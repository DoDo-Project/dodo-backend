package com.dodo.backend.healthanalysis.service;

import com.dodo.backend.activityhistory.service.ActivityHistoryService;
import com.dodo.backend.auth.client.GptClient;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisDetailResponse;
import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AiReportCreateRequest;
import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AnalysisUpdateRequest;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisDeleteResponse;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisListItem;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisListResponse;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AiReportCreateResponse;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisUpdateResponse;
import com.dodo.backend.healthanalysis.entity.AnalysisStatus;
import com.dodo.backend.healthanalysis.entity.AnalysisType;
import com.dodo.backend.healthanalysis.entity.HealthAnalysis;
import com.dodo.backend.healthanalysis.exception.HealthAnalysisException;
import com.dodo.backend.healthanalysis.mapper.HealthAnalysisMapper;
import com.dodo.backend.healthanalysis.repository.HealthAnalysisRepository;
import com.dodo.backend.heartrate.service.HeartRateService;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.petweight.service.PetWeightService;
import com.dodo.backend.userpet.service.UserPetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.ANALYSIS_NOT_FOUND;
import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.ACCESS_DENIED;
import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.DELETE_PERMISSION_DENIED;
import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.INVALID_REQUEST;
import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.PET_NOT_FOUND;
import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.REPORT_HISTORY_PERMISSION_DENIED;
import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.UPDATE_PERMISSION_DENIED;
import static com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode.VIEW_PERMISSION_DENIED;

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
    private final PetWeightService petWeightService;
    private final ActivityHistoryService activityHistoryService;
    private final HeartRateService heartRateService;
    private final ObjectMapper objectMapper;
    private final HealthAnalysisMapper healthAnalysisMapper;

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

        Map<String, Object> healthData = buildHealthData(normalizedType, petId, request.getSpecialNotes());
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
     * 건강 분석 상세 정보를 조회합니다.
     *
     * @param userId 요청 사용자 ID
     * @param analysisId 조회할 분석 ID
     * @return 건강 분석 상세 응답 DTO
     * @throws HealthAnalysisException 분석이 없거나 조회 권한이 없는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public AnalysisDetailResponse getAnalysisDetail(UUID userId, Long analysisId) {
        HealthAnalysis analysis = healthAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new HealthAnalysisException(ANALYSIS_NOT_FOUND));

        Long petId = analysis.getPet().getPetId();
        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new HealthAnalysisException(VIEW_PERMISSION_DENIED);
        }

        return AnalysisDetailResponse.toDto(
                "건강 분석 상세 조회에 성공했습니다.",
                analysis.getAnalysisId(),
                petId,
                analysis.getHealthAnalysisTitle(),
                analysis.getHealthAnalysisSummary(),
                parseJsonSafely(analysis.getHealthAnalysisFullContent()),
                analysis.getAnalysisDate(),
                analysis.getAnalysisType() == null ? null : analysis.getAnalysisType().name(),
                analysis.getAnalysisStatus() == null ? null : analysis.getAnalysisStatus().name()
        );
    }

    /**
     * 건강 분석 제목/요약을 수정합니다.
     *
     * @param userId 요청 사용자 ID
     * @param analysisId 수정할 분석 ID
     * @param request 수정 요청 데이터
     * @return 수정 응답 DTO
     * @throws HealthAnalysisException 잘못된 요청, 분석 없음, 권한 없음인 경우
     */
    @Override
    @Transactional
    public AnalysisUpdateResponse updateAnalysis(UUID userId, Long analysisId, AnalysisUpdateRequest request) {
        if (request == null) {
            throw new HealthAnalysisException(INVALID_REQUEST);
        }

        String title = request.getHealthAnalysisTitle();
        String summary = request.getHealthAnalysisSummary();
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasSummary = summary != null && !summary.isBlank();
        if (!hasTitle && !hasSummary) {
            throw new HealthAnalysisException(INVALID_REQUEST);
        }

        HealthAnalysis analysis = healthAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new HealthAnalysisException(ANALYSIS_NOT_FOUND));

        Long petId = analysis.getPet().getPetId();
        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new HealthAnalysisException(UPDATE_PERMISSION_DENIED);
        }

        healthAnalysisMapper.updateHealthAnalysis(analysisId, request);
        return AnalysisUpdateResponse.toDto("성공적으로 내용이 수정되었습니다.");
    }

    /**
     * 건강 분석 결과를 삭제합니다.
     *
     * @param userId 요청 사용자 ID
     * @param analysisId 삭제할 분석 ID
     * @return 삭제 응답 DTO
     * @throws HealthAnalysisException 분석이 없거나 삭제 권한이 없는 경우
     */
    @Override
    @Transactional
    public AnalysisDeleteResponse deleteAnalysis(UUID userId, Long analysisId) {
        HealthAnalysis analysis = healthAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new HealthAnalysisException(ANALYSIS_NOT_FOUND));

        Long petId = analysis.getPet().getPetId();
        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new HealthAnalysisException(DELETE_PERMISSION_DENIED);
        }

        healthAnalysisRepository.delete(analysis);
        return AnalysisDeleteResponse.toDto("성공적으로 삭제되었습니다.");
    }

    /**
     * 반려동물 기준 건강 분석 결과 목록을 조회합니다.
     *
     * @param userId 요청 사용자 ID
     * @param petId 반려동물 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param period 분석 기간 타입
     * @return 건강 분석 목록 조회 응답 DTO
     * @throws HealthAnalysisException 요청값이 잘못되었거나 권한/반려동물 검증에 실패한 경우
     */
    @Override
    @Transactional(readOnly = true)
    public AnalysisListResponse getAnalysisList(UUID userId, Long petId, int page, int size, String period) {
        if (page < 0 || size <= 0) {
            throw new HealthAnalysisException(INVALID_REQUEST);
        }

        if (!petService.existsPetById(petId)) {
            throw new HealthAnalysisException(PET_NOT_FOUND);
        }

        if (!userPetService.isApprovedPetOwner(userId, petId)) {
            throw new HealthAnalysisException(REPORT_HISTORY_PERMISSION_DENIED);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<HealthAnalysis> analysisPage = getAnalysisPageByPeriod(petId, period, pageable);

        List<AnalysisListItem> items = analysisPage.getContent().stream()
                .map(analysis -> AnalysisListItem.toDto(
                        analysis.getAnalysisId(),
                        analysis.getHealthAnalysisTitle(),
                        analysis.getHealthAnalysisSummary(),
                        parseJsonSafely(analysis.getHealthAnalysisFullContent()),
                        analysis.getAnalysisDate(),
                        analysis.getAnalysisType() == null ? null : analysis.getAnalysisType().name(),
                        analysis.getAnalysisStatus() == null ? null : analysis.getAnalysisStatus().name()
                ))
                .toList();

        return AnalysisListResponse.toDto("건강 분석 결과 조회를 성공했습니다.", analysisPage, items);
    }

    /**
     * 분석 단위(DAILY/WEEKLY/MONTHLY)에 따라 데이터를 조회하고 GPT 전달용 payload를 구성합니다.
     *
     * @param analysisType 분석 단위
     * @param petId        반려동물 ID
     * @return GPT 전달용 데이터 맵
     */
    private Map<String, Object> buildHealthData(String analysisType, Long petId, List<String> specialNotes) {
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

        List<Map<String, Object>> petWeightData = petWeightService.getWeightsForAnalysis(
                petId,
                analysisType,
                startDate,
                endDate
        );
        List<Map<String, Object>> activityHistoryData = activityHistoryService.getActivitiesForAnalysis(
                petId,
                analysisType,
                startDateTime,
                endDateTime
        );
        List<Map<String, Object>> heartRateData = heartRateService.getHeartRatesForAnalysis(
                petId,
                analysisType,
                startDateTime,
                endDateTime
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("petId", petId);
        payload.put("petName", petName);
        payload.put("analysisType", analysisType);
        payload.put("reportDate", today.toString());
        payload.put("periodStart", startDateTime);
        payload.put("periodEnd", endDateTime);
        payload.put("specialNotes", specialNotes == null ? List.of() : specialNotes);
        payload.put("petWeights", petWeightData);
        payload.put("activityHistories", activityHistoryData);
        payload.put("heartRates", heartRateData);
        return payload;
    }

    /**
     * JSON 문자열을 안전하게 객체로 파싱합니다.
     *
     * @param json JSON 문자열
     * @return 파싱된 객체(Map/List), 파싱 실패 시 원본 문자열
     */
    private Object parseJsonSafely(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            log.warn("healthAnalysisFullContent JSON 파싱 실패 - raw 문자열을 반환합니다. analysisFullContent={}", json, e);
            return json;
        }
    }

    /**
     * period 조건에 따라 건강 분석 목록 페이지를 조회합니다.
     *
     * @param petId 반려동물 ID
     * @param period 분석 기간 타입
     * @param pageable 페이징 정보
     * @return 건강 분석 목록 페이지
     */
    private Page<HealthAnalysis> getAnalysisPageByPeriod(Long petId, String period, Pageable pageable) {
        if (period == null || period.isBlank()) {
            return healthAnalysisRepository.findAllByPet_PetIdOrderByAnalysisDateDesc(petId, pageable);
        }

        String normalizedPeriod = period.trim().toUpperCase(Locale.ROOT);
        AnalysisType analysisType;
        try {
            analysisType = AnalysisType.valueOf(normalizedPeriod);
        } catch (IllegalArgumentException e) {
            throw new HealthAnalysisException(INVALID_REQUEST);
        }

        return healthAnalysisRepository.findAllByPet_PetIdAndAnalysisTypeOrderByAnalysisDateDesc(
                petId,
                analysisType,
                pageable
        );
    }

}
