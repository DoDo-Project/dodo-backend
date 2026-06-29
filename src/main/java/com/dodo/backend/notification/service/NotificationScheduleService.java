package com.dodo.backend.notification.service;

import com.dodo.backend.notification.dto.request.NotificationRequest.NotificationScheduleCreateRequest;
import com.dodo.backend.notification.dto.response.NotificationResponse.NotificationScheduleCreateResponse;

import java.util.UUID;

public interface NotificationScheduleService {

    NotificationScheduleCreateResponse createSchedule(UUID adminId, NotificationScheduleCreateRequest request);
}
