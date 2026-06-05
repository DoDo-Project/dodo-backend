package com.dodo.backend.user.mapper;

import com.dodo.backend.user.dto.request.UserRequest;
import com.dodo.backend.user.dto.request.UserRequest.UserUpdateRequest;
import com.dodo.backend.user.entity.User;
import com.dodo.backend.user.entity.UserRole;
import com.dodo.backend.user.entity.UserStatus;
import com.dodo.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MyBatis 매퍼인 {@link UserMapper}의 SQL 실행 결과를 검증하는 통합 테스트 클래스입니다.
 * <p>
 * MyBatis와 JPA가 동일한 데이터베이스 세션을 공유하므로 영속성 컨텍스트 관리를 통한 정합성 검증을 수행합니다.
 */
@SpringBootTest
@Transactional
@Slf4j
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    private User testUser;

    /**
     * 각 테스트 실행 전 가입 대기 유저를 생성하고 물리적 데이터 반영 후 1차 캐시를 비웁니다.
     */
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("mapper_test@example.com")
                .name("매퍼테스터")
                .nickname("초기닉네임")
                .region("서울")
                .hasFamily(true)
                .userStatus(UserStatus.REGISTER)
                .role(UserRole.USER)
                .notificationEnabled(true)
                .profileUrl("https://i.pravatar.cc/150")
                .userCreatedAt(LocalDateTime.now())
                .build();
        userRepository.save(testUser);
        em.flush();
        em.clear();
        log.info("기초 데이터 저장 및 영속성 컨텍스트 초기화 수행 완료");
    }

    /**
     * 회원가입 추가 정보를 반영하는 SQL 쿼리가 정상 작동하는지 확인합니다.
     */
    @Test
    @DisplayName("매퍼를 통한 회원 정보 업데이트 및 상태 변경")
    void updateUserRegistrationInfoTest() {
        // given
        User updateParam = User.builder()
                .email(testUser.getEmail())
                .nickname("가입완료닉네임")
                .region("경기")
                .hasFamily(false)
                .profileUrl("https://example.com/images/profile-updated.jpg")
                .build();
        log.info("회원가입 정보 업데이트 매퍼 쿼리 실행");

        // when
        userMapper.updateUserRegistrationInfo(updateParam);

        // then
        em.clear();
        User updatedUser = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        assertThat(updatedUser.getNickname()).isEqualTo("가입완료닉네임");
        assertThat(updatedUser.getProfileUrl()).isEqualTo("https://example.com/images/profile-updated.jpg");
        assertThat(updatedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        log.info("회원가입 정보 업데이트 SQL 실행 결과 검증 성공");
    }

    /**
     * 유저 식별자를 이용해 상태값만 변경하는 SQL 쿼리를 검증합니다.
     */
    @Test
    @DisplayName("매퍼를 통한 유저 상태값 명시적 변경")
    void updateUserStatusTest() {
        // given
        UUID userId = testUser.getUsersId();
        String newStatus = UserStatus.DELETED.name();
        log.info("계정 상태 변경 매퍼 테스트 시작 - 변경될 상태: {}", newStatus);

        // when
        userMapper.updateUserStatus(userId, newStatus);

        // then
        em.clear();
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getUserStatus().name()).isEqualTo(newStatus);
        log.info("계정 상태 변경 SQL 실행 결과 검증 완료");
    }

    /**
     * 동적 SQL을 통해 제공된 필드만 수정되는지 검증합니다.
     */
    @Test
    @DisplayName("매퍼를 통한 프로필 필드 선택적 수정")
    void updateUserProfileInfoTest() {
        // given
        UUID userId = testUser.getUsersId();

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .nickname("수정닉네임")
                .region(null)
                .build();

        log.info("프로필 선택적 수정 매퍼 테스트 시작 - 대상 ID: {}, 변경 닉네임: {}", userId, updateRequest.getNickname());

        // when
        userMapper.updateUserProfileInfo(updateRequest, userId);

        // then
        em.clear();
        User result = userRepository.findById(userId).orElseThrow();

        assertThat(result.getNickname()).isEqualTo("수정닉네임");
        assertThat(result.getRegion()).isEqualTo("서울");

        log.info("동적 쿼리 필드 선택적 수정 검증 성공");
    }
}
