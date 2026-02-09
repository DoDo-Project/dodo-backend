package com.dodo.backend.common.handler;

import com.dodo.backend.common.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 웹소켓 연결 시점에 JWT 토큰을 검증하고 인증 정보를 주입하는 인터셉터 클래스입니다.
 * <p>
 * STOMP CONNECT 프레임 수신 시 Authorization 헤더의 토큰 유효성을 검사하며,
 * 유효하지 않은 토큰이나 누락된 헤더에 대해 명확한 예외 메시지를 던져 클라이언트에게 에러 원인을 전달합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class StompHandler implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 클라이언트로부터 메시지가 수신되어 채널로 전송되기 전에 실행되며 CONNECT 시점의 인증을 처리합니다.
     * <p>
     * 1. Authorization 헤더 존재 여부 확인
     * 2. Bearer 접두사 유효성 확인
     * 3. JWT 토큰 유효성 및 만료 여부 검증
     * 검증 실패 시 MessageDeliveryException을 발생시켜 클라이언트에게 ERROR 프레임을 전송합니다.
     *
     * @param message 클라이언트가 전송한 메시지 객체
     * @param channel 메시지가 전송될 채널
     * @return 처리된 메시지 객체
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

            if (authorizationHeader == null) {
                throw new MessageDeliveryException("Authorization 헤더가 누락되었습니다.");
            }

            if (!authorizationHeader.startsWith("Bearer ")) {
                throw new MessageDeliveryException("유효하지 않은 인증 형식입니다.");
            }

            String token = authorizationHeader.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                throw new MessageDeliveryException("토큰이 유효하지 않거나 만료되었습니다.");
            }

            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            accessor.setUser(authentication);

            log.info("WebSocket 인증 성공: {}", authentication.getName());
        }
        return message;
    }
}