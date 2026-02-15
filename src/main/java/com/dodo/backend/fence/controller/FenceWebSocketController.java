package com.dodo.backend.fence.controller;

import com.dodo.backend.fence.dto.response.FenceResponse.FenceLocationCheckResponse;
import com.dodo.backend.fence.exception.FenceException;
import com.dodo.backend.fence.service.FenceService;
import com.dodo.backend.fence.socket.FenceSocketType;
import com.dodo.backend.fence.socket.FenceWebSocketMessage;
import com.dodo.backend.fence.socket.request.FenceWebSocketRequest.FenceLocationRequest;
import com.dodo.backend.fence.socket.response.FenceWebSocketResponse.FenceLocationBroadcastResponse;
import com.dodo.backend.fence.socket.response.FenceWebSocketResponse.FenceLocationErrorResponse;
import com.dodo.backend.fence.socket.response.FenceWebSocketResponse.FenceLocationSuccessResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 울타리 실시간 위치 웹소켓 메시지를 처리하는 컨트롤러입니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class FenceWebSocketController {

    private final FenceService fenceService;
    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic topic;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Redis Pub/Sub 전송을 위해 라우팅 정보(petId)와 실제 메시지를 포장하는 내부 래퍼 클래스입니다.
     */
    @Getter
    @AllArgsConstructor
    private static class RedisPublishWrapper {
        private Long petId;
        private Object message;
    }

    /**
     * 임베디드에서 전송한 실시간 위치를 수신하고 울타리 내부 여부를 판정합니다.
     * <p>
     * 처리 결과는 브로드캐스트 채널(`/sub/fence/location/{petId}`)과
     * 개인 응답 채널(`/user/queue/reply`)로 각각 전송됩니다.
     *
     * @param petId     경로 변수 반려동물 ID
     * @param message   수신 메시지
     * @param principal 인증된 사용자 정보
     */
    @MessageMapping("/fence/location/{petId}")
    public void receiveFenceLocation(
            @DestinationVariable Long petId,
            FenceWebSocketMessage<FenceLocationRequest> message,
            Principal principal
    ) {
        FenceLocationRequest request = validateAndExtract(message);

        try {
            FenceLocationCheckResponse result = fenceService.checkFenceLocationByPet(
                    petId,
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getMeasuredAt()
            );

            FenceLocationBroadcastResponse broadcast = FenceLocationBroadcastResponse.builder()
                    .petId(petId)
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .measuredAt(request.getMeasuredAt())
                    .insideFence(result.getInsideFence())
                    .distanceMeter(result.getDistanceMeter())
                    .radius(result.getRadius())
                    .message(result.getInsideFence()
                            ? "설정 반경 " + result.getRadius() + "m 이내입니다."
                            : "설정 반경 " + result.getRadius() + "m 이탈입니다.")
                    .build();

            try {
                RedisPublishWrapper wrapper = new RedisPublishWrapper(
                        petId,
                        FenceWebSocketMessage.success(broadcast)
                );
                String jsonWrapper = objectMapper.writeValueAsString(wrapper);
                redisTemplate.convertAndSend(topic.getTopic(), jsonWrapper);
            } catch (JsonProcessingException e) {
                log.error("Fence Redis 메시지 발행 중 직렬화 오류: {}", e.getMessage());
                throw new RuntimeException("서버 내부 오류가 발생했습니다.");
            }

            FenceLocationSuccessResponse ack = FenceLocationSuccessResponse.builder()
                    .code(200)
                    .message("위치 데이터 수신 및 처리 완료")
                    .build();

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/reply",
                    FenceWebSocketMessage.success(ack)
            );
        } catch (FenceException e) {
            sendError(principal, e.getErrorCode().getHttpStatus().value(), e.getErrorCode().getMessage(), request);
        } catch (IllegalArgumentException e) {
            sendError(principal, 400, "잘못된 요청입니다.", request);
        } catch (Exception e) {
            log.error("울타리 위치 처리 중 서버 오류 발생: {}", e.getMessage(), e);
            sendError(principal, 500, "서버 내부 오류가 발생했습니다.", request);
        }
    }

    /**
     * 개인 응답 채널(`/user/queue/reply`)로 표준 에러 메시지를 전송합니다.
     *
     * @param principal       사용자 인증 정보
     * @param code            에러 코드
     * @param message         에러 메시지
     * @param originalMessage 원본 요청 메시지
     */
    private void sendError(Principal principal, int code, String message, Object originalMessage) {
        FenceLocationErrorResponse errorResponse = FenceLocationErrorResponse.builder()
                .code(code)
                .message(message)
                .originalMessage(originalMessage)
                .build();

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/reply",
                FenceWebSocketMessage.error(errorResponse)
        );
    }

    /**
     * 수신 메시지의 타입과 필수 필드를 검증합니다.
     *
     * @param message 수신 메시지
     * @return 유효성 검증을 통과한 요청 DTO
     */
    private FenceLocationRequest validateAndExtract(FenceWebSocketMessage<FenceLocationRequest> message) {
        if (message == null || message.getType() != FenceSocketType.SEND || message.getPayload() == null) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        FenceLocationRequest payload = message.getPayload();

        if (payload.getLatitude() == null || payload.getLongitude() == null || payload.getMeasuredAt() == null) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        return payload;
    }
}
