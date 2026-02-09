package com.dodo.backend.routepoint.controller;

import com.dodo.backend.routepoint.service.RoutePointService;
import com.dodo.backend.routepoint.socket.WebSocketMessage;
import com.dodo.backend.routepoint.socket.request.WebSocketRequest.RouteDataRequest;
import com.dodo.backend.routepoint.socket.response.WebSocketResponse.RouteDataDetailResponse;
import com.dodo.backend.routepoint.socket.response.WebSocketResponse.RouteDataErrorResponse;
import com.dodo.backend.routepoint.socket.response.WebSocketResponse.RouteDataSuccessResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 웹소켓(STOMP)을 통해 실시간 활동 경로 데이터를 처리하고 전파하는 컨트롤러입니다.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class RoutePointController {

    private final RoutePointService routePointService;
    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic topic;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Redis Pub/Sub 전송을 위해 라우팅 정보(historyId)와 실제 메시지를 포장하는 내부 래퍼 클래스입니다.
     */
    @Getter
    @AllArgsConstructor
    private static class RedisPublishWrapper {
        private Long historyId;
        private Object message;
    }

    /**
     * 클라이언트로부터 데이터를 수신하여 저장하고, 방송(Broadcast) 및 개인 응답(Ack)을 각각 지정된 DTO 규격으로 전송합니다.
     * <p>
     * 서비스 레이어로부터 반환된 Map 객체에서 상태값과 생성된 경로 ID를 추출하여 각 응답 메시지에 포함시킵니다.
     * Redis로 발행되는 메시지는 historyId가 포함되지 않은 순수 데이터 형식을 가지며,
     * RedisSubscriber가 라우팅할 수 있도록 RedisPublishWrapper로 감싸져서 전송됩니다.
     *
     * @param historyId 활동 기록 식별자
     * @param message   요청 메시지 객체
     * @param principal 인증된 사용자 정보
     * @throws IllegalArgumentException 경로 변수의 historyId와 요청 본문의 historyId가 일치하지 않을 경우
     */
    @MessageMapping("/activities/history/routes/{historyId}")
    public void receiveRouteData(
            @DestinationVariable Long historyId,
            WebSocketMessage<RouteDataRequest> message,
            Principal principal
    ) {
        RouteDataRequest requestData = message.getPayload();

        if (!historyId.equals(requestData.getHistoryId())) {
            throw new IllegalArgumentException("활동 기록 식별자가 일치하지 않습니다.");
        }

        Map<String, Object> result = routePointService.saveRouteAndGetStatus(historyId, requestData);
        String currentStatus = (String) result.get("status");
        Long routePointId = (Long) result.get("routePointId");

        RouteDataDetailResponse broadcastPayload = RouteDataDetailResponse.toDto(
                routePointId,
                requestData.getLatitude(),
                requestData.getLongitude(),
                requestData.getMeasuredAt()
        );

        WebSocketMessage<RouteDataDetailResponse> broadcastMessage = WebSocketMessage.success(broadcastPayload);

        try {
            RedisPublishWrapper wrapper = new RedisPublishWrapper(historyId, broadcastMessage);
            String jsonWrapper = objectMapper.writeValueAsString(wrapper);

            redisTemplate.convertAndSend(topic.getTopic(), jsonWrapper);
        } catch (JsonProcessingException e) {
            log.error("Redis 메시지 발행 중 직렬화 오류: {}", e.getMessage());
            throw new RuntimeException("시스템 오류가 발생했습니다.");
        }

        RouteDataSuccessResponse ackPayload = RouteDataSuccessResponse.toDto(
                200,
                "데이터 수신 및 Redis 발행 완료",
                currentStatus,
                null
        );

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/reply",
                WebSocketMessage.success(ackPayload)
        );
    }

    /**
     * 메시지 처리 중 발생하는 예외를 핸들링하여 표준 에러 규격으로 응답합니다.
     *
     * @param e         발생한 예외
     * @param principal 인증된 사용자 정보
     */
    @MessageExceptionHandler
    public void handleException(Exception e, Principal principal) {
        log.error("웹소켓 처리 중 예외 발생: {}", e.getMessage());

        RouteDataErrorResponse errorPayload = RouteDataErrorResponse.toDto(
                500,
                e.getMessage(),
                null
        );

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/reply",
                WebSocketMessage.error(errorPayload)
        );
    }
}