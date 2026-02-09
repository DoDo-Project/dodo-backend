package com.dodo.backend.routepoint.socket;

import lombok.*;

/**
 * 웹소켓 통신을 위한 공통 메시지 래퍼(Wrapper) 클래스입니다.
 * <p>
 * 메시지의 타입({@link SocketType})과 실제 데이터(Payload)를 포함하여
 * 일관된 메시지 형식을 제공합니다.
 *
 * @param <T> Payload 데이터 타입
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessage<T> {

    private SocketType type;
    private T payload;

    /**
     * 성공 응답 메시지를 생성합니다.
     * <p>
     * 메시지 타입은 자동으로 {@link SocketType#SUCCESS}로 설정됩니다.
     *
     * @param payload 클라이언트에게 반환할 성공 데이터
     * @param <T>     데이터 타입
     * @return 설정된 성공 메시지 객체
     */
    public static <T> WebSocketMessage<T> success(T payload) {
        return WebSocketMessage.<T>builder()
                .type(SocketType.SUCCESS)
                .payload(payload)
                .build();
    }

    /**
     * 에러 응답 메시지를 생성합니다.
     * <p>
     * 메시지 타입은 자동으로 {@link SocketType#ERROR}로 설정됩니다.
     *
     * @param payload 클라이언트에게 반환할 에러 상세 정보
     * @param <T>     데이터 타입
     * @return 설정된 에러 메시지 객체
     */
    public static <T> WebSocketMessage<T> error(T payload) {
        return WebSocketMessage.<T>builder()
                .type(SocketType.ERROR)
                .payload(payload)
                .build();
    }
}