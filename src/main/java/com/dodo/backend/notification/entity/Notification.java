package com.dodo.backend.notification.entity;

import com.dodo.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 알림(Notification) 정보를 관리하는 엔티티입니다.
 * <p>
 * 데이터베이스 {@code notification} 테이블과 매핑되며, 알림 수신 사용자, 제목/본문,
 * 알림 유형, 관련 리소스 ID, 읽음 여부, 생성 일시 정보를 포함합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_title", nullable = false, length = 255)
    private String notificationTitle;

    @Column(name = "notification_body", nullable = false, columnDefinition = "TEXT")
    private String notificationBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "related_id", nullable = false)
    private Long relatedId;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreatedDate
    @Column(name = "notification_created_at", nullable = false, updatable = false)
    private LocalDateTime notificationCreatedAt;

    /**
     * 알림 읽음 여부를 변경합니다.
     *
     * @param isRead 변경할 읽음 여부
     */
    public void updateReadStatus(Boolean isRead) {
        this.isRead = isRead;
    }
}
