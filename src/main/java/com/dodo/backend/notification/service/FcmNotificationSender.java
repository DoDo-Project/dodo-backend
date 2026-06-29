package com.dodo.backend.notification.service;

import com.dodo.backend.fcmtoken.entity.FcmToken;
import com.dodo.backend.fcmtoken.repository.FcmTokenRepository;
import com.dodo.backend.notification.entity.NotificationType;
import com.dodo.backend.user.entity.User;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationSender {

    private final FcmTokenRepository fcmTokenRepository;
    private final ResourceLoader resourceLoader;

    @Value("${fcm.certification:}")
    private String fcmCertification;

    public void sendToUsers(List<User> users, String title, String body, NotificationType type, Long relatedId) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<UUID> userIds = users.stream()
                .map(User::getUsersId)
                .toList();
        List<FcmToken> tokens = fcmTokenRepository.findByUserUsersIdIn(userIds);
        if (tokens.isEmpty()) {
            return;
        }

        FirebaseMessaging messaging = getMessaging();
        if (messaging == null) {
            return;
        }

        tokens.forEach(token -> send(messaging, token, title, body, type, relatedId));
    }

    private void send(FirebaseMessaging messaging, FcmToken token, String title, String body, NotificationType type, Long relatedId) {
        Message message = Message.builder()
                .setToken(token.getToken())
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("notificationType", type.name())
                .putData("relatedId", String.valueOf(relatedId))
                .build();

        try {
            messaging.send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 발송 실패 - tokenId: {}, reason: {}", token.getFcmTokenId(), e.getMessage());
        }
    }

    private FirebaseMessaging getMessaging() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                initializeFirebaseApp();
            }
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.warn("FCM 초기화 실패로 푸시 발송을 건너뜁니다. reason: {}", e.getMessage());
            return null;
        }
    }

    private synchronized void initializeFirebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        if (!StringUtils.hasText(fcmCertification)) {
            throw new IllegalStateException("fcm.certification 설정이 없습니다.");
        }

        Resource resource = resourceLoader.getResource(fcmCertification);
        try (InputStream inputStream = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }
}
