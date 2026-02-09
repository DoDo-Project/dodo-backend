package com.dodo.backend.common.config;

import com.dodo.backend.common.subscribe.RedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 인프라와 통신 및 실시간 메시징(Pub/Sub) 설정을 담당하는 클래스입니다.
 * <p>
 * 데이터 직렬화 방식을 설정하여 리프레시 토큰 등을 관리하며,
 * 다중 서버 환경에서 웹소켓 메시지 동기화를 위한 Redis Pub/Sub 설정을 포함합니다.
 */
@Configuration
@EnableRedisRepositories
public class RedisConfig {

    /**
     * Redis 메시지 리스너 컨테이너를 설정합니다.
     * <p>
     * 특정 채널(Topic)로부터 메시지가 발행되면 이를 가로채서
     * 등록된 리스너(Subscriber)에게 전달하는 역할을 합니다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @param listenerAdapter   메시지를 처리할 어댑터
     * @param topic            구독할 채널 정보
     * @return RedisMessageListenerContainer 인스턴스
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic topic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, topic);
        return container;
    }

    /**
     * 메시지를 실제로 처리할 핸들러를 설정합니다.
     * <p>
     * Redis로부터 메시지가 수신되면 {@link RedisSubscriber}의 onMessage 메서드가 호출되도록 매핑합니다.
     *
     * @param subscriber 실제 비즈니스 로직을 수행할 구독자 서비스
     * @return MessageListenerAdapter 인스턴스
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    /**
     * 실시간 경로 공유를 위한 Redis 채널(Topic)을 설정합니다.
     *
     * @return "route-points" 채널을 관리하는 ChannelTopic 객체
     */
    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("route-points");
    }

    /**
     * Redis 데이터 조작을 위한 Template 설정을 커스텀합니다.
     * <p>
     * Key는 문자열로, Value는 JSON 형식(GenericJackson2JsonRedisSerializer)으로 직렬화합니다.
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return 설정이 완료된 RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return redisTemplate;
    }
}