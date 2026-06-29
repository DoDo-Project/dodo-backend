package com.dodo.backend.fcmtoken.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자별 FCM 푸시 토큰 정보를 관리하는 엔티티입니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "fcm_token",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fcm_token_token", columnNames = "token")
        }
)
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_id")
    private Long fcmTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    /**
     * FCM 토큰의 소유자와 장치 정보를 갱신합니다.
     *
     * @param user 토큰을 등록한 사용자
     * @param deviceType 장치 유형
     * @param deviceName 장치 이름
     */
    public void updateTokenInfo(User user, DeviceType deviceType, String deviceName) {
        this.user = user;
        this.deviceType = deviceType;
        this.deviceName = deviceName;
    }
}
