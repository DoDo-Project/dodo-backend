package com.dodo.backend.main.service;

import com.dodo.backend.main.dto.response.MainResponse.MainPageResponse;

import java.util.UUID;

/**
 * 메인 페이지 데이터를 조회하는 서비스 인터페이스입니다.
 */
public interface MainService {

    /**
     * 로그인 사용자의 메인 페이지 정보를 조회합니다.
     *
     * @param userId 인증된 사용자 ID
     * @return 메인 페이지 응답 본문
     */
    MainPageResponse getMainPage(UUID userId);
}
