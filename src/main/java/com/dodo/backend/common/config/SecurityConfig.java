package com.dodo.backend.common.config;

import com.dodo.backend.common.jwt.JwtAuthenticationFilter;
import com.dodo.backend.common.jwt.JwtTokenProvider;
import com.dodo.backend.user.exception.UserErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 애플리케이션의 보안 관련 설정을 담당하는 구성 클래스입니다.
 * <p>
 * Spring Security를 활성화하여 웹 요청에 대한 인증 및 인가 규칙을 정의합니다.
 * JWT 기반 인증을 위한 필터를 등록하고, 세션 정책을 Stateless로 설정합니다.
 * 또한 인증/인가 실패 시 발생하는 예외를 커스텀 에러 응답 포맷으로 변환하여 클라이언트에게 반환합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * HTTP 요청에 대한 보안 필터 체인(Security Filter Chain)을 구성합니다.
     * <p>
     * 1. CSRF 보호 비활성화 (JWT 사용)<br>
     * 2. 세션 관리 정책을 STATELESS로 설정<br>
     * 3. API 엔드포인트별 접근 권한 설정 (로그인/문서 등은 허용, 그 외는 인증 필요)<br>
     * 4. 커스텀 예외 핸들링(AuthenticationEntryPoint, AccessDeniedHandler) 등록<br>
     * 5. UsernamePasswordAuthenticationFilter 앞단에 JwtAuthenticationFilter 추가
     *
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain 인스턴스
     * @throws Exception 보안 구성 중 오류 발생 시
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
			.requestMatchers(
                                "/v3/api-docs/**",
                                "/scalar/**",
                                "/auth/social-login",
                                "/view/login",
                                "/google-login",
                                "/naver-login",
                                "/view/socket",
                                "/auth/reissue",
                                "/auth/devices",
                                "/auth/reissue/devices",
                                "/users/nickname/check",
                                "/ws-dodo"
                        ).permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).denyAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 설정을 정의합니다.
     * <p>
     *
     * @return CorsConfigurationSource 인스턴스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
		"https://api.dodok.p-e.kr",
                "https://dodo-frontend-three.vercel.app",
                "http://localhost:8080"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 인증되지 않은 사용자(비로그인, 유효하지 않은 토큰 등)가 보호된 리소스에 접근했을 때 처리할 핸들러를 정의합니다.
     * <p>
     * 401 Unauthorized 상태 코드와 함께 {@link UserErrorCode#LOGIN_REQUIRED} 에러 정보를 JSON으로 반환합니다.
     *
     * @return AuthenticationEntryPoint 인스턴스
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                sendErrorResponse(response, UserErrorCode.LOGIN_REQUIRED);
    }

    /**
     * 인증은 되었으나 해당 리소스에 대한 접근 권한이 없는 사용자가 접근했을 때 처리할 핸들러를 정의합니다.
     * <p>
     * 403 Forbidden 상태 코드와 함께 {@link UserErrorCode#ACCESS_DENIED} 에러 정보를 JSON으로 반환합니다.
     *
     * @return AccessDeniedHandler 인스턴스
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                sendErrorResponse(response, UserErrorCode.ACCESS_DENIED);
    }

    /**
     * 필터 레벨에서 발생한 예외에 대해 공통된 에러 응답 포맷(JSON)을 작성하여 클라이언트로 전송합니다.
     *
     * @param response   HTTP 응답 객체
     * @param errorCode  클라이언트에게 전달할 에러 코드 정보 (UserErrorCode)
     * @throws IOException 입출력 처리 중 오류 발생 시
     */
    private void sendErrorResponse(HttpServletResponse response, UserErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(errorCode.getHttpStatus().value());

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("status", errorCode.getHttpStatus().value());
        errorBody.put("message", errorCode.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
