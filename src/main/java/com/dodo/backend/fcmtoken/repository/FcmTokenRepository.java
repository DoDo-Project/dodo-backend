package com.dodo.backend.fcmtoken.repository;

import com.dodo.backend.fcmtoken.entity.FcmToken;
import com.dodo.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FCM 토큰 엔티티의 영속성 처리를 담당하는 Repository입니다.
 */
@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    Optional<FcmToken> findByTokenAndUser(String token, User user);

    List<FcmToken> findByUserUsersIdIn(Collection<UUID> userIds);
}
