package com.dodo.backend.notification.service;

import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationScheduleCreateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleCreateResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleListResponse;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationSimpleResponse;
import com.dodo.backend.notification.entity.NotificationScheduleStatus;

import java.util.UUID;

/**
 * 알림 스케줄 비즈니스 로직을 정의하는 서비스 인터페이스입니다.
 */
public interface NotificationScheduleService {

    /**
     * 알림 스케줄을 생성합니다.
     *
     * @param adminId 요청 관리자 ID
     * @param request 알림 스케줄 생성 요청
     * @return 알림 스케줄 생성 결과
     */
    NotificationScheduleCreateResponse createSchedule(UUID adminId, NotificationScheduleCreateRequest request);

    /**
     * 알림 스케줄 목록을 조회합니다.
     *
     * @param adminId 요청 관리자 ID
     * @param page 조회할 페이지 번호
     * @param size 페이지당 알림 스케줄 수
     * @param status 알림 스케줄 상태 필터
     * @return 알림 스케줄 목록 조회 결과
     */
    NotificationScheduleListResponse getSchedules(UUID adminId, int page, int size, NotificationScheduleStatus status);

    /**
     * 알림 스케줄을 취소합니다.
     *
     * @param adminId 요청 관리자 ID
     * @param scheduleId 취소할 알림 스케줄 ID
     * @return 알림 스케줄 취소 성공 메시지
     */
    NotificationSimpleResponse cancelSchedule(UUID adminId, Long scheduleId);
}
