package com.dodo.backend.user.repository;

import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 관리자 화면에 표시할 유저를 권한, 상태 및 검색어 조건으로 조회합니다.
     * 검색어는 이메일, 이름, 닉네임에 대소문자 구분 없이 부분 일치로 적용됩니다.
     *
     * @param role 조회할 유저 권한
     * @param status 조회할 계정 상태, 전체 상태 조회 시 {@code null}
     * @param keyword 이메일, 이름, 닉네임 검색어, 검색하지 않을 경우 {@code null}
     * @param pageable 페이지 및 정렬 정보
     * @return 조건에 맞는 유저 페이지
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.role = :role
              AND (:status IS NULL OR u.userStatus = :status)
              AND (:keyword IS NULL
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<User> findUsersForAdmin(@Param("role") UserRole role,
                                 @Param("status") UserStatus status,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

}
