package com.dodo.backend.fence.controller;

import com.dodo.backend.fence.dto.response.FenceResponse.FenceLocationCheckResponse;
import com.dodo.backend.fence.exception.FenceErrorCode;
import com.dodo.backend.fence.exception.FenceException;
import com.dodo.backend.fence.service.FenceService;
import com.dodo.backend.fence.socket.FenceSocketType;
import com.dodo.backend.fence.socket.FenceWebSocketMessage;
import com.dodo.backend.fence.socket.request.FenceWebSocketRequest.FenceLocationRequest;
import com.dodo.backend.fence.socket.response.FenceWebSocketResponse.FenceLocationErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link FenceWebSocketController}의 웹소켓 메시지 수신/응답 흐름을 검증하는 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
class FenceWebSocketControllerTest {

    @InjectMocks
    private FenceWebSocketController fenceWebSocketController;

    @Mock
    private FenceService fenceService;

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
     * 정상적인 위치 메시지 수신 시 Redis 발행과 개인 ACK 응답이 수행되는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 위치 수신 성공: Redis 발행 및 SUCCESS ACK 전송")
    void receiveFenceLocation_Success() throws Exception {
        // given
        Long petId = 2L;
        FenceLocationRequest request = FenceLocationRequest.builder()
                .latitude(new BigDecimal("37.5665"))
                .longitude(new BigDecimal("126.9780"))
                .measuredAt(LocalDateTime.now())
                .build();

        FenceWebSocketMessage<FenceLocationRequest> message = FenceWebSocketMessage.<FenceLocationRequest>builder()
                .type(FenceSocketType.SEND)
                .payload(request)
                .build();

        String devicePrincipal = UUID.nameUUIDFromBytes(("DEVICE:" + petId).getBytes(StandardCharsets.UTF_8)).toString();
        given(principal.getName()).willReturn(devicePrincipal);
        given(topic.getTopic()).willReturn("route-points");
        given(objectMapper.writeValueAsString(any()))
                .willReturn("{\"petId\":2,\"insideFence\":true}");
        given(fenceService.checkFenceLocationByPet(eq(petId), any(), any(), any()))
                .willReturn(FenceLocationCheckResponse.toDto(true, new BigDecimal("14.80"), 500));

        // when
        fenceWebSocketController.receiveFenceLocation(petId, message, principal);

        // then
        verify(fenceService).checkFenceLocationByPet(eq(petId), any(), any(), any());
        verify(redisTemplate).convertAndSend(eq("route-points"), argThat((String json) ->
                json.contains("\"petId\":2") && json.contains("\"insideFence\":true")
        ));
        verify(messagingTemplate).convertAndSendToUser(
                eq(devicePrincipal),
                eq("/queue/reply"),
                any(FenceWebSocketMessage.class)
        );
    }

    /**
     * 서비스 레이어에서 FenceException 발생 시 ERROR ACK가 전송되는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 위치 수신 실패: FenceException 발생 시 ERROR ACK 전송")
    void receiveFenceLocation_FenceException() {
        // given
        Long petId = 999L;
        FenceLocationRequest request = FenceLocationRequest.builder()
                .latitude(new BigDecimal("37.5665"))
                .longitude(new BigDecimal("126.9780"))
                .measuredAt(LocalDateTime.now())
                .build();

        FenceWebSocketMessage<FenceLocationRequest> message = FenceWebSocketMessage.<FenceLocationRequest>builder()
                .type(FenceSocketType.SEND)
                .payload(request)
                .build();

        String devicePrincipal = UUID.nameUUIDFromBytes(("DEVICE:" + petId).getBytes(StandardCharsets.UTF_8)).toString();
        given(principal.getName()).willReturn(devicePrincipal);
        given(fenceService.checkFenceLocationByPet(eq(petId), any(), any(), any()))
                .willThrow(new FenceException(FenceErrorCode.PET_NOT_FOUND));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        // when
        fenceWebSocketController.receiveFenceLocation(petId, message, principal);

        // then
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
        verify(messagingTemplate).convertAndSendToUser(
                eq(devicePrincipal),
                eq("/queue/reply"),
                payloadCaptor.capture()
        );

        Object captured = payloadCaptor.getValue();
        assertInstanceOf(FenceWebSocketMessage.class, captured);
        FenceWebSocketMessage<?> ack = (FenceWebSocketMessage<?>) captured;
        assertEquals(FenceSocketType.ERROR, ack.getType());
        assertInstanceOf(FenceLocationErrorResponse.class, ack.getPayload());
    }

    /**
     * 유효하지 않은 메시지 타입(SEND 아님) 수신 시 예외가 발생하는지 검증합니다.
     */
    @Test
    @DisplayName("울타리 위치 수신 실패: 잘못된 메시지 타입이면 IllegalArgumentException 발생")
    void receiveFenceLocation_InvalidMessageType() {
        // given
        Long petId = 1L;
        FenceLocationRequest request = FenceLocationRequest.builder()
                .latitude(new BigDecimal("37.5665"))
                .longitude(new BigDecimal("126.9780"))
                .measuredAt(LocalDateTime.now())
                .build();

        FenceWebSocketMessage<FenceLocationRequest> message = FenceWebSocketMessage.<FenceLocationRequest>builder()
                .type(FenceSocketType.SUCCESS)
                .payload(request)
                .build();

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> fenceWebSocketController.receiveFenceLocation(petId, message, principal));
    }
}
