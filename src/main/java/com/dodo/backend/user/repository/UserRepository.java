package com.dodo.backend.user.repository;

import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자(User) 도메인의 복잡한 조회 및 동적 쿼리를 담당하는 MyBatis Mapper 인터페이스입니다.
 * <p>
 * 기본적인 CRD(생성, 조회, 삭제)는 JPA 기반의 {@link com.dodo.backend.user.repository.UserRepository}를 우선 사용합니다.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByNickname(String nickname);

    List<User> findByUserStatusAndNotificationEnabledTrue(UserStatus userStatus);

    List<User> findByUsersIdInAndUserStatusAndNotificationEnabledTrue(Collection<UUID> usersIds, UserStatus userStatus);

}
