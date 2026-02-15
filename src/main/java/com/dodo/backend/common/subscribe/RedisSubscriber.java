package com.dodo.backend.common.subscribe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

/**
 * Redis Pub/Sub 채널을 통해 전파된 데이터를 수신하여 웹소켓 클라이언트에게 배포하는 서비스 클래스입니다.
 * <p>
 * 다중 서버 환경에서 특정 서버가 수신한 경로 데이터를 모든 서버가 공유받을 수 있도록
 * 브로드캐스팅(Broadcasting) 역할을 수행합니다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Redis 채널에서 메시지가 발행(Publish)되면 호출되는 콜백 메서드입니다.
     * <p>
     * 수신된 JSON 래퍼(Wrapper) 객체에서 라우팅 키(historyId 또는 petId)와 실제 메시지(내용물)를 분리합니다.
     * 라우팅 키를 사용하여 적절한 웹소켓 채널을 찾은 뒤, 껍데기를 벗긴 실제 메시지만 전송합니다.
     *
     * @param wrapperMessage Redis 채널을 통해 수신된 래퍼 JSON 문자열
     */
    public void onMessage(String wrapperMessage) {
        try {
            JsonNode rootNode = objectMapper.readTree(wrapperMessage);

            long historyId = rootNode.path("historyId").asLong();
            long petId = rootNode.path("petId").asLong();
            JsonNode realPayload = rootNode.path("message");

            if (realPayload.isMissingNode()) {
                log.warn("유효하지 않은 Redis 메시지 형식입니다: {}", wrapperMessage);
                return;
            }

            if (historyId != 0) {
                log.info("Redis 채널 수신 (historyId: {}) -> 웹소켓 전파 완료", historyId);

                messagingTemplate.convertAndSend(
                        "/sub/activities/history/routes/" + historyId,
                        realPayload.toString()
                );
                return;
            }

            if (petId != 0) {
                log.info("Redis 채널 수신 (petId: {}) -> 웹소켓 전파 완료", petId);

                messagingTemplate.convertAndSend(
                        "/sub/fence/location/" + petId,
                        realPayload.toString()
                );
                return;
            }

            log.warn("라우팅 키가 없는 Redis 메시지입니다: {}", wrapperMessage);

        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}
