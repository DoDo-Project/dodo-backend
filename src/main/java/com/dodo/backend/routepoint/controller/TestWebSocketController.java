package com.dodo.backend.routepoint.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 웹소켓 시뮬레이션 테스트 페이지를 반환하는 뷰 컨트롤러입니다.
 * <p>
 * 개발 및 테스트 단계에서 프론트엔드 없이 백엔드 로직을 검증하기 위해 사용됩니다.
 */
@Slf4j
@Controller
public class TestWebSocketController {

    /**
     * 소켓 테스트 페이지 진입
     * <p>
     * 접속 URL: http://localhost:8080/view/socket
     * 뷰 경로: resources/templates/websocket/socket-test.html
     */
    @GetMapping("/view/socket")
    public String socketTestPage() {
        log.info("웹소켓 시뮬레이션 페이지가 요청되었습니다.");
        return "websocket/socket-test";
    }
}