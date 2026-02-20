package com.dodo.backend.common.config;

import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Scalar API Reference 및 OpenAPI 명세를 담당하는 구성 클래스입니다.
 * <p>
 * 이 클래스는 API 문서의 메타데이터 설정 및 JWT Bearer 토큰 인증 체계를 정의합니다.
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI 설정을 생성하고 Bean으로 등록합니다.
     * <p>
     * 주요 설정 내용:
     * <ul>
     * <li>API 기본 정보 (제목, 설명, 버전)</li>
     * <li>Security Scheme 정의 (JWT Bearer 인증 방식)</li>
     * <li>전역 Security Requirement 적용 (모든 API에 자물쇠 아이콘 표시)</li>
     * </ul>
     *
     * @return 인증 설정이 포함된 OpenAPI 객체
     */
    @Bean
    public OpenAPI openAPI() {

        String securityJwtName = "JWT_Auth";

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityJwtName);

        Components components = new Components().addSecuritySchemes(securityJwtName,
                new SecurityScheme()
                        .name(securityJwtName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        Info info = new Info()
                .title("DoDo API Docs")
                .description("DoDo 프로젝트 백엔드 서비스 API 명세서입니다.")
                .version("v0.0.1");

        return new OpenAPI()
                .info(info)
		.addServersItem(new Server().url("https://api.dodok.p-e.kr"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}