package com.dodo.backend.notification.service;

import com.dodo.backend.notification.entity.NotificationType;
import com.dodo.backend.user.entity.User;

import java.util.List;

record NotificationScheduleDispatch(
        List<User> targets,
        String title,
        String body,
        NotificationType type,
        Long relatedId
) {
}
