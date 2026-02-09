package com.dodo.backend.common.config;

import com.dodo.backend.common.handler.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP(Simple Text Oriented Messaging Protocol) 기반의 웹소켓 메시지 브로커 설정 클래스입니다.
 * <p>
 * 실시간 반려동물 경로 공유 및 상태 모니터링을 위해 클라이언트와의 연결 엔드포인트를 정의하고,
 * 메시지 발행(Publish) 및 구독(Subscribe)을 위한 라우팅 경로를 구성합니다.
 * 또한 {@link StompHandler}를 인터셉터로 등록하여 연결 시 인증 처리를 수행합니다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;

    /**
     * 메시지 브로커의 동작을 구성하여 클라이언트와 서버 간의 메시지 흐름을 제어합니다.
     * <p>
     * <ul>
     * <li>{@code /queue}: 1:1 메시징(User-specific)을 위한 내장 브로커 경로입니다. (예: @SendToUser 응답)</li>
     * <li>{@code /topic}: 1:N 브로드캐스팅(Pub/Sub)을 위한 내장 브로커 경로입니다. (예: Redis 연동 시 사용)</li>
     * <li>{@code /app}: 클라이언트가 서버의 비즈니스 로직(@MessageMapping)을 호출할 때 사용하는 접두사입니다.</li>
     * </ul>
     *
     * @param config 메시지 브로커 설정을 담당하는 레지스트리 객체
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/sub");
        config.setApplicationDestinationPrefixes("/pub");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 웹소켓 연결을 위한 STOMP 엔드포인트를 등록합니다.
     * <p>
     * 클라이언트는 {@code ws://도메인/ws-dodo} 경로로 연결 요청(Handshake)을 수행합니다.
     * <p>
     * <ul>
     * <li>{@code setAllowedOriginPatterns("*")}: 모든 도메인에서의 CORS 요청을 허용합니다.</li>
     * </ul>
     *
     * @param registry STOMP 엔드포인트 등록 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-dodo")
                .setAllowedOriginPatterns("*");
    }

    /**
     * 클라이언트로부터 들어오는(Inbound) 메시지를 처리하는 채널에 인터셉터를 설정합니다.
     * <p>
     * {@link StompHandler}를 등록하여 메시지가 처리되기 전에 JWT 토큰 검증 및 인증 정보 주입을 수행하도록 합니다.
     *
     * @param registration 채널 등록 객체
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }
}