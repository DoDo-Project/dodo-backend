package com.dodo.backend.fence.socket;

import lombok.*;

/**
 * 울타리 웹소켓 통신 공통 메시지 래퍼 클래스입니다.
 *
 * @param <T> 페이로드 타입
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FenceWebSocketMessage<T> {

    private FenceSocketType type;
    private T payload;

    /**
     * 성공 메시지를 생성합니다.
     *
     * @param payload 응답 페이로드
     * @param <T>     페이로드 타입
     * @return 성공 래퍼 메시지
     */
    public static <T> FenceWebSocketMessage<T> success(T payload) {
        return FenceWebSocketMessage.<T>builder()
                .type(FenceSocketType.SUCCESS)
                .payload(payload)
                .build();
    }

    /**
     * 실패 메시지를 생성합니다.
     *
     * @param payload 응답 페이로드
     * @param <T>     페이로드 타입
     * @return 실패 래퍼 메시지
     */
    public static <T> FenceWebSocketMessage<T> error(T payload) {
        return FenceWebSocketMessage.<T>builder()
                .type(FenceSocketType.ERROR)
                .payload(payload)
                .build();
    }
}
