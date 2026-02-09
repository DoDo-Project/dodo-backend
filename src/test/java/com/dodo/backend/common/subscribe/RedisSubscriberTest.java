package com.dodo.backend.common.subscribe;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link RedisSubscriber}의 단위 테스트 클래스입니다.
 * <p>
 * Redis로부터 수신한 래퍼(Wrapper) 메시지를 파싱하여 적절한 웹소켓 채널로 브로드캐스팅하는지 검증합니다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class RedisSubscriberTest {

    @InjectMocks
    private RedisSubscriber redisSubscriber;

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    /**
     * 정상적인 Wrapper 메시지를 수신했을 때, historyId를 추출하여 해당 채널로 내부 메시지만 전송하는지 테스트합니다.
     */
    @Test
    @DisplayName("Wrapper 메시지를 수신하면 껍데기를 벗기고 내부 메시지만 전송")
    void onMessage_Success() {
        log.info("테스트 시작: onMessage_Success");

        // given
        String wrapperJson = "{" +
                "\"historyId\": 4," +
                "\"message\": {" +
                "\"type\": \"SUCCESS\"," +
                "\"payload\": {" +
                "\"data\": { \"latitude\": 37.5665 }" +
                "}" +
                "}" +
                "}";
        log.info("수신된 Redis 메시지: {}", wrapperJson);

        // when
        log.info("Subscriber onMessage 실행");
        redisSubscriber.onMessage(wrapperJson);

        // then
        log.info("검증 시작: 웹소켓 전송 확인");

        verify(messagingTemplate).convertAndSend(
                eq("/sub/activities/history/routes/4"),
                argThat((String json) -> json.contains("\"latitude\":37.5665"))
        );

        log.info("테스트 종료: 성공");
    }

    /**
     * historyId가 누락된 잘못된 메시지를 수신했을 때, 웹소켓 전송이 수행되지 않는지 테스트합니다.
     */
    @Test
    @DisplayName("historyId가 없는 잘못된 메시지는 무시")
    void onMessage_Invalid_NoHistoryId() {
        log.info("테스트 시작: onMessage_Invalid_NoHistoryId");

        // given
        String invalidJson = "{\"message\": { \"type\": \"TEST\" }}";
        log.info("잘못된 형식의 메시지 준비: {}", invalidJson);

        // when
        redisSubscriber.onMessage(invalidJson);

        // then
        log.info("검증 시작: 전송 메서드가 호출되지 않았는지 확인");
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());

        log.info("테스트 종료: 성공 (무시됨 확인)");
    }
}