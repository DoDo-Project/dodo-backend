package com.dodo.backend.routepoint.socket;

/**
 * 웹소켓 통신 메시지의 유형(Type)을 정의하는 Enum 클래스입니다.
 * <p>
 * 클라이언트와 서버 간의 데이터 흐름을 명확히 하기 위해
 * 요청(SEND)과 응답(SUCCESS, ERROR)의 성격을 구분하는 기준으로 사용됩니다.
 */
public enum SocketType {
    SEND,
    SUCCESS,
    ERROR
}