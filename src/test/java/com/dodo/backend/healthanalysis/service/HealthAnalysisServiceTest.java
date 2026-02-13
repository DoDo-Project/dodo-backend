package com.dodo.backend.healthanalysis.service;

import com.dodo.backend.activityhistory.service.ActivityHistoryService;
import com.dodo.backend.auth.client.GptClient;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisDetailResponse;
import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AiReportCreateRequest;
import com.dodo.backend.healthanalysis.dto.request.HealthAnalysisRequest.AnalysisUpdateRequest;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisDeleteResponse;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AiReportCreateResponse;
import com.dodo.backend.healthanalysis.dto.response.HealthAnalysisResponse.AnalysisUpdateResponse;
import com.dodo.backend.healthanalysis.entity.HealthAnalysis;
import com.dodo.backend.healthanalysis.exception.HealthAnalysisErrorCode;
import com.dodo.backend.healthanalysis.exception.HealthAnalysisException;
import com.dodo.backend.healthanalysis.mapper.HealthAnalysisMapper;
import com.dodo.backend.healthanalysis.repository.HealthAnalysisRepository;
import com.dodo.backend.heartrate.service.HeartRateService;
import com.dodo.backend.pet.entity.Pet;
import com.dodo.backend.pet.service.PetService;
import com.dodo.backend.petweight.service.PetWeightService;
import com.dodo.backend.userpet.service.UserPetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link HealthAnalysisServiceImpl}의 분석 생성 로직을 검증하는 단위 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class HealthAnalysisServiceTest {

    @InjectMocks
    private HealthAnalysisServiceImpl healthAnalysisService;

    @Mock
    private HealthAnalysisRepository healthAnalysisRepository;

    @Mock
    private PetService petService;

    @Mock
    private UserPetService userPetService;

    @Mock
    private GptClient gptClient;

    @Mock
    private PetWeightService petWeightService;

    @Mock
    private ActivityHistoryService activityHistoryService;

    @Mock
    private HeartRateService heartRateService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HealthAnalysisMapper healthAnalysisMapper;

    /**
     * 건강 분석 리포트 생성 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 생성 성공: 유효한 요청이면 GPT 분석 결과를 저장하고 응답을 반환한다.")
    void createAiReport_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 2L;
        AiReportCreateRequest request = AiReportCreateRequest.builder()
                .analysisType("daily")
                .build();

        Pet pet = Pet.builder().petId(petId).petName("바둑이").build();

        Map<String, Object> gptResult = Map.of(
                "title", "바둑이의 2026년 2월 13일 건강 분석",
                "summary", "심박 데이터 중심으로 상태를 점검했습니다.",
                "content", "심박수 변동은 안정 범위입니다.",
                "fullContent", "{\"chartData\":{}}"
        );

        HealthAnalysis saved = HealthAnalysis.builder().build();
        ReflectionTestUtils.setField(saved, "analysisId", 10L);

        given(gptClient.isSupportedAnalysisType("DAILY")).willReturn(true);
        given(petService.existsPetById(petId)).willReturn(true);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(petService.getPetById(petId)).willReturn(pet);
        given(petWeightService.getWeightsForAnalysis(eq(petId), eq("DAILY"), any(), any())).willReturn(List.of());
        given(activityHistoryService.getActivitiesForAnalysis(eq(petId), eq("DAILY"), any(), any())).willReturn(List.of());
        given(heartRateService.getHeartRatesForAnalysis(eq(petId), eq("DAILY"), any(), any())).willReturn(List.of());
        given(gptClient.generateHealthAnalysis(eq("DAILY"), eq(petId), any(Map.class))).willReturn(gptResult);
        given(healthAnalysisRepository.save(any(HealthAnalysis.class))).willReturn(saved);

        // when
        AiReportCreateResponse response = healthAnalysisService.createAiReport(userId, petId, request);

        // then
        assertNotNull(response);
        assertEquals(10L, response.getAnalysisId());
        assertEquals("건강 분석 보고서 생성이 완료되었습니다.", response.getMessage());

        ArgumentCaptor<Map<String, Object>> healthDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gptClient).generateHealthAnalysis(eq("DAILY"), eq(petId), healthDataCaptor.capture());

        Map<String, Object> capturedHealthData = healthDataCaptor.getValue();
        assertEquals(petId, capturedHealthData.get("petId"));
        assertEquals("바둑이", capturedHealthData.get("petName"));
        assertEquals("DAILY", capturedHealthData.get("analysisType"));
        assertTrue(capturedHealthData.get("petWeights") instanceof List<?>);
        assertTrue(capturedHealthData.get("activityHistories") instanceof List<?>);
        assertTrue(capturedHealthData.get("heartRates") instanceof List<?>);
    }

    /**
     * 요청 본문이 비정상일 때 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 생성 실패: 요청이 null이면 INVALID_REQUEST 예외가 발생한다.")
    void createAiReport_Fail_RequestNull() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 2L;

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.createAiReport(userId, petId, null)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(gptClient, never()).generateHealthAnalysis(any(), anyLong(), any(Map.class));
    }

    /**
     * 분석 타입이 지원되지 않을 때 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 생성 실패: 지원하지 않는 분석 타입이면 INVALID_REQUEST 예외가 발생한다.")
    void createAiReport_Fail_UnsupportedAnalysisType() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 2L;
        AiReportCreateRequest request = AiReportCreateRequest.builder()
                .analysisType("YEARLY")
                .build();

        given(gptClient.isSupportedAnalysisType("YEARLY")).willReturn(false);

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.createAiReport(userId, petId, request)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(petService, never()).existsPetById(anyLong());
    }

    /**
     * 반려동물이 존재하지 않을 때 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 생성 실패: 반려동물이 존재하지 않으면 PET_NOT_FOUND 예외가 발생한다.")
    void createAiReport_Fail_PetNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 2L;
        AiReportCreateRequest request = AiReportCreateRequest.builder()
                .analysisType("DAILY")
                .build();

        given(gptClient.isSupportedAnalysisType("DAILY")).willReturn(true);
        given(petService.existsPetById(petId)).willReturn(false);

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.createAiReport(userId, petId, request)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.PET_NOT_FOUND, exception.getErrorCode());
        verify(userPetService, never()).isApprovedPetOwner(any(), anyLong());
    }

    /**
     * 승인된 보호자가 아닐 때 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 생성 실패: 승인된 보호자가 아니면 ACCESS_DENIED 예외가 발생한다.")
    void createAiReport_Fail_AccessDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long petId = 2L;
        AiReportCreateRequest request = AiReportCreateRequest.builder()
                .analysisType("DAILY")
                .build();

        given(gptClient.isSupportedAnalysisType("DAILY")).willReturn(true);
        given(petService.existsPetById(petId)).willReturn(true);
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.createAiReport(userId, petId, request)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(gptClient, never()).generateHealthAnalysis(any(), anyLong(), any(Map.class));
    }

    /**
     * 건강 분석 상세 조회 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 상세 조회 성공: 권한이 있으면 상세 정보를 반환한다.")
    void getAnalysisDetail_Success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Long analysisId = 1L;
        Long petId = 2L;

        HealthAnalysis analysis = HealthAnalysis.builder()
                .analysisDate(java.time.LocalDateTime.now())
                .analysisType(com.dodo.backend.healthanalysis.entity.AnalysisType.DAILY)
                .analysisStatus(com.dodo.backend.healthanalysis.entity.AnalysisStatus.COMPLETED)
                .healthAnalysisTitle("테스트 제목")
                .healthAnalysisSummary("테스트 요약")
                .healthAnalysisFullContent("{\"chartData\":{}}")
                .build();
        ReflectionTestUtils.setField(analysis, "analysisId", analysisId);
        ReflectionTestUtils.setField(analysis, "pet", Pet.builder().petId(petId).build());

        given(healthAnalysisRepository.findById(analysisId)).willReturn(java.util.Optional.of(analysis));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);
        given(objectMapper.readValue("{\"chartData\":{}}", Object.class)).willReturn(Map.of("chartData", Map.of()));

        // when
        AnalysisDetailResponse response = healthAnalysisService.getAnalysisDetail(userId, analysisId);

        // then
        assertEquals("건강 분석 상세 조회에 성공했습니다.", response.getMessage());
        assertEquals(analysisId, response.getAnalysisId());
        assertEquals(petId, response.getPetId());
        assertEquals("DAILY", response.getAnalysisType());
        assertEquals("COMPLETED", response.getAnalysisStatus());
    }

    /**
     * 건강 분석 상세 조회 시 분석 ID가 없으면 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 상세 조회 실패: 분석이 없으면 ANALYSIS_NOT_FOUND 예외가 발생한다.")
    void getAnalysisDetail_Fail_NotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long analysisId = 99L;

        given(healthAnalysisRepository.findById(analysisId)).willReturn(java.util.Optional.empty());

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.getAnalysisDetail(userId, analysisId)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.ANALYSIS_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 건강 분석 수정 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 수정 성공: 권한이 있고 요청값이 유효하면 Mapper를 통해 수정한다.")
    void updateAnalysis_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long analysisId = 11L;
        Long petId = 2L;

        AnalysisUpdateRequest request = AnalysisUpdateRequest.builder()
                .healthAnalysisTitle("수정 제목")
                .healthAnalysisSummary("수정 요약")
                .build();

        HealthAnalysis analysis = HealthAnalysis.builder().build();
        ReflectionTestUtils.setField(analysis, "analysisId", analysisId);
        ReflectionTestUtils.setField(analysis, "pet", Pet.builder().petId(petId).build());

        given(healthAnalysisRepository.findById(analysisId)).willReturn(java.util.Optional.of(analysis));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);

        // when
        AnalysisUpdateResponse response = healthAnalysisService.updateAnalysis(userId, analysisId, request);

        // then
        assertEquals("성공적으로 내용이 수정되었습니다.", response.getMessage());
        verify(healthAnalysisMapper).updateHealthAnalysis(analysisId, request);
    }

    /**
     * 건강 분석 수정 시 분석 ID가 없으면 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 수정 실패: 분석이 없으면 ANALYSIS_NOT_FOUND 예외가 발생한다.")
    void updateAnalysis_Fail_NotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long analysisId = 11L;
        AnalysisUpdateRequest request = AnalysisUpdateRequest.builder()
                .healthAnalysisTitle("수정 제목")
                .build();

        given(healthAnalysisRepository.findById(analysisId)).willReturn(java.util.Optional.empty());

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.updateAnalysis(userId, analysisId, request)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.ANALYSIS_NOT_FOUND, exception.getErrorCode());
        verify(healthAnalysisMapper, never()).updateHealthAnalysis(anyLong(), any());
    }

    /**
     * 건강 분석 수정 시 권한이 없으면 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 수정 실패: 권한이 없으면 UPDATE_PERMISSION_DENIED 예외가 발생한다.")
    void updateAnalysis_Fail_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long analysisId = 11L;
        Long petId = 2L;
        AnalysisUpdateRequest request = AnalysisUpdateRequest.builder()
                .healthAnalysisSummary("수정 요약")
                .build();

        HealthAnalysis analysis = HealthAnalysis.builder().build();
        ReflectionTestUtils.setField(analysis, "analysisId", analysisId);
        ReflectionTestUtils.setField(analysis, "pet", Pet.builder().petId(petId).build());

        given(healthAnalysisRepository.findById(analysisId)).willReturn(java.util.Optional.of(analysis));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.updateAnalysis(userId, analysisId, request)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.UPDATE_PERMISSION_DENIED, exception.getErrorCode());
        verify(healthAnalysisMapper, never()).updateHealthAnalysis(anyLong(), any());
    }

    /**
     * 건강 분석 수정 시 수정 필드가 비어있으면 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 수정 실패: 수정할 필드가 없으면 INVALID_REQUEST 예외가 발생한다.")
    void updateAnalysis_Fail_InvalidRequest() {
        // given
        UUID userId = UUID.randomUUID();
        Long analysisId = 11L;
        AnalysisUpdateRequest request = AnalysisUpdateRequest.builder().build();

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.updateAnalysis(userId, analysisId, request)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(healthAnalysisRepository, never()).findById(anyLong());
    }

    /**
     * 건강 분석 삭제 성공 시나리오를 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 삭제 성공: 권한이 있으면 분석을 삭제하고 성공 메시지를 반환한다.")
    void deleteAnalysis_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Long analysisId = 21L;
        Long petId = 2L;

        HealthAnalysis analysis = HealthAnalysis.builder().build();
        ReflectionTestUtils.setField(analysis, "analysisId", analysisId);
        ReflectionTestUtils.setField(analysis, "pet", Pet.builder().petId(petId).build());

        given(healthAnalysisRepository.findById(analysisId)).willReturn(java.util.Optional.of(analysis));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(true);

        // when
        AnalysisDeleteResponse response = healthAnalysisService.deleteAnalysis(userId, analysisId);

        // then
        assertEquals("성공적으로 삭제되었습니다.", response.getMessage());
        verify(healthAnalysisRepository).delete(analysis);
    }

    /**
     * 건강 분석 삭제 시 분석 ID가 없으면 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 삭제 실패: 분석이 없으면 ANALYSIS_NOT_FOUND 예외가 발생한다.")
    void deleteAnalysis_Fail_NotFound() {
        // given
        UUID userId = UUID.randomUUID();
        Long analysisId = 21L;

        given(healthAnalysisRepository.findById(analysisId)).willReturn(java.util.Optional.empty());

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.deleteAnalysis(userId, analysisId)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.ANALYSIS_NOT_FOUND, exception.getErrorCode());
        verify(healthAnalysisRepository, never()).delete(any(HealthAnalysis.class));
    }

    /**
     * 건강 분석 삭제 시 권한이 없으면 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("건강 분석 삭제 실패: 권한이 없으면 DELETE_PERMISSION_DENIED 예외가 발생한다.")
    void deleteAnalysis_Fail_PermissionDenied() {
        // given
        UUID userId = UUID.randomUUID();
        Long analysisId = 21L;
        Long petId = 2L;

        HealthAnalysis analysis = HealthAnalysis.builder().build();
        ReflectionTestUtils.setField(analysis, "analysisId", analysisId);
        ReflectionTestUtils.setField(analysis, "pet", Pet.builder().petId(petId).build());

        given(healthAnalysisRepository.findById(analysisId)).willReturn(java.util.Optional.of(analysis));
        given(userPetService.isApprovedPetOwner(userId, petId)).willReturn(false);

        // when
        HealthAnalysisException exception = assertThrows(HealthAnalysisException.class, () ->
                healthAnalysisService.deleteAnalysis(userId, analysisId)
        );

        // then
        assertEquals(HealthAnalysisErrorCode.DELETE_PERMISSION_DENIED, exception.getErrorCode());
        verify(healthAnalysisRepository, never()).delete(any(HealthAnalysis.class));
    }
}
