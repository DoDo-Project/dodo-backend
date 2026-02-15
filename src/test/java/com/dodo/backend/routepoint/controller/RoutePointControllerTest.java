package com.dodo.backend.routepoint.controller;

import com.dodo.backend.routepoint.service.RoutePointService;
import com.dodo.backend.routepoint.socket.WebSocketMessage;
import com.dodo.backend.routepoint.socket.request.WebSocketRequest.RouteDataRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link RoutePointController}의 단위 테스트 클래스입니다.
 * <p>
 * 웹소켓 메시지 수신, 서비스 호출, Redis 발행 및 Ack 응답 전송 로직을 검증합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class RoutePointControllerTest {

    @InjectMocks
    private RoutePointController routePointController;

    @Mock
    private RoutePointService routePointService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ChannelTopic topic;

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Principal principal;

    /**
     * 정상적인 경로 데이터를 수신했을 때, 서비스를 호출하고 Redis 발행 및 Ack 응답이 수행되는지 테스트합니다.
     */
    @Test
    @DisplayName("정상적인 경로 데이터 수신 시 Redis 발행 및 Ack 전송 성공")
    void receiveRouteData_Success() {
        log.info("테스트 시작: receiveRouteData_Success");

        // given
        Long historyId = 4L;
        Long generatedRoutePointId = 100L;
        String status = "IN_PROGRESS";

        RouteDataRequest requestData = RouteDataRequest.builder()
                .historyId(historyId)
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .measuredAt(LocalDateTime.now())
                .build();

        WebSocketMessage<RouteDataRequest> message = WebSocketMessage.<RouteDataRequest>builder()
                .payload(requestData)
                .build();

        given(principal.getName()).willReturn("user1");
        given(topic.getTopic()).willReturn("dodo-topic");
        given(routePointService.saveRouteAndGetStatus(eq(historyId), any()))
                .willReturn(Map.of("status", status, "routePointId", generatedRoutePointId));

        log.info("데이터 준비 완료 - historyId: {}, status: {}", historyId, status);

        // when
        log.info("컨트롤러 메서드 호출");
        routePointController.receiveRouteData(historyId, message, principal);

        // then
        log.info("검증 시작: 서비스 호출 여부 확인");
        verify(routePointService).saveRouteAndGetStatus(eq(historyId), any());

        log.info("검증 시작: Redis 발행 여부 확인 (Wrapper 포장 확인)");
        verify(redisTemplate).convertAndSend(eq("dodo-topic"), argThat((String json) ->
                json.contains("\"historyId\":4") && json.contains("\"message\":")
        ));

        log.info("검증 시작: 사용자 Ack 전송 여부 확인");
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/queue/reply"),
                any(WebSocketMessage.class)
        );

        log.info("테스트 종료: 성공");
    }

    /**
     * 경로 변수의 historyId와 메시지 페이로드의 historyId가 일치하지 않을 경우 예외가 발생하는지 테스트합니다.
     */
    @Test
    @DisplayName("경로 변수 ID와 페이로드 ID가 불일치하면 예외 발생")
    void receiveRouteData_IdMismatch() {
        log.info("테스트 시작: receiveRouteData_IdMismatch");

        // given
        Long pathId = 4L;
        Long payloadId = 5L;

        RouteDataRequest requestData = RouteDataRequest.builder()
                .historyId(payloadId)
                .build();

        WebSocketMessage<RouteDataRequest> message = WebSocketMessage.<RouteDataRequest>builder()
                .payload(requestData)
                .build();

        log.info("데이터 준비 완료 - pathId: {}, payloadId: {}", pathId, payloadId);

        // when & then
        log.info("예외 발생 검증 시작");
        assertThatThrownBy(() -> routePointController.receiveRouteData(pathId, message, principal))
                .isInstanceOf(IllegalArgumentException.class);

        log.info("테스트 종료: 성공 (예외 발생 확인됨)");
    }

    /**
     * 활동이 종료 상태(COMPLETED/CANCELED)일 때는 성공 응답이 아닌 에러 ACK가 전송되는지 테스트합니다.
     */
    @Test
    @DisplayName("활동 종료 상태이면 Redis 발행 없이 ERROR ACK를 전송한다")
    void receiveRouteData_InactiveStatus_ErrorAck() {
        log.info("테스트 시작: receiveRouteData_InactiveStatus_ErrorAck");

        // given
        Long historyId = 4L;
        RouteDataRequest requestData = RouteDataRequest.builder()
                .historyId(historyId)
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .measuredAt(LocalDateTime.now())
                .build();

        WebSocketMessage<RouteDataRequest> message = WebSocketMessage.<RouteDataRequest>builder()
                .payload(requestData)
                .build();

        given(principal.getName()).willReturn("user1");
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("status", "COMPLETED");
        serviceResult.put("routePointId", null);
        given(routePointService.saveRouteAndGetStatus(eq(historyId), any()))
                .willReturn(serviceResult);

        // when
        routePointController.receiveRouteData(historyId, message, principal);

        // then
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/queue/reply"),
                any(WebSocketMessage.class)
        );

        log.info("테스트 종료: 성공");
    }
}
