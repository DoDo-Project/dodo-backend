package com.dodo.backend.routepoint.service;

import com.dodo.backend.activityhistory.entity.ActivityHistory;
import com.dodo.backend.activityhistory.entity.ActivityHistoryStatus;
import com.dodo.backend.activityhistory.repository.ActivityHistoryRepository;
import com.dodo.backend.heartrate.service.HeartRateService;
import com.dodo.backend.routepoint.entity.RoutePoint;
import com.dodo.backend.routepoint.repository.RoutePointRepository;
import com.dodo.backend.routepoint.socket.request.WebSocketRequest.RouteDataRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * {@link RoutePointService}의 단위 테스트 클래스입니다.
 * <p>
 * 활동 상태 검증, 경로 데이터 저장, 심박수 서비스 호출 여부 등을 테스트합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class RoutePointServiceTest {

    @InjectMocks
    private RoutePointServiceImpl routePointService;

    @Mock
    private RoutePointRepository routePointRepository;

    @Mock
    private ActivityHistoryRepository activityHistoryRepository;

    @Mock
    private HeartRateService heartRateService;

    @Test
    @DisplayName("누적한 Haversine 거리를 km로 변환해 소수점 셋째 자리까지 HALF_UP 반올림한다")
    void calculateTotalDistance_ReturnsRoundedKilometers() {
        // given: 적도 위에서 정확히 1.2345km에 해당하는 경도 차이
        Long historyId = 1L;
        double longitudeDelta = Math.toDegrees(1.2345 / 6371);
        RoutePoint start = RoutePoint.builder()
                .latitude(BigDecimal.ZERO)
                .longitude(BigDecimal.ZERO)
                .build();
        RoutePoint end = RoutePoint.builder()
                .latitude(BigDecimal.ZERO)
                .longitude(BigDecimal.valueOf(longitudeDelta))
                .build();

        given(routePointRepository.findAllByActivityHistory_HistoryIdOrderByRoutePointsMeasuredAtAsc(historyId))
                .willReturn(List.of(start, end));

        // when
        BigDecimal result = routePointService.calculateTotalDistance(historyId);

        // then
        assertThat(result).isEqualByComparingTo("1.235");
    }

    @Test
    @DisplayName("경로 좌표가 없으면 총 이동 거리로 0을 반환한다")
    void calculateTotalDistance_ReturnsZero_WhenNoRoutePoints() {
        Long historyId = 1L;
        given(routePointRepository.findAllByActivityHistory_HistoryIdOrderByRoutePointsMeasuredAtAsc(historyId))
                .willReturn(List.of());

        BigDecimal result = routePointService.calculateTotalDistance(historyId);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("경로 좌표가 하나이면 총 이동 거리로 0을 반환한다")
    void calculateTotalDistance_ReturnsZero_WhenOneRoutePoint() {
        Long historyId = 1L;
        RoutePoint point = RoutePoint.builder()
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .build();
        given(routePointRepository.findAllByActivityHistory_HistoryIdOrderByRoutePointsMeasuredAtAsc(historyId))
                .willReturn(List.of(point));

        BigDecimal result = routePointService.calculateTotalDistance(historyId);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * 활동이 진행 중(IN_PROGRESS)이고 위치 및 심박수 데이터가 모두 포함된 경우,
     * 정상적으로 저장 로직이 수행되고 상태와 생성된 ID를 반환하는지 테스트합니다.
     */
    @Test
    @DisplayName("활동 중일 때 위치와 심박수 데이터가 모두 저장되어야 한다")
    void saveRouteAndGetStatus_Success_AllData() {
        log.info("테스트 시작: saveRouteAndGetStatus_Success_AllData");

        // given
        Long historyId = 1L;
        Long generatedId = 100L;

        // Mocking Entity & DTO
        ActivityHistory mockHistory = mock(ActivityHistory.class);
        RoutePoint mockSavedRoutePoint = mock(RoutePoint.class);

        RouteDataRequest request = RouteDataRequest.builder()
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .heartrate(95)
                .measuredAt(LocalDateTime.now())
                .build();

        log.info("Mock 데이터 준비 - historyId: {}, lat: {}, lon: {}, heartrate: {}",
                historyId, request.getLatitude(), request.getLongitude(), request.getHeartrate());

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(mockHistory));
        given(mockHistory.getActivityHistoryStatus()).willReturn(ActivityHistoryStatus.IN_PROGRESS);

        given(routePointRepository.save(any(RoutePoint.class))).willReturn(mockSavedRoutePoint);
        given(mockSavedRoutePoint.getRoutePointId()).willReturn(generatedId);

        // when
        log.info("서비스 메서드 실행");
        Map<String, Object> result = routePointService.saveRouteAndGetStatus(historyId, request);

        // then
        log.info("검증 시작: RoutePoint 저장 호출 확인");
        verify(routePointRepository).save(any(RoutePoint.class));

        log.info("검증 시작: 심박수 서비스 호출 확인");
        verify(heartRateService).saveHeartRate(eq(mockHistory), eq(95), any());

        log.info("결과값 검증: status=IN_PROGRESS, routePointId={}", generatedId);
        assertThat(result.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(result.get("routePointId")).isEqualTo(generatedId);

        log.info("테스트 종료: 성공");
    }

    /**
     * 활동이 이미 종료된 상태(COMPLETED)인 경우, 데이터를 저장하지 않고 현재 상태만 반환하는지 테스트합니다.
     */
    @Test
    @DisplayName("활동이 종료된 상태면 데이터를 저장하지 않고 상태만 반환해야 한다")
    void saveRouteAndGetStatus_Fail_ActivityCompleted() {
        log.info("테스트 시작: saveRouteAndGetStatus_Fail_ActivityCompleted");

        // given
        Long historyId = 1L;
        ActivityHistory mockHistory = mock(ActivityHistory.class);

        RouteDataRequest request = RouteDataRequest.builder()
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(mockHistory));
        given(mockHistory.getActivityHistoryStatus()).willReturn(ActivityHistoryStatus.COMPLETED);

        log.info("Mock 설정: 활동 상태 COMPLETED");

        // when
        log.info("서비스 메서드 실행");
        Map<String, Object> result = routePointService.saveRouteAndGetStatus(historyId, request);

        // then
        log.info("검증 시작: 저장소 메서드가 호출되지 않았는지 확인");
        verify(routePointRepository, never()).save(any());
        verify(heartRateService, never()).saveHeartRate(any(), any(), any());

        log.info("결과값 검증: status=COMPLETED, routePointId=null");
        assertThat(result.get("status")).isEqualTo("COMPLETED");
        assertThat(result.get("routePointId")).isNull();

        log.info("테스트 종료: 성공");
    }

    /**
     * 요청한 historyId에 해당하는 활동 기록이 존재하지 않을 경우 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("존재하지 않는 활동 기록 ID로 요청 시 예외가 발생해야 한다")
    void saveRouteAndGetStatus_Fail_NotFound() {
        log.info("테스트 시작: saveRouteAndGetStatus_Fail_NotFound");

        // given
        Long historyId = 999L;
        RouteDataRequest request = RouteDataRequest.builder().build();

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.empty());
        log.info("Mock 설정: ID {}에 대한 활동 기록 없음", historyId);

        // when & then
        log.info("예외 발생 검증 시작");
        assertThatThrownBy(() -> routePointService.saveRouteAndGetStatus(historyId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 활동 기록을 찾을 수 없습니다");

        log.info("테스트 종료: 성공 (예외 발생 확인됨)");
    }

    /**
     * 위치 정보는 없고 심박수 데이터만 있는 경우, RoutePoint는 저장하지 않고 심박수만 저장하는지 테스트합니다.
     */
    @Test
    @DisplayName("위치 정보가 없으면 RoutePoint는 저장하지 않고 심박수만 저장해야 한다")
    void saveRouteAndGetStatus_Success_HeartRateOnly() {
        log.info("테스트 시작: saveRouteAndGetStatus_Success_HeartRateOnly");

        // given
        Long historyId = 1L;
        ActivityHistory mockHistory = mock(ActivityHistory.class);

        RouteDataRequest request = RouteDataRequest.builder()
                .heartrate(100)
                .measuredAt(LocalDateTime.now())
                .build(); // lat, lon is null

        log.info("Mock 데이터 준비: 위치 정보 없음, 심박수 100");

        given(activityHistoryRepository.findById(historyId)).willReturn(Optional.of(mockHistory));
        given(mockHistory.getActivityHistoryStatus()).willReturn(ActivityHistoryStatus.IN_PROGRESS);

        // when
        log.info("서비스 메서드 실행");
        Map<String, Object> result = routePointService.saveRouteAndGetStatus(historyId, request);

        // then
        log.info("검증 시작: RoutePoint 저장은 생략되었는지 확인");
        verify(routePointRepository, never()).save(any());

        log.info("검증 시작: 심박수 저장은 호출되었는지 확인");
        verify(heartRateService).saveHeartRate(eq(mockHistory), eq(100), any());

        log.info("결과값 검증: status=IN_PROGRESS");
        assertThat(result.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(result.get("routePointId")).isNull();

        log.info("테스트 종료: 성공");
    }
}
