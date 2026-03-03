package com.dodo.backend.auth.repository;

import com.dodo.backend.auth.entity.RefreshToken;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 리프레시 토큰 리포지토리(RefreshTokenRepository)의 기능 검증을 위한 테스트 클래스입니다.
 * <p>
 * {@link DataRedisTest}를 사용하여 Redis 저장 및 조회 로직을 테스트합니다.
 */
@DataRedisTest
@TestPropertySource(properties = "app.jpa.auditing.enabled=false")
@Slf4j
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * 각 테스트 케이스 실행 후 Redis에 저장된 테스트 데이터를 일괄 삭제합니다.
     * <p>
     * 테스트 간 데이터 독립성을 보장하기 위해 수행됩니다.
     */
    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        log.info("테스트 종료 후 Redis 데이터를 모두 삭제했습니다.");
    }

    /**
     * 리프레시 토큰을 저장한 후 유저 식별자(userId)를 통해 정상적으로 조회되는지 검증합니다.
     * <p>
     * 저장된 데이터의 필드 값이 원본 데이터와 일치하는지 확인합니다.
     */
    @Test
    @DisplayName("RefreshToken 저장 및 ID(userId)로 조회 테스트")
    void saveAndFindById() {
        log.info("saveAndFindById 테스트 시작");

        // given
        String userId = "test-user-uuid";
        String tokenValue = "refresh-token-value-1234";
        String role = "USER";

        RefreshToken refreshToken = RefreshToken.builder()
                .usersId(userId)
                .refreshToken(tokenValue)
                .role(role)
                .build();

        log.info("생성할 객체 정보 - userId: {}, role: {}", userId, role);

        // when
        refreshTokenRepository.save(refreshToken);
        log.info("Redis에 데이터 저장 완료");

        RefreshToken foundToken = refreshTokenRepository.findById(userId).orElse(null);

        if (foundToken != null) {
            log.info("ID로 조회 성공 - 조회된 userId: {}", foundToken.getUsersId());
        } else {
            log.error("ID로 조회 실패 - 데이터가 null입니다.");
        }

        // then
        assertThat(foundToken).isNotNull();
        assertThat(foundToken.getUsersId()).isEqualTo(userId);
        assertThat(foundToken.getRefreshToken()).isEqualTo(tokenValue);
        assertThat(foundToken.getRole()).isEqualTo(role);

        log.info("검증 통과, 테스트 종료");
    }

    /**
     * 토큰 문자열 값을 기준으로 Redis에 저장된 토큰 엔티티를 조회할 수 있는지 검증합니다.
     * <p>
     * 인덱싱된 필드({@code @Indexed})를 통한 검색 기능이 정상 작동하는지 확인합니다.
     */
    @Test
    @DisplayName("RefreshToken 값으로 조회(findByRefreshToken) 테스트")
    void findByRefreshToken() {
        log.info("findByRefreshToken 테스트 시작");

        // given
        String userId = "test-user-uuid-2";
        String tokenValue = "unique-refresh-token-value";
        String role = "ADMIN";

        RefreshToken refreshToken = RefreshToken.builder()
                .usersId(userId)
                .refreshToken(tokenValue)
                .role(role)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("테스트 데이터 저장 완료 - tokenValue: {}", tokenValue);

        // when
        RefreshToken foundToken = refreshTokenRepository.findByRefreshToken(tokenValue).orElse(null);

        if (foundToken != null) {
            log.info("Token 값으로 조회 성공 - 조회된 userId: {}", foundToken.getUsersId());
        } else {
            log.error("Token 값으로 조회 실패 - 데이터가 null입니다.");
        }

        // then
        assertThat(foundToken).isNotNull();
        assertThat(foundToken.getUsersId()).isEqualTo(userId);
        assertThat(foundToken.getRefreshToken()).isEqualTo(tokenValue);

        log.info("검증 통과, 테스트 종료");
    }
}
